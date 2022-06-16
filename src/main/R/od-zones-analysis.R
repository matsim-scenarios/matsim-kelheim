library(tidyverse)
library(tidyr)
library(dplyr)
library(sf) #=> geography



## LK Kelheim
zone1 <- st_read("D:/svn/public-svn/matsim/scenarios/countries/de/kelheim/shp/dilutionArea.shp", crs="EPSG:25832")
#zone1 <- st_read("D:/svn/shared-svn/projects/matsim-kelheim/data/sektor3/kelheim-kelfleet-sektor3.shp")

## Regensburg City
zone2 <- st_read("D:/KelFleet/Regensburg/regensburg-utm32n.shp")

trips <- read_delim("D:/KelFleet/kexi-base-case/kexi.output_trips.csv.gz",
                    delim = ";",
                    trim_ws = T, 
                    col_types = cols(person = col_character()))# %>%
          #filter(main_mode!="freight")

trip_starts <- trips %>% 
  mutate(wkt = paste("MULTIPOINT((", start_x, " ", start_y, "))", sep = "")) %>% 
  st_as_sf(wkt = "wkt", crs = "EPSG:25832")

trip_ends <- trips %>% 
  mutate(wkt = paste("MULTIPOINT((", end_x, " ", end_y, "))", sep = "")) %>% 
  st_as_sf(wkt = "wkt", crs = "EPSG:25832")

zone1_starts <- trip_starts %>% 
  filter(c(st_contains(zone1, ., sparse = FALSE)))

zone2_starts <- trip_starts %>% 
  filter(c(st_contains(zone2, ., sparse = FALSE)))

zone1_ends <- trip_ends %>% 
  filter(c(st_contains(zone1, ., sparse = FALSE)))

zone2_ends <- trip_ends %>% 
  filter(c(st_contains(zone2, ., sparse = FALSE)))


zone1_to_zone2 <- semi_join(as_tibble(zone1_starts), as_tibble(zone2_ends), by = "trip_id")  

#write_delim(zone1_to_zone2,
#            "D:/KelFleet/kexi-base-case/kexi.output_trips_kelheimToRegensburg.csv",
#            delim = ";")

zone2_to_zone1 <- semi_join(as_tibble(zone2_starts), as_tibble(zone1_ends), by = "trip_id")  
