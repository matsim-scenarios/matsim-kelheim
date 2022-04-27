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
setwd("/Users/tomkelouisa/Documents/VSP/Kehlheimfiles")

# read data
allData <- read.csv2("Data_request_TUB_for_Kelheim-Actual_Data-VIA_edited.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8")

# In the VIA data they differentiate between requested PU time and requested DO time. Only 450 requests do not have a requested PU time
# Therefore the rows will get joined (otherwise it will lead to errors)
allData <- allData %>%
  unite(Requested.PU.time,Requested.DO.time,col="Requested.time",sep="")

# convert time columns + determine weekday for every request
allData <- allData %>% 
  mutate(Ride.request.time = ymd_hms(Ride.request.time),
         Requested.time = ymd_hms(Requested.time),
         No.show.time = ymd_hms(No.show.time),
         Actual.PU.time = ymd_hms(Actual.PU.time),
         Actual.DO.time = ymd_hms(Actual.DO.time),
         Cancellation.time = ymd_hms(Cancellation.time)
  ) %>%
  mutate(weekday = wday(Ride.request.time, label = TRUE))


# if TRUE, then interactive plots are produced. if FALSE, plots are dumped out as pngs to 'plots' folder
interactiveMode = TRUE

#####################################################################
####################################################
### SCRIPT ###

####################################################
## PLOT ALL REQUESTS ##

requests <- allData %>% 
  select(Ride.request.time) %>%
  mutate(date = date(Ride.request.time))

reqProTag <- requests %>% 
  group_by( date = date(Ride.request.time)) %>%
  tally()

###########
# plot time line
p <- reqProTag %>%
  ggplot( aes(x=date, y=n)) +
  ggtitle("Zeitverlauf der Anfragen pro Tag (VIA)") +
  geom_area(fill="#69b3a2", alpha=0.5) +
  geom_line(color="#69b3a2") +
  ylab("Requests") +
  xlab("Tag") + 
  theme_ipsum()

##would put this behind an if or else condition but does not work for me :/
plotFile = "plots/KEXI_202106_202201_requests_VIA.png"
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
  ggtitle("Durchschn. Anzahl Requests pro Wochentag (VIA)") +
  geom_bar(color="#69b3a2", stat = "identity") +
  ylab("Durchschn. Anzahl Requests") +
  xlab("Wochentag") +
  theme_ipsum()

##would put this behind an if or else condition but does not work for me :/
plotFile = "plots/KEXI_202106_202201_requests_weekdays_VIA.png"
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
  mutate (interval = floor( (minute(Ride.request.time) + hour(Ride.request.time) * 60 ) /5) )  %>%
  filter(!is.na(interval)) %>% 
  group_by(interval) %>% 
  tally()

p <- requestsPerInterval %>%
  ggplot( aes(x=interval * 5/60, y=n)) +
  ggtitle("Requests pro 5 Minuten-Intervall (VIA)") +
  geom_area(fill="#69b3a2", alpha=0.5) +
  geom_line(color="#69b3a2") +
  ylab("Anzahl Requests pro Intervall") +
  xlab("Stunde") + 
  theme_ipsum()

##would put this behind an if or else condition but does not work for me :/
plotFile = "plots/KEXI_202106_202201_requests_daily_VIA.png"
paste("printing plot to ", plotFile)
png(plotFile, width = 1200, height = 800)  
p
dev.off()
if(interactiveMode){
  ggplotly(p)  
} 


####################################################
## PLOT COMPLETED RIDES ##

# filter and prepare data:  let us use Actual.PU.time as core time stamp
completedRides <- allData %>% 
  filter(Status == "Completed") %>%
  select(Actual.PU.time)

#group per day
ridesProTag <- completedRides %>% 
  group_by(date = date(Actual.PU.time)) %>%
  tally()
  
# plot time line
p <- ridesProTag %>%
  ggplot( aes(x=date, y=n)) +
  geom_area(fill="#69b3a2", alpha=0.5) +
  geom_line(color="#69b3a2") +
  ylab("Fahrten") +
  xlab("Tag") + 
  ggtitle("Zeitverlauf der Fahrten pro Tag (VIA)") +
  theme_ipsum()

##would put this behind an if or else condition but does not work for me :/
plotFile = "plots/KEXI_202106_202201_rides_VIA.png"
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
  ggtitle("Durchschn. Anzahl Fahrten pro Wochentag (VIA)") +
  theme_ipsum()

##would put this behind an if or else condition but does not work for me :/
plotFile = "plots/KEXI_202106_202201_rides_weekdays_VIA.png"
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
  mutate(weekday = wday(Actual.PU.time)) %>%
  filter(weekday == 7) %>% 
  group_by(date = date(Actual.PU.time)) %>%
  tally()

p <- saturdays %>%
  ggplot( aes(x=date, y=n)) +
  ggtitle("Saturdays (VIA)") +
  geom_area(fill="#69b3a2", alpha=0.5) +
  geom_line(color="#69b3a2") +
  ylab("Fahrten") +
  xlab("Tag") + 
  ggtitle("Zeitverlauf der Fahrten pro Samstag (VIA)") +
  theme_ipsum()

##would put this behind an if or else condition but does not work for me :/
plotFile = "plots/KEXI_202106_202201_rides_saturdays_VIA.png"
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
  mutate (interval = floor( (minute(Actual.PU.time) + hour(Actual.PU.time) * 60) / 5)  )  %>%
  group_by(interval) %>% 
  tally()
# Write File for Dashboard for " Real Demand Time Distribution" plot
ridesPerIntervals <- ridesPerInterval %>%
                      mutate(interval5 = format(round(interval*5/60, 2), nsmall = 2))
class.df <- data.frame(ridesPerIntervals$interval5,ridesPerIntervals$n, stringsAsFactors = FALSE)
write.csv2(class.df,"KEXI_202106_202201_rides_daily_VIA.csv",quote=FALSE,row.names=FALSE)

p <- ridesPerInterval %>%
  ggplot( aes(x=interval*5/60, y=n)) +
  ggtitle("Fahrten pro 5-Minuten-Intervall (VIA)") +
  geom_area(fill="#69b3a2", alpha=0.5) +
  geom_line(color="#69b3a2") +
  ylab("Anzahl Fahrten") +
  xlab("Stunde") +
  theme_ipsum()

##would put this behind an if or else condition but does not work for me :/
plotFile = "plots/KEXI_202106_202201_rides_daily_VIA.png"
paste("printing plot to ", plotFile)
png(plotFile, width = 1200, height = 800)  
p
dev.off()
if(interactiveMode){
  ggplotly(p)  
} 



#plot day time of saturdays
saturdays_day <- completedRides %>% 
  mutate(weekday = wday(Actual.PU.time), interval = floor( (minute(Actual.PU.time) + hour(Actual.PU.time) * 60) / 5) ) %>%
  filter(weekday == 7) %>% 
  group_by(interval) %>% 
  tally()

p <- saturdays_day %>%
  ggplot( aes(x=interval*5/60, y=n)) +
  ggtitle("SA: Fahrten pro 5-Minuten-Intervall (VIA)") +
  geom_area(fill="#69b3a2", alpha=0.5) +
  geom_line(color="#69b3a2") +
  ylab("Anzahl Fahrten") +
  xlab("Stunde") +
  theme_ipsum()


##would put this behind an if or else condition but does not work for me :/
plotFile = "plots/KEXI_202106_202201_rides_saturdays_daily_VIA.png"
paste("printing plot to ", plotFile)
png(plotFile, width = 1200, height = 800)  
p
dev.off()
if(interactiveMode){
  ggplotly(p)  
} 






#################################################################

angefragtPerInterval <- allData %>% 
  select(Requested.time) %>%
  mutate (interval = floor( (minute(Requested.time) + hour(Requested.time) * 60) / 5)  )  %>%
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
  select(Ride.request.time, Requested.time) %>%
  mutate(diff = seconds(Requested.time - Ride.request.time)) %>%
  filter(!is.na(diff))

mean(requests_timeDiffs$diff) /3600

hist(requests_timeDiffs$diff/3600)

rides_timeDiffs <- allData %>% 
  filter(Status == "Completed") %>%
  select(Ride.request.time, Requested.time) %>%
  mutate(diff = seconds(Requested.time - Ride.request.time)) %>%
  filter(!is.na(diff))

mean(rides_timeDiffs$diff) /3600

hist(rides_timeDiffs$diff/3600)

p <- requests_timeDiffs %>% 
  ggplot( aes(x=Ride.request.time, y=Requested.time)) +
    #ggtitle("Requests pro 5 Minuten-Intervall") + 
    #geom_area(fill="#69b3a2", alpha=0.5) +
    geom_line() 

ggplotly(p)



  







