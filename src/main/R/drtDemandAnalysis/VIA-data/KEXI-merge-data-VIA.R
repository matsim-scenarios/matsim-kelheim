library(lubridate)
library(tidyverse)
library(dplyr)

# set working directory
setwd("C:/Users/Simon/Documents/shared-svn/projects/KelRide/data/KEXI/")

# read data
VIAdata2021 <- read.csv2("Via_data_2022-02-08/Data_request_TUB_for_Kelheim-Actual_Data-VIA_edited.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8")
VIAdata2022 <- read.csv2("Via_data_2022-10-10/Data_request_TUB_for_Kelheim-Actual_Data-VIA_Feb_to_Oct_2022_edited.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", sep=",")

#due to different columns names and some added columns we have to make the datasets compatible to each other
overlap <-interval(ymd("2022-01-24"), ymd("2022-01-29"))

VIAdata2022 <- VIAdata2022 %>%
  rename("Ride.request.time" = Request.Creation.Time,
         "Status" = Request.Status,
         "Booking.method" = Booking.Method,
  "Number.of.passengers" = Number.of.Passengers,
         "Requested.PU.time" = Requested.Pickup.Time,
         "Requested.DO.time" = Requested.Dropoff.Time,
         "Origin.latitude" = Origin.Lat,
         "Origin.Longitude" = Origin.Lng,
         "Destination.latitude" = Destination.Lat,
         "Destination.Longitude" = Destination.Lng,
  "Cancellation.time" = Cancellation.Time,
         "No.show.time" = No.Show.Time,
         "Actual.PU.time" = Actual.Pickup.Time,
         "Actual.DO.time" = Actual.Dropoff.Time) %>%
  mutate(Ride.ID = NA,
         Reason.For.Travel = ifelse(Reason.For.Travel != "AV","DR","AV"),
  Ride.request.time = ymd_hms(Ride.request.time)) %>%
  filter(! date(Ride.request.time) %within% overlap)

write.csv2(VIAdata2022, "Via_data_2022-10-10/Data_request_TUB_for_Kelheim-Actual_Data-VIA_Feb_to_Oct_2022_edited_cleaned.csv", quote = FALSE, row.names = FALSE)

VIAdata2021 <- VIAdata2021 %>%
  mutate(Reason.For.Travel = "DR",
         Ride.request.time = ymd_hms(Ride.request.time))

allData <- union(VIAdata2021, VIAdata2022)

#filter
completedRides <- allData %>%
  filter(Status == "Completed")

completedRides2021 <- VIAdata2021 %>%
  filter(Status == "Completed")

completedRides2022 <- VIAdata2022 %>%
  filter(Status == "Completed")

saturday_rides <- completedRides %>% 
  mutate(Actual.PU.time = ymd_hms(Actual.PU.time)) %>%
  mutate(weekday = wday(Actual.PU.time, label = TRUE)) %>%
  filter(weekday == "Sa")

saturday_rides2021 <- completedRides2021 %>%
  mutate(Actual.PU.time = ymd_hms(Actual.PU.time)) %>%
  mutate(weekday = wday(Actual.PU.time, label = TRUE)) %>%
  filter(weekday == "Sa")

saturday_rides2022 <- completedRides2022 %>%
  mutate(Actual.PU.time = ymd_hms(Actual.PU.time)) %>%
  mutate(weekday = wday(Actual.PU.time, label = TRUE)) %>%
  filter(weekday == "Sa")

#dump output
write.csv2(completedRides, "VIA_Rides_202106_202210.csv", quote = FALSE, row.names = FALSE)
write.csv2(saturday_rides, "VIA_Rides_Saturdays_202106_202210.csv", quote = FALSE, row.names = FALSE)
write.csv2(completedRides2021, "VIA_Rides_202106_202201.csv", quote = FALSE, row.names = FALSE)
write.csv2(saturday_rides2021, "VIA_Rides_Saturdays_202106_202201.csv", quote = FALSE, row.names = FALSE)
write.csv2(completedRides2022, "VIA_Rides_202201_202210.csv", quote = FALSE, row.names = FALSE)
write.csv2(saturday_rides2022, "VIA_Rides_Saturdays_202201_202210.csv", quote = FALSE, row.names = FALSE)

