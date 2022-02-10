library(lubridate)
library(tidyverse)
library(dplyr)
library(ggplot2)
library(plotly)
library(hrbrthemes)

#####################################################################
####################################################
### INPUT DEFINITIONS ###

# set working directory
#setwd("D:/svn/shared-svn/projects/KelRide/data/KEXI/")

# read data
allData <- read.csv2("IOKI_RequestList_202006_202105.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8")

# convert time columns
allData <- allData %>% 
  mutate(Fahrtwunsch.erstellt = dmy_hms(Fahrtwunsch.erstellt),
         Angefragte.Fahrtzeit = dmy_hms(Angefragte.Fahrtzeit),
         Kalkulierte.Abfahrtszeit = dmy_hms(Kalkulierte.Abfahrtszeit),
         Kalkulierte.Ankunftszeit = dmy_hms(Kalkulierte.Ankunftszeit),
         Abfahrtszeit = dmy_hms(Abfahrtszeit),
         Ankunftszeit = dmy_hms(Ankunftszeit),
         Stornierungszeit = dmy_hms(Stornierungszeit)
  )

# if TRUE, then interactive plots are produced. if FALSE, plots are dumped out as pngs to 'plots' folder
interactiveMode = TRUE

#####################################################################
####################################################
### SCRIPT ###

####################################################
## PLOT ALL REQUESTS ##

requests <- allData %>% 
  select(Fahrtwunsch.erstellt) %>% 
  mutate(date = date(Fahrtwunsch.erstellt))

reqProTag <- requests %>% 
  group_by( date = date(Fahrtwunsch.erstellt)) %>% 
  tally()

###########
# plot time line
p <- reqProTag %>%
  ggplot( aes(x=date, y=n)) +
  ggtitle("Zeitverlauf der Anfragen pro Tag") + 
  geom_area(fill="#69b3a2", alpha=0.5) +
  geom_line(color="#69b3a2") +
  ylab("Requests") +
  xlab("Tag") + 
  theme_ipsum()

##would put this behind an if or else condition but does not work for me :/
plotFile = "plots/KEXI_202006_202105_requests.png"
paste("printing plot to ", plotFile)
png(plotFile, width = 1200, height = 800)  
p
dev.off()
if(interactiveMode){
  ggplotly(p)  
} 

############  
#group by weekday
reqProWochentag <- reqProTag %>% 
  group_by( weekday = wday(date, label = TRUE )) %>% 
  filter(!is.na(weekday)) %>% 
  summarise(avg = mean(n))
#plot avg nr of requests per weekday
p <- reqProWochentag %>%
  ggplot( aes(x=weekday, y=avg)) +
  ggtitle("Durchschn. Anzahl Requests pro Wochentag") +
  geom_bar(color="#69b3a2", stat = "identity") + 
  ylab("Durchschn. Anzahl Requests") +
  xlab("Wochentag") + 
  theme_ipsum()

##would put this behind an if or else condition but does not work for me :/
plotFile = "plots/KEXI_202006_202105_requests_weekdays.png"
paste("printing plot to ", plotFile)
png(plotFile, width = 1200, height = 800)  
p
dev.off()
if(interactiveMode){
  ggplotly(p)  
} 


#####################
#plot day time line of all requests
#rm <- requests %>% 
#  mutate (minutes = minute(Fahrtwunsch.erstellt) + hour(Fahrtwunsch.erstellt) * 60) %>% 
#  group_by(minutes) %>% 
#  tally()

#p <- rm %>%
#  ggplot( aes(x=minutes/60, y=n)) +
#  ggtitle("Requests pro Minute") + 
#  geom_area(fill="#69b3a2", alpha=0.5) +
#  geom_line(color="#69b3a2") +
#  ylab("nr of requests per minute") +
#  theme_ipsum()
#ggplotly(p)

# 5min intervals
requestsPerInterval <- requests %>% 
  mutate (interval = floor( (minute(Fahrtwunsch.erstellt) + hour(Fahrtwunsch.erstellt) * 60 ) /5) )  %>% 
  filter(!is.na(interval)) %>% 
  group_by(interval) %>% 
  tally()

p <- requestsPerInterval %>%
  ggplot( aes(x=interval * 5/60, y=n)) +
  ggtitle("Requests pro 5 Minuten-Intervall") + 
  geom_area(fill="#69b3a2", alpha=0.5) +
  geom_line(color="#69b3a2") +
  ylab("Anzahl Requests pro Intervall") +
  xlab("Stunde") + 
  theme_ipsum()

