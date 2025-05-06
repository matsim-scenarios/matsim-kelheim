library(tidyverse)
library(lubridate)
library(plotly)
library(leaflet)
library(rmarkdown)
library(modelr)
library(splines)
library(forecast)
library(fitdistrplus)
library(jsonlite)
library(httr)
library(ggpubr)

# colors for model plots
colors <- c("predicted" = "red", "Mon" = "darkblue", "Tue" = "deepskyblue4", "Wed" = "deepskyblue2", "Thu" = "cadetblue4", "Fri" = "chartreuse4")
colors2 <- c("Identity line" = "black", "Mon" = "darkblue", "Tue" = "deepskyblue4", "Wed" = "deepskyblue2", "Thu" = "cadetblue4", "Fri" = "chartreuse4")

############################################## read data inputs ###############################################################################################################################

# Ingolstadt weather
ingolstadt_weather <- read_delim("https://bulk.meteostat.net/v2/daily/10860.csv.gz",",",col_names = FALSE)
colnames(ingolstadt_weather) <- c("date", "tavg", "tmin", "tmax", "prcp", "snow", "wdir", "wspd", "wpgt", "pres", "tsun")

# Weatherstack data
weatherstack_kelheim <- read_delim("../../shared-svn/projects/KelRide/data/badWeather/data/Kelheim_weather_since_july_2008.csv",delim = ",")

# Stringency
json <- fromJSON(txt = "../../shared-svn/projects/KelRide/data/badWeather/data/2022-12-31.json")
json <- unlist(json)

#Mobility
demand <- read_delim("../../shared-svn/projects/KelRide/data/badWeather/data/allDemandByDate.csv")
requests <- read_delim("../../shared-svn/projects/KelRide/data/badWeather/data/allRequestsByDate.csv")
rejections <- read_delim("../../shared-svn/projects/KelRide/data/badWeather/data/rejectionsByDate.csv")

df_requests_rejections <- requests %>% 
  left_join(rejections, by="date") %>% 
  replace_na(list(noRejections = 0)) %>% 
  mutate(rejectionShare = round(noRejections / noRequests, 2))

#Holidays
holidays2020 <- read_csv2("../../shared-svn/projects/KelRide/data/badWeather/data/Holidays2020.csv") %>% dplyr::select(1,2,3)
holidays2021 <- read_csv2("../../shared-svn/projects/KelRide/data/badWeather/data/Holidays2021.csv") %>% dplyr::select(1,2,3)
holidays2022 <- read_csv2("../../shared-svn/projects/KelRide/data/badWeather/data/Holidays2022.csv") %>% dplyr::select(1,2,3)
holidays <- rbind(holidays2020,holidays2021,holidays2022)
holidays <- holidays %>% mutate(EndDateTime1 = as.Date(as.POSIXct(EndDateTime1, format = "%m.%d.%Y %H:%M")),
                               StartDateTime1 = as.Date(as.POSIXct(StartDateTime1, format = "%m.%d.%Y %H:%M")))


# holidays are saved in format: startDate day before holiday, endDate day after holiday..
df_holidays <- holidays %>% 
  mutate(date = StartDateTime1 + 1,
         dateComp = EndDateTime1 - 1)

df_holidays <- df_holidays %>% 
  filter(date == dateComp) %>% 
  dplyr::select(date) %>% 
  mutate(isHoliday = TRUE)

# get school holidays data from openholidaysapi (feierte-api only has information about official holidays)
response <- GET("https://openholidaysapi.org/SchoolHolidays?countryIsoCode=DE&subdivisionCode=DE-BY&languageIsoCode=DE&validFrom=2020-01-01&validTo=2022-12-31")
schoolHolidays <- fromJSON(content(response, "text")) %>%
  mutate(startDate = as.Date(as.POSIXct(startDate, format = "%Y-%m-%d")),
         endDate = as.Date(as.POSIXct(endDate, format = "%Y-%m-%d")))

df_schoolHolidays <- schoolHolidays %>%
  rowwise() %>%
  mutate(date = list(seq(startDate, endDate, by = "day"))) %>%
  unnest(cols = date) %>% 
  dplyr::select(date) %>% 
  mutate(isSchoolHoliday = TRUE)

# Weatherstack
weatherstack_kelheim_daily <- weatherstack_kelheim %>%
  group_by(date) %>%
  count(description)

# Stringency 
deu_stringency <- json[grep("DEU.stringency_actual",names(json))]
date_stringency <- sapply(strsplit(names(deu_stringency),split = ".",fixed = TRUE),"[[",2)
df_stringency <- data.frame(date = date_stringency,stringency = deu_stringency)
df_stringency <- df_stringency %>% mutate(stringency = as.numeric(stringency), date = as.Date(date))

stringency2022 <- df_stringency %>% filter(date > as.Date("2021-12-31"))
meanStringency2022 <- mean(stringency2022$stringency)

# dates of missing covid data since 2023.
stringency2023 <- data.frame(date = as.Date(c(ymd("2023-01-01"):ymd("2023-12-31")), origin = "1970-01-01")) %>% 
  mutate(stringency = 11.11)

df_stringency <- rbind(df_stringency,stringency2023)

############################################## adapt and join data ###############################################################################################################################

# Ingolstadt
type_of_weather <- unique(weatherstack_kelheim$description)
map_vector <- c("Clear","Sunny","Cloudy","Light","Light","Light","Light","Light","Light","Light","Light","Medium","Cloudy","Light","Light","Heavy","Heavy","Heavy","Light","Medium","Heavy","Heavy",
                "Light","Heavy","Heavy","Heavy","Heavy","Heavy","Heavy","Light","Medium","Medium","Light","Heavy","Light","Light","Light","Light","Light","Heavy","Light","Medium","Heavy","Heavy","Heavy")
names(map_vector)<- type_of_weather

ingolstadt_weather <- ingolstadt_weather %>% 
  mutate(season = ifelse(month(date) %in% c(12,1,2),"winter",NA)) %>%
  mutate(season = ifelse(month(date) %in% c(3,4,5),"spring",season)) %>%
  mutate(season = ifelse(month(date) %in% c(6,7,8),"summer",season)) %>%
  mutate(season = ifelse(month(date) %in% c(9,10,11),"autumn",season))

day_description_impact <- weatherstack_kelheim_daily %>% pivot_wider(names_from = description,values_from = n)

#remove NAs
day_description_impact[is.na(day_description_impact)] = 0

day_description_impact <- day_description_impact %>% pivot_longer(cols = all_of(type_of_weather),names_to = "description",values_to = "value")

day_description_impact <- day_description_impact
day_description_impact$description <- map_vector[(day_description_impact$description)]

day_description_impact <- day_description_impact %>% group_by(date)%>%
  top_n(n = 1,value) %>% group_by(date) %>% top_n(n = 1,description) %>% rename(weather_impact = value)

#####Join the data#####
result_data <- demand %>% 
  left_join(day_description_impact, by = "date") %>% 
  inner_join(ingolstadt_weather,by = "date") %>% 
  inner_join(df_stringency,by = "date") %>%
  mutate(date = as.Date(date,format = "%Y-%m-%d"))

#Also need to be added: weekday and simplified date variable
result_data <- result_data %>% 
  mutate(wday = as.character(wday(date,week_start = 1))) %>%
  dplyr::arrange(result_data, result_data$date) %>%
  distinct() %>%
  mutate(trend = as.integer(date) - as.integer(min(result_data$date)))

# join requests to also calc regression for requests and weather data
result_data <- result_data %>% 
  left_join(requests, by="date")

result_data_incl_2023 <- result_data %>% 
  left_join(df_holidays, by = "date") %>% 
  left_join(df_schoolHolidays, by = "date") %>%
  replace_na(list(isHoliday = FALSE,snow = 0, isSchoolHoliday = FALSE)) %>%
  #%>% filter(noRides != 0)
  filter(date <= as.Date("2023-12-31"))

