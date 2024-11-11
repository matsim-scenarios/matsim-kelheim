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
stringency2023 <- data.frame(date = as.Date(c(ymd("2023-01-01"):ymd("2023-07-08")), origin = "1970-01-01")) %>% 
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
result_data <- demand %>% left_join(day_description_impact, by = "date") %>% inner_join(ingolstadt_weather,by = "date") %>% inner_join(df_stringency,by = "date") %>% mutate(date = as.Date(date,format = "%Y-%m-%d"))

#Also need to be added: weekday and simplified date variable
result_data <- result_data %>% 
  mutate(wday = as.character(wday(date,week_start = 1))) %>%
  dplyr::arrange(result_data, result_data$date) %>%
  distinct() %>%
  mutate(trend = as.integer(date) - as.integer(min(result_data$date)))

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
year_breaks <- unique(format(result_data$date, "%Y"))
year_breaks <- as.Date(paste(year_breaks, "-01-01", sep = ""))  # Convert to Date objects for proper placement

requests_time <- ggplot() +
  geom_point(data = requests %>% mutate(wday = as.character(wday(date,week_start = 1))) %>% filter(date <= as.Date("2022-12-31")) %>% filter(wday!=1 & wday!=5 & wday!=6 & wday!=7), mapping = aes(x = date, y = noRequests), color = "black") +
  geom_point(data = rejections %>% mutate(wday = as.character(wday(date,week_start = 1))) %>% filter(date <= as.Date("2022-12-31")) %>% filter(wday!=1 & wday!=5 & wday!=6 & wday!=7), mapping = aes(x = date, y = noRejections), color = "purple2") +
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

wday_plot <- ggplot(plot_data, aes(x=wday_char,y=noRides))+
  geom_boxplot(aes(color=wday_char), lwd=0.75) +
  xlab("Weekday") + 
  ylab("Number of rides") +
  # labs(title="Daily no of KEXI rides per weekday") +
  theme(plot.title = element_text(hjust=0.5), legend.title = element_blank()) +
  theme(text = element_text(size = 17)) +
  scale_color_manual(values = c("darkblue", "deepskyblue4", "deepskyblue2", "cadetblue", "chartreuse4","darkgoldenrod2","darkorchid4"))

holiday_plot <- ggplot(plot_data) +
  geom_boxplot(aes(x = isHoliday, y = noRides)) +
    xlab(NULL) +
    ylab("Number of rides") +
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
         noRides!=0)

# new after discussion on 31.10.24
before_sep_21 <- result_data %>%
  filter(date < ymd("2021-09-18"))

ioki_data <- result_data %>%
  filter(date <= ymd("2021-04-30"))

via_data <- result_data %>%
  filter(date > ymd("2021-04-30"))

# result_data <- result_data %>% filter(wday!=6 & wday!=7,isHoliday == FALSE, noRides!=0) #%>%
# new after discussion on 31.10.24
  # filter(date %within% interval(ymd("2021-09-18"), ymd("2022-12-18")))


############################################## more exploratory plots #########################################################################################################################################
noRides_time <- ggplot(result_data) +
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
  ggtitle("noRides over time")

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
  geom_point(mapping=aes(x = date,y = tavg))+
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
  ggtitle("tavg over time")

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
  scale_x_date(date_breaks = "1 month", date_labels = "%b") +
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
  geom_point(mapping=aes(x = date,y = stringency))+
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
  ggtitle("stringency over time")

snow_time <- ggplot(result_data) +
  geom_point(mapping=aes(x = date,y = snow))+
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
  ggtitle("snow over time")

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

wdir_time <- ggplot(result_data) +
  geom_point(mapping=aes(x = date,y = wdir))+
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
  ggtitle("wdir over time")

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

# add all plots to list
plots <- list(noRides_time, tmin_time, tavg_time, tmax_time, tdiff_time, stringency_time, snow_time, prcp_time,
              pres_time, wdir_time, wpgt_time, wspd_time, noRides_time_incl_holidays, holidays_time_incl_holidays, schoolHolidays_time_incl_holidays)