##would put this behind an if or else condition but does not work for me :/
plotFile = "plots/KEXI_202006_202105_requests_daily.png"
paste("printing plot to ", plotFile)
png(plotFile, width = 1200, height = 800)  
p
dev.off()
if(interactiveMode){
  ggplotly(p)  
} 


####################################################
## PLOT COMPLETED RIDES ##

# filter and prepare data:  let us use Abfahrtszeit as core time stamp
completedRides <- allData %>% 
  filter(Stornierungsgrund == "ride_completed") %>% 
  select(Abfahrtszeit)

#group per day
ridesProTag <- completedRides %>% 
  group_by(date = date(Abfahrtszeit)) %>% 
  tally()
  
# plot time line
p <- ridesProTag %>%
  ggplot( aes(x=date, y=n)) +
  geom_area(fill="#69b3a2", alpha=0.5) +
  geom_line(color="#69b3a2") +
  ylab("Fahrten") +
  xlab("Tag") + 
  ggtitle("Zeitverlauf der Fahrten pro Tag") + 
  theme_ipsum()

##would put this behind an if or else condition but does not work for me :/
plotFile = "plots/KEXI_202006_202105_rides.png"
paste("printing plot to ", plotFile)
png(plotFile, width = 1200, height = 800)  
p
dev.off()
if(interactiveMode){
  ggplotly(p)  
} 


################
#group by weekday
ridesProWochentag <- ridesProTag %>% 
  group_by( weekday = wday(date, label = TRUE) ) %>% 
  filter(!is.na(weekday)) %>% 
  summarise(avg = mean(n))
#plot avg nr of requests per weekday
p <- ridesProWochentag %>%
  ggplot( aes(x=weekday, y=avg)) +
  geom_bar(color="#69b3a2", stat = "identity") + 
  ylab("Durchschn. Anzahl Fahrten") +
  xlab("Tag") + 
  ggtitle("Durchschn. Anzahl Fahrten pro Wochentag") + 
  theme_ipsum()

##would put this behind an if or else condition but does not work for me :/
plotFile = "plots/KEXI_202006_202105_rides_weekdays.png"
paste("printing plot to ", plotFile)
png(plotFile, width = 1200, height = 800)  
p
dev.off()
if(interactiveMode){
  ggplotly(p)  
} 

################
# PLOT Saturday RIDES
saturdays <- completedRides %>% 
  mutate(weekday = wday(Abfahrtszeit)) %>% 
  filter(weekday == 7) %>% 
  group_by(date = date(Abfahrtszeit)) %>% 
  tally()

p <- saturdays %>%
  ggplot( aes(x=date, y=n)) +
  ggtitle("Saturdays") + 
  geom_area(fill="#69b3a2", alpha=0.5) +
  geom_line(color="#69b3a2") +
  ylab("Fahrten") +
  xlab("Tag") + 
  ggtitle("Zeitverlauf der Fahrten pro Samstag") + 
  theme_ipsum()

##would put this behind an if or else condition but does not work for me :/
plotFile = "plots/KEXI_202006_202105_rides_saturdays.png"
paste("printing plot to ", plotFile)
png(plotFile, width = 1200, height = 800)  
p
dev.off()
if(interactiveMode){
  ggplotly(p)  
} 

##############
#plot day time line of all rides
#mm <- completedRides %>% 
#  mutate (minutes = minute(Abfahrtszeit) + hour(Abfahrtszeit) * 60) %>% 
#  group_by(minutes) %>% 
#  tally()

#p <- mm %>%
#  ggplot( aes(x=minutes/60, y=n)) +
#  ggtitle("Rides pro Minute") + 
#  geom_area(fill="#69b3a2", alpha=0.5) +
#  geom_line(color="#69b3a2") +
#  ylab("nr of rides per minute") +
#  theme_ipsum()
#ggplotly(p)

ridesPerInterval <- completedRides %>% 
  mutate (interval = floor( (minute(Abfahrtszeit) + hour(Abfahrtszeit) * 60) / 5)  )  %>% 
  group_by(interval) %>% 
  tally()