#Append holidays
result_data <- result_data %>% 
  left_join(df_holidays, by = "date") %>% 
  left_join(df_schoolHolidays, by = "date") %>%
  replace_na(list(isHoliday = FALSE,snow = 0, isSchoolHoliday = FALSE)) %>%
#%>% filter(noRides != 0)
filter(date <= as.Date("2022-12-31"))

sundays <- result_data %>% 
  filter(wday == 7)

head(result_data)

summer <- mean(result_data$tavg[result_data$season == "summer"])

spring <- mean(result_data$tavg[result_data$season == "spring"])

autumn <- mean(result_data$tavg[result_data$season == "autumn"])

winter <- mean(result_data$tavg[result_data$season == "winter"])

result_data <- result_data %>% 
  mutate(tdiff = ifelse(season == "winter",tavg-winter,NA)) %>%
  mutate(tdiff = ifelse(season == "spring",tavg-spring,tdiff)) %>%
  mutate(tdiff = ifelse(season == "autumn",tavg-autumn,tdiff)) %>%
  mutate(tdiff = ifelse(season == "summer",tavg-summer,tdiff)) %>% 
  mutate(wday_char = wday(date,
       label  = TRUE,
       abbr = TRUE,
       locale = "USA"))

############################################## exploratory plots ###############################################################################################################################
year_breaks <- unique(format(result_data_incl_2023$date, "%Y"))
year_breaks <- as.Date(paste(year_breaks, "-01-01", sep = ""))  # Convert to Date objects for proper placement

requests_time <- ggplot() +
  geom_point(data = requests %>% mutate(wday = as.character(wday(date,week_start = 1))) %>% filter(date <= as.Date("2022-12-31")) %>% filter(wday!=1 & wday!=5 & wday!=6 & wday!=7), mapping = aes(x = date, y = noRequests), color = "black") +
  #geom_point(data = rejections %>% mutate(wday = as.character(wday(date,week_start = 1))) %>% filter(date <= as.Date("2022-12-31")) %>% filter(wday!=1 & wday!=5 & wday!=6 & wday!=7), mapping = aes(x = date, y = noRejections), color = "purple2") +
  geom_vline(xintercept = as.numeric(year_breaks), color = "red", linetype = "dashed", size = 1) +
  geom_text(data = data.frame(x = year_breaks, y = rep(min(result_data$noRides), length(year_breaks)), year = substr(year_breaks, 3, 4)),
            aes(x = x, y = y, label = year), color = "red", size = 5, vjust = -1) +
  theme_light() +
  xlab("Date") +
  scale_x_date(date_breaks = "1 month", date_labels = "%b") +
  ggtitle("noRequests over time")

requests_rejections_time <- ggplot() +
  # Add requests points with legend label "Requests"
  geom_point(data = requests %>% 
               mutate(wday = as.character(wday(date, week_start = 1)), label = "Requests") %>% 
               filter(date <= as.Date("2022-12-31")) %>% 
               filter(wday != 1 & wday != 5 & wday != 6 & wday != 7),
             mapping = aes(x = date, y = noRequests, color = label)) +
  # Add rejections points with legend label "Rejections"
  geom_point(data = rejections %>% 
               mutate(wday = as.character(wday(date, week_start = 1)), label = "Rejections") %>% 
               filter(date <= as.Date("2022-12-31")) %>% 
               filter(wday != 1 & wday != 5 & wday != 6 & wday != 7),
             mapping = aes(x = date, y = noRejections, color = label)) +
  geom_vline(xintercept = as.numeric(year_breaks), color = "red", linetype = "dashed", size = 1) +
  geom_text(data = data.frame(x = year_breaks, y = rep(min(result_data$noRides), length(year_breaks)), 
                              year = substr(year_breaks, 3, 4)),
            aes(x = x, y = y, label = year), color = "red", size = 5, vjust = -1) +
  theme_light() +
  xlab("Date") +
  scale_x_date(date_breaks = "1 month", date_labels = "%b") +
  scale_color_manual(values = c("Requests" = "black", "Rejections" = "blue2")) + # Define colors for legend
  labs(color = "Legend") + # Add legend title
  ggtitle("noRequests / noRejections over time")

requests_rejections_time

rejections_time <- ggplot() +
  geom_point(data = rejections %>% mutate(wday = as.character(wday(date,week_start = 1))) %>% filter(date <= as.Date("2022-12-31")) %>% filter(wday!=1 & wday!=5 & wday!=6 & wday!=7), mapping = aes(x = date, y = noRejections)) +
  geom_vline(xintercept = as.numeric(year_breaks), color = "red", linetype = "dashed", size = 1) +
  geom_text(data = data.frame(x = year_breaks, y = rep(min(result_data$noRides), length(year_breaks)), year = substr(year_breaks, 3, 4)),
            aes(x = x, y = y, label = year), color = "red", size = 5, vjust = -1) +
  theme_light() +
  xlab("Date") +
  theme(
    legend.position = "bottom", legend.title = element_blank(),
    axis.ticks.x = element_line(),
    axis.ticks.y = element_line(),
    axis.ticks.length = unit(5, "pt"),
    axis.text.x = element_text(angle = 90, hjust = 1),
    text = element_text(size = 12)
  ) +
  scale_x_date(date_breaks = "1 month", date_labels = "%b") +
  ggtitle("noRejections over time")
rejections_time



plot_data <- result_data
  
plot_data$isHoliday[plot_data$isHoliday==TRUE] <- "Holiday"
plot_data$isHoliday[plot_data$isHoliday==FALSE] <- "Non-holiday"

wday_plot <- ggplot(plot_data %>% mutate(wday_char = factor(wday_char, levels = c("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"))), aes(x=wday_char,y=noRides))+
  geom_boxplot(aes(color=wday_char), lwd=1.5) +
  xlab("Weekday") + 
  ylab("nRides") +
  # labs(title="Daily no of KEXI rides per weekday") +
  theme_minimal() +
  theme(plot.title = element_text(hjust=0.5), legend.title = element_blank(), legend.position = "none") +
  theme(text = element_text(size = 50)) +
  theme(axis.ticks.x = element_line(size = 1), 
                   axis.ticks.y = element_line(size = 1),
                   axis.ticks.length = unit(15, "pt")) +
  scale_color_manual(values = c("darkblue", "deepskyblue4", "deepskyblue2", "cadetblue", "chartreuse4","darkgoldenrod2","darkorchid4"))

ggsave("daily-kexi-rides-per-weekday.png", wday_plot, dpi = 500, w = 12, h = 9) 
ggsave("daily-kexi-rides-per-weekday.pdf", wday_plot, dpi = 500, w = 12, h = 9) 

holiday_plot <- ggplot(plot_data) +
  geom_boxplot(aes(x = isHoliday, y = noRides)) +
    xlab(NULL) +
    ylab("nRides") +
    labs(title="Daily no of KEXI rides per holiday / non-holiday") +
  theme(plot.title = element_text(hjust=0.5))

ggplotly(wday_plot)
ggplotly(holiday_plot)

############################################## filter data for different time periods ###############################################################################################################################
result_data_incl_holidays <- result_data %>% 
  filter(wday!=1 & wday!=5 & wday!=6 & wday!=7,
         noRides!=0)

result_data <- result_data %>% 
  filter(wday!=1 & wday!=5 & wday!=6 & wday!=7,
         isHoliday == FALSE,
         noRides!=0) %>% 
  filter(!(date %within% interval(ymd("2021-05-18"), ymd("2021-06-30"))))

# new after discussion on 31.10.24
before_sep_21 <- result_data %>%
  filter(date < ymd("2021-09-18"))

ioki_data <- result_data %>%
  filter(date <= ymd("2021-04-30"))

