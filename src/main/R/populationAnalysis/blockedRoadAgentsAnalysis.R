library(tidyverse)
library(matsim)
library(ggalluvial)
library(lubridate)


setwd("Y:/net/ils/matsim-kelheim/run-roadBlock/output/")

baseDir <- "Y:/net/ils/matsim-kelheim/run-roadBlock/output/kelheim-v2.0-network-with-pt.xml.gz-seed5678-CORE/"
policyDir <- "Y:/net/ils/matsim-kelheim/run-roadBlock/output/output-casekelheim-v2.0-network-with-pt_blocked-RegensburgerStr.xml.gz-seed5678-CORE/"

tsvFile <- "analysis-road-usage/blocked_infrastructure_trip_comparison.tsv"

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
         modes.y,
         main_mode.x,
         main_mode.y,
         wait_time.x,
         wait_time.y,
         euclidean_distance.x,
         euclidean_distance.y) %>%
  rename("trav_time_base" = trav_time.x,
         "trav_time_policy" = trav_time.y,
         "traveled_distance_base" = traveled_distance.x,
         "traveled_distance_policy" = traveled_distance.y,
         "modes_base" = modes.x,
         "modes_policy" = modes.y,
         "main_mode_base" = main_mode.x,
         "main_mode_policy" = main_mode.y,
         "wait_time_base" = wait_time.x,
         "wait_time_policy" = wait_time.y,
         "euclidean_distance_base" = euclidean_distance.x,
         "euclidean_distance_policy" = euclidean_distance.y)

tripsCombined <- tripsCombined %>% 
  mutate(trav_time_diff_s = trav_time_policy - trav_time_base,
         traveled_distance_diff_m = traveled_distance_policy - traveled_distance_base,
         trav_time_base = seconds(trav_time_base),
         trav_time_policy = seconds(trav_time_policy),
         wait_time_base = seconds(wait_time_base),
         wait_time_policy = seconds(wait_time_policy))

meanTravTimeBase <- mean(tripsCombined$trav_time_base)
meanTravTimePolicy <- mean(tripsCombined$trav_time_policy)
meanTravDistBase <- mean(tripsCombined$traveled_distance_base)
meanTravDistPolicy <- mean(tripsCombined$traveled_distance_policy)
meanEuclDistBase <- mean(tripsCombined$euclidean_distance_base)
meanEuclDistPolicy <- mean(tripsCombined$euclidean_distance_policy)
meanWaitTimeBase <- mean(tripsCombined$wait_time_base)
meanWaitTimePolicy <- mean(tripsCombined$wait_time_policy)

tripsChangedMainMode <- tripsCombined %>%
  filter(main_mode_base != main_mode_policy)

noTripsChangedMainMode <- nrow(tripsChangedMainMode)

#save avg values into df
avgValues <- setNames(data.frame(matrix(ncol = 9, nrow = 0)), c("meanTravTimeBase[s]", "meanTravTimePolicy[s]", "meanTravDistBase[m]", "meanTravDistPolicy[m]", "meanEuclDistBase[m]",
                                                                "meanEuclDistPolicy[m]", "meanWaitTimeBase[s]", "meanWaitTimePolicy[s]","nrTripsChangedMainMode"))

avgValuesDataset <- data.frame(meanTravTimeBase, meanTravTimePolicy,meanTravDistBase,meanTravDistPolicy,meanEuclDistBase,meanEuclDistPolicy,meanWaitTimeBase,meanWaitTimePolicy,noTripsChangedMainMode)
names(avgValuesDataset) <- names(avgValues)
avgValues <- rbind(avgValues,avgValuesDataset)

tsvFileName <- paste0("avg_params_blocked_infrastructure_agents.tsv")
write.table(avgValues,paste0(policyDir,"analysis-road-usage/",tsvFileName),quote=FALSE, row.names=FALSE, dec=".", sep="\t")
print(paste0("avg values for agents affected by blocked infrastructure ",policyDir,"analysis-road-usage/",tsvFileName))




