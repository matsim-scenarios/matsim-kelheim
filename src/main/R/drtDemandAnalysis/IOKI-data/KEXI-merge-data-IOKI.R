library(lubridate)
library(tidyverse)
library(dplyr)

# set working directory
#setwd("D:/svn/shared-svn/projects/KelRide/data/KEXI/")


# read data
data2020 <- read.csv2("2021-04/IOKI_TABLEAU_Request_List_2020.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8")
data2021 <- read.csv2("2021-05/IOKI_TABLEAU_Request_List_2021.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8")

#data from 2021 contains duplicates from 2020 so we need to sort that out
datafrom2021ToAdd <- anti_join(data2021,data2020, by = "Fahrt.ID")
allData <- rbind(data2020,datafrom2021ToAdd)

#filter
completedRides <- allData %>% 
  filter(Stornierungsgrund == "ride_completed")

saturday_rides <- completedRides %>% 
  mutate(Abfahrtszeit = dmy_hms(Abfahrtszeit)) %>% 
  mutate(weekday = wday(Abfahrtszeit, label = TRUE)) %>% 
  filter(weekday == "Sa")

#dump output
write.csv2(allData, "IOKI_RequestList_202006_202105.csv", quote = FALSE)
write.csv2(completedRides, "IOKI_Rides_202006_202105.csv", quote = FALSE)
write.csv2(saturday_rides, "IOKI_Rides_Saturdays_202006_202105.csv", quote = FALSE)

