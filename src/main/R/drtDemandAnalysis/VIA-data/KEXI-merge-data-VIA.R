library(lubridate)
library(tidyverse)
library(dplyr)

# set working directory
setwd("C:/Users/Simon/Documents/shared-svn/projects/KelRide/data/KEXI/")

# read data
VIAdata2021 <- read.csv2("Via_data_2022-02-08/Data_request_TUB_for_Kelheim-Actual_Data-VIA_raw.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", sep=",", skip = 1)
VIAdata2022_1 <- read.csv2("Via_data_2022-10-10/Data_request_TUB_for_Kelheim-Actual_Data-VIA_Feb_to_Oct_2022_raw.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", sep=",", skip = 1)
VIAdata2022_2 <- read.csv2("Via_data_2023-01-17/Data_request_TUB_for_Kelheim-Actual_Data-Oct-Dec_2022-Data_TUB_for_Kelheim-Actual_Data-Oct_to_Dec_22.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", sep=",", skip = 1)
VIAdata2023_1 <- read.csv2("Via_data_2023-04-19/Data_request_TUB_for_Kelheim-Actual_Data-Jan-Mar_2023-Kelheim-Actual_Data-Jan-Mar_2023.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", sep=",", skip = 1)
VIAdata2023_2 <- read.csv2("Via_data_2023-07-10/Data_request_TUB_for_Kelheim-Actual_Data-Apr-Jul_2023-Kelheim-Actual_Data-Apr-Jul_23.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", sep=",", skip = 1)

# here it makes sense to switch to column names from 2022 data and newer as
# column names for all files but the 2021 data are the same
VIAdata2021 <- VIAdata2021 %>%
  rename("Request.Creation.Time" = Ride.request.time,
         "Request.Status" = Status,
         "Booking.Method" = Booking.method,
         "Number.of.Passengers" = Number.of.passengers,
         "Requested.Pickup.Time" = Requested.PU.time,
         "Requested.Dropoff.Time" = Requested.DO.time,
         "Origin.Lat" = Origin.latitude,
         "Origin.Lng" = Origin.Longitude,
         "Destination.Lat" = Destination.latitude,
         "Destination.Lng" = Destination.Longitude,
         "Cancellation.Time" = Cancellation.time,
         "No.Show.Time" = No.show.time,
         "Actual.Pickup.Time" = Actual.PU.time,
         "Actual.Dropoff.Time" = Actual.DO.time) %>%
  mutate(Reason.For.Travel = "DR",
         Request.Creation.Time = ymd_hms(Request.Creation.Time))

VIAdata2022_1 <- VIAdata2022_1 %>%
  mutate(Ride.ID = NA,
         Reason.For.Travel = ifelse(Reason.For.Travel != "AV","DR","AV"),
         Request.Creation.Time = ymd_hms(Request.Creation.Time))

VIAdata2022_2 <- VIAdata2022_2 %>%
  mutate(Ride.ID = NA,
         Reason.For.Travel = ifelse(Reason.For.Travel != "AV","DR","AV"),
         Request.Creation.Time = ymd_hms(Request.Creation.Time))

VIAdata2023_1 <- VIAdata2023_1 %>%
  mutate(Ride.ID = NA,
         Reason.For.Travel = ifelse(Reason.For.Travel != "AV","DR","AV"),
         Request.Creation.Time = ymd_hms(Request.Creation.Time))

VIAdata2023_2 <- VIAdata2023_2 %>%
  mutate(Ride.ID = NA,
         Reason.For.Travel = ifelse(Reason.For.Travel != "AV","DR","AV"),
         Request.Creation.Time = ymd_hms(Request.Creation.Time))

write.csv2(VIAdata2021, "Via_data_2022-02-08/Data_request_TUB_for_Kelheim-Actual_Data-VIA_edited.csv", quote = FALSE, row.names = FALSE)
write.csv2(VIAdata2022_1, "Via_data_2022-10-10/Data_request_TUB_for_Kelheim-Actual_Data-VIA_Feb_to_Oct_2022_edited_cleaned.csv", quote = FALSE, row.names = FALSE)
write.csv2(VIAdata2022_2, "Via_data_2023-01-17/Data_request_TUB_for_Kelheim-Actual_Data-Oct-Dec_2022-Data_TUB_for_Kelheim-Actual_Data-Oct_to_Dec_22_edited.csv", quote = FALSE, row.names = FALSE)
write.csv2(VIAdata2023_1, "Via_data_2023-04-19/Data_request_TUB_for_Kelheim-Actual_Data-Jan-Mar_2023-Kelheim-Actual_Data-Jan-Mar_2023_edited.csv", quote = FALSE, row.names = FALSE)
write.csv2(VIAdata2023_2, "Via_data_2023-07-10/Data_request_TUB_for_Kelheim-Actual_Data-Apr-Jul_2023-Kelheim-Actual_Data-Apr-Jul_23_edited.csv", quote = FALSE, row.names = FALSE)

