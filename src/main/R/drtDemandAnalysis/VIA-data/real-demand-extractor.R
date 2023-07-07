library(tidyverse)
library(lubridate)

raw_data <- read.csv("VIA_Rides_202106_202303.csv", sep=";") %>%
  filter(Actual.Pickup.Time != "")  %>%
  separate(Requested.Pickup.Time, into = c("requested_departure_date", "requested_departure_time"), sep = " ") %>%
  separate(Actual.Pickup.Time, into = c("actual_departure_date", "actual_departure_time"), sep = " ") %>%
  separate(Request.Creation.Time, into = c("request_generation_date", "request_generation_time"), sep = " ")

processed_data <- tibble(
  requested_date = c(raw_data$requested_departure_date),
  requested_time = c(raw_data$requested_departure_time),
  number_of_passengers = c(raw_data$Number.of.Passengers),
  from_x = c(raw_data$Origin.Lng),
  from_y = c(raw_data$Origin.Lat),
  to_x = c(raw_data$Destination.Lng),
  to_y = c(raw_data$Destination.Lat),
  actual_departure_time = c(raw_data$actual_departure_time)
  ) %>%
  mutate(time_in_seconds = period_to_seconds(hms(requested_time))) %>%
  mutate(delay_in_departure = period_to_seconds(hms(actual_departure_time)) - period_to_seconds(hms(requested_time))) %>%
  relocate(time_in_seconds, .before = from_x) %>%
  filter(!is.na(requested_time))
  #TODO here, we filter out the entry with requested arrival time (which is not many)


# daily_data <- processed_data %>%
#   filter(requested_date == "26.11.2020")
# ggplot(data = daily_data) +
#   geom_histogram(mapping = aes(x=delay_in_departure), binwidth=60) 
#  scale_x_continuous(limits=c(-3600,3600))

# 2022-02-17 Thursday
output_data_20220217 <- processed_data %>%
  filter(requested_date == "2022-02-17")
write_csv(output_data_20220217, "Kelheim/extracted-daily-demands/2022-02-17-demand.csv")

# 2022-04-05 Tuesday
output_data_20220405 <- processed_data %>%
  filter(requested_date == "2022-04-05")
write_csv(output_data_20220405, "Kelheim/extracted-daily-demands/2022-04-05-demand.csv")

# 2022-05-17 Tuesday
output_data_20220517 <- processed_data %>%
  filter(requested_date == "2022-05-17")
write_csv(output_data_20220517, "Kelheim/extracted-daily-demands/2022-05-17-demand.csv")


#2022-09-28 Wednesday
output_data_20220928 <- processed_data %>%
  filter(requested_date == "2022-09-28")
write_csv(output_data_20220928, "Kelheim/extracted-daily-demands/2022-09-28-demand.csv")

#2023-01-19 Thursday
output_data_20230119 <- processed_data %>%
  filter(requested_date == "2023-01-19")
write_csv(output_data_20230119, "Kelheim/extracted-daily-demands/2023-01-19-demand.csv")

