library(tidyverse)
library(sf)
library(clock)
library(lubridate)
library(scales)
library(xtable)
library(ggpubr)

#### FILE PATHS ####

data_path <- "rides_clean.csv"

#data_dist is created in r5r.R
data_dist_path <- "rides_dist.csv"

raster_path <- "../shapefiles/grid_4326.shp"

#### READ DATA ####

data <- read.csv(data_path)%>% 
  mutate(av_dr = ifelse(is.na(Grund.für.die.Fahrt),"DR",Grund.für.die.Fahrt))
data_dist <- read.csv(data_dist_path)

raster <- st_read(raster_path)

####GENERAL NUMBERS####

n_distinct(data$Fahrgast.ID)

#trips per user
user_dist <- data %>% 
  filter(Buchungsmethode != "Agent") %>% 
  group_by(Fahrgast.ID) %>% 
  summarise(count = n()) %>% 
  group_by(count) %>% 
  summarise(count_count = n()) %>% 
  mutate(abs = count*count_count)

ggplot(user_dist)+
  geom_histogram(aes(x=count, y = abs), stat = "identity")

#number of trips with multiple travelers
data %>% 
  filter(Anzahl.der.Fahrgäste >1) %>% 
  nrow()

#number of trips by Buchungsart 
data %>% 
  group_by(Buchungsart) %>% 
  summarise(count = n())

#number of trips by Planungspräferenz
data %>% 
  group_by(Planungspräferenz) %>% 
  summarise(count = n())

#number of trips by Buchungsart and Planungspräferenz
data %>% 
  group_by(Buchungsart, Planungspräferenz) %>% 
  summarise(count = n())

#number of trips by Buchungsmethode
data %>% 
  group_by(Buchungsmethode) %>% 
  summarise(count = n())

#number of trips by Buchungsart and Buchungsmethode
table <-data %>% 
  group_by(Buchungsmethode, Buchungsart) %>% 
  summarise(count = n()) %>% 
  pivot_wider(names_from = Buchungsart, values_from = count)

xtable(table)

#### COUNT ORIGIN/DESTINATION in GRID ####
#create sf from data
origins <- st_as_sf(data, coords = c("Start.Längengrad", "Start.Breitengrad"), crs = 4326 )
destinations <- st_as_sf(data, coords = c("Zielort.Längengrad", "Zielort.Breitengrad"), crs = 4326 )


#join origins to raster and remove raster cells that don't contain any rides
raster_origins <- raster %>% 
  mutate(counts = lengths(st_intersects(., origins)))
raster_origins <- subset(raster_origins, raster_origins$counts !=0)

#join destinations to raster and remove raster cells that don't contain any rides
raster_dest <- raster %>% 
  mutate(counts = lengths(st_intersects(., destinations)),)
raster_dest <- subset(raster_dest, raster_dest$counts !=0)

#join both raster sfs to one 
all_raster <- full_join(raster_origins, st_drop_geometry(raster_dest), by = "id") %>% 
  mutate(ratio = (counts.x-counts.y))

st_write(all_raster, "../Shapefiles/raster_origins_destinations.shp", append = FALSE)

#### DEMAND BY MONTH ####

#COUNT FOR EACH MONTH
count_month <- data %>% 
  mutate(month = format(as.Date(Erstellungsdatum.der.Fahrtanfrage), "%Y-%m")) %>% 
  group_by(month) %>% 
  summarise(count = n())

ggplot(count_month)+
  geom_bar(aes(x = month, y = count), stat = "identity", fill = c("#2a788e"))+
  xlab("month")+
  theme(axis.text.x = element_text(angle = 45, vjust = 0.5))

#COUNT FOR EACH MONTH AV/DR
count_month_2 <- data %>% 
  mutate(month = format(as.Date(Erstellungsdatum.der.Fahrtanfrage), "%Y-%m")) %>% 
  group_by(month, av_dr) %>% 
  summarise(count = n())

