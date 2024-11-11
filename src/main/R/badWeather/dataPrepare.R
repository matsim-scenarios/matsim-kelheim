library(lubridate)
library(tidyverse)
library(dplyr)

#this script is an adaptation of an Rmd script which was then deleted.
# I created an .R script because it is more understandable. -sme0823


#read in ikoki data
ioki2020 <- read_csv2("../../shared-svn/projects/KelRide/data/KEXI/2021-04/IOKI_TABLEAU_Request_List_2020.csv")
ioki2021 <- read_csv2("../../shared-svn/projects/KelRide/data/KEXI/2021-05/IOKI_TABLEAU_Request_List_2021.csv")

ioki2020 <- ioki2020 %>% select(1:20,Passagieranzahl,`Nutzer ID`,`Fahrzeug ID`,`Eindeutige Anfrage`,Ersteller)

ioki2021 <- ioki2021 %>% anti_join(ioki2020, by = "Fahrt ID")
allData_ioki <- rbind(ioki2020,ioki2021)

via0621_0624 <- read_csv2("../../shared-svn/projects/KelRide/data/KEXI/VIA_data/raw-data/Fahrtanfragen.csv")

# request id 5526535 has faulty request time
allData_via <- via0621_0624 %>% 
  filter(`Fahrtanfragen ID` != 5526535) %>% 
  mutate(Actual.Pickup.Time = ymd_hms(`Tatsächliche Einstiegszeit`), 
         Request.Creation.Time = ymd_hms(`Erstellungszeit der Fahrtanfrage`),
         Request.ID = `Fahrtanfragen ID`,
         Request.Status = `Status der Fahrtanfrage`,
         Reason.For.Travel = `Grund für die Fahrt`) %>% 
  select(Request.ID,Request.Status,Actual.Pickup.Time,Reason.For.Travel,Request.Creation.Time) %>% 
  # Reason for travel is used to distinguish AV and conventional. the column was added after the av launch in 09-22
  mutate(Reason.For.Travel = replace_na(Reason.For.Travel, "DR"))

unique(allData_via$Request.Status)
unique(allData_ioki$Stornierungsgrund)
ids <- unique(allData_via$Request.ID)

naVia <- allData_via %>% filter(is.na(Actual.Pickup.Time))
naIoki2 <- allData_ioki %>% filter(is.na(Abfahrtszeit))

# get all requests per day for each dataset and join them
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

unique(allData_ioki$Stornierungsgrund)

# get all rejections (because of unavailable vehicle) per dataset and join
rejections_ioki <-  allData_ioki %>% 
  filter(Stornierungsgrund == "no_vehicle_available") %>% 
  mutate(`Fahrtwunsch erstellt` = as.Date(dmy_hms(`Fahrtwunsch erstellt`))) %>% filter(!is.na(`Fahrtwunsch erstellt`)) %>%
  mutate(dummy = 1) %>%
  group_by(`Fahrtwunsch erstellt`) %>% summarize(noRejections = as.integer(sum(dummy))) %>%
  rename(date = `Fahrtwunsch erstellt`)

rejections_via <- allData_via %>%
  filter(Request.Status == "Seat Unavailable") %>% 
  filter(!is.na(`Request.Creation.Time`)) %>%
  filter(Reason.For.Travel=="DR") %>%
  mutate(`Request.Creation.Time` = as.Date(`Request.Creation.Time`)) %>%
  mutate(dummy = 1) %>%
  group_by(Request.Creation.Time) %>% summarize(noRejections = as.integer(sum(dummy))) %>%
  rename(date = `Request.Creation.Time`)

rejections_all <- rbind(rejections_ioki, rejections_via)

# get all completed rides per dataset and join
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

write.csv2(demandData_all,"../../shared-svn/projects/KelRide/data/badWeather/data/allDemandByDate.csv", quote = FALSE, row.names=FALSE)
write.csv2(demandDataSince2022,"../../shared-svn/projects/KelRide/data/badWeather/data/allDemandByDateSince2022.csv", quote = FALSE, row.names=FALSE)
write.csv2(requests_all,"../../shared-svn/projects/KelRide/data/badWeather/data/allRequestsByDate.csv", quote = FALSE, row.names=FALSE)
write.csv2(rejections_all,"../../shared-svn/projects/KelRide/data/badWeather/data/rejectionsByDate.csv", quote = FALSE, row.names=FALSE)
