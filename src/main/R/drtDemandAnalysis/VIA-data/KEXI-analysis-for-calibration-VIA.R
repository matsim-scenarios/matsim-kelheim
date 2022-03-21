# Title     : Analysis of KEXI request data
# Objective : get KPIs for drt demand calibration
# Created by: Simon
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
allRides <- read.csv2("VIA_Rides_202106_202201.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8")

# In the VIA data they differentiate between requested PU time and requested DO time. Only 450 requests do not have a requested PU time
# Therefore the rows will get joined (otherwise it will lead to errors)
allRides <- allRides %>%
  unite(Requested.PU.time,Requested.DO.time,col="Requested.time",sep="")

# convert time columns
allRides <- allRides %>% mutate(Ride.request.time = ymd_hms(Ride.request.time),
                                Requested.time = ymd_hms(Requested.time),
                                No.show.time = ymd_hms(No.show.time),
                                Actual.PU.time = ymd_hms(Actual.PU.time),
                                Actual.DO.time = ymd_hms(Actual.DO.time),
                                Cancellation.time = ymd_hms(Cancellation.time),
                                date = date(Actual.PU.time),
                                weekday = wday(date, label = TRUE)
  )

# some entries seem to have errors (mssing Pickup time). As the total number only is 167 we just filter them for now -sm 02-2022
# noPUTime <- allRides %>%
#   filter(is.na(Actual.PU.time))
#
# write.csv2(noPUTime, "VIA_Rides_202106_202201_noPUTime.csv", quote = FALSE)

allRides <- allRides %>%
  filter(! is.na(Actual.PU.time))

weekdayRides <- allRides %>%
  filter(weekday != "Fr",
         weekday != "Sa",
         weekday != "So",
         weekday != "Mo")

#Possibly add a lockdown in late 2021 / early 2022 here,
# although the "low periods" observed in the "Zeitverlauf der Fahrten pro Tag (VIA)"-plot seem be explainable through holiday times (christmas and summer)
summer_holiday <- interval(ymd("2021-07-30"), ymd("2021-09-13"))
autumn_holiday <- interval(ymd("2021-11-01"), ymd("2021-11-05"))
holiday_bettag <- interval(ymd("2021-11-17"), ymd("2021-11-17"))
holidays_christmas <- interval(ymd("2021-12-24"), ymd("2022-01-08"))

ridesToConsider <- weekdayRides %>%
  filter(! date %within% summer_holiday,
         ! date %within% autumn_holiday,
         ! date %within% holiday_bettag,
         ! date %within% holidays_christmas,
  ) %>%
  mutate( travelTime_s = Actual.DO.time - Actual.PU.time) %>%
  # The dataset appears to have one entry with Actual.DO.time < Actual.PU.time, which produces a negative travelTime
  #It (Ride ID: 17036) therefore is excluded
  filter(travelTime_s > 0)

##########################################################################################################################################################
#calculate Distance on an ellipsoid (the geodesic) between the calculated start and end points of each tour
ridesToConsider <- ridesToConsider  %>%
  rowwise() %>%
  mutate(distance_m = as.double(distGeo(c(as.double(Origin.Longitude), as.double(Origin.latitude)),
                                              c(as.double(Destination.Longitude), as.double(Destination.latitude)))))

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

j <- ridesToConsider %>%
  mutate(travelTime_s = seconds(travelTime_s))
hist(j$travelTime_s, plot = TRUE)
boxplot(j$travelTime_s)
avgTravelTime_s <- mean(ridesToConsider$travelTime_s)
avgTravelTime_s

hist(j$distance_m, plot = TRUE)
boxplot(j$distance_m)

avgDistance_m <- mean(ridesToConsider$distance_m)
avgDistance_m

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
  mutate(travelTime_s = seconds(travelTime_s)) %>%
  filter(travelTime_s < 1500)
avgTravelTime_s <- mean(j$travelTime_s)
avgTravelTime_s

hist(j$travelTime_s, plot = TRUE)
boxplot(j$travelTime_s, main = "Boxplot KEXI Travel Time", ylab = "travel time [s]")
abline(h = avgTravelTime_s - 2 * sd(j$travelTime_s), col="red",lty=2)
abline(h = avgTravelTime_s + 2 * sd(j$travelTime_s), col="red",lty=2)

k <- ridesToConsider %>%
  filter(distance_m <= 5000)

avgDistance_m <- mean(k$distance_m)
avgDistance_m
hist(k$distance_m, plot = TRUE)
boxplot(k$distance_m, main = "Boxplot KEXI Travel Distance", ylab = "travel distance [m]")
abline(h = avgDistance_m - 2 * sd(k$distance_m), col="red",lty=2)
abline(h = avgDistance_m + 2 * sd(k$distance_m), col="red",lty=2)

############################################################################################################################################################

#calculate avg rides per day
ridesPerDay <- ridesToConsider %>%
  group_by(date) %>%
  tally()


avgRides <- mean(ridesPerDay$n)
avgRides

#a typical day here can be seen as a day with no of rides close to the average no of rides (119)
typicalDays <- filter(ridesPerDay, between(n, avgRides - 3, avgRides + 3))

boxplot(ridesPerDay$n, main = "Boxplot KEXI Rides per day", ylab = "rides")
abline(h = avgRides - 2 * sd(ridesPerDay$n), col="red",lty=2)
abline(h = avgRides + 2 * sd(ridesPerDay$n), col="red",lty=2)