p <- ridesPerInterval %>%
  ggplot( aes(x=interval*5/60, y=n)) +
  ggtitle("Fahrten pro 5-Minuten-Intervall") + 
  geom_area(fill="#69b3a2", alpha=0.5) +
  geom_line(color="#69b3a2") +
  ylab("Anzahl Fahrten") +
  xlab("Stunde") +
  theme_ipsum()

##would put this behind an if or else condition but does not work for me :/
plotFile = "plots/KEXI_202006_202105_rides_daily.png"
paste("printing plot to ", plotFile)
png(plotFile, width = 1200, height = 800)  
p
dev.off()
if(interactiveMode){
  ggplotly(p)  
} 



#plot day time of saturdays
saturdays_day <- completedRides %>% 
  mutate(weekday = wday(Abfahrtszeit), interval = floor( (minute(Abfahrtszeit) + hour(Abfahrtszeit) * 60) / 5) ) %>% 
  filter(weekday == 7) %>% 
  group_by(interval) %>% 
  tally()

p <- saturdays_day %>%
  ggplot( aes(x=interval*5/60, y=n)) +
  ggtitle("SA: Fahrten pro 5-Minuten-Intervall") + 
  geom_area(fill="#69b3a2", alpha=0.5) +
  geom_line(color="#69b3a2") +
  ylab("Anzahl Fahrten") +
  xlab("Stunde") +
  theme_ipsum()


##would put this behind an if or else condition but does not work for me :/
plotFile = "plots/KEXI_202006_202105_rides_saturdays_daily.png"
paste("printing plot to ", plotFile)
png(plotFile, width = 1200, height = 800)  
p
dev.off()
if(interactiveMode){
  ggplotly(p)  
} 






#################################################################

angefragtPerInterval <- allData %>% 
  select(Angefragte.Fahrtzeit) %>% 
  mutate (interval = floor( (minute(Angefragte.Fahrtzeit) + hour(Angefragte.Fahrtzeit) * 60) / 5)  )  %>% 
  group_by(interval) %>% 
  tally() %>% 
  rename(nAngefragt = n)


#joined <- full_join(requestsPerInterval, ridesPerInterval, by="interval", suffix = c("Requests", "Abfahrt")) %>%
joined <- full_join(requestsPerInterval, ridesPerInterval, by="interval", suffix = c("Requests", "Abfahrt")) %>% 
  full_join(.,angefragtPerInterval, by = "interval") %>% 
  filter(!is.na(interval)) %>% 
  mutate(interval = as.numeric(interval)) %>% 
  replace_na(list(interval = -1000,nRequests = 0, nRides=0))

gathered <- joined %>% 
  gather(key = "variable", value = "value", -interval)

p <- gathered %>%
  ggplot( aes(x=interval * 5/60, y=value)) +
  #ggtitle("Requests pro 5 Minuten-Intervall") + 
  #geom_area(fill="#69b3a2", alpha=0.5) +
  geom_line(aes(color = variable)) +
  ylab("Anzahl pro Intervall") +
  xlab("Stunde") + 
  theme_ipsum() +
  scale_color_manual(values = c("darkgreen" , "darkred", "steelblue"))
ggplotly(p)



requests_timeDiffs <- allData %>% 
  select(Fahrtwunsch.erstellt, Angefragte.Fahrtzeit) %>% 
  mutate(diff = seconds(Angefragte.Fahrtzeit - Fahrtwunsch.erstellt)) %>% 
  filter(!is.na(diff))

mean(requests_timeDiffs$diff) /3600

hist(requests_timeDiffs$diff/3600)

rides_timeDiffs <- allData %>% 
  filter(Stornierungsgrund == "ride_completed") %>% 
  select(Fahrtwunsch.erstellt, Angefragte.Fahrtzeit) %>% 
  mutate(diff = seconds(Angefragte.Fahrtzeit - Fahrtwunsch.erstellt)) %>% 
  filter(!is.na(diff))

mean(rides_timeDiffs$diff) /3600

hist(rides_timeDiffs$diff/3600)

p <- requests_timeDiffs %>% 
  ggplot( aes(x=Fahrtwunsch.erstellt, y=Angefragte.Fahrtzeit)) +
    #ggtitle("Requests pro 5 Minuten-Intervall") + 
    #geom_area(fill="#69b3a2", alpha=0.5) +
    geom_line() 

ggplotly(p)



  