requests_rejections_ioki <- df_requests_rejections %>% 
  filter(date <= ymd("2021-04-30"))

via_data <- result_data %>%
  filter(date > ymd("2021-04-30"))

requests_rejections_via <- df_requests_rejections %>% 
  filter(date > ymd("2021-04-30") & date <= ymd("2022-12-31"))

result_data_2023 <- result_data_incl_2023 %>% 
  filter(date >= ymd("2023-01-01") & date < ymd("2024-01-01"))

# result_data <- result_data %>% filter(wday!=6 & wday!=7,isHoliday == FALSE, noRides!=0) #%>%
# new after discussion on 31.10.24
  # filter(date %within% interval(ymd("2021-09-18"), ymd("2022-12-18")))


############################################## more exploratory plots #########################################################################################################################################
noRides_time <- ggplot(result_data) +
  geom_point(mapping=aes(x = date,y = noRides), size=4)+
  annotate("rect",
           xmin = as.Date("2021-05-01"), xmax = as.Date("2021-06-30"), 
           ymin = -Inf, ymax = Inf,  fill = "#D55E00", alpha=.3) +
  annotate("text", 
           x = as.Date("2021-05-31"),
           y = mean(range(result_data$noRides)),
           label = "Change of operator,\nperiod removed",
           color = "black", 
           size = 9,
           angle = 90) +
  # geom_point(data = result_data %>% filter(wday_char == "Mon"), mapping = aes(x = date, y = noRides, color = "Mon"), size = 3) +
  # geom_point(data = result_data %>% filter(wday_char == "Tue"), mapping = aes(x = date, y = noRides, color = "Tue"), size = 3) +
  # geom_point(data = result_data %>% filter(wday_char == "Wed"), mapping = aes(x = date, y = noRides, color = "Wed"), size = 3) +
  # geom_point(data = result_data %>% filter(wday_char == "Thu"), mapping = aes(x = date, y = noRides, color = "Thu"), size = 3) +
  # geom_point(data = result_data %>% filter(wday_char == "Fri"), mapping = aes(x = date, y = noRides, color = "Fri"), size = 3) +
  #geom_vline(xintercept = as.numeric(year_breaks), color = "red", linetype = "dashed", size = 1) +
  #geom_text(data = data.frame(x = year_breaks, y = rep(min(result_data$noRides), length(year_breaks)), year = substr(year_breaks, 3, 4)),
  #          aes(x = x, y = y, label = year), color = "red", size = 5, vjust = -1) +
  theme_minimal() +
  xlab("Date") +
  ylab("nRides") +
  scale_x_date(breaks= seq(as.Date("2020-03-01"), as.Date("2022-12-31"), by = "3 months"), date_labels = "%m/%y") +
  theme(text = element_text(size = 50), legend.position = "bottom", legend.title=element_blank()) +
  theme(axis.ticks.x = element_line(size = 1), 
                   axis.ticks.y = element_line(size = 1),
                   axis.ticks.length = unit(15, "pt")) +
  scale_color_manual(values = colors)

noRides_time_incl_2023 <- ggplot(result_data_incl_2023) +
  geom_point(data = result_data %>% filter(wday_char == "Mon"), mapping = aes(x = date, y = noRides, color = "Mon")) +
  geom_point(data = result_data %>% filter(wday_char == "Tue"), mapping = aes(x = date, y = noRides, color = "Tue")) +
  geom_point(data = result_data %>% filter(wday_char == "Wed"), mapping = aes(x = date, y = noRides, color = "Wed")) +
  geom_point(data = result_data %>% filter(wday_char == "Thu"), mapping = aes(x = date, y = noRides, color = "Thu")) +
  geom_point(data = result_data %>% filter(wday_char == "Fri"), mapping = aes(x = date, y = noRides, color = "Fri")) +
  geom_vline(xintercept = as.numeric(year_breaks), color = "red", linetype = "dashed", size = 1) +
  geom_text(data = data.frame(x = year_breaks, y = rep(min(result_data$noRides), length(year_breaks)), year = substr(year_breaks, 3, 4)),
            aes(x = x, y = y, label = year), color = "red", size = 5, vjust = -1) +
  theme_light() +
  xlab("Date") +
  theme(
    legend.position = "bottom", legend.title = element_blank(),
    axis.ticks.x = element_line(), 
    axis.ticks.y = element_line(),
    axis.ticks.length = unit(5, "pt"),
    axis.text.x = element_text(angle = 90, hjust = 1),
    text = element_text(size = 12)
  ) +
  scale_x_date(date_breaks = "1 month", date_labels = "%b") +
  scale_color_manual(values = colors) +
  ggtitle("noRides over time incl 2023")


tmin_time <- ggplot(result_data) +
  geom_point(mapping=aes(x = date,y = tmin))+
  geom_vline(xintercept = as.numeric(year_breaks), color = "red", linetype = "dashed", size = 1) +
  geom_text(data = data.frame(x = year_breaks, y = rep(min(result_data$noRides), length(year_breaks)), year = substr(year_breaks, 3, 4)),
            aes(x = x, y = y, label = year), color = "red", size = 5, vjust = -1) +
  theme_light() +
  xlab("Date") +
  theme(axis.ticks.x = element_line(), 
        axis.ticks.y = element_line(),
        axis.ticks.length = unit(5, "pt"),
        axis.text.x = element_text(angle = 90, hjust = 1)) +
  scale_x_date(date_breaks = "1 month", date_labels = "%b") +
  theme(text = element_text(size = 12)) +
  ggtitle("tmin over time")

tavg_time <- ggplot(result_data) +
  geom_point(mapping=aes(x = date,y = tavg), size =4)+
  annotate("rect",
           xmin = as.Date("2021-05-01"), xmax = as.Date("2021-06-30"), 
           ymin = -Inf, ymax = Inf,  fill = "#D55E00", alpha=.3) +
  annotate("text", 
           x = as.Date("2021-05-31"),
           y = mean(range(result_data$tavg)),
           label = "Change of operator,\nperiod removed",
           color = "black", 
           size = 9,
           angle = 90) +
  #geom_vline(xintercept = as.numeric(year_breaks), color = "red", linetype = "dashed", size = 1) +
  #geom_text(data = data.frame(x = year_breaks, y = rep(min(result_data$noRides), length(year_breaks)), year = substr(year_breaks, 3, 4)),
  #          aes(x = x, y = y, label = year), color = "red", size = 5, vjust = -1) +
  theme_minimal() +
  xlab("Date") +
  ylab("tavg (C°)") +
  scale_x_date(breaks= seq(as.Date("2020-03-01"), as.Date("2022-12-31"), by = "3 months"), date_labels = "%m/%y") +
  theme(text = element_text(size = 50)) +
  theme(axis.ticks.x = element_line(size = 1), 
                   axis.ticks.y = element_line(size = 1),
                   axis.ticks.length = unit(15, "pt"))
  #ggtitle("tavg over time")

tmax_time <- ggplot(result_data) +
  geom_point(mapping=aes(x = date,y = tmax))+
  geom_vline(xintercept = as.numeric(year_breaks), color = "red", linetype = "dashed", size = 1) +
  geom_text(data = data.frame(x = year_breaks, y = rep(min(result_data$noRides), length(year_breaks)), year = substr(year_breaks, 3, 4)),
            aes(x = x, y = y, label = year), color = "red", size = 5, vjust = -1) +
  theme_light() +
  xlab("Date") +
  theme(axis.ticks.x = element_line(), 
        axis.ticks.y = element_line(),
        axis.ticks.length = unit(5, "pt"),
        axis.text.x = element_text(angle = 90, hjust = 1)) +
  scale_x_date(date_breaks = "1 month", date_labels = "%m/%y") +
  theme(text = element_text(size = 12)) +
  ggtitle("tmax over time")