ggplot(count_month_2)+
  geom_bar(aes(x = month, y = count, fill = av_dr), stat = "identity")+
  xlab("month")+
  theme(axis.text.x = element_text(angle = 45, vjust = 0.5))+
  labs(fill = "vehicle type")

#COUNT USERS PER MONTH
count_users <- data %>% 
  mutate(month = format(as.Date(Erstellungsdatum.der.Fahrtanfrage), "%Y-%m")) %>%
  group_by(month) %>% 
  summarise(users = n_distinct(Fahrgast.ID))

ggplot(count_users)+
  geom_bar(aes(x = month, y = users), stat = "identity")

#COUNT UNIQUE USERS PER MONTH
count_users_2 <- data %>% 
  mutate(month = format(as.Date(Erstellungsdatum.der.Fahrtanfrage), "%Y-%m")) %>%
  mutate(unique_user = !duplicated(Fahrgast.ID)) %>% 
  group_by(month, unique_user) %>% 
  summarise(users = n_distinct(Fahrgast.ID))

ggplot(count_users_2)+
  geom_bar(aes(x = month, y = users, fill= unique_user), stat = "identity", position = "fill")

df %>%
  mutate(unique_customers = !duplicated(customer_id)) %>%
  group_by(month, year) %>%
  summarise(unique_customers = sum(unique_customers))

#AVERAGE DISTANCE TRAVELED PER MONTH

data_dist <- read.csv("rides_dist.csv") #direct distance calculated with r5r

avg_distance_month <- data_dist %>% 
  mutate(month = format(as.Date(Erstellungsdatum.der.Fahrtanfrage), "%Y-%m")) %>% 
  drop_na(distance) %>% 
  group_by(month) %>%
  summarise(mean = mean(as.numeric(distance)),
            median = median(as.numeric(distance))) %>% 
  pivot_longer(cols = c("mean", "median"),
               names_to = "type",
               values_to = "distance")

ggplot(avg_distance_month)+
  geom_bar(aes(x = month, y = distance, fill = type), 
           stat = "identity",
           position = "dodge")

#AVERAGE TRIP DURATION PER MONTH
avg_duration_month <- data %>% 
  mutate(month = format(as.Date(Erstellungsdatum.der.Fahrtanfrage), "%Y-%m")) %>% 
  drop_na(Fahrtdauer) %>% 
  group_by(month) %>%
  summarise(mean = mean(as.numeric(Fahrtdauer)),
            median = median(as.numeric(Fahrtdauer))) %>% 
  pivot_longer(cols = c("mean", "median"),
               names_to = "type",
               values_to = "duration")

ggplot(avg_duration_month)+
  geom_bar(aes(x = month, y = duration, fill = type), 
           stat = "identity",
           position = "dodge")


#COUNT BOOKING TYPE PER MONTH
buchungsart_month <- data %>%
  mutate(month = format(as.Date(Erstellungsdatum.der.Fahrtanfrage), "%Y-%m")) %>% 
  group_by(month, Buchungsart) %>% 
  summarise(count = n())

ggplot(buchungsart_month)+
  geom_bar(aes(x = month, y = count, fill = Buchungsart),
           stat = "identity",
           position = "fill")+
  xlab("month")+
  theme(axis.text.x = element_text(angle = 45, vjust = 0.5))+
  labs(fill = "booking type")

#COUNT BOOKING METHOD PER MONTH
buchungsmethode_month <- data %>%
  mutate(month = format(as.Date(Erstellungsdatum.der.Fahrtanfrage), "%Y-%m")) %>% 
  group_by(month, Buchungsmethode) %>% 
  summarise(count = n())

ggplot(buchungsmethode_month)+
  geom_bar(aes(x = month, y = count, fill = Buchungsmethode),
           stat = "identity",
           position = "fill")+
  xlab("month")+
  theme(axis.text.x = element_text(angle = 45, vjust = 0.5))+
  labs(fill = "booking method")

