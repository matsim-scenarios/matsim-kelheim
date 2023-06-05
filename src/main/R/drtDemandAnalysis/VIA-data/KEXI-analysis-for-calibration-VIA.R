# Title     : Analysis of KEXI request data
# Objective : get KPIs for drt demand calibration
# Created by: simei94
# Created on: 10.02.2022

library(lubridate)
library(tidyverse)
library(dplyr)
library(ggplot2)
library(plotly)
library(hrbrthemes)
library(geosphere)

#####################################################################
####################################################
### INPUT DEFINITIONS ###

# set working directory
#setwd("D:/svn/shared-svn/projects/KelRide/data/KEXI/")
setwd("C:/Users/Simon/Documents/shared-svn/projects/KelRide/data/KEXI/")

# read data
VIArides2021 <- read.csv2("VIA_Rides_202106_202201.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", na.strings="")
VIArides2022_1 <- read.csv2("VIA_Rides_202201_202210.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", na.strings="")
VIArides2022_2 <- read.csv2("VIA_Rides_202210_202212.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", na.strings="")
VIArides2023_1 <- read.csv2("VIA_Rides_202212_202303.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", na.strings="")

VIAridesAll <- union(VIArides2021, VIArides2022_1)
VIAridesAll <- union(VIAridesAll, VIArides2022_2)
VIAridesAll <- union(VIAridesAll, VIArides2023_1) %>%
  filter(!is.na(Actual.Pickup.Time))

VIAridesSince2022 <- VIAridesAll %>%
  filter(year(Actual.Pickup.Time) >= year(ymd("2022-01-01")))

datasets <- list(VIArides2021, VIArides2022_1, VIArides2022_2, VIArides2023_1, VIAridesSince2022, VIAridesAll)
names <- c("VIA_data_202106_202201","VIA_data_202201_202210","VIA_data_202210_202212","VIA_data_202212_202303","VIAdataSince2022","VIAdataAll")
i <- 1

avgValues <- setNames(data.frame(matrix(ncol = 14, nrow = 0)), c("dataset", "avgBookingsPerDay", "avgDistance_<5km[m]", "avgDistance_withoutFilter[m]", "avgTravelTime[s]",
                                                                "avgBookingsPerDayInclCompanions", "noRides1Passenger", "noRides2Passengers", "noRides3Passenger", "noRides4Passenger", "noRides5Passenger",
                                                                "noRides6Passenger", "noRides7Passenger", "noRides8Passenger"))

