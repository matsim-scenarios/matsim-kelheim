library(tidyverse)
library(lubridate)
library(xtable)

#TESTING IDS
testingCustomerIds_extended <- as.character(c(1,  
                                              43, 
                                              649,
                                              673,
                                              3432,
                                              3847, 
                                              3887, 
                                              4589, 
                                              7409,
                                              7477,
                                              9808,
                                              9809,
                                              8320,
                                              12777,
                                              13288, 
                                              13497, 
                                              13498 ))


data_raw <- read.csv2("C:/Users/J/Documents/Thesis/thesis/fahrtanfragen.csv", na.strings = "")%>% 
  filter(!Fahrgast.ID %in% testingCustomerIds_extended)


####CLEAN, FILTER & SAVE####
# delete rows that don't have a boarding time, place or are not marked as completed
data_completed <- data_raw %>% 
  drop_na(Tatsächliche.Einstiegszeit,
          Längengrad..Einstieg.) %>% 
  filter(Status.der.Fahrtanfrage == "Completed")

#dataset of AV rides
data_av <- data_completed %>% 
  filter(Grund.für.die.Fahrt == "AV")

#dataset of conventional rides
data_dr <- data_completed %>% 
  filter(Grund.für.die.Fahrt == "DR" | is.na(Grund.für.die.Fahrt))
#dataset including both AV and conventional, but filtered for time and duration 
data_plausible <- data_completed %>% 
  filter(as.Date(Erstellungsdatum.der.Fahrtanfrage) <"2024-01-01") %>% 
  filter(as.numeric(Fahrtdauer) > 1 & as.numeric(Fahrtdauer) < 30)

write.csv(data_plausible, "rides_clean.csv", row.names = FALSE)
write.csv(data_dr, "rides_dr.csv", row.names = FALSE)

####ANALYSIS RAW DATA####
#seat unavailable
#overall
unavailable <- data_raw %>% 
  filter(as.Date(Erstellungsdatum.der.Fahrtanfrage) <"2024-01-01",
         as.Date(Erstellungsdatum.der.Fahrtanfrage) >"2021-06-01") %>% 
  mutate(monat = format(as.Date(Erstellungsdatum.der.Fahrtanfrage), "%Y-%m")) %>%
  group_by(monat, Status.der.Fahrtanfrage) %>% 
  summarise(count = n())
#on-demand
unavailable <- data_raw %>% 
  filter(as.Date(Erstellungsdatum.der.Fahrtanfrage) <"2024-01-01",
         as.Date(Erstellungsdatum.der.Fahrtanfrage) >"2021-06-01") %>% 
  filter(Buchungsart =="On Demand") %>% 
  mutate(monat = format(as.Date(Erstellungsdatum.der.Fahrtanfrage), "%Y-%m")) %>%
  group_by(monat, Status.der.Fahrtanfrage) %>%
  summarise(count = n())
#plot
ggplot(unavailable)+
  geom_bar(aes(x = monat, y = count, fill = Status.der.Fahrtanfrage),
           stat = "identity",
           position = "fill")+
  xlab("month")+
  theme(axis.text.x = element_text(angle = 45, vjust = 0.5))+
  labs(fill = "booking status")

#Status der Fahrtanfrage + generate table
table <- data_raw %>% 
  group_by(Status.der.Fahrtanfrage) %>% 
  count() %>% 
  mutate(percent = round((n/sum(n))*100,2))
table <-t(table)
xtable(table)

#number of users
n_distinct(data_raw$Fahrgast.ID)

#buchungsart 
data_raw %>% 
  filter(Status.der.Fahrtanfrage=="Completed" | Status.der.Fahrtanfrage=="Unaccepted Proposal") %>% 
  group_by(Buchungsart, Status.der.Fahrtanfrage) %>% 
  summarise(count = n())

### NACHFRAGE MONATE ###

#COUNT TRIPS PER MONTH: completed, unaccepted, seat unavailable
count_month <- data_raw %>% 
  filter(as.Date(Erstellungsdatum.der.Fahrtanfrage) <"2024-01-01",
         as.Date(Erstellungsdatum.der.Fahrtanfrage) >"2021-07-01") %>% 
  filter(Status.der.Fahrtanfrage=="Completed" | Status.der.Fahrtanfrage=="Unaccepted Proposal" |Status.der.Fahrtanfrage=="Seat Unavailable") %>% 
  mutate(monat = format(as.Date(Erstellungsdatum.der.Fahrtanfrage), "%Y-%m")) %>% 
  group_by(monat, Status.der.Fahrtanfrage) %>% 
  summarise(count = n())

