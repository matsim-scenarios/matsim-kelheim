library(lubridate)
library(tidyverse)
library(dplyr)

#this script is an adaptation of an Rmd script which was then deleted.
# I created an .R script because it is more understandable. -sme0823


#read in ikoki data
ioki2020 <- read_csv2("C:/Users/Simon/Documents/shared-svn/projects/KelRide/data/KEXI/2021-04/IOKI_TABLEAU_Request_List_2020.csv")
ioki2021 <- read_csv2("C:/Users/Simon/Documents/shared-svn/projects/KelRide/data/KEXI/2021-05/IOKI_TABLEAU_Request_List_2021.csv")

ioki2020 <- ioki2020 %>% select(1:20,Passagieranzahl,`Nutzer ID`,`Fahrzeug ID`,`Eindeutige Anfrage`,Ersteller)

ioki2021 <- ioki2021 %>% anti_join(ioki2020, by = "Fahrt ID")
allData_ioki <- rbind(ioki2020,ioki2021)

# read in via data
via0621_0122 <- read_csv2("C:/Users/Simon/Documents/shared-svn/projects/KelRide/data/KEXI/Via_data_2022-02-08/Data_request_TUB_for_Kelheim-Actual_Data-VIA_edited.csv")
via0222_1022 <- read_csv2("C:/Users/Simon/Documents/shared-svn/projects/KelRide/data/KEXI/Via_data_2022-10-10/Data_request_TUB_for_Kelheim-Actual_Data-VIA_Feb_to_Oct_2022_edited_cleaned.csv")
via1022_1222 <- read_csv2("C:/Users/Simon/Documents/shared-svn/projects/KelRide/data/KEXI/Via_data_2023-01-17/Data_request_TUB_for_Kelheim-Actual_Data-Oct-Dec_2022-Data_TUB_for_Kelheim-Actual_Data-Oct_to_Dec_22_edited.csv")
via1222_0323 <- read_csv2("C:/Users/Simon/Documents/shared-svn/projects/KelRide/data/KEXI/Via_data_2023-04-19/Data_request_TUB_for_Kelheim-Actual_Data-Jan-Mar_2023-Kelheim-Actual_Data-Jan-Mar_2023_edited.csv")
via0323_0723 <- read_csv2("C:/Users/Simon/Documents/shared-svn/projects/KelRide/data/KEXI/Via_data_2023-07-10/Data_request_TUB_for_Kelheim-Actual_Data-Apr-Jul_2023-Kelheim-Actual_Data-Apr-Jul_23_edited.csv")

via0621_0122 <- via0621_0122 %>% 
  select(Request.ID,Request.Status,Actual.Pickup.Time,Reason.For.Travel,Request.Creation.Time) %>%
  mutate(Actual.Pickup.Time = ymd_hms(Actual.Pickup.Time))

via0222_1022 <- via0222_1022 %>% 
  select(Request.ID,Request.Status,Actual.Pickup.Time,Reason.For.Travel,Request.Creation.Time) %>%
  mutate(Actual.Pickup.Time = ymd_hms(Actual.Pickup.Time), Request.Creation.Time = ymd_hms(Request.Creation.Time)) %>% anti_join(via0621_0122, by = "Request.ID")

via1022_1222 <- via1022_1222 %>% 
  select(Request.ID,Request.Status,Actual.Pickup.Time,Reason.For.Travel,Request.Creation.Time) %>%
  mutate(Actual.Pickup.Time = ymd_hms(Actual.Pickup.Time), Request.Creation.Time = ymd_hms(Request.Creation.Time)) %>% anti_join(via0222_1022, by = "Request.ID")

via1222_0323 <- via1222_0323 %>% 
  select(Request.ID,Request.Status,Actual.Pickup.Time,Reason.For.Travel,Request.Creation.Time) %>%
  mutate(Actual.Pickup.Time = ymd_hms(Actual.Pickup.Time), Request.Creation.Time = ymd_hms(Request.Creation.Time)) %>% anti_join(via1022_1222, by = "Request.ID")