allData <- union(VIAdata2021, VIAdata2022_1)
allData <- union(allData, VIAdata2022_2)
allData <- union(allData, VIAdata2023_1)
allData <- union(allData, VIAdata2023_2) %>%
  distinct(Request.ID, .keep_all = TRUE)

#filter
completedRides <- allData %>%
  filter(Request.Status == "Completed")

completedRides2021 <- VIAdata2021 %>%
  filter(Request.Status == "Completed")

completedRides2022_1 <- VIAdata2022_1 %>%
  filter(Request.Status == "Completed")

completedRides2022_2 <- VIAdata2022_2 %>%
  filter(Request.Status == "Completed")

completedRides2023_1 <- VIAdata2023_1 %>%
  filter(Request.Status == "Completed")

completedRides2023_2 <- VIAdata2023_2 %>%
  filter(Request.Status == "Completed")

saturday_rides <- completedRides %>% 
  mutate(Actual.Pickup.Time = ymd_hms(Actual.Pickup.Time)) %>%
  mutate(weekday = wday(Actual.Pickup.Time, label = TRUE)) %>%
  filter(weekday == "Sa")

saturday_rides2021 <- completedRides2021 %>%
  mutate(Actual.Pickup.Time = ymd_hms(Actual.Pickup.Time)) %>%
  mutate(weekday = wday(Actual.Pickup.Time, label = TRUE)) %>%
  filter(weekday == "Sa")

saturday_rides2022_1 <- completedRides2022_1 %>%
  mutate(Actual.Pickup.Time = ymd_hms(Actual.Pickup.Time)) %>%
  mutate(weekday = wday(Actual.Pickup.Time, label = TRUE)) %>%
  filter(weekday == "Sa")

saturday_rides2022_2 <- completedRides2022_2 %>%
  mutate(Actual.Pickup.Time = ymd_hms(Actual.Pickup.Time)) %>%
  mutate(weekday = wday(Actual.Pickup.Time, label = TRUE)) %>%
  filter(weekday == "Sa")

saturday_rides2023_1 <- completedRides2023_1 %>%
  mutate(Actual.Pickup.Time = ymd_hms(Actual.Pickup.Time)) %>%
  mutate(weekday = wday(Actual.Pickup.Time, label = TRUE)) %>%
  filter(weekday == "Sa")

saturday_rides2023_2 <- completedRides2023_2 %>%
  mutate(Actual.Pickup.Time = ymd_hms(Actual.Pickup.Time)) %>%
  mutate(weekday = wday(Actual.Pickup.Time, label = TRUE)) %>%
  filter(weekday == "Sa")

#dump output
write.csv2(completedRides, "VIA_Rides_202106_202303.csv", quote = FALSE, row.names = FALSE)
write.csv2(saturday_rides, "VIA_Rides_Saturdays_202106_202303.csv", quote = FALSE, row.names = FALSE)
write.csv2(completedRides2021, "VIA_Rides_202106_202201.csv", quote = FALSE, row.names = FALSE)
write.csv2(saturday_rides2021, "VIA_Rides_Saturdays_202106_202201.csv", quote = FALSE, row.names = FALSE)
write.csv2(completedRides2022_1, "VIA_Rides_202201_202210.csv", quote = FALSE, row.names = FALSE)
write.csv2(saturday_rides2022_1, "VIA_Rides_Saturdays_202201_202210.csv", quote = FALSE, row.names = FALSE)
write.csv2(completedRides2022_2, "VIA_Rides_202210_202212.csv", quote = FALSE, row.names = FALSE)
write.csv2(saturday_rides2022_2, "VIA_Rides_Saturdays_202210_202212.csv", quote = FALSE, row.names = FALSE)
write.csv2(completedRides2023_1, "VIA_Rides_202212_202303.csv", quote = FALSE, row.names = FALSE)
write.csv2(saturday_rides2023_1, "VIA_Rides_Saturdays_202212_202303.csv", quote = FALSE, row.names = FALSE)
write.csv2(completedRides2023_2, "VIA_Rides_202304_202307.csv", quote = FALSE, row.names = FALSE)
write.csv2(saturday_rides2023_2, "VIA_Rides_Saturdays_202304_202307.csv", quote = FALSE, row.names = FALSE)