tdiff_time <- ggplot(result_data) +
  geom_point(mapping=aes(x = date,y = tdiff))+
  geom_vline(xintercept = as.numeric(year_breaks), color = "red", linetype = "dashed", size = 1) +
  geom_text(data = data.frame(x = year_breaks, y = rep(min(result_data$noRides), length(year_breaks)), year = substr(year_breaks, 3, 4)),
            aes(x = x, y = y, label = year), color = "red", size = 5, vjust = -1) +
  theme_light() +
  xlab("Date") +
  theme(axis.ticks.x = element_line(), 
        axis.ticks.y = element_line(),
        axis.ticks.length = unit(5, "pt"),
        axis.text.x = element_text(angle = 90, hjust = 1)) +
  scale_x_date(date_breaks = "1 month", date_labels = "%b") +
  theme(text = element_text(size = 12)) +
  ggtitle("tdiff over time")

stringency_time <- ggplot(result_data) +
  geom_point(mapping=aes(x = date,y = stringency), size = 4)+
  annotate("rect",
           xmin = as.Date("2021-05-01"), xmax = as.Date("2021-06-30"), 
           ymin = -Inf, ymax = Inf,  fill = "#D55E00", alpha=.3) +
  annotate("text", 
           x = as.Date("2021-05-31"),
           y = mean(range(result_data$stringency)),
           label = "Change of operator,\nperiod removed",
           color = "black", 
           size = 9,
           angle = 90) +
  #geom_vline(xintercept = as.numeric(year_breaks), color = "red", linetype = "dashed", size = 1) +
  #geom_text(data = data.frame(x = year_breaks, y = rep(min(result_data$noRides), length(year_breaks)), year = substr(year_breaks, 3, 4)),
  #          aes(x = x, y = y, label = year), color = "red", size = 5, vjust = -1) +
  theme_minimal() +
  scale_x_date(breaks= seq(as.Date("2020-03-01"), as.Date("2022-12-31"), by = "3 months"), date_labels = "%m/%y") +
  theme(text = element_text(size = 50)) +
  theme(axis.ticks.x = element_line(size = 1), 
                   axis.ticks.y = element_line(size = 1),
                   axis.ticks.length = unit(15, "pt")) +
  ylab("stringency") +
  xlab("Date")

snow_time <- ggplot(result_data) +
  geom_point(mapping=aes(x = date,y = snow), size = 4)+
  annotate("rect",
           xmin = as.Date("2021-05-01"), xmax = as.Date("2021-06-30"), 
           ymin = -Inf, ymax = Inf,  fill = "#D55E00", alpha=.3) +
  annotate("text", 
           x = as.Date("2021-05-31"),
           y = 55,
           label = "Change of operator,\nperiod removed",
           color = "black", 
           size = 9,
           angle = 90) +
  #geom_vline(xintercept = as.numeric(year_breaks), color = "red", linetype = "dashed", size = 1) +
  #geom_text(data = data.frame(x = year_breaks, y = rep(min(result_data$noRides), length(year_breaks)), year = substr(year_breaks, 3, 4)),
  #          aes(x = x, y = y, label = year), color = "red", size = 5, vjust = -1) +
  theme_minimal() +
  xlab("Date") +
  ylab("snow (mm)") +
  scale_x_date(breaks= seq(as.Date("2020-03-01"), as.Date("2022-12-31"), by = "3 months"), date_labels = "%m/%y") +
  theme(text = element_text(size = 50)) +
  theme(axis.ticks.x = element_line(size = 1), 
                   axis.ticks.y = element_line(size = 1),
                   axis.ticks.length = unit(15, "pt"))
  #ggtitle("snow over time")

wdir_time <- ggplot(result_data) +
  geom_point(mapping=aes(x = date,y = wdir), size = 4)+
  annotate("rect",
           xmin = as.Date("2021-05-01"), xmax = as.Date("2021-06-30"), 
           ymin = -Inf, ymax = Inf,  fill = "#D55E00", alpha=.3) +
  annotate("text", 
           x = as.Date("2021-05-31"),
           y = mean(range(result_data$wdir)),
           label = "Change of operator,\nperiod removed",
           color = "black", 
           size = 9,
           angle = 90) +
  # geom_vline(xintercept = as.numeric(year_breaks), color = "red", linetype = "dashed", size = 1) +
  # geom_text(data = data.frame(x = year_breaks, y = rep(min(result_data$noRides), length(year_breaks)), year = substr(year_breaks, 3, 4)),
            # aes(x = x, y = y, label = year), color = "red", size = 5, vjust = -1) +
  theme_minimal() +
  xlab("Date") +
  ylab("wdir (°)") +
  scale_x_date(breaks= seq(as.Date("2020-03-01"), as.Date("2022-12-31"), by = "3 months"), date_labels = "%m/%y") +
  theme(text = element_text(size = 50)) +
  theme(axis.ticks.x = element_line(size=1), 
        axis.ticks.y = element_line(size=1),
        axis.ticks.length = unit(15, "pt"))
  # ggtitle("wdir over time")

ggarrange(
  noRides_time, tavg_time, ggparagraph(text="   ", face = "italic", size = 6, color = "black"), 
  snow_time, ggparagraph(text="   ", face = "italic", size = 6, color = "black"), 
  wdir_time, ggparagraph(text="   ", face = "italic", size = 6, color = "black"), 
  stringency_time,
  labels = c("A", "B", "C", "", "", "D", "E", ""), 
  align = "v", 
  nrow = 8, ncol = 1, 
  font.label = list(size = 37), 
  legend = "bottom", 
  heights = c(1, 1, 0.1, 1, 0.1, 1, 0.1, 1)
)

ggsave("ExploratoryAnalysis_BadWeather.pdf", dpi = 500, w = 24, h = 30)


prcp_time <- ggplot(result_data) +
  geom_point(mapping=aes(x = date,y = prcp))+
  geom_vline(xintercept = as.numeric(year_breaks), color = "red", linetype = "dashed", size = 1) +
  geom_text(data = data.frame(x = year_breaks, y = rep(min(result_data$noRides), length(year_breaks)), year = substr(year_breaks, 3, 4)),
            aes(x = x, y = y, label = year), color = "red", size = 5, vjust = -1) +
  theme_light() +
  xlab("Date") +
  theme(axis.ticks.x = element_line(), 
        axis.ticks.y = element_line(),
        axis.ticks.length = unit(5, "pt"),
        axis.text.x = element_text(angle = 90, hjust = 1)) +
  scale_x_date(date_breaks = "1 month", date_labels = "%b") +
  theme(text = element_text(size = 12)) +
  ggtitle("prcp over time")

pres_time <- ggplot(result_data) +
  geom_point(mapping=aes(x = date,y = pres))+
  geom_vline(xintercept = as.numeric(year_breaks), color = "red", linetype = "dashed", size = 1) +
  geom_text(data = data.frame(x = year_breaks, y = rep(min(result_data$noRides), length(year_breaks)), year = substr(year_breaks, 3, 4)),
            aes(x = x, y = y, label = year), color = "red", size = 5, vjust = -1) +
  theme_light() +
  xlab("Date") +
  theme(axis.ticks.x = element_line(), 
        axis.ticks.y = element_line(),
        axis.ticks.length = unit(5, "pt"),
        axis.text.x = element_text(angle = 90, hjust = 1)) +
  scale_x_date(date_breaks = "1 month", date_labels = "%b") +
  theme(text = element_text(size = 12)) +
  ggtitle("pres over time")

