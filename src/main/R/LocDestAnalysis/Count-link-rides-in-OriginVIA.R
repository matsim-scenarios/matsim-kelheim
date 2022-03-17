library(geosphere)
library(tidyverse)
library(dplyr)
library(terra)

setwd("/Users/tomkelouisa/Documents/VSP/Kehlheim/src/main/R/LocDestAnalysis")
source("neareststop.R")
source("countdrivenstops.R")



setwd("/Users/tomkelouisa/Documents/VSP/Kehlheimfiles")
# Daten von VIA einlesen
viaDataframe <- read.csv("VIA_Rides_202106_202201origin.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", sep= ";")


if(("fromstopID" %in% colnames(viaDataframe))==FALSE){
  #VSP-stops müssen den VIA Daten hinzugefügt werd
  ######################################

  # Daten der jeweiligen Haltestellen einlesen
  #stops <- read.csv("kelheim-drt-stops-locations(1).csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8")
  stops <- read.csv("KEXI_Haltestellen_Liste_Kelheim.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8",sep= ";")
  #convert UTM32 Coordinates to Longitude and Latitude, Haltestellen Daten sind als X und Y Koordinaten (UTM 32) angegeben
  #UTMCoord <- cbind(stops$X, stops$Y)
  #v <- vect(UTMCoord, crs="+proj=utm +zone=32 +datum=WGS84  +units=m")
  #y <- project(v, "+proj=longlat +datum=WGS84")
  #lonlat <- geom(y)[, c("x", "y")]
  #stops<-stops %>% mutate(stoplongitude=lonlat[,"x"],stoplatitude=lonlat[,"y"])


  ########################################
  # fügt zu den VIA Daten den Haltestellenwert von VSP hinzu

  viaDataframe <- viaDataframe  %>%
    rowwise() %>%
    #mutate(fromstopID=as.integer(neareststop(Origin.Longitude, Origin.latitude,"Stop.ID")))
    mutate(fromstopID=as.integer(neareststop(Origin.Longitude, Origin.latitude,"Haltestellen.Nr.")))

  viaDataframe <- viaDataframe  %>%
    rowwise() %>%
    #mutate(tostop_ID = as.integer(neareststop(Destination.Longitude, Destination.latitude,"Stop.ID")))
    mutate(tostop_ID=as.integer(neareststop(Destination.Longitude, Destination.latitude,"Haltestellen.Nr.")))

  setwd("/Users/tomkelouisa/Documents/VSP/Kehlheimfiles")
  write.csv2(stops,"stop.csv",quote = FALSE)
  write.csv2(viaDataframe, "VIA_Rides_202106_202201neu.csv",quote = FALSE)

}
# zählt die Anzahl der Fahrten von einem Stop zum nächsten
countdrivenlinks(viaDataframe,"fromstopID","tostop_ID","Stop.ID","Via-Origin-drt-count-Analysis-KEXI.tsv")