# iterate through list and plot each plot as ggplotly (interactive)
i <- 0
for(plot in plots) {
  ggsave(paste0("C:/Users/Simon/Desktop/wd/2024-11-06/bad-weather-exploratory-plot-",i,".png"), plot)
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

correlations <- result_data  %>% ungroup() %>%
  dplyr::select(-noRides,-description ,-date,-season,-wday,-wday_char, -weather_impact, -isHoliday) %>%
  map_dbl(cor,y = result_data$noRides) %>%
  sort(decreasing = TRUE)
print(correlations)

correlations_incl_holidays <- result_data_incl_holidays  %>% ungroup() %>%
  dplyr::select(-noRides,-description ,-date,-season,-wday,-wday_char, -weather_impact, -isHoliday) %>%
  map_dbl(cor,y = result_data_incl_holidays$noRides) %>%
  sort(decreasing = TRUE)
print(correlations_incl_holidays)

# correlations for 2 time periods separately:
correlations_ioki <- ioki_data  %>% ungroup() %>%
  dplyr::select(-noRides,-description ,-date,-season,-wday,-wday_char, -weather_impact, -isHoliday) %>%
  map_dbl(cor,y = ioki_data$noRides) %>%
  sort(decreasing = TRUE)
print(correlations_ioki)

correlations_via <- via_data  %>% ungroup() %>%
  dplyr::select(-noRides,-description ,-date,-season,-wday,-wday_char, -weather_impact, -isHoliday) %>%
  map_dbl(cor,y = via_data$noRides) %>%
  sort(decreasing = TRUE)
print(correlations_via)

constant_vars <- result_data %>%
  dplyr::select(-noRides, -description, -date, -season, -wday, -wday_char, -weather_impact, -isHoliday) %>%
  summarise(across(everything(), ~ sd(.x, na.rm = TRUE))) %>%
  pivot_longer(everything(), names_to = "variable", values_to = "std_dev") %>%
  filter(std_dev == 0)

correlations <- data.frame(correlation_general = round(correlations,2),
                           correlation_ioki = round(correlations_ioki,2),
                           correlation_via = round(correlations_via,2)) %>%
  rownames_to_column("variable")


barplot <- ggplot(correlations, aes(x=variable, y=correlation_general)) +
  geom_bar(fill="white",color="black",stat = "identity") +
  geom_text(aes(label=correlation_general),size = 3, position = position_stack(vjust = 0.5)) +
  ggtitle("correlation with noRides per ind. variable for whole time period")
barplot

############################################## first regression models for different time periods ####################################################################################################
first_model_general <- lm(noRides ~ tavg+trend,data = result_data)
summary(first_model_general)
confint(first_model_general)

first_model_ioki <- lm(noRides ~ tavg+trend,data = ioki_data)
summary(first_model_ioki)
confint(first_model_ioki)

first_model_via <- lm(noRides ~ tavg+trend,data = via_data)
summary(first_model_via)
confint(first_model_via)



############################################## first regression model ###############################################################################################################################

data <- result_data

omega_model <- lm(noRides ~ stringency+wspd+wpgt+wdir+snow+tmax+tavg+tmin+tdiff+pres,data = data)

summary(omega_model)
confint(omega_model)

model <- omega_model
test_data <- data %>% add_predictions(model = model) %>% add_residuals(model = model) %>% mutate(error = ifelse(abs(resid)>=20,"extreme","normal"))

ggplot(test_data %>% filter(year(date)>=2020)) +
  geom_point(data=test_data %>% filter(wday_char=="Mon"),mapping=aes(x = date,y = noRides,color="Mon"))+
  geom_point(data=test_data %>% filter(wday_char=="Tue"),mapping=aes(x = date,y = noRides,color="Tue"))+
  geom_point(data=test_data %>% filter(wday_char=="Wed"),mapping=aes(x = date,y = noRides,color="Wed"))+
  geom_point(data=test_data %>% filter(wday_char=="Thu"),mapping=aes(x = date,y = noRides,color="Thu"))+
  geom_point(data=test_data %>% filter(wday_char=="Fri"),mapping=aes(x = date,y = noRides,color="Fri"))+
  geom_line(mapping=aes(x = date,y = pred,color="predicted"), size = 1.2)+
  theme_minimal() +
  xlab("Date") +
  theme(legend.position = "bottom", legend.title = element_blank()) +
  theme(axis.ticks.x = element_line(), 
                   axis.ticks.y = element_line(),
                   axis.ticks.length = unit(5, "pt")) +
  scale_x_date(date_breaks = "4 month", date_labels = "%b/%y") +
  theme(text = element_text(size = 17)) +
  scale_color_manual(values = colors) +
  ggtitle("First Linear regression model")

#ggsave("C:/Users/Simon/Desktop/wd/2023-07-31/first-regression-model.png", modelPlot)


ggplot(test_data %>% filter(year(date)>=2020))+
            geom_line(aes(x = date,y = resid,color = "gray"))+
          #  geom_ref_line(h = 0)+
           xlab("Date") +
           ylab("Residuals") +
           theme_minimal() +
  theme(text = element_text(size = 17)) +
           theme(axis.ticks.x = element_line(), 
      axis.ticks.y = element_line(),
      axis.ticks.length = unit(5, "pt"), legend.position = "none") +
            ggtitle("Residuals over time for first linear regression model")


omega_date_model <- lm(noRides ~ stringency+wspd+wpgt+wdir+snow+tmax+tavg+tmin+tdiff+pres+trend,data = data)
summary(omega_date_model)

omega_date_model_prcp <- lm(noRides ~ wspd+wpgt+wdir+snow+tmax+tavg+tmin+tdiff+pres+trend+prcp,data = data)
summary(omega_date_model_prcp)

omega_date_model_prcp_tavg <- lm(noRides ~ wspd+wpgt+wdir+snow+tmin+pres+trend+prcp,data = data)
summary(omega_date_model_prcp_tavg)

omega_model_trend_tmin <- lm(noRides ~ tmin+trend,data = data)
summary(omega_model_trend_tmin)

omega_model_trend_tmin_beforeSep21 <- lm(noRides ~ tmin+trend,data = before_sep_21)
summary(omega_model_trend_tmin_beforeSep21)

print(cor(before_sep_21$noRides, before_sep_21$tmin))

print(cor(before_sep_21$noRides, before_sep_21$tmax))



omega_date_model_prcp_before_sep21 <- lm(noRides ~ stringency+wspd+wpgt+wdir+snow+tmax+tavg+tmin+tdiff+pres+trend+prcp,data = before_sep_21)
summary(omega_date_model_prcp_before_sep21)

omega_date_only_model <- lm(noRides ~ wspd+wpgt+wdir+snow+tmax+tavg+tmin+tdiff+pres+trend,data = data)
summary(omega_date_only_model)


model <- omega_date_only_model
test_data <- data %>% add_predictions(model = model) %>% add_residuals(model = model) %>% mutate(error = ifelse(abs(resid)>=20,"extreme","normal"))

cor_stringency_noRides <- cor(test_data$stringency, test_data$noRides)
cor_trend_noRides <- cor(test_data$trend, test_data$noRides)
cor_stringency_trend <- cor(test_data$stringency, test_data$trend)

print(paste("correlation of stringency and trend:",cor_stringency_trend))
print(paste("correlation of stringency and noRides:",cor_stringency_noRides))
print(paste("correlation of trend and noRides:",cor_trend_noRides))

ggplotly(ggplot(test_data %>% filter(year(date)>=2020)) +
  geom_point(data=test_data %>% filter(wday_char=="Mon"),mapping=aes(x = date,y = noRides,color="Mon"))+
  geom_point(data=test_data %>% filter(wday_char=="Tue"),mapping=aes(x = date,y = noRides,color="Tue"))+
  geom_point(data=test_data %>% filter(wday_char=="Wed"),mapping=aes(x = date,y = noRides,color="Wed"))+
  geom_point(data=test_data %>% filter(wday_char=="Thu"),mapping=aes(x = date,y = noRides,color="Thu"))+
  geom_point(data=test_data %>% filter(wday_char=="Fri"),mapping=aes(x = date,y = noRides,color="Fri"))+
  geom_point(aes(x = date,y = pred,color="predicted"))+
  scale_color_manual(values = colors)+
  ggtitle("Linear regression model with date parameter"))
ggplotly(ggplot(test_data %>% filter(year(date)>=2020))+
            geom_line(aes(x = date,y = resid,color = "gray50"))+
            geom_ref_line(h = 0)+
            ggtitle("Residuals over time"))

barplot <- ggplot(test_data, aes(x = resid ))+
  geom_histogram(aes(y = stat(density)),colour="black", fill="white", binwidth=7)+
  ggtitle("Residuals histogram")

############################################## reduced regression models ###############################################################################################################################

reduced_1_model <- lm(noRides ~ snow+tavg+trend, data = data)
summary(reduced_1_model)

model <- reduced_1_model
test_data <- data %>% add_predictions(model = model) %>% add_residuals(model = model) %>% mutate(error = ifelse(abs(resid)>=20,"extreme","normal"))

############################################## cross-correlation check ###############################################################################################################################

cor_check <- data %>%
  dplyr::select(tavg,trend,snow)
print(cor(cor_check))

data <- data %>%
  mutate(snowDependentTemperature = tavg * snow,
         trendDependentSnow = snow * trend)

reduced_3_model <- lm(noRides ~ snow+tavg+trend+snowDependentTemperature+trendDependentSnow, data = data)
summary(reduced_3_model)
confint(reduced_3_model) #95% confidence interval

############################################## final linear regression model ###############################################################################################################################

final_model <- lm(noRides ~ snow+tavg+trend, data = data)
summary(final_model)
confint(final_model) #95% confidence interval

model <- final_model

test_data <- data %>% add_predictions(model = model) %>% add_residuals(model = model) %>% mutate(error = ifelse(abs(resid)>=20,"extreme","normal"))

ggplot(test_data %>% filter(year(date)>=2020)) +
  geom_point(data=test_data %>% filter(wday_char=="Mon"),mapping=aes(x = date,y = noRides,color="Mon"))+
  geom_point(data=test_data %>% filter(wday_char=="Tue"),mapping=aes(x = date,y = noRides,color="Tue"))+
  geom_point(data=test_data %>% filter(wday_char=="Wed"),mapping=aes(x = date,y = noRides,color="Wed"))+
  geom_point(data=test_data %>% filter(wday_char=="Thu"),mapping=aes(x = date,y = noRides,color="Thu"))+
  geom_point(data=test_data %>% filter(wday_char=="Fri"),mapping=aes(x = date,y = noRides,color="Fri"))+
  geom_line(aes(x = date,y = pred,color="predicted"), size = 1.2)+
  theme_minimal() +
  xlab("Date") +
  theme(legend.position = "bottom", legend.title = element_blank()) +
  theme(axis.ticks.x = element_line(), 
                   axis.ticks.y = element_line(),
                   axis.ticks.length = unit(5, "pt")) +
  scale_x_date(date_breaks = "4 month", date_labels = "%b/%y") +
  theme(text = element_text(size = 17)) +
  scale_color_manual(values = colors) +
  ggtitle("Linear regression model with independent variables snow, tavg and trend")

ggplot(test_data %>% filter(year(date)>=2020)) +
# geom_point(aes(x = pred,y = noRides)) +
  geom_point(data=test_data %>% filter(wday_char=="Mon"),mapping=aes(x = pred,y = noRides,color="Mon"))+
  geom_point(data=test_data %>% filter(wday_char=="Tue"),mapping=aes(x = pred,y = noRides,color="Tue"))+
  geom_point(data=test_data %>% filter(wday_char=="Wed"),mapping=aes(x = pred,y = noRides,color="Wed"))+
  geom_point(data=test_data %>% filter(wday_char=="Thu"),mapping=aes(x = pred,y = noRides,color="Thu"))+
  geom_point(data=test_data %>% filter(wday_char=="Fri"),mapping=aes(x = pred,y = noRides,color="Fri"))+
geom_abline(aes(intercept = 0, slope = 1,color="Identity line"), size = 1.5) +
theme_minimal() +
xlab("Predicted noRides") +
ylab("Observed noRides") +
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

ggplot(test_data %>% filter(year(date)>=2020), aes(x = pred,y = resid))+
            geom_point()+
          #  geom_ref_line(h = 0)+
            scale_color_manual(values = colors)+
  geom_smooth(method ="loess", se = FALSE, color = "#666666", size = 1.5) +
           xlab("Predicted noRides") +
           ylab("Residuals") +
           theme_minimal() +
           theme(axis.ticks.x = element_line(), 
      axis.ticks.y = element_line(),
      axis.ticks.length = unit(5, "pt"), legend.position = "none") +
  theme(text = element_text(size = 17)) + 
  ggtitle("Residuals over predicted values for linear regression model with independent variables snow, tavg and trend")


barplot <- ggplot(test_data, aes(x = resid ))+
  geom_histogram(aes(y = after_stat(density)),colour="black", fill="white", binwidth=9)+
  ggtitle("Final residuals distributions with independent variables snow, tavg and trend")

# test_data <- test_data %>% filter(resid>=-50)
m <- mean(test_data$resid)
s <- sd(test_data$resid)
n <- nrow(test_data)
p <- (1 : n) / n - 0.5 / n

plot1 <- ggplot(test_data) +
  geom_qq(aes(sample=rnorm(resid,10,4)))+
  geom_abline(intercept = 10, slope = 4,color = "red", size = 1.5, alpha = 0.8)+
  theme_minimal() +
  theme(text = element_text(size = 17)) +
  ggtitle("Normal QQ-Plot for the final linear regression model") +
  xlab("Theoretical Quantiles") +
  ylab("Model Residual Quantiles")


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