####DEMAND BY HOUR OF DAY####
#COUNT TRIPS BY HOUR OF DAY
count_hour <-data %>% 
  drop_na(Angefragte.Einstiegszeit) %>% 
  mutate(hour = format(as_datetime(Angefragte.Einstiegszeit), "%H")) %>% 
  group_by(hour) %>% 
  summarise(count = n())

ggplot(count_hour)+
  geom_bar(aes(x=hour, y = count), stat = "identity", fill = c("#2a788e"))

#AVG DISTANCE BY HOUR OF DAY
avg_distance_hour <- data %>% 
  drop_na(Fahrtdistanz) %>% 
  drop_na(Angefragte.Einstiegszeit) %>% 
  mutate(hour = format(as_datetime(Angefragte.Einstiegszeit), "%H")) %>% 
  group_by(hour) %>%
  summarise(mean = mean(as.numeric(Fahrtdistanz)),
            median = median(as.numeric(Fahrtdistanz))) %>% 
  pivot_longer(cols = c("mean", "median"),
               names_to = "type",
               values_to = "distance")

ggplot(avg_distance_hour)+
  geom_bar(aes(x = hour, y = distance, fill = type), 
           stat = "identity",
           position = "dodge")

####DEMAND BY WEEKDAY####
count_weekday <- data %>% 
  drop_na(Angefragte.Einstiegszeit) %>% 
  mutate(weekday= wday(as.Date(Angefragte.Einstiegszeit), label = TRUE, locale="en_UK")) %>% 
  filter(weekday != "Sun") %>% 
  group_by(weekday) %>% 
  summarise(count = n())

ggplot(count_weekday)+
  geom_bar(aes(x=weekday, y= count), stat = "identity",  fill = c("#7ad151"))

count_weekday_hour <- data %>% 
  drop_na(Angefragte.Einstiegszeit) %>% 
  mutate(weekday= wday(as.Date(Angefragte.Einstiegszeit), label = TRUE)) %>% 
  filter(weekday != "So") %>% 
  mutate(hour = format(as_datetime(Angefragte.Einstiegszeit), "%H")) %>%
  group_by(weekday, hour) %>% 
  summarise(count = n())

ggplot(count_weekday_hour, aes(x=hour, y = count, fill = weekday))+
  geom_bar( stat = "identity")+
  facet_wrap(~weekday)

#### PREPARATION TIME ####

#REQUESTED DEPARTURE/ARRIVAL TIME 

data_ein_aus <- data %>% 
  mutate(ein_aus = if_else(is.na(Angefragte.Einstiegszeit), "aus", "ein")) %>% 
  mutate(monat = format(as.Date(Erstellungsdatum.der.Fahrtanfrage), "%Y-%m")) %>% 
  group_by(monat, ein_aus) %>% 
  summarise(count = n())

#pre-booking: REQUEST TIME vs. REQUESTED DEPARTURE TIME
avg_prep <- data %>% 
  select(-Series.ID, -Session.ID, -Anzahl.der.Fahrgäste,-Wiederholungsart, -Fahrer.ID, -Fahrzeug.ID,
         -Letzte.Schicht.ID, -Service, -Geteilte.Fahrt, -Geteilte.Fahrtdauer..Min..,-No.Show.Grund, -No.Show.Zeit,
         -Direkte.Fahrtdauer..Min.., -Fahrtbewertung..1.5., -Fahrtfeedback, -Feedback.Labels, -Unternehmens.ID, -Unternehmensname,
         -Reason.For.Manual.Status.Update) %>% 
  drop_na(Angefragte.Einstiegszeit) %>%  
  mutate(prep = (as.numeric(as_datetime(Angefragte.Einstiegszeit)-as_datetime(Erstellungszeit.der.Fahrtanfrage)))/60) %>% 
  mutate(monat = format(as.Date(Erstellungsdatum.der.Fahrtanfrage), "%Y-%m")) 

prep_values <- avg_prep %>% 
  group_by(Buchungsart) %>% 
  summarise(mean = mean(prep))

