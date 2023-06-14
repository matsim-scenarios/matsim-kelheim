library(tidyverse)
library(matsim)
library(ggalluvial)
library(lubridate)
# library(ggplot2)


setwd("Y:/net/ils/matsim-kelheim/run-roadBlock/output/")

baseDir <- "Y:/net/ils/matsim-kelheim/run-roadBlock/output/kelheim-v2.0-network-with-pt.xml.gz-seed1111-CORE/"
policyDir <- "Y:/net/ils/matsim-kelheim/run-roadBlock/output/output-casekelheim-v2.0-network-with-pt_blocked-Maximiliansbruecke.xml.gz-seed1111/"

tsvFile <- "analysis-road-usage/blocked_infrastructure_trip_comparison.tsv"

""

ifelse(endsWith(policyDir, "/"),tsvFile <- tsvFile, tsvFile <- paste0("/",tsvFile))


affectedTrips <- read.csv2(paste0(policyDir,tsvFile), stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", sep="\t")

tripsBase <- readTripsTable(pathToMATSimOutputDirectory = baseDir)
tripsPolicy <- readTripsTable(pathToMATSimOutputDirectory = policyDir)

# filter trips with usage of blocked infrastructure in base case only
tripsBase <- tripsBase %>% 
  filter(trip_id %in% affectedTrips$trip_id)

tripsPolicy <- tripsPolicy %>% 
  filter(trip_id %in% affectedTrips$trip_id)
 
# join relevant trips by id and join + filter important stats only
tripsCombined <- left_join(tripsBase, tripsPolicy, by = "trip_id") %>% 
  filter(trip_number.x == trip_number.y) %>% 
  select(trip_id,
         trav_time.x,
         trav_time.y,
         traveled_distance.x,
         traveled_distance.y,
         modes.x,
         modes.y) %>% 
  rename("trav_time_base" = trav_time.x,
         "trav_time_policy" = trav_time.y,
         "traveled_distance_base" = traveled_distance.x,
         "traveled_distance_policy" = traveled_distance.y,
         "modes_base" = modes.x,
         "modes_policy" = modes.y)

tripsCombined <- tripsCombined %>% 
  mutate(trav_time_diff_s = trav_time_policy - trav_time_base,
         traveled_distance_diff_m = traveled_distance_policy - traveled_distance_base)

tripsChangedModeChain <- tripsCombined %>% 
  filter(modes_base != modes_policy)
  



