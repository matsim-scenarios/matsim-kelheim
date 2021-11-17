# Title     : TODO
# Objective : get KPIs for drt demand calibration
# Created by: Tilmann
# Created on: 16.11.2021

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
setwd("D:/svn/shared-svn/projects/KelRide/data/KEXI/")

# read data
allRides <- read.csv2("IOKI_Rides_202006_202105.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8")

# convert time columns
allRides <- allRides %>% mutate(Fahrtwunsch.erstellt = dmy_hms(Fahrtwunsch.erstellt),
         Angefragte.Fahrtzeit = dmy_hms(Angefragte.Fahrtzeit),
         Kalkulierte.Abfahrtszeit = dmy_hms(Kalkulierte.Abfahrtszeit),
         Kalkulierte.Ankunftszeit = dmy_hms(Kalkulierte.Ankunftszeit),
         Abfahrtszeit = dmy_hms(Abfahrtszeit),
         Ankunftszeit = dmy_hms(Ankunftszeit),
         Stornierungszeit = dmy_hms(Stornierungszeit),
         date = date(Abfahrtszeit),
         weekday = wday(date, label = TRUE)
  )

weekdayRides <- allRides %>%
  filter(weekday != "Fr",
         weekday != "Sa",
         weekday != "So",
         weekday != "Mo")

growingPhase <- interval(ymd("2020-01-01"), ymd("2020-09-30"))
autumn_holiday <- interval(ymd("2020-11-02"), ymd("2020-11-06"))
holiday_bettag <- interval(ymd("2020-11-18"), ymd("2020-11-18"))
holidays_christmas <- interval(ymd("2020-12-21"), ymd("2021-01-08"))
holidays_easter <- interval(ymd("2021-03-29"), ymd("2021-04-09"))
holiday_himmelfahrt <- interval(ymd("2021-05-13"), ymd("2021-05-14"))
holiday_pfingsten <- interval(ymd("2021-05-24"), ymd("2021-06-04"))

ridesToConsider <- weekdayRides %>%
  filter(! date %within% growingPhase,
         ! date %within% autumn_holiday,
         ! date %within% holiday_bettag,
         ! date %within% holidays_christmas,
         ! date %within% holidays_easter,
         ! date %within% holiday_himmelfahrt,
         ! date %within% holiday_pfingsten) %>%
  mutate( travelTime_s = Ankunftszeit - Abfahrtszeit)

############################################################################################################################################################
### it seems like we have corrupt data sets. for example, there are rides from that officially took less than 10 seconds, travelling from one station to a different station.
### have a look at 2020-12-17 16:11:46 and 2020-12-17 16:12:56 for example (requested by the same user actually). They point exactly in opposite directions and were booked by an admin.
### The travel times are obviously wrong. It is unclear, how we should deal with these data sets. As the given departure times are also very close to each other, it seems likely, that
### those rides were never really undertaken. So we should exclude these data sets (which obviously has an influence on the nr of rides per day).

j <- ridesToConsider %>%
  mutate(travelTime_s = seconds(travelTime_s))
hist(j$travelTime_s, plot = TRUE)
boxplot(j$travelTime_s)
avgTravelTime_s <- mean(ridesToConsider$travelTime_s)
avgTravelTime_s
#ridesLessThan10Seconds <- ridesToConsider %>%
#  filter(travelTime_s <= 180)

# there are 171 rides below tt=120s and 108 rides below tt=60s out of 7764 considerable rides. For three minutes, this goes up to 471 rides.
# so for a first version, we cut everyhing below 2 minutes
# by doing so, we increase mean travel time from 436 to 447 seconds

ridesToConsider <- ridesToConsider %>%
  filter(travelTime_s >= 120)

j <- ridesToConsider %>%
  mutate(travelTime_s = seconds(travelTime_s))
hist(j$travelTime_s, plot = TRUE)
boxplot(j$travelTime_s)
avgTravelTime_s <- mean(ridesToConsider$travelTime_s)
avgTravelTime_s

############################################################################################################################################################

ridesPerDay <- ridesToConsider %>%
  group_by(date) %>%
  tally()

avgRides <- mean(ridesPerDay$n)