#development over time (month)
avg_prep %>% 
  group_by(monat) %>% 
  summarise(prep = mean(prep), count = n()) %>% 
  ggplot()+
  geom_histogram(aes(y = prep, x = monat),
                bins = 48, stat = "identity", position = "dodge")

#development short preparation times
avg_prep %>% 
  filter(prep <5) %>% 
  group_by(monat) %>% 
  summarise(prep = mean(prep), count = n()) %>% 
  ggplot()+
  geom_histogram(aes(y = prep, x = monat, fill = count),
                 bins = 48, stat = "identity", position = "dodge")

#development long preparation times
avg_prep %>% 
  filter(prep >30) %>% 
  group_by(monat) %>% 
  summarise(prep = mean(prep), count = n()) %>% 
  ggplot()+
  geom_histogram(aes(y = prep, x = monat, fill = count),
                 bins = 48, stat = "identity", position = "dodge")

#development over time, divded in bins
prep_bins_month <- avg_prep %>% 
  mutate(prep_bin = case_when(
    prep >= -1 & prep < 5 ~ "0-5 mins",
    prep >= 5 & prep < 30 ~ "5-30 mins",
    prep >= 30 & prep < 60 ~ "30-60 mins",
    prep >= 60 & prep < 360 ~ "1-6 hs",
    prep >= 360 & prep < 1440 ~ "6-24 hs",
    prep >= 1440 & prep < 4320 ~ "1-3 days",
    prep >= 4320 & prep <= Inf ~ "more than 3 days",
    TRUE ~ NA_character_
  )) %>%
  group_by(monat,prep_bin) %>% 
  summarise(count = n())

levels <- c("0-5 mins",
            "5-30 mins",
            "30-60 mins",
            "1-6 hs","6-24 hs", "1-3 days", "more than 3 days")
ggplot(prep_bins_month) +
  geom_bar(aes(x=monat, y= count, fill = forcats::fct_rev(factor(prep_bin, levels = levels))), stat = "identity", position = "stack")+
  scale_fill_viridis_d(end = 0.9)+
  xlab("month")+
  theme(axis.text.x = element_text(angle = 45, vjust = 0.5))+
  labs(fill = "Preparation Time")


#### WAIT TIME ####
avg_wait <- data %>% 
  select(-Series.ID, -Session.ID, -Anzahl.der.Fahrgäste,-Wiederholungsart, -Fahrer.ID, -Fahrzeug.ID,
         -Letzte.Schicht.ID, -Service, -Geteilte.Fahrt, -Geteilte.Fahrtdauer..Min..,-No.Show.Grund, -No.Show.Zeit,
         -Direkte.Fahrtdauer..Min.., -Fahrtbewertung..1.5., -Fahrtfeedback, -Feedback.Labels, -Unternehmens.ID, -Unternehmensname,
         -Reason.For.Manual.Status.Update,
         -Startzone, -Zielzone, -Fahrtanfragezone, -Anbietername, -Stornierungsgrund, -Stornierungsquelle) %>% 
  mutate(prep = (as.numeric(as_datetime(Angefragte.Einstiegszeit)-as_datetime(Erstellungszeit.der.Fahrtanfrage)))/60) %>% 
  mutate(monat = format(as.Date(Erstellungsdatum.der.Fahrtanfrage), "%Y-%m"))%>%
  filter(is.na(Stornierungszeit)) %>% 
  mutate(wait_1 = (as.numeric(as_datetime(Ursprünglich.geplante.Einstiegszeit)-as_datetime(Angefragte.Einstiegszeit)))/60) %>% 
  mutate(wait_1_5 = as.numeric(as_datetime(Zuletzt.geplante.Einstiegszeit)-as_datetime(Ursprünglich.geplante.Einstiegszeit))/60) %>% 
  mutate(wait_2 = as.numeric(as_datetime(Fahrzeug.ist.da.SMS.Versandzeit)-as_datetime(Ursprünglich.geplante.Einstiegszeit))/60) %>%
  mutate(wait_2_5 = (as.numeric(as_datetime(Fahrzeug.ist.da.SMS.Versandzeit)-as_datetime(ifelse(is.na(Zuletzt.geplante.Einstiegszeit), Ursprünglich.geplante.Einstiegszeit,Zuletzt.geplante.Einstiegszeit))))/60) %>%
  mutate(wait_3 = (as.numeric(as_datetime(Tatsächliche.Einstiegszeit)-(as_datetime(Fahrzeug.ist.da.SMS.Versandzeit))))/60) %>%
  mutate(wait_4 = (as.numeric(as_datetime(Tatsächliche.Einstiegszeit)-(as_datetime(Ursprünglich.geplante.Einstiegszeit))))/60)


