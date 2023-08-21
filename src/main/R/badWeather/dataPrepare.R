library(lubridate)
library(tidyverse)

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

via0222_1022 <- via0222_1022 %>% anti_join(via0621_0122, by = "Request.ID")
allData_via <- rbind(via0621_0122,via0222_1022)

#print(via0621_0122)

allData_ioki <- allData_ioki %>% mutate(`Abfahrtszeit` = as.Date(dmy_hms(`Abfahrtszeit`))) %>% filter(!is.na(`Abfahrtszeit`))
allData_via <- allData_via %>% mutate(`Actual.Pickup.Time` = as.Date(ymd_hms(`Actual.Pickup.Time`)))

allData_ioki <- allData_ioki %>% mutate(ncompl = ifelse(Stornierungsgrund == "ride_completed",1,0))

demandData_ioki <- allData_ioki %>% group_by(`Abfahrtszeit`) %>% summarize(noRides = sum(ncompl))


allData_via <- allData_via %>% mutate(ncompl = ifelse(Request.Status == "Completed",1,0))

demandData_via <- allData_via %>% group_by(`Actual.Pickup.Time`) %>% summarize(noRides = sum(ncompl,na.rm = TRUE))

#Join 2 tables
#rename
demandData_ioki <- demandData_ioki %>% rename(date = `Abfahrtszeit`)
demandData_via <- demandData_via %>% rename(date = `Actual.Pickup.Time`)

demandData_all <- rbind(demandData_ioki,demandData_via)

write.csv2(demandData_all,"C:/Users/Simon/Documents/shared-svn/projects/KelRide/data/badWeather/data/allDemandByDateTest.csv", quote = FALSE, row.names=FALSE)