via0323_0723 <- via0323_0723 %>% 
  select(Request.ID,Request.Status,Actual.Pickup.Time,Reason.For.Travel,Request.Creation.Time) %>%
  mutate(Actual.Pickup.Time = ymd_hms(Actual.Pickup.Time), Request.Creation.Time = ymd_hms(Request.Creation.Time)) %>% anti_join(via1222_0323, by = "Request.ID")

test <- unique(via1022_1222$Request.Status)

# allData_via <- rbind(via0621_0122,via0222_1022,via1022_1222,via1222_0323,via0323_0723)
allData_via <- bind_rows(via0621_0122,via0222_1022)
allData_via <- bind_rows(allData_via,via1022_1222)
allData_via <- bind_rows(allData_via,via1222_0323)
allData_via <- bind_rows(allData_via,via0323_0723)

naVia <- allData_via %>% filter(is.na(Actual.Pickup.Time))
#naVia2 <- allData_via %>% filter(is.na(Actual.Dropoff.Time))
naIoki2 <- allData_ioki %>% filter(is.na(Abfahrtszeit))

#print(via0621_0122)

requests_ioki <- allData_ioki %>% mutate(`Fahrtwunsch erstellt` = as.Date(dmy_hms(`Fahrtwunsch erstellt`))) %>% filter(!is.na(`Fahrtwunsch erstellt`)) %>%
  mutate(dummy = 1) %>%
  group_by(`Fahrtwunsch erstellt`) %>% summarize(noRequests = as.integer(sum(dummy))) %>%
  rename(date = `Fahrtwunsch erstellt`)

requests_via <- allData_via %>%
  filter(!is.na(`Request.Creation.Time`)) %>%
  filter(Reason.For.Travel=="DR") %>%
  mutate(`Request.Creation.Time` = as.Date(`Request.Creation.Time`)) %>%
  mutate(dummy = 1) %>%
  group_by(Request.Creation.Time) %>% summarize(noRequests = as.integer(sum(dummy))) %>%
  rename(date = `Request.Creation.Time`)

requests_all <- rbind(requests_ioki,requests_via)

allData_ioki <- allData_ioki %>% mutate(`Abfahrtszeit` = as.Date(dmy_hms(`Abfahrtszeit`))) %>% filter(!is.na(`Abfahrtszeit`))
allData_via <- allData_via %>% 
  filter(!is.na(`Actual.Pickup.Time`)) %>% 
  filter(Reason.For.Travel=="DR") %>% 
  mutate(`Actual.Pickup.Time` = as.Date(`Actual.Pickup.Time`))

allData_ioki <- allData_ioki %>% mutate(ncompl = ifelse(Stornierungsgrund == "ride_completed",1,0))

demandData_ioki <- allData_ioki %>% group_by(`Abfahrtszeit`) %>% summarize(noRides = sum(ncompl))


allData_via <- allData_via %>% mutate(ncompl = ifelse(Request.Status == "Completed",1,0))

demandData_via <- allData_via %>% group_by(`Actual.Pickup.Time`) %>% summarize(noRides = sum(ncompl,na.rm = TRUE))

#Join 2 tables
#rename
demandData_ioki <- demandData_ioki %>% rename(date = `Abfahrtszeit`)
demandData_via <- demandData_via %>% rename(date = `Actual.Pickup.Time`)

demandData_all <- rbind(demandData_ioki,demandData_via)

demandDataSince2022 <- demandData_all %>% filter(date >= as.Date(ymd("2022-01-01")))

write.csv2(demandData_all,"C:/Users/Simon/Documents/shared-svn/projects/KelRide/data/badWeather/data/allDemandByDate.csv", quote = FALSE, row.names=FALSE)
write.csv2(demandDataSince2022,"C:/Users/Simon/Documents/shared-svn/projects/KelRide/data/badWeather/data/allDemandByDateSince2022.csv", quote = FALSE, row.names=FALSE)
write.csv2(requests_all,"C:/Users/Simon/Documents/shared-svn/projects/KelRide/data/badWeather/data/allRequestsByDate.csv", quote = FALSE, row.names=FALSE)