wpgt_time <- ggplot(result_data) +
  geom_point(mapping=aes(x = date,y = wpgt))+
  geom_vline(xintercept = as.numeric(year_breaks), color = "red", linetype = "dashed", size = 1) +
  geom_text(data = data.frame(x = year_breaks, y = rep(min(result_data$noRides), length(year_breaks)), year = substr(year_breaks, 3, 4)),
            aes(x = x, y = y, label = year), color = "red", size = 5, vjust = -1) +
  theme_light() +
  xlab("Date") +
  theme(axis.ticks.x = element_line(), 
        axis.ticks.y = element_line(),
        axis.ticks.length = unit(5, "pt"),
        axis.text.x = element_text(angle = 90, hjust = 1)) +
  scale_x_date(date_breaks = "1 month", date_labels = "%b") +
  theme(text = element_text(size = 12)) +
  ggtitle("wpgt over time")

wspd_time <- ggplot(result_data) +
  geom_point(mapping=aes(x = date,y = wspd))+
  geom_vline(xintercept = as.numeric(year_breaks), color = "red", linetype = "dashed", size = 1) +
  geom_text(data = data.frame(x = year_breaks, y = rep(min(result_data$noRides), length(year_breaks)), year = substr(year_breaks, 3, 4)),
            aes(x = x, y = y, label = year), color = "red", size = 5, vjust = -1) +
  theme_light() +
  xlab("Date") +
  theme(axis.ticks.x = element_line(), 
        axis.ticks.y = element_line(),
        axis.ticks.length = unit(5, "pt"),
        axis.text.x = element_text(angle = 90, hjust = 1)) +
  scale_x_date(date_breaks = "1 month", date_labels = "%b") +
  theme(text = element_text(size = 12)) +
  ggtitle("wspd over time")

# ggarrange(stringency, snow, wdir, wpsd, tavg, tmin, tmax, tdiff, pres, wpgt, 
# labels = c("(a)", "(b)", "(c)", "(d)", "(e)", "(f)", "(g)", "(h)", "(i)", "(j)"), 
# nrow = 5, ncol = 2,font.label = list(size = 37), legend = "bottom")

# plot data including holidays:
noRides_time_incl_holidays <- ggplot(result_data_incl_holidays) +
  geom_point(data = result_data_incl_holidays %>% filter(wday_char == "Mon"), mapping = aes(x = date, y = noRides, color = "Mon"), size=1) +
  geom_point(data = result_data_incl_holidays %>% filter(wday_char == "Tue"), mapping = aes(x = date, y = noRides, color = "Tue"), size=1) +
  geom_point(data = result_data_incl_holidays %>% filter(wday_char == "Wed"), mapping = aes(x = date, y = noRides, color = "Wed"), size=1) +
  geom_point(data = result_data_incl_holidays %>% filter(wday_char == "Thu"), mapping = aes(x = date, y = noRides, color = "Thu"), size=1) +
  geom_point(data = result_data_incl_holidays %>% filter(wday_char == "Fri"), mapping = aes(x = date, y = noRides, color = "Fri"), size=1) +
  geom_vline(xintercept = as.numeric(year_breaks), color = "red", linetype = "dashed", size = 1) +
  geom_text(data = data.frame(x = year_breaks, y = rep(min(result_data_incl_holidays$noRides), length(year_breaks)), year = substr(year_breaks, 3, 4)),
            aes(x = x, y = y, label = year), color = "red", size = 5, vjust = -1) +
  theme_light() +
  xlab("Date") +
  theme(
    legend.position = "bottom", legend.title = element_blank(),
    axis.ticks.x = element_line(), 
    axis.ticks.y = element_line(),
    axis.ticks.length = unit(5, "pt"),
    axis.text.x = element_text(angle = 90, hjust = 1),
    text = element_text(size = 12)
  ) +
  scale_x_date(date_breaks = "1 month", date_labels = "%b") +
  scale_color_manual(values = colors) +
  ggtitle("noRides over time incl holidays")

holidays_time_incl_holidays <- ggplot(result_data_incl_holidays) +
  geom_point(mapping=aes(x = date,y = isHoliday), size=1)+
  geom_vline(xintercept = as.numeric(year_breaks), color = "red", linetype = "dashed", size = 1) +
  geom_text(data = data.frame(x = year_breaks, y = rep(min(result_data_incl_holidays$noRides), length(year_breaks)), year = substr(year_breaks, 3, 4)),
            aes(x = x, y = y, label = year), color = "red", size = 5, vjust = -1) +
  theme_light() +
  xlab("Date") +
  theme(axis.ticks.x = element_line(), 
        axis.ticks.y = element_line(),
        axis.ticks.length = unit(5, "pt"),
        axis.text.x = element_text(angle = 90, hjust = 1)) +
  scale_x_date(date_breaks = "1 month", date_labels = "%b") +
  theme(text = element_text(size = 12)) +
  ggtitle("holidays over time incl holidays")

schoolHolidays_time_incl_holidays <- ggplot(result_data_incl_holidays) +
  geom_point(mapping=aes(x = date,y = isSchoolHoliday), size=1)+
  geom_vline(xintercept = as.numeric(year_breaks), color = "red", linetype = "dashed", size = 1) +
  geom_text(data = data.frame(x = year_breaks, y = rep(min(result_data_incl_holidays$noRides), length(year_breaks)), year = substr(year_breaks, 3, 4)),
            aes(x = x, y = y, label = year), color = "red", size = 5, vjust = -1) +
  theme_light() +
  xlab("Date") +
  theme(axis.ticks.x = element_line(), 
        axis.ticks.y = element_line(),
        axis.ticks.length = unit(5, "pt"),
        axis.text.x = element_text(angle = 90, hjust = 1)) +
  scale_x_date(date_breaks = "1 month", date_labels = "%b") +
  theme(text = element_text(size = 12)) +
  ggtitle("school holidays over time incl holidays")
schoolHolidays_time_incl_holidays

boxplot_rejectionShare_ioki <- ggplot(requests_rejections_ioki, aes(y = rejectionShare)) +
  geom_boxplot() +
  labs(title = "Boxplot of rejectionShare 0620-0421",
       y = "rejectionShare") +
  theme_minimal()

boxplot_rejectionShare_via <- ggplot(requests_rejections_via, aes(y = rejectionShare)) +
  geom_boxplot() +
  labs(title = "Boxplot of rejectionShare 0521-1222",
       y = "rejectionShare") +
  theme_minimal()

# add all plots to list
plots <- list(noRides_time_incl_2023, noRides_time, tmin_time, tavg_time, tmax_time, tdiff_time, stringency_time, snow_time, prcp_time,
              pres_time, wdir_time, wpgt_time, wspd_time, noRides_time_incl_holidays, holidays_time_incl_holidays,
              schoolHolidays_time_incl_holidays, boxplot_rejectionShare_ioki, boxplot_rejectionShare_via)

# iterate through list and plot each plot as ggplotly (interactive)
i <- 0
for(plot in plots) {
  ggsave(paste0("./bad-weather-exploratory-plot-",i,".png"), plot)
  print(ggplotly(plot))
  i <- i + 1
}

############################################## Pearson correlation coefficients ###############################################################################################################################

result_data$description = factor(result_data$description)
result_data$season = factor(result_data$season)
result_sum  = data.frame(c("noRides","description","weather_impact","tavg","tmin","tmax","prcp","snow","wspd","wpgt","pres","tdiff"),
                         c("Number of rides in day (dependent variable)","Weather description - the type of the weather with highest absolute duration among descriptions during a day","Number of hours of selected description with maximal hours a day",
                           "The average air temperature in °C","The minimum air temperature in °C	","The maximum air temperature in °C","The daily precipitation total in mm","The maximum snow depth in mm","The average wind speed in km/h",
                           "The peak wind gust in km/h","The average sea-level air pressure in hPa","Difference between season mean temperature and daily average temperature"),
                         c("Mean: 80.2","Clear, Cloudy, Heavy, Light, Medium, Sunny","Mean: 12 °C","Mean: 10.37 °C","Mean: 5.81 °C","Mean: 15.06","Mean: 1.76","Mean: 0.2348","Mean: 8.6 km/h","Mean: 32.75 km/h","Mean: 1019.3 hPa","Mean: 0.12701 °C"))