ggplot(count_month)+
  geom_bar(aes(x = monat, y = count, fill =factor(Status.der.Fahrtanfrage, levels = c("Unaccepted Proposal", "Seat Unavailable", "Completed"))), stat = "identity")+
  theme(axis.text.x = element_text(angle = 45, vjust = 0.5))+
  scale_fill_viridis_d(end = 0.9)+
  labs(fill = "booking status")+
  xlab("month")

#COUNT REQUESTS AND USERS
count_month <- data_raw %>% 
  filter(as.Date(Erstellungsdatum.der.Fahrtanfrage) <"2024-01-01",
         as.Date(Erstellungsdatum.der.Fahrtanfrage) >"2021-06-01") %>% 
  mutate(monat = format(as.Date(Erstellungsdatum.der.Fahrtanfrage), "%Y-%m")) %>% 
  group_by(monat) %>% 
  summarise(count = n(), ids = length(unique(Fahrgast.ID)))

ggplot(count_month)+
  geom_bar(aes(x = monat, y = count, fill = ids), stat = "identity")+
  theme(axis.text.x = element_text(angle = 45, vjust = 0.5))

###NACHFRAGE WOCHE###
#COUNT TRIPS PER WEEKDAY: completed, unaccepted, seat unavailable
count_weekday <- data_raw %>% 
  filter(Status.der.Fahrtanfrage=="Completed" | Status.der.Fahrtanfrage=="Unaccepted Proposal" |Status.der.Fahrtanfrage=="Seat Unavailable") %>% 
  drop_na(Angefragte.Einstiegszeit) %>% 
  mutate(weekday= wday(as.Date(Angefragte.Einstiegszeit), label = TRUE, locale="en_UK")) %>% 
  filter(weekday != "Sun") %>% 
  group_by(weekday, Status.der.Fahrtanfrage) %>% 
  summarise(count = n())

plot_count_wday <- ggplot(count_weekday)+
  geom_bar(aes(x = weekday, y = count, fill =factor(Status.der.Fahrtanfrage, levels = c("Unaccepted Proposal", "Seat Unavailable", "Completed"))), stat = "identity")+
  theme(axis.text.x = element_text(angle = 45, vjust = 0.5))+
  scale_fill_viridis_d(end = 0.9)+
  labs(fill = "booking status")+
  xlab("day of the week")

###NACHFRAGE TAG###
#COUNT TRIPS PER TIME OF DAY: completed, unaccepted, seat unavailable
count_hour <-data_raw %>% 
  filter(Status.der.Fahrtanfrage=="Completed" | Status.der.Fahrtanfrage=="Unaccepted Proposal" |Status.der.Fahrtanfrage=="Seat Unavailable") %>% 
  drop_na(Angefragte.Einstiegszeit) %>% 
  mutate(hour = format(as_datetime(Angefragte.Einstiegszeit), "%H")) %>% 
  filter(hour !="00",
         hour !="04",
         hour !="05") %>% 
  group_by(hour, Status.der.Fahrtanfrage) %>% 
  summarise(count = n())

plot_count_hour <- ggplot(count_hour)+
  geom_bar(aes(x = hour, y = count, fill =factor(Status.der.Fahrtanfrage, levels = c("Unaccepted Proposal", "Seat Unavailable", "Completed"))), stat = "identity")+
  theme(axis.text.x = element_text(angle = 45, vjust = 0.5))+
  scale_fill_viridis_d(end = 0.9)+
  labs(fill = "booking status")+
  xlab("hour")


ggarrange(plot_count_hour, plot_count_wday, ncol=2, common.legend = TRUE, legend="bottom")

###WAITTIME ###

#calculate wait time (requested vs planned departure time) and compare completed vs. unaccepted rides

avg_wait <- data_raw %>% 
  filter(Status.der.Fahrtanfrage=="Completed" | Status.der.Fahrtanfrage=="Unaccepted Proposal") %>% 
  mutate(monat = format(as.Date(Erstellungsdatum.der.Fahrtanfrage), "%Y-%m")) %>%
  drop_na(Zeit.von.Fahrtanfrage.bis.zum.geplanten.Einstieg..nur.On.Demand.) %>%  
  mutate(prep = (as.numeric(as_datetime(Angefragte.Einstiegszeit)-as_datetime(Erstellungszeit.der.Fahrtanfrage)))/60)

avg_wait_status <- avg_wait %>% 
  filter(prep >0) %>% 
  group_by(monat, Status.der.Fahrtanfrage) %>% 
  summarise(mean_wait = mean(as.numeric(Zeit.von.Fahrtanfrage.bis.zum.geplanten.Einstieg..nur.On.Demand.)),
            mean_prep = mean(prep),
            count = n()) 

ggplot(avg_wait_status) +
  geom_bar(aes(x = monat, y = mean_wait, fill = Status.der.Fahrtanfrage), stat = "identity", position = "dodge")


