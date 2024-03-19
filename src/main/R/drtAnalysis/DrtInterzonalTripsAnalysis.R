library(tidyverse)
library(matsim)
library(sf)

# input shp files and the corresponding crs are defined here
CRS <- 25832
zone1shpFile <- "../../shared-svn/projects/KelRide/data/ServiceAreas/2021-autumn-possibleAreasForAutomatedVehicles/Altstadt.shp"
zone2shpFile <- "../../shared-svn/projects/KelRide/data/ServiceAreas/2021-autumn-possibleAreasForAutomatedVehicles/Donaupark.shp"

# input legs or trips file is defined here
it <- 999
directory <- "Y:/net/ils/matsim-kelheim/run-roadBlock/output/kelheim-v2.0-network-with-pt.xml.gz-seed5678-CORE/"

itDir <- paste0(directory,"ITERS/it.",it,"/")
legsOrTripsFile <- paste0(itDir,list.files(path = itDir, pattern = "*legs_av*"))

zone1 <- st_read(zone1shpFile, crs=CRS)
zone2 <- st_read(zone2shpFile, crs=CRS)

legsOrTripsTable <- read.csv2(legsOrTripsFile) %>%
  rename(start_x = "fromX",
         start_y = "fromY",
         end_x = "toX",
         end_y = "toY")

# filter for legs / trips starting and ending in zones
departureInZone1 <- filterByRegion(legsOrTripsTable,zone1,crs=CRS,start.inshape = TRUE, end.inshape = FALSE) %>%
  mutate(leg_id = paste0(personId,departureTime))
arrivalInZone1 <- filterByRegion(legsOrTripsTable,zone1,crs=CRS,start.inshape = FALSE, end.inshape = TRUE) %>%
  mutate(leg_id = paste0(personId,departureTime))

departureInZone2 <- filterByRegion(legsOrTripsTable,zone2,crs=CRS,start.inshape = TRUE, end.inshape = FALSE) %>%
  mutate(leg_id = paste0(personId,departureTime))
arrivalInZone2 <- filterByRegion(legsOrTripsTable,zone2,crs=CRS,start.inshape = FALSE, end.inshape = TRUE) %>%
  mutate(leg_id = paste0(personId,departureTime))

#combine the above datasets to find legs / trips starting in one zone and ending in the other
zone1ToZone2 <- semi_join(departureInZone1, arrivalInZone2, by="leg_id")
zone2ToZone1 <- semi_join(departureInZone2, arrivalInZone1, by="leg_id")

interzonalLegs <- union(zone1ToZone2,zone2ToZone1)

meanTravTime <- mean(as.double(interzonalLegs$travelTime))
meanTravDist <- mean(as.double(interzonalLegs$travelDistance_m))
meanDirectTravDist <- mean(as.double(interzonalLegs$directTravelDistance_m))
meanWaitTime <- mean(as.double(interzonalLegs$waitTime))
interzonalLegsAbs <- nrow(interzonalLegs)
totalNoLegs <- nrow(legsOrTripsTable)
interzonalLegsRel <- interzonalLegsAbs / totalNoLegs

#save avg values into df
avgValues <- setNames(data.frame(matrix(ncol = 6, nrow = 0)), c("meanTravTime[s]", "meanTravDist[m]", "meanDirectTravDist[m]", "meanWaitTime[s]","interzonalLegsAbsolute", "interzonalLegsRelative"))

avgValuesDataset <- data.frame(meanTravTime, meanTravDist,meanDirectTravDist,meanWaitTime,interzonalLegsAbs,interzonalLegsRel)
names(avgValuesDataset) <- names(avgValues)
avgValues <- rbind(avgValues,avgValuesDataset)

if(!file.exists(paste0(directory,"analysis-stop-2-stop"))) {
  print("creating analysis sub-directory")
  dir.create(paste0(directory,"analysis-stop-2-stop"))
}

analysisDir <- paste0(directory,"/analysis-stop-2-stop/")

write.table(interzonalLegs,paste0(analysisDir,"drt_legs_av_interzonal_AS_DP.csv"),quote=FALSE, row.names=FALSE, dec=".", sep=";")
write.table(avgValues,paste0(analysisDir,"avgValues_legs_interzonal_AS_DP.tsv"),quote=FALSE, row.names=FALSE, dec=".", sep="\t")