#relationship preparation time and difference requested/planned departure time
avg_wait %>% 
  drop_na(prep, wait_1) %>% 
  ggplot()+
  geom_point(aes(x = prep, y = wait_1))

wait_1_dist <- avg_wait %>% 
  drop_na(wait_1) %>%
  mutate(wait_1_bins = case_when(
    wait_1 >= -60 & wait_1 < -10 ~ "-60--10",
    wait_1 >= -10 & wait_1 < -5 ~ "-10--5",
    wait_1 >= -5 & wait_1 < -2 ~ "-5--2",
    wait_1 >= -2 & wait_1 < 2 ~ "-2-2",
    wait_1 >= 2 & wait_1 < 5 ~ "2-5",
    wait_1 >= 5 & wait_1 < 10 ~ "5-10",
    wait_1 >= 10 & wait_1 < 15 ~ "10-15",
    wait_1 >= 15 & wait_1 < 30 ~ "15-30",
    wait_1 >= 30 & wait_1 <= 60 ~ "30-60",
    TRUE ~ NA_character_
  )) %>% 
  group_by(wait_1_bins) %>% 
  summarise(count = n())

#Ursprüngliche vs. letzte Einstiegszeit

wait_1_5 <- avg_wait %>% 
  mutate(Änderung = ifelse(is.na(Änderung), 0, Änderung)) %>%  
  mutate(Änderung_bins = case_when(
    Änderung >= -60 & Änderung < -10 ~ "-60--10",
    Änderung >= -10 & Änderung < -5 ~ "-10--5",
    Änderung >= -5 & Änderung < -2 ~ "-5--2",
    Änderung >= -2 & Änderung < 2 ~ "-2-2",
    Änderung >= 2 & Änderung < 5 ~ "2-5",
    Änderung >= 5 & Änderung < 10 ~ "5-10",
    Änderung >= 10 & Änderung < 15 ~ "10-15",
    Änderung >= 15 & Änderung < 30 ~ "15-30",
    Änderung >= 30 & Änderung <= 60 ~ "30-60",
    TRUE ~ NA_character_
  )) %>% 
  group_by(Änderung_bins) %>% 
  summarise(count = n())


#distribution planned departure time/SMS time
wait_2_dist <-avg_wait %>% 
  drop_na(wait_2) %>%
  mutate(Original = case_when(
    wait_2 >= -60 & wait_2 < -10 ~ "-60--10",
    wait_2 >= -10 & wait_2 < -5 ~ "-10--5",
    wait_2 >= -5 & wait_2 < -2 ~ "-5--2",
    wait_2 >= -2 & wait_2 < 2 ~ "-2-2",
    wait_2 >= 2 & wait_2 < 5 ~ "2-5",
    wait_2 >= 5 & wait_2 < 10 ~ "5-10",
    wait_2 >= 10 & wait_2 < 15 ~ "10-15",
    wait_2 >= 15 & wait_2 < 30 ~ "15-30",
    wait_2 >= 30 & wait_2 <= 60 ~ "30-60",
    TRUE ~ NA_character_
  )) %>% 
  mutate(Last = case_when(
    wait_2_5 >= -60 & wait_2_5 < -10 ~ "-60--10",
    wait_2_5 >= -10 & wait_2_5 < -5 ~ "-10--5",
    wait_2_5 >= -5 & wait_2_5 < -2 ~ "-5--2",
    wait_2_5 >= -2 & wait_2_5 < 2 ~ "-2-2",
    wait_2_5 >= 2 & wait_2_5 < 5 ~ "2-5",
    wait_2_5 >= 5 & wait_2_5 < 10 ~ "5-10",
    wait_2_5 >= 10 & wait_2_5 < 15 ~ "10-15",
    wait_2_5 >= 15 & wait_2_5 < 30 ~ "15-30",
    wait_2_5 >= 30 & wait_2_5 <= 60 ~ "30-60",
    TRUE ~ NA_character_
  )) %>%
  pivot_longer(cols = c("Last", "Original"),
               names_to = "wait_type",
               values_to = "bins") %>% 
  group_by(wait_type, bins) %>% 
  summarise(count = n())