colnames(result_sum) = c("Variable","Description","Stat")

correlations_requests <- result_data  %>% ungroup() %>%
  dplyr::select(-noRides,-description ,-date,-season,-wday,-wday_char, -weather_impact, -isHoliday, -noRides) %>%
  map_dbl(cor,y = result_data$noRequests) %>%
  sort()
print((correlations_requests))

correlations <- result_data  %>% ungroup() %>%
  dplyr::select(-noRides,-description ,-date,-season,-wday,-wday_char, -weather_impact, -isHoliday) %>%
  map_dbl(cor,y = result_data$noRides) %>%
  sort()
print(correlations)

correlations_incl_holidays <- result_data_incl_holidays  %>% ungroup() %>%
  dplyr::select(-noRides,-description ,-date,-season,-wday,-wday_char, -weather_impact, -isHoliday) %>%
  map_dbl(cor,y = result_data_incl_holidays$noRides) %>%
  sort()
print(correlations_incl_holidays)

# correlations for 2 time periods separately:
correlations_ioki <- ioki_data  %>% ungroup() %>%
  dplyr::select(-noRides,-description ,-date,-season,-wday,-wday_char, -weather_impact, -isHoliday) %>%
  map_dbl(cor,y = ioki_data$noRides) %>%
  sort()
print(correlations_ioki)

correlations_via <- via_data  %>% ungroup() %>%
  dplyr::select(-noRides,-description ,-date,-season,-wday,-wday_char, -weather_impact, -isHoliday) %>%
  map_dbl(cor,y = via_data$noRides) %>%
  sort()
print(correlations_via)

correlations_incl_2023 <- result_data_incl_2023  %>% ungroup() %>%
  dplyr::select(-noRides,-description ,-date,-season,-wday, -weather_impact, -isHoliday, -tsun) %>%
  map_dbl(cor,y = result_data_incl_2023$noRides)
print(correlations_incl_2023)

correlations_incl_2023["tdiff"] <- NA
correlations_incl_2023 <- sort(correlations_incl_2023, na.last=TRUE)
print(correlations_incl_2023)

correlations_2023 <- result_data_2023  %>% ungroup() %>%
  dplyr::select(-noRides,-description ,-date,-season,-wday, -weather_impact, -isHoliday, -stringency, -tsun) %>%
  map_dbl(cor,y = result_data_2023$noRides)
print(correlations_2023)

correlations_2023["stringency"] <- NA
correlations_2023["tdiff"] <- NA
correlations_2023 <- sort(correlations_2023, na.last=TRUE)
print(correlations_2023)

# Use 'correlations' as the reference order
variable_order <- names(correlations)

# Reorder all correlation vectors to match 'variable_order'
correlations <- round(correlations, 2)
correlations_incl_holidays <- round(correlations_incl_holidays[variable_order], 2)
correlations_ioki <- round(correlations_ioki[variable_order], 2)
correlations_via <- round(correlations_via[variable_order], 2)
correlations_incl_2023 <- round(correlations_incl_2023[variable_order], 2)
correlations_2023 <- round(correlations_2023[variable_order], 2)

# Create the final data frame with consistent ordering
correlations_df <- data.frame(
  variable = variable_order,
  correlation_excl_2023 = correlations,
  correlation_incl_holidays = correlations_incl_holidays,
  correlation_ioki = correlations_ioki,
  correlation_via = correlations_via,
  correlation_incl_2023 = correlations_incl_2023,
  correlation_2023 = correlations_2023,
  stringsAsFactors = FALSE  # Ensures 'variable' is treated as a character vector
)


# barplot <- ggplot(as.data.frame(correlations), aes(x=variable, y=correlation_general)) +
#   geom_bar(fill="white",color="black",stat = "identity") +
#   geom_text(aes(label=correlation_general),size = 3, position = position_stack(vjust = 0.5)) +
#   ggtitle("correlation with noRides per ind. variable for whole time period")
# barplot

############################################## first regression model requests ############################################################################
test <- lm(noRequests ~ tavg+trend+prcp+snow,data = result_data)
summary(test)
confint(test)

############################################## first regression models for different time periods ####################################################################################################
first_model_general <- lm(noRides ~ wdir+tmax+tavg+tmin+snow+trend, data = result_data)
# first_model_general <- lm(noRides ~ wdir+tavg+snow+trend, data = result_data)
summary(first_model_general)
confint(first_model_general)

AIC_first_model_general <- AIC(first_model_general)
BIC_first_model_general <- BIC(first_model_general)

first_model_tavg <- lm(noRides ~ wdir+tavg+snow+trend, data = result_data)
summary(first_model_tavg)

first_model_tmax <- lm(noRides ~ wdir+tmax+snow+trend, data = result_data)
summary(first_model_tmax)

first_model_tmin <- lm(noRides ~ wdir+tmin+snow+trend, data = result_data)
summary(first_model_tmin)

# cross correlation of trend and stringency and first model with stringency replacing trend
cross_cor <- result_data %>% 
  dplyr::select(trend, stringency)
print(cor(cross_cor))

first_model_stringency <- lm(noRides ~ wdir+tmax+tavg+tmin+snow+stringency, data = result_data)
summary(first_model_stringency)
confint(first_model_stringency)

AIC(first_model_stringency)
BIC(first_model_stringency)



first_model_ioki <- lm(noRides ~ tavg+trend,data = ioki_data)
summary(first_model_ioki)
confint(first_model_ioki)

first_model_via <- lm(noRides ~ tavg+trend,data = via_data)
summary(first_model_via)
confint(first_model_via)

model_trend_only <- lm(noRides ~ trend, data = result_data)
summary(model_trend_only)
confint(first_model_general)



#in the following: 2 different databases: including 2023: 0620-1223, 2023:0123-1223

# R^2 0.417, alle var ***, Res std err 29.08, no confint around 0
first_model_incl_2023_tavg <- lm(noRides ~ tavg+trend,data = result_data_incl_2023)
summary(first_model_incl_2023_tavg)
confint(first_model_incl_2023_tavg)

# R^2 0.3983, intercept, trend ***, snow non-sign, Res std err 29.54, snow confint around 0
first_model_incl_2023_snow <- lm(noRides ~ snow+trend,data = result_data_incl_2023)
summary(first_model_incl_2023_snow)
confint(first_model_incl_2023_snow)

# R^2 0.4165, intercept, trend, tavg ***, snow non-sign, Res std err 29.09, snow confint around 0
first_model_incl_2023_tavg_snow <- lm(noRides ~ tavg+snow+trend,data = result_data_incl_2023)
summary(first_model_incl_2023_tavg_snow)
confint(first_model_incl_2023_tavg_snow)

# R^2 0.0168
first_model_incl_2023_weather_only <- lm(noRides ~ tavg+snow,data = result_data_incl_2023)
summary(first_model_incl_2023_weather_only)
confint(first_model_incl_2023_weather_only)


# R^2 0.1778, tavg, trend ***, intercept non-sign, Res std err 27.2, intercept confint around 0
first_model_2023_tavg <- lm(noRides ~ tavg+trend,data = result_data_2023)
summary(first_model_2023_tavg)
confint(first_model_2023_tavg)

# R^2 0.1142, trend ***, snow, intercept non-sign, Res std err 28.23, snow, intercept confint around 0
first_model_2023_snow <- lm(noRides ~ snow+trend,data = result_data_2023)
summary(first_model_2023_snow)
confint(first_model_2023_snow)

# R^2 0.1118, trend ***, pres, intercept non-sign, Res std err 28.27, pres, intercept confint around 0
first_model_2023_pres <- lm(noRides ~ pres+trend,data = result_data_2023)
summary(first_model_2023_pres)
confint(first_model_2023_pres)

