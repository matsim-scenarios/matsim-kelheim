library(lubridate)
library(tidyverse)
library(dplyr)

# set working directory
setwd("C:/Users/Simon/Documents/shared-svn/projects/KelRide/data/KEXI/")

# read data
VIAdata2021 <- read.csv2("Via_data_2022-02-08/Data_request_TUB_for_Kelheim-Actual_Data-VIA_edited.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8")

#filter
completedRides <- VIAdata2021 %>%
  filter(Status == "Completed")

saturday_rides <- completedRides %>% 
  mutate(Actual.PU.time = ymd_hms(Actual.PU.time)) %>%
  mutate(weekday = wday(Actual.PU.time, label = TRUE)) %>%
  filter(weekday == "Sa")

#dump output
write.csv2(completedRides, "VIA_Rides_202106_202201.csv", quote = FALSE)
write.csv2(saturday_rides, "VIA_Rides_Saturdays_202106_202201.csv", quote = FALSE)