#distribution sms/actual departure
wait_3_dist <-avg_wait %>% 
  drop_na(wait_3) %>%
  mutate(wait_3_bins = case_when(
    wait_3 >= 0 & wait_3 < 2 ~ "0-2",
    wait_3 >= 2 & wait_3 < 5 ~ "2-5",
    wait_3 >= 5 & wait_3 < 10 ~ "5-10",
    wait_3 >= 10 & wait_3 < 15 ~ "10-15",
    wait_3 >= 15 & wait_3 < 30 ~ "15-30",
    wait_3 >= 30 & wait_3 <= 60 ~ "30-60",
    TRUE ~ NA_character_
  )) %>% 
  group_by(wait_3_bins) %>% 
  summarise(count = n())


#distribution planned departure/actual departure
wait_4_dist <-avg_wait %>% 
  drop_na(wait_4) %>%
  mutate(wait_4_bins = case_when(
    wait_4 >= -60 & wait_4 < -10 ~ "-60--10",
    wait_4 >= -10 & wait_4 < -5 ~ "-10--5",
    wait_4 >= -5 & wait_4 < -2 ~ "-5--2",
    wait_4 >= -2 & wait_4 < 2 ~ "-2-2",
    wait_4 >= 2 & wait_4 < 5 ~ "2-5",
    wait_4 >= 5 & wait_4 < 10 ~ "5-10",
    wait_4 >= 10 & wait_4 < 15 ~ "10-15",
    wait_4 >= 15 & wait_4 < 30 ~ "15-30",
    wait_4 >= 30 & wait_4 <= 60 ~ "30-60",
    TRUE ~ NA_character_
  )) %>% 
  group_by(wait_4_bins) %>% 
  summarise(count = n())


####PLOTS####

ylimits <- c(0, max(c(wait_1_dist$count, wait_2_dist$count, wait_1_5$count)))

plot_wait_1 <-ggplot(wait_1_dist)+
  geom_bar(aes(x=factor(wait_1_bins, 
                        levels = c("-60--10", "-10--5", "-5--2",
                                   "-2-2","2-5","5-10",
                                   "10-15","15-30","30-60",NA)), y = count),stat = "identity", fill = c("#35b779"))+
  xlab("Original Planned - Requested Departure Time (mins)")+
  ylim(ylimits)

plot_wait_1_5 <- ggplot(wait_1_5)+
  geom_bar(aes(x=factor(Änderung_bins, 
                        levels = c("-60--10", "-10--5", "-5--2",
                                   "-2-2","2-5","5-10",
                                   "10-15","15-30","30-60",NA)), y = count),stat = "identity", fill = c("#3e4989"))+
  xlab("Last Planned - Original Planned Departure Time (mins)")+
  ylim(ylimits)

plot_wait_2 <-ggplot(wait_2_dist)+
  geom_bar(aes(x=factor(bins, 
                        levels = c("-60--10", "-10--5", "-5--2",
                                   "-2-2","2-5","5-10",
                                   "10-15","15-30","30-60",NA)), y = count, fill = wait_type),stat = "identity", position = "dodge")+
  scale_fill_manual(values = c("#b5de2b","#26828e"))+
  xlab("Original/Last Planned Departure - Vehicle Arrival Time (mins)")+
  ylim(ylimits)+
  theme(legend.position=c(.75,.75),
        legend.background = element_rect(fill = "transparent"),
        legend.title = element_blank())

