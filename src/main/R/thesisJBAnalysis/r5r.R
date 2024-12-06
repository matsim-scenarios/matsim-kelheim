library(osmextract)
library(sf)
library(r5r)
library(tidyverse)


#PATHS
rides_path <- "rides_clean.csv"
network_path <- "C:/Users/J/Documents/Thesis/thesis/osm/" #this needs to be a .pbf file, download from OSM using oe_get()

#load and filter rides 
rides <- read.csv(rides_path) %>% 
  drop_na(Angefragte.Einstiegszeit) 

# allocate RAM memory to Java
options(java.parameters = "-Xmx3G")
rJava::.jinit()

#setup r5r
r5r_core <- setup_r5(data_path = network_path, verbose = TRUE)


##### NETWORK DISTANCE KEXI TRIPS ####

rides <- rides %>% 
  rename(id = Fahrtanfragen.ID)

origins <- rides %>% 
  dplyr::select(id, Breitengrad..Einstieg., Längengrad..Einstieg., Fahrtdauer) %>% 
  st_as_sf(coords = c("Längengrad..Einstieg.","Breitengrad..Einstieg." ), crs = 4326 ) %>% 
  st_cast("POINT")

destinations <- rides %>% 
  dplyr::select(id, Breitengrad..Ausstieg., Längengrad..Ausstieg., Fahrtdauer) %>% 
  st_as_sf(coords = c("Längengrad..Ausstieg.", "Breitengrad..Ausstieg."), crs = 4326 )%>% 
  st_cast("POINT")


mode <- c("CAR")
max_walk_time <- 600 # minutes
max_trip_duration <- 600 # minutes
departure_datetime <- as.POSIXct("13-03-2024 12:00:00",
                                 format = "%d-%m-%Y %H:%M:%S", tz= "GMT")


det <- detailed_itineraries(r5r_core = r5r_core,
                               origins = origins,
                               destinations = destinations,
                               mode = mode,
                               departure_datetime = departure_datetime,
                               max_walk_time = max_walk_time,
                               shortest_path = TRUE)
  det <- det %>% 
  dplyr::select(from_id, distance, total_duration) %>% 
  mutate(from_id = as.integer(from_id))
  
rides_dist <- left_join(rides, det, join_by("id" == "from_id")) %>% 
  filter(Anbietername != "AV") %>% 
  mutate(Fahrtdauer_s = Fahrtdauer*60)
write.csv(rides_dist, "rides_dist.csv")

#share of trips < 1,7km
(rides_dist %>%  filter(distance<1700) %>% nrow())/nrow(rides_dist)

#share of trips < 3,9km
(rides_dist %>%  filter(distance<3900) %>% nrow())/nrow(rides_dist)

#model all trips
model <- lm(Fahrtdauer_s ~ distance, data = rides_dist)
summary(model)
model$coefficients

ggplot(data = rides_dist, aes(x = distance, y = Fahrtdauer_s)) +
  geom_point() +
  geom_abline(slope = 0.06835826, intercept = 281.77568059, color = "#b5de2b", linetype="dashed", linewidth=1.5)+
  xlab("distance (m)")+
  ylab("travel time (s)")

#model non-shared rides
solo_rides <- rides_dist %>% filter(Geteilte.Fahrt=="No")
model <- lm(Fahrtdauer_s ~ distance, data = solo_rides)
summary(model)
model$coefficients

ggplot(data = solo_rides, aes(x = distance, y = Fahrtdauer_s)) +
  geom_point() +
  geom_abline(slope = 0.05595693 , intercept = 206.44990288, color = "#2a788e", linetype="dashed", linewidth=1.5)+
  xlab("distance (m)")+
  ylab("travel time (min)")

#### stop R5R
r5r::stop_r5(r5r_core)
rJava::.jgc(R.gc = TRUE)