for(dataset in datasets) {
  print(paste0("Starting to calculate stats for dataset ",names[i]))

  # In the VIA data they differentiate between requested PU time and requested DO time. Only 450 requests do not have a requested PU time
  # Therefore the rows will get joined (otherwise it will lead to errors)
  dataset <- dataset %>%
    unite(Requested.Pickup.Time,Requested.Dropoff.Time,col="Requested.Time",sep="",na.rm = TRUE) %>%
    filter(Reason.For.Travel == "DR")

  # convert time columns
  dataset <- dataset %>% mutate(Request.Creation.Time = ymd_hms(Request.Creation.Time),
                                          Requested.Time = ymd_hms(Requested.Time),
                                          No.Show.Time = ymd_hms(No.Show.Time),
                                          Actual.Pickup.Time = ymd_hms(Actual.Pickup.Time),
                                          Actual.Dropoff.Time = ymd_hms(Actual.Dropoff.Time),
                                          Cancellation.Time = ymd_hms(Cancellation.Time),
                                          date = date(Actual.Pickup.Time),
                                          weekday = wday(date, label = TRUE)
  )

  # some entries seem to have errors (mssing Pickup time). As the total number only is 167 we just filter them for now -sm 02-2022
  # noPUTime <- dataset %>%
  #   filter(is.na(Actual.Pickup.Time))
  #
  # write.csv2(noPUTime, "VIA_Rides_202106_202201_noPUTime.csv", quote = FALSE)

  weekdayRides <- dataset %>%
    filter(weekday != "Fr",
           weekday != "Sa",
           weekday != "So",
           weekday != "Mo")

  #Possibly add a lockdown in late 2021 / early 2022 here,
  # although the "low periods" observed in the "Zeitverlauf der Fahrten pro Tag (VIA)"-plot seem be explainable through holiday times (christmas and summer)
  # 2021
  summer_holiday21 <- interval(ymd("2021-07-30"), ymd("2021-09-13"))
  autumn_holiday21 <- interval(ymd("2021-11-01"), ymd("2021-11-05"))
  holiday_bettag21 <- interval(ymd("2021-11-17"), ymd("2021-11-17"))
  holidays_christmas21 <- interval(ymd("2021-12-24"), ymd("2022-01-08"))
  # 2022
  winter_holiday22 <- interval(ymd("2022-02-28"), ymd("2022-03-04"))
  easter_holiday22 <- interval(ymd("2022-04-11"), ymd("2022-04-23"))
  holiday_himmelfahrt22 <- interval(ymd("2022-05-26"), ymd("2022-05-26"))
  pfingsten_holiday22 <- interval(ymd("2022-06-06"), ymd("2022-06-18"))
  summer_holiday22 <- interval(ymd("2022-08-01"), ymd("2022-09-12"))
  holiday_einheit22 <- interval(ymd("2022-10-03"), ymd("2022-10-03"))
  autumn_holiday22 <- interval(ymd("2022-10-31"), ymd("2022-11-04"))
  holiday_bettag22 <- interval(ymd("2022-11-16"), ymd("2022-11-16"))
  holidays_christmas22 <- interval(ymd("2022-12-24"), ymd("2023-01-07"))
  # 2023
  winter_holiday23 <- interval(ymd("2023-02-20"), ymd("2023-02-24"))
  easter_holiday23 <- interval(ymd("2023-04-03"), ymd("2023-04-15"))
  holiday_arbeit23 <- interval(ymd("2023-05-01"), ymd("2023-05-01"))
  holiday_himmelfahrt23 <- interval(ymd("2023-05-18"), ymd("2023-05-18"))
  pfingsten_holiday23 <- interval(ymd("2023-05-30"), ymd("2023-06-09"))
  summer_holiday23 <- interval(ymd("2023-07-31"), ymd("2023-09-11"))
  holiday_einheit23 <- interval(ymd("2023-10-03"), ymd("2023-10-03"))
  autumn_holiday23 <- interval(ymd("2023-10-30"), ymd("2023-11-03"))
  holiday_bettag23 <- interval(ymd("2023-11-22"), ymd("2023-11-22"))
  holidays_christmas23 <- interval(ymd("2023-12-23"), ymd("2024-01-05"))


  ridesToConsider <- weekdayRides %>%
    filter(! date %within% summer_holiday21,
           ! date %within% autumn_holiday21,
           ! date %within% holiday_bettag21,
           ! date %within% holidays_christmas21,
           ! date %within% winter_holiday22,
           ! date %within% easter_holiday22,
           ! date %within% holiday_himmelfahrt22,
           ! date %within% pfingsten_holiday22,
           ! date %within% summer_holiday22,
           ! date %within% holiday_einheit22,
           ! date %within% autumn_holiday22,
           ! date %within% holiday_bettag22,
           ! date %within% holidays_christmas22,
           ! date %within% winter_holiday23,
           ! date %within% easter_holiday23,
           ! date %within% holiday_arbeit23,
           ! date %within% holiday_himmelfahrt23,
           ! date %within% pfingsten_holiday23,
           ! date %within% summer_holiday23,
           ! date %within% holiday_einheit23,
           ! date %within% autumn_holiday23,
           ! date %within% holiday_bettag23,
           ! date %within% holidays_christmas23,
    ) %>%
    mutate( travelTime_s = seconds(Actual.Dropoff.Time - Actual.Pickup.Time)) %>%
    # The dataset appears to have one entry with Actual.Dropoff.Time < Actual.Pickup.Time, which produces a negative travelTime
    #It (Ride ID: 17036) therefore is excluded
    filter(travelTime_s > 0)

  ##########################################################################################################################################################
  #calculate Distance on an ellipsoid (the geodesic) between the calculated start and end points of each tour
  ridesToConsider <- ridesToConsider  %>%
    rowwise() %>%
    mutate(distance_m = as.double(distGeo(c(as.double(Origin.Lng), as.double(Origin.Lat)),
                                          c(as.double(Destination.Lng), as.double(Destination.Lat)))))

  ################################################################################################################################################################
  #tested the different distance-calculation functions on the geosphere package
  #result: variation is only about 1m
  # coord <- c(as.double(ridesToConsider$Kalkulierter.Abfahrtsort..lon.[1]), as.double(ridesToConsider$Kalkulierter.Abfahrtsort..lat.[1]))
  # coord2 <- c(as.double(ridesToConsider$Kalkulierter.Ankunftsort..lon.[1]), as.double(ridesToConsider$Kalkulierter.Ankunftsort..lat.[1]))
  #
  # coord
  #
  # dist <- distHaversine(coord, coord2)
  # dist2 <- distGeo(coord, coord2)
  # dist6 <- distCosine(coord, coord2)
  # dist7 <- distMeeus(coord, coord2)
  # dist8 <- distRhumb(coord, coord2)
  # dist8 <- distVincentyEllipsoid(coord, coord2)
  # dist9 <- distVincentySphere(coord, coord2)

  ############################################################################################################################################################

  #ridesLessThan10Seconds <- ridesToConsider %>%
  #  filter(travelTime_s <= 180)

  # there are 47 rides below tt=120s and 22 rides below tt=60s out of 7542 considerable rides. For three minutes, this goes up to 157 rides.
  # so for a first version, we cut everyhing below 2 minutes
  # by doing so, we increase mean travel time from 508 to 511 seconds

  # below120s <- ridesToConsider %>%
  #   filter(travelTime_s < 120)
  # below60s <- ridesToConsider %>%
  #   filter(travelTime_s < 60)
  # below180s <- ridesToConsider %>%
  #   filter(travelTime_s < 180)
  # over1500s <- ridesToConsider %>%
  #   filter(travelTime_s > 1500)
  # over1000s <- ridesToConsider %>%
  #   filter(travelTime_s > 1000)

  ridesToConsider <- ridesToConsider %>%
    filter(travelTime_s >= 120)

  #calculate avg travel time of all rides
  j <- ridesToConsider %>%
    # mutate(travelTime_s = seconds(travelTime_s)) %>%
    filter(travelTime_s < 1500)
  avgTravelTime_s <- mean(j$travelTime_s)
  avgTravelTime_s

  hist_TravelTime_s <- ggplot(j, aes(x=travelTime_s)) +
    geom_histogram() +
    labs(title=paste("Histogram of KEXI travel time for dataset", names[i]))

  plotFile = paste0("plots/",names[i],"/hist_KEXI_travel_time_s.png")
  paste0("printing plot to ", plotFile)
  ggsave(plotFile, limitsize = FALSE)


  boxplot_TravelTime_s <- ggplot(j, aes(y=travelTime_s)) +
    stat_boxplot(geom="errorbar", width=3) +
    geom_boxplot(width=5) +
    scale_y_continuous(n.breaks = 10) +
    scale_x_discrete() +
    stat_summary(fun=mean, geom="errorbar",aes(ymax=..y.., ymin=..y.., x=0),
                 width=5, colour="red") +
    labs(x="", y="travel time [s]", title=paste("Boxplot KEXI Travel Time for dataset", names[i])) +
    # labs(x="", y="travel time [s]") + #for paper only
    theme(plot.title = element_text(hjust=0.5, size=20, face="bold"), axis.text.y = element_text(size=24),
          axis.title.y = element_text(size=25, face="bold"))

  plotFile = paste0("plots/",names[i],"/boxplot_KEXI_travel_time_s.png")
  paste0("printing plot to ", plotFile)
  ggsave(plotFile, limitsize = FALSE)

  # boxplot(j$travelTime_s, main = "Boxplot KEXI Travel Time", ylab = "travel time [s]",
  #         pars = list(mar = c(5.0,5.0,5.0), boxwex = 1.5, cex.lab=1.4, cex.axis=1.4, cex.main=1.4))
  #abline(h = avgTravelTime_s - 2 * sd(j$travelTime_s), col="red",lty=2)
  #abline(h = avgTravelTime_s + 2 * sd(j$travelTime_s), col="red",lty=2)

  #calculate avg travel distance of all rides
  k <- ridesToConsider %>%
    filter(distance_m <= 5000)

  avgDistance_m <- mean(k$distance_m)
  avgDistance_m

  avgDistance_m_withoutFilter <- mean(ridesToConsider$distance_m)
  avgDistance_m_withoutFilter

  hist_distance_m <- ggplot(k, aes(x=distance_m)) +
    geom_histogram() +
    labs(title=paste("Histogram of KEXI travel distance for dataset", names[i]))

  plotFile = paste0("plots/",names[i],"/hist_KEXI_travel_distance_m.png")
  paste0("printing plot to ", plotFile)
  ggsave(plotFile, limitsize = FALSE)

  boxplot_distance_m <- ggplot(k, aes(y=distance_m)) +
    stat_boxplot(geom="errorbar", width=3) +
    geom_boxplot(width=5) +
    scale_y_continuous(n.breaks = 10) +
    scale_x_discrete() +
    stat_summary(fun=mean, geom="errorbar",aes(ymax=..y.., ymin=..y.., x=0),
                 width=5, colour="red") +
    labs(x="", y="travel distance [m]", title=paste("Boxplot KEXI Travel Distance for dataset", names[i])) +
    # labs(x="", y="travel distance [m]") + #for paper only
    theme(plot.title = element_text(hjust=0.5, size=20, face="bold"), axis.text.y = element_text(size=24),
          axis.title.y = element_text(size=25, face="bold"))

  plotFile = paste0("plots/",names[i],"/boxplot_KEXI_travel_distance_m.png")
  paste0("printing plot to ", plotFile)
  ggsave(plotFile, limitsize = FALSE)

  # boxplot(k$distance_m, main = "Boxplot KEXI Travel Distance", ylab = "travel distance [m]")
  # abline(h = avgDistance_m - 2 * sd(k$distance_m), col="red",lty=2)
  # abline(h = avgDistance_m + 2 * sd(k$distance_m), col="red",lty=2)

  ############################################################################################################################################################

  #calculate nr of passengers bins
  distr <-ridesToConsider %>%
    group_by(Number.of.Passengers) %>%
    summarize(n =n())

  passengerDistribution <- data.frame(c(1,2,3,4,5,6,7,8),c(0,0,0,0,0,0,0,0))
  colnames(passengerDistribution) <- c("Number.of.Passengers","n")

  for ( j in 1:8 ) {
    ifelse(any(distr$Number.of.Passengers==j), passengerDistribution$n[j]<-distr$n[j],"no changes")
  }

  passengersPerDay <- ridesToConsider %>%
    group_by(date) %>%
    summarise(noPassengers = sum(Number.of.Passengers))

  #calculate avg bookings per day
  dailyValues <- ridesToConsider %>%
    group_by(date) %>%
    summarise(noBookings = n()) %>%
    left_join(passengersPerDay, by="date")


  avgBookings <- mean(dailyValues$noBookings)
  avgBookings

  avgBookingsInclCompanions <- mean(dailyValues$noPassengers)
  avgBookingsInclCompanions

  #save avg values into df
  avgValuesDataset <- data.frame(names[i],avgBookings,avgDistance_m,avgDistance_m_withoutFilter,avgTravelTime_s,avgBookingsInclCompanions,
                                 as.integer(passengerDistribution$n[1]),as.integer(passengerDistribution$n[2]),as.integer(passengerDistribution$n[3]),as.integer(passengerDistribution$n[4]),
                                 as.integer(passengerDistribution$n[5]),as.integer(passengerDistribution$n[6]),as.integer(passengerDistribution$n[7]),as.integer(passengerDistribution$n[8]))
  names(avgValuesDataset) <- names(avgValues)
  avgValues <- rbind(avgValues,avgValuesDataset)

  boxplot_daily_bookings <- ggplot(dailyValues, aes(y=noBookings)) +
    stat_boxplot(geom="errorbar", width=3) +
    geom_boxplot(width=5) +
    scale_y_continuous(n.breaks = 8) +
    scale_x_discrete() +
    stat_summary(fun=mean, geom="errorbar",aes(ymax=..y.., ymin=..y.., x=0),
                 width=5, colour="red") +
    labs(x="", y="bookings", title=paste("Boxplot KEXI bookings per day for dataset", names[i])) +
    # labs(x="", y="travel distance [m]") + #for paper only
    theme(plot.title = element_text(hjust=0.5, size=20, face="bold"), axis.text.y = element_text(size=24),
          axis.title.y = element_text(size=25, face="bold"))

  plotFile = paste0("plots/",names[i],"/boxplot_KEXI_daily_bookings.png")
  paste0("printing plot to ", plotFile)
  ggsave(plotFile, limitsize = FALSE)

  #a typical day here can be seen as a day with no of bookings close to the average no of bookings (119)
  # typicalDays <- filter(dailyValues, between(n, avgBookings - 3, avgBookings + 3))

  # #5 days are chosen as typical references
  # typicalDay_jul <- ymd("2021-07-21")
  # typicalDay_sep <- ymd("2021-09-15")
  # typicalDay_oct <- ymd("2021-10-12")
  # typicalDay_dec <- ymd("2021-12-01")
  # typicalDay_jan <- ymd("2022-01-27")
  #
  # typicalDaysList <- list(typicalDay_jul, typicalDay_sep, typicalDay_oct, typicalDay_dec, typicalDay_jan)
  #
  # # this is so ugly and hard coded right now, as you have to change the day you want to plot
  # #but a for loop for this just does not seem to work -sm apr22
  # typicalDayBookingsPerInterval <- BookingsToConsider %>%
  #   filter(date == typicalDay_jan) %>%
  #   mutate (interval = floor( (minute(Actual.Pickup.Time) + hour(Actual.Pickup.Time) * 60) / 5)  )  %>%
  #   group_by(interval) %>%
  #   tally()
  #
  # p <- typicalDayBookingsPerInterval %>%
  #   ggplot( aes(x=interval*5/60, y=n)) +
  #   ggtitle(paste("Fahrten pro 5-Minuten-Intervall (VIA): typischer Tag im ", month(typicalDay_jan, label=TRUE))) +
  #   geom_area(fill="#69b3a2", alpha=0.5) +
  #   geom_line(color="#69b3a2") +
  #   ylab("Anzahl Fahrten") +
  #   xlab("Stunde") +
  #   theme_ipsum()
  #
  # plotFile = paste("typicalDays/KEXI_bookings_VIA_", month(typicalDay_jan, label=TRUE), ".png")
  # paste("printing plot to ", plotFile)
  # png(plotFile, width = 1200, height = 800)
  # p
  # dev.off()
  # ggplotly(p)

  # boxplot(dailyValues$noBookings, main = "Boxplot KEXI bookings per day", ylab = "bookings")
  # abline(h = avgBookings - 2 * sd(dailyValues$noBookings), col="red",lty=2)
  # abline(h = avgBookings + 2 * sd(dailyValues$noBookings), col="red",lty=2)


  i <- i + 1
}

csvFileName <- paste0("avg_params_kexi.csv")
write.table(avgValues,csvFileName,quote=FALSE, row.names=FALSE, dec=".", sep=";")
print(paste0("avg values for all analyzed datasets were printed to ",csvFileName))