plot_wait_3<-ggplot(wait_3_dist)+
  geom_bar(aes(x=factor(wait_3_bins, 
                        levels = c("0-2","2-5","5-10",
                                   "10-15","15-30","30-60",NA)), y = count),stat = "identity", fill = c("#440154"))+
  xlab("Vehicle Arrival - Actual Boarding Time (mins)")


plot_wait_4 <-ggplot(wait_4_dist)+
  geom_bar(aes(x=factor(wait_4_bins, 
                        levels = c("-60--10", "-10--5", "-5--2",
                                   "-2-2","2-5","5-10",
                                   "10-15","15-30","30-60",NA)), y = count),stat = "identity", fill = c("#31688e"))+
  xlab("Planned Departure - Actual Boarding Time (mins)")+
  ylim(ylimits)

ggarrange(plot_wait_1, plot_wait_1_5,plot_wait_2, plot_wait_3, 
          ncol=2, nrow=2)


#wait time for accessibility calculations

avg_wait <- avg_wait %>% 
  mutate(Einstiegszeit_kommuniziert = ifelse(is.na(Zuletzt.geplante.Einstiegszeit), Ursprünglich.geplante.Einstiegszeit, Zuletzt.geplante.Einstiegszeit)) %>% 
  mutate(end_wait = ifelse(is.na(Fahrzeug.ist.da.SMS.Versandzeit),Tatsächliche.Einstiegszeit,
                           ifelse(Fahrzeug.ist.da.SMS.Versandzeit < Einstiegszeit_kommuniziert, Einstiegszeit_kommuniziert, Fahrzeug.ist.da.SMS.Versandzeit))) %>% 
  mutate(op_1 = (as.numeric(as_datetime(end_wait)-as_datetime(Angefragte.Einstiegszeit)))) %>% 
  mutate(op_2 = as.numeric(as_datetime(end_wait)-as_datetime(Ursprünglich.geplante.Einstiegszeit))) %>% 
  mutate(op_3 = as.numeric(as_datetime(end_wait)-as_datetime(ifelse(is.na(Zuletzt.geplante.Einstiegszeit),Ursprünglich.geplante.Einstiegszeit, Zuletzt.geplante.Einstiegszeit)))) 

test <- avg_wait %>% 
  filter(prep<=5) %>% 
  drop_na(op_1)
avg_op_1 = mean(test$op_1)
test <- avg_wait %>% 
  drop_na(op_2)
avg_op_2 = mean(test$op_2)
test <- avg_wait %>% 
  drop_na(op_3) %>% 
  filter(op_3>=-600)
avg_op_3 = mean(test$op_3)

#WAIT TIME THROUGH THE DAY
avg_wait_hour <- avg_wait %>% 
  filter(prep <=5) %>% 
  mutate(hour = format(as_datetime(Ursprünglich.geplante.Einstiegszeit), "%H")) %>% 
  mutate(on_demand_wait = (as.numeric(as_datetime(Tatsächliche.Einstiegszeit)-as_datetime(Angefragte.Einstiegszeit)))) %>% 
  group_by(hour) %>% 
  summarise(mean = mean(on_demand_wait))

ggplot(avg_wait_hour)+
  geom_bar(aes(x=hour, y = mean), stat = "identity", fill = c("#2a788e"))

#WAIT TIME OVER TIME
avg_wait_month <- avg_wait %>% 
  drop_na(wait_4) %>% 
  mutate(month = format(as.Date(Erstellungsdatum.der.Fahrtanfrage), "%Y-%m")) %>% 
  group_by(month) %>% 
  summarise(mean = mean(wait_4))

ggplot(avg_wait_month)+
  geom_bar(aes(x=month, y = mean), stat = "identity", fill = c("#2a788e"))