# R^2 0.1755, trend, tavg ***, snow, pres, intercept non-sign, Res std err 27.24, pres, snow, intercept confint around 0
first_model_2023_tavg_snow_pres <- lm(noRides ~ tavg+snow+pres+trend,data = result_data_2023)
summary(first_model_2023_tavg_snow_pres)
confint(first_model_2023_tavg_snow_pres)

# R^2 0.04848
first_model_2023_weather_only <- lm(noRides ~ tavg+snow+pres,data = result_data_2023)
summary(first_model_2023_weather_only)
confint(first_model_2023_weather_only)

############################################## backwards elimination ##############################################################################################################
# first_mode_general with elimination of single parameters
# wdir eliminated
wdir_eliminated <- lm(noRides ~ tmax+tavg+tmin+snow+trend, data = result_data)
summary(wdir_eliminated)
confint(wdir_eliminated)
AIC(wdir_eliminated)
BIC(wdir_eliminated)

# wdir eliminated + only one temperature var
wdir_tmax_tmin_eliminated <- lm(noRides ~ tavg+snow+trend, data = result_data)
summary(wdir_tmax_tmin_eliminated)
confint(wdir_tmax_tmin_eliminated)
AIC(wdir_tmax_tmin_eliminated)
BIC(wdir_tmax_tmin_eliminated)

# wdir eliminated + only one temperature var + snow eliminated
wdir_tmax_tmin_snow_eliminated <- lm(noRides ~ tavg+trend, data = result_data)
summary(wdir_tmax_tmin_snow_eliminated)
confint(wdir_tmax_tmin_snow_eliminated)
AIC(wdir_tmax_tmin_snow_eliminated)
BIC(wdir_tmax_tmin_snow_eliminated)

# if we remove trend, R^2=0
wdir_tmax_tmin_snow_trend_eliminated <- lm(noRides ~ tavg, data = result_data)
summary(wdir_tmax_tmin_snow_trend_eliminated)
confint(wdir_tmax_tmin_snow_trend_eliminated)
AIC(wdir_tmax_tmin_snow_trend_eliminated)
BIC(wdir_tmax_tmin_snow_trend_eliminated)


############################################## calc Mean squared error (MSE) for trend ##############################################################################################################
# the noRides over time plot (until 12-22) shows, that the relation between trend and noRides is rather described
# by a MSE funtion than a linear one. Thus, MSE for trend will be calculated
# basic idea for function: f(x)=alpha*(1-exp(-x/beta))
# with alpha = y value to which f(x) converges and beta = intercept

# result_data <- result_data %>% 
#   filter(date <= ymd("2022-12-31") & date >= ymd("2022-01-01"))

rows <- nrow(result_data)

input <- c(1,1)
err <- rep(1, nrow(result_data))

calcMSE <- function(input) {
  alpha <- input[1]
  beta <- input[2]
  
  for (i in 1:rows) {
    est_demand <- alpha * (1 - exp(-result_data$trend[i] / beta))
    
    if (is.nan(est_demand) || is.infinite(est_demand)) {
      cat("Invalid value at iteration", i, "\n", "est_demand=", est_demand, " alpha=", alpha, " beta=", beta, " trend=", result_data$trend[i], " ")
      return(Inf)
    }
    
    err[i] <- result_data$noRides[i] - est_demand
  }
  return(sum(err^2) / length(err))
}

# apparently it is ok if est_demand turns out to be |Inf| but not in the first iteration fo optimization
optParams <- optim(input, calcMSE)
optParams

alpha <- optParams$par[1]
beta <- optParams$par[2]

#calc adjustedNoRides = noRides - alpha * (1 - exp(-trend / beta)) with optimized alpha and beta
result_data <- result_data %>% 
  mutate(adjustedNoRides = noRides - as.integer(optParams$par[1] * (1 - exp(-trend / optParams$par[2]))),
         est_demand = as.integer(optParams$par[1] * (1 - exp(-trend / optParams$par[2]))))

result_data  %>% ungroup() %>%
  dplyr::select(-noRides,-description ,-date,-season,-wday,-wday_char, -weather_impact, -isHoliday, -adjustedNoRides) %>%
  map_dbl(cor,y = result_data$adjustedNoRides) %>%
  sort()

# R^2 0.00401...
test_model <- lm(adjustedNoRides ~ tavg, data = result_data)
summary(test_model)
AIC(test_model)
BIC(test_model)

noRides_time_est_demand <- ggplot(result_data) +
  geom_point(mapping=aes(x = date,y = noRides), size=4)+
  geom_line(mapping = aes(x=date, y = est_demand), color="red", size=1.5) +
  annotate("rect",
           xmin = as.Date("2021-05-01"), xmax = as.Date("2021-06-30"), 
           ymin = -Inf, ymax = Inf,  fill = "#D55E00", alpha=.3) +
  annotate("text", 
           x = as.Date("2021-05-31"),
           y = mean(range(result_data$noRides)),
           label = "Change of operator,\nperiod removed",
           color = "black", 
           size = 11,
           angle = 90) +
  theme_minimal() +
  xlab("date") +
  ylab("nRides") +
  scale_x_date(breaks= seq(as.Date("2020-03-01"), as.Date("2022-12-31"), by = "3 months"), date_labels = "%m/%y") +
  theme(text = element_text(size = 50), legend.position = "bottom", legend.title=element_blank()) +
  theme(axis.ticks.x = element_line(size = 1), 
        axis.ticks.y = element_line(size = 1),
        axis.ticks.length = unit(15, "pt"),
        axis.text = element_text(size=45))
  # ggtitle("noRides over time + estimated trend (red)")
noRides_time_est_demand

ggsave("nRides_est_demand_time.pdf", noRides_time_est_demand, dpi = 500, w = 24, h = 9) 

adjustedNoRides_time_2 <- ggplot(result_data) +
  geom_point(mapping=aes(x = date,y = adjustedNoRides), size=4)+
  # geom_line(mapping=aes(x = date,y = snow-50), color="red")+
  # geom_line(mapping = aes(x=date, y = est_demand), color="red") +
  annotate("rect",
           xmin = as.Date("2021-05-01"), xmax = as.Date("2021-06-30"), 
           ymin = -Inf, ymax = Inf,  fill = "#D55E00", alpha=.3) +
  annotate("text", 
           x = as.Date("2021-05-31"),
           y = mean(range(result_data$adjustedNoRides)),
           label = "Change of operator,\nperiod removed",
           color = "black", 
           size = 11,
           angle = 90) +
  theme_minimal() +
  xlab("date") +
  ylab("adjusted nRides") +
  scale_x_date(breaks= seq(as.Date("2020-03-01"), as.Date("2022-12-31"), by = "3 months"), date_labels = "%m/%y") +
  theme(text = element_text(size = 50), legend.position = "bottom", legend.title=element_blank()) +
  theme(axis.ticks.x = element_line(size = 1), 
        axis.ticks.y = element_line(size = 1),
        axis.ticks.length = unit(15, "pt"),
        axis.text = element_text(size=45))
  # ggtitle("adjusted nor rides over time")
adjustedNoRides_time_2

ggsave("adjusted_nRides_time.pdf", adjustedNoRides_time_2, dpi = 500, w = 24, h = 9) 

combined_mse_plot <- ggplot(result_data) +
  geom_point(aes(x = date, y = adjustedNoRides, color = "adjusted nRides"), size = 4) +
  geom_point(aes(x = date, y = noRides, color = "nRides"), size = 4) +
  geom_line(aes(x = date, y = est_demand, color = "estimated demand"), size = 1.5) +
  annotate("rect",
           xmin = as.Date("2021-05-01"), xmax = as.Date("2021-06-30"), 
           ymin = -Inf, ymax = Inf, fill = "#D55E00", alpha = 0.3) +
  annotate("text", 
           x = as.Date("2021-05-31"),
           y = 50,
           label = "Change of operator,\nperiod removed",
           color = "black", 
           size = 11,
           angle = 90) +
  theme_minimal() +
  xlab("date") +
  ylab("nRides") +
  scale_x_date(breaks = seq(as.Date("2020-03-01"), as.Date("2022-12-31"), by = "3 months"), date_labels = "%m/%y") +
  scale_color_manual(values = c("adjusted nRides" = "blue", "nRides" = "black", "estimated demand" = "red")) +
  theme(text = element_text(size = 50), legend.position = "bottom", legend.title = element_blank()) +
  theme(axis.ticks.x = element_line(size = 1), 
        axis.ticks.y = element_line(size = 1),
        axis.ticks.length = unit(15, "pt"),
        axis.text = element_text(size = 45)) 
