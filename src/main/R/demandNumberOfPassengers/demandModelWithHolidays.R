library(tidyverse)
library(lubridate)
library(plotly)

options(digits = 18)

ioki <-read_csv2("data/IOKI_Rides_202006_202105.csv")%>% select(2,`Fahrt.ID`,Stornierungsgrund,Passagieranzahl) %>%
  mutate(Fahrtwunsch.erstellt = as.Date(as.POSIXct(Fahrtwunsch.erstellt, format = "%d.%m.%Y %H:%M:%S")))
via <-read_csv2("data/VIA_Rides_202106_202210.csv")  %>% select(2,3,4,`Number.of.passengers`) %>%
  mutate(Ride.request.time = as.Date(Ride.request.time))


colnames(ioki) <-colnames(via)
allData <- rbind(ioki,via) %>% filter(!is.na(Ride.request.time)) %>%
  mutate(weekday = wday(Ride.request.time,week_start = 1)) %>%
  filter(weekday !=1, weekday !=5, weekday != 6,weekday!=7) #exclude Mo, Fr, Sat, Sun.


growingPhase <- interval(ymd("2020-01-01"), ymd("2020-09-30"))
autumn_holiday <- interval(ymd("2020-11-02"), ymd("2020-11-06"))
holiday_bettag <- interval(ymd("2020-11-18"), ymd("2020-11-18"))
holidays_christmas <- interval(ymd("2020-12-21"), ymd("2021-01-08"))
holidays_easter <- interval(ymd("2021-03-29"), ymd("2021-04-09"))
holiday_himmelfahrt <- interval(ymd("2021-05-13"), ymd("2021-05-14"))
holiday_pfingsten <- interval(ymd("2021-05-24"), ymd("2021-06-04"))
summer_holiday21 <- interval(ymd("2021-07-30"), ymd("2021-09-13"))
autumn_holiday21 <- interval(ymd("2021-11-01"), ymd("2021-11-05"))
holiday_bettag21 <- interval(ymd("2021-11-17"), ymd("2021-11-17"))
holidays_christmas21 <- interval(ymd("2021-12-24"), ymd("2022-01-08"))
winter_holiday22 <- interval(ymd("2022-02-28"), ymd("2022-03-04"))
easter_holiday22 <- interval(ymd("2022-04-11"), ymd("2022-04-23"))
holiday_himmelfahrt22 <- interval(ymd("2022-05-26"), ymd("2022-05-26"))
pfingsten_holiday22 <- interval(ymd("2022-06-06"), ymd("2022-06-18"))
summer_holiday22 <- interval(ymd("2022-08-01"), ymd("2022-09-12"))
holiday_einheit22 <- interval(ymd("2022-10-03"), ymd("2022-10-03"))

holidays <-c(growingPhase,
             autumn_holiday,
             holiday_bettag,
             holidays_christmas,
             holidays_easter,
             holiday_himmelfahrt,
             holiday_pfingsten,
             summer_holiday21,
             autumn_holiday21,
             holiday_bettag21,
             holidays_christmas21,
             winter_holiday22,
             easter_holiday22,
             holiday_himmelfahrt22,
             pfingsten_holiday22,
             summer_holiday22,
             holiday_einheit22
)

allRelevantData <- allData %>% filter(!Ride.request.time %within% holidays)

demand <-allRelevantData %>% group_by(Number.of.passengers) %>% count()

write.csv(demand, "numberOfPassengersDemand.csv",row.names = FALSE)

print(demand)