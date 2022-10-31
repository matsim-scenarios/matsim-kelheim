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
setwd("C:/Users/Simon/Documents/shared-svn/projects/KelRide/data/KEXI/")

# read data
VIAdata2021 <- read.csv2("Via_data_2022-02-08/Data_request_TUB_for_Kelheim-Actual_Data-VIA_edited.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8")
VIAdata2022 <- read.csv2("Via_data_2022-10-10/Data_request_TUB_for_Kelheim-Actual_Data-VIA_Feb_to_Oct_2022_edited_cleaned.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8")

VIAdata2021 <- VIAdata2021 %>%
  mutate(Reason.For.Travel = "DR")

VIAdataAll <- union(VIAdata2021, VIAdata2022)

datasets <- list(VIAdata2021, VIAdata2022, VIAdataAll)
names <- c("VIAdata2021","VIAdata2022","VIAdataAll")
i <- 1

print("Starting to print different plots!")

# In the VIA data they differentiate between requested PU time and requested DO time. Only some requests do not have a requested PU time
# Therefore the rows will get joined (otherwise it will lead to errors)
for(dataset in datasets) {
  dataset <- dataset %>%
    unite(Requested.PU.time,Requested.DO.time,col="Requested.time",sep="")

  # convert time columns + determine weekday for every request
  dataset <- dataset %>%
    mutate(Ride.request.time = ymd_hms(Ride.request.time),
           Requested.time = ymd_hms(Requested.time),
           No.show.time = ymd_hms(No.show.time),
           Actual.PU.time = ymd_hms(Actual.PU.time),
           Actual.DO.time = ymd_hms(Actual.DO.time),
           Cancellation.time = ymd_hms(Cancellation.time)
    ) %>%
    mutate(weekday = wday(Ride.request.time, label = TRUE))

  #####################################################################
  ####################################################
  ### SCRIPT ###

  ####################################################
  ## PLOT ALL REQUESTS ##

  requests <- dataset %>%
    select(Ride.request.time) %>%
    mutate(date = date(Ride.request.time))

  reqProTag <- requests %>%
    group_by( date = date(Ride.request.time)) %>%
    tally()

  ###########
  # plot time line
  p <- ggplot(data=reqProTag) +
    geom_line(mapping=aes(x=date, y=n), col="#69b3a2") +
    geom_area(mapping=aes(x=date, y=n), fill="#69b3a2", alpha=0.5) +
    labs(x="Tag",y="Requests", title="Zeitverlauf der Anfragen pro Tag (VIA)") +
    scale_x_date(breaks = "3 month")

  
  plotFile = paste0("plots/",names[i],"/KEXI_requests_VIA.png")
  paste0("printing plot to ", plotFile)
  ggsave(plotFile, limitsize = FALSE)
  p

  ############
  #group by weekday
  reqProWochentag <- reqProTag %>%
    group_by( weekday = wday(date, label = TRUE )) %>%
    filter(!is.na(weekday)) %>%
    summarise(avg = mean(n))
  #plot avg nr of requests per weekday
  p <- ggplot(data=reqProWochentag) +
    geom_bar(mapping=aes(x=weekday, y=avg), stat="identity") +
    labs(x="Wochentag",y="Durchschn. Anzahl Requests", title="Durchschn. Anzahl Requests pro Wochentag (VIA)")

  
  plotFile = paste0("plots/",names[i],"/KEXI_requests_weekdays_VIA.png")
  paste0("printing plot to ", plotFile)
  ggsave(plotFile, limitsize = FALSE)
  p


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

  p <- ggplot(data=requestsPerInterval) +
    geom_line(mapping=aes(x=interval * 5/60, y=n), col="#69b3a2") +
    geom_area(mapping=aes(x=interval * 5/60, y=n), fill="#69b3a2", alpha=0.5) +
    labs(x="Stunde", y="Anzahl Requests pro Intervall", title="Requests pro 5 Minuten-Intervall (VIA)")

  
  plotFile = paste0("plots/",names[i],"/KEXI_requests_daily_VIA.png")
  paste0("printing plot to ", plotFile)
  ggsave(plotFile, limitsize = FALSE)
  p


  ####################################################
  ## PLOT COMPLETED RIDES ##

  # filter and prepare data:  let us use Actual.PU.time as core time stamp
  completedRides <- dataset %>%
    filter(Status == "Completed") %>%
    select(Actual.PU.time)

  #group per day
  ridesProTag <- completedRides %>%
    group_by(date = date(Actual.PU.time)) %>%
    tally()

  summer_holiday <- interval(ymd("2021-07-30"), ymd("2021-09-13"))
  autumn_holiday <- interval(ymd("2021-11-01"), ymd("2021-11-05"))
  holiday_bettag <- interval(ymd("2021-11-17"), ymd("2021-11-17"))
  holidays_christmas <- interval(ymd("2021-12-24"), ymd("2022-01-08"))

  daysToConsider <- ridesProTag %>%
    filter(! date %within% summer_holiday,
           ! date %within% autumn_holiday,
           ! date %within% holiday_bettag,
           ! date %within% holidays_christmas,
    )

  # plot time line
  p <- ggplot(data=ridesProTag) +
    geom_line(mapping=aes(x=date, y=n), col="#69b3a2") +
    geom_area(mapping=aes(x=date, y=n), fill="#69b3a2", alpha=0.5) +
    labs(x="Tag", y="Fahrten", title="Zeitverlauf der Fahrten pro Tag (VIA)") +
    scale_x_date(breaks = "3 month")

  plotFile = paste0("plots/",names[i],"/KEXI_rides_VIA.png")
  paste0("printing plot to ", plotFile)
  ggsave(plotFile, limitsize = FALSE)
  p



  # for paper only
  # p <- ggplot(ridesProTag, aes(x=date, y=n)) +
  #   geom_line(color="seagreen4", size=1) +
  #   geom_area(fill="seagreen4", alpha=0.6) +
  #   labs(y="rides", x="day") + #for paper only
  #   theme(axis.text.y = element_text(size=39), axis.title.y = element_text(size=40, face="bold"),
  #         axis.text.x = element_text(size=34), axis.title.x = element_text(size=40, face="bold"))
  #
  #
  #
  # plotFile = "C:/Users/Simon/Desktop/wd/2022-09-27/KEXI_202106_202201_rides_VIA.png"
  # paste0("printing plot to ", plotFile)
  # png(plotFile, width = 2400, height = 800)
  # p
  # dev.off()
  # if(interactiveMode){
  #   ggplotly(p)
  # }


  ################
  #group by weekday
  ridesProWochentag <- ridesProTag %>%
    group_by( weekday = wday(date, label = TRUE) ) %>%
    filter(!is.na(weekday)) %>%
    summarise(avg = mean(n))
  #plot avg nr of requests per weekday
  p <- ggplot(data=ridesProWochentag) +
    geom_bar(mapping=aes(x=weekday, y=avg), stat="identity") +
    labs(x="Tag", y="Durchschn. Anzahl Fahrten", title="Durchschn. Anzahl Fahrten pro Wochentag (VIA)")

  
  plotFile = paste0("plots/",names[i],"/KEXI_rides_weekdays_VIA.png")
  paste0("printing plot to ", plotFile)
  ggsave(plotFile, limitsize = FALSE)
  p

  ################
  # PLOT Saturday RIDES
  saturdays <- completedRides %>%
    mutate(weekday = wday(Actual.PU.time)) %>%
    filter(weekday == 7) %>%
    group_by(date = date(Actual.PU.time)) %>%
    tally()

  p <- ggplot(data=saturdays) +
    geom_line(mapping=aes(x=date, y=n), col="#69b3a2") +
    geom_area(mapping=aes(x=date, y=n), fill="#69b3a2", alpha=0.5) +
    labs(x="Tag",y="Fahrten", title="Zeitverlauf der Fahrten pro Samstag (VIA)") +
    scale_x_date(breaks = "3 month")

  
  plotFile = paste0("plots/",names[i],"/KEXI_rides_saturdays_VIA.png")
  paste0("printing plot to ", plotFile)
  ggsave(plotFile, limitsize = FALSE)
  p

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
  write.csv2(class.df,paste0("KEXI_",names[i],"_rides_daily_VIA.csv"),quote=FALSE,row.names=FALSE)

  p <- ggplot(data=ridesPerInterval) +
    geom_line(mapping=aes(x=interval*5/60, y=n), col="#69b3a2") +
    geom_area(mapping=aes(x=interval*5/60, y=n), fill="#69b3a2", alpha=0.5) +
    labs(x="Stunde",y="Anzahl Fahrten", title="Fahrten pro 5-Minuten-Intervall (VIA)")

  
  plotFile = paste0("plots/",names[i],"/KEXI_rides_daily_VIA.png")
  paste0("printing plot to ", plotFile)
  ggsave(plotFile, limitsize = FALSE)
  p



  #plot day time of saturdays
  saturdays_day <- completedRides %>%
    mutate(weekday = wday(Actual.PU.time), interval = floor( (minute(Actual.PU.time) + hour(Actual.PU.time) * 60) / 5) ) %>%
    filter(weekday == 7) %>%
    group_by(interval) %>%
    tally()

  p <- ggplot(data=saturdays_day) +
    geom_line(mapping=aes(x=interval*5/60, y=n), col="#69b3a2") +
    geom_area(mapping=aes(x=interval*5/60, y=n), fill="#69b3a2", alpha=0.5) +
    labs(x="Stunde",y="Anzahl Fahrten", title="SA: Fahrten pro 5-Minuten-Intervall (VIA)")

  
  plotFile = paste0("plots/",names[i],"/KEXI_rides_saturdays_daily_VIA.png")
  paste0("printing plot to ", plotFile)
  ggsave(plotFile, limitsize = FALSE)
  p

  #################################################################

  angefragtPerInterval <- dataset %>%
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

  p <- ggplot(data=gathered) +
    geom_line(mapping=aes(x=interval * 5/60, y=value), col=gathered$variable) +
    geom_area(mapping=aes(x=interval * 5/60, y=value), fill="#69b3a2", alpha=0.5) +
    labs(x="Stunde",y="Anzahl pro Intervall") +
    scale_color_manual(values = c("darkgreen" , "darkred", "steelblue"))



  requests_timeDiffs <- dataset %>%
    select(Ride.request.time, Requested.time) %>%
    mutate(diff = seconds(Requested.time - Ride.request.time)) %>%
    filter(!is.na(diff))

  mean(requests_timeDiffs$diff) /3600

  hist(requests_timeDiffs$diff/3600)

  rides_timeDiffs <- dataset %>%
    filter(Status == "Completed") %>%
    select(Ride.request.time, Requested.time) %>%
    mutate(diff = seconds(Requested.time - Ride.request.time)) %>%
    filter(!is.na(diff))

  mean(rides_timeDiffs$diff) /3600

  hist(rides_timeDiffs$diff/3600)

  p <- ggplot(data=requests_timeDiffs) +
    geom_line(mapping=aes(x=Ride.request.time, y=Requested.time))
  ggplotly(p)


  i <- i + 1
}

print("Printing of plots has finished!")



  