combined_mse_plot
ggsave("combined_mse_plot.pdf", combined_mse_plot, dpi = 500, w = 24, h = 9) 

mean(mean(result_data$noRides), mean(result_data$adjustedNoRides))

############################################## cross-correlation check ###############################################################################################################################

cor_check <- result_data %>%
  dplyr::select(tavg,trend)
print(cor(cor_check))

data <- result_data %>%
  mutate(trendDependentTemperature = trend * tavg)

reduced_3_model <- lm(noRides ~ tavg+trend+trendDependentTemperature, data = data)
summary(reduced_3_model)
confint(reduced_3_model) #95% confidence interval

AIC(reduced_3_model)
BIC(reduced_3_model)

############################################## final linear regression model ###############################################################################################################################

final_model <- wdir_tmax_tmin_snow_eliminated
summary(final_model)
confint(final_model) #95% confidence interval

model <- final_model

test_data <- result_data %>% add_predictions(model = model) %>% add_residuals(model = model) %>% mutate(error = ifelse(abs(resid)>=20,"extreme","normal"))

plot_final_model <- ggplot(test_data %>% filter(year(date)>=2020)) +
  geom_point(mapping=aes(x = date,y = noRides), size=4)+
  geom_line(aes(x = date,y = pred,color="predicted"), size = 1.2)+
  annotate("rect",
           xmin = as.Date("2021-05-01"), xmax = as.Date("2021-06-30"), 
           ymin = -Inf, ymax = Inf,  fill = "#D55E00", alpha=.3) +
  annotate("text", 
           x = as.Date("2021-05-31"),
           y = mean(range(result_data$noRides)),
           label = "Change of operator,\nperiod removed",
           color = "black", 
           size = 9,
           angle = 90) +
  theme_minimal() +
  xlab("Date") +
  ylab("nRides") +
  scale_x_date(breaks= seq(as.Date("2020-03-01"), as.Date("2022-12-31"), by = "3 months"), date_labels = "%m/%y") +
  theme(text = element_text(size = 50), legend.position = "bottom", legend.title=element_blank()) +
  theme(axis.ticks.x = element_line(size = 1.5), 
        axis.ticks.y = element_line(size = 1),
        axis.ticks.length = unit(15, "pt"),
        axis.text = element_text(size=45)) +
  scale_color_manual(values = colors)
  # ggtitle("Linear regression model with independent variables snow, tavg and trend")

ggsave("scatterplot-final-linear-regression-model.pdf", plot_final_model, dpi = 500, w = 24, h = 9) 

plot_final_model

ggplot(test_data %>% filter(year(date)>=2020)) +
# geom_point(aes(x = pred,y = noRides)) +
  # geom_point(data=test_data %>% filter(wday_char=="Mon"),mapping=aes(x = pred,y = noRides,color="Mon"))+
  geom_point(data=test_data %>% filter(wday_char=="Tue"),mapping=aes(x = pred,y = noRides,color="Tue"))+
  geom_point(data=test_data %>% filter(wday_char=="Wed"),mapping=aes(x = pred,y = noRides,color="Wed"))+
  geom_point(data=test_data %>% filter(wday_char=="Thu"),mapping=aes(x = pred,y = noRides,color="Thu"))+
  # geom_point(data=test_data %>% filter(wday_char=="Fri"),mapping=aes(x = pred,y = noRides,color="Fri"))+
geom_abline(aes(intercept = 0, slope = 1,color="Identity line"), size = 1.5) +
theme_minimal() +
xlab("Predicted nRides") +
ylab("Observed nRides") +
theme(axis.ticks.x = element_line(), 
      axis.ticks.y = element_line(),
      axis.ticks.length = unit(5, "pt")) +
  theme(text = element_text(size = 17)) +
  ggtitle("Observed vs. Predicted noRides") +
  scale_color_manual(values = colors2)
  
ggplot(test_data %>% filter(year(date)>=2020))+
            geom_line(aes(x = date,y = resid,color = "gray50"))+
          #  geom_ref_line(h = 0)+
            scale_color_manual(values = colors)+
           xlab("Date") +
           ylab("Residuals") +
           theme_minimal() +
           theme(axis.ticks.x = element_line(), 
      axis.ticks.y = element_line(),
      axis.ticks.length = unit(5, "pt"), legend.position = "none") +
            ggtitle("Residuals over time for linear regression model with independent variables snow, tavg and trend")

residuals_predicted_nRides <- ggplot(test_data %>% filter(year(date)>=2020), aes(x = pred,y = resid))+
            geom_point(size=3)+
          #  geom_ref_line(h = 0)+
            # scale_color_manual(values = colors)+
  geom_smooth(method ="loess", se = FALSE, color = "#666666", size = 1.5) +
  theme_minimal() +
  xlab("Predicted nRides") +
  ylab("Residuals") +
  theme(text = element_text(size = 45)) +
  theme(axis.ticks.x = element_line(size=1), 
        axis.ticks.y = element_line(size=1),
        axis.ticks.length = unit(5, "pt"), legend.position = "none",
        axis.text = element_text(size=45))
  # ggtitle("Residuals over predicted values for linear regression model with independent variables snow, tavg and trend")

ggsave("residuals-predictedValues-final-linear-regression-model.pdf", residuals_predicted_nRides, dpi = 500, w = 12, h = 9) 

barplot <- ggplot(test_data, aes(x = resid ))+
  geom_histogram(aes(y = after_stat(density)),colour="black", fill="white", binwidth=9)+
  ggtitle("Final residuals distributions with independent variables snow, tavg and trend")

# test_data <- test_data %>% filter(resid>=-50)
m <- mean(test_data$resid)
s <- sd(test_data$resid)
n <- nrow(test_data)
p <- (1 : n) / n - 0.5 / n

plot1 <- ggplot(test_data) +
  geom_qq(aes(sample=rnorm(resid,10,4)),size=3)+
  geom_abline(intercept = 10, slope = 4,color = "red", size = 1.5, alpha = 0.8)+
  theme_minimal() +
  xlab("Theoretical Quantiles") +
  ylab("Model Residual Quantiles") +
  theme(text = element_text(size = 45)) +
  theme(axis.ticks.x = element_line(size=1), 
        axis.ticks.y = element_line(size=1),
        axis.ticks.length = unit(5, "pt"), legend.position = "none",
        axis.text = element_text(size=45))
  # ggtitle("Normal QQ-Plot for the final linear regression model")

plot1

ggsave("qq-plot-final-linear-regression-model.pdf", plot1, dpi = 500, w = 12, h = 9) 

anno <- list( 
  list( 
     x = 0.2,  
    y = 1.0,  
    text = "Normal QQ Plot",  
    xref = "paper",  
    yref = "paper",  
    xanchor = "center",  
    yanchor = "bottom",  
    showarrow = FALSE 
  ),  
  list( 
     x = 0.75,  
    y = 1.0,  
    text = "Normal PP Plot",  
    xref = "paper",  
    yref = "paper",  
    xanchor = "center",  
    yanchor = "bottom",  
    showarrow = FALSE 
  ))

ggplotly(plot1)

# subplot(plot1,plot2) %>% layout(annotations = anno)
