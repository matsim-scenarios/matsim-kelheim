library(geosphere)
library(tidyverse)
library(dplyr)
library(terra)



setwd("/Users/tomkelouisa/Documents/VSP/")
# Daten von VIA einlesen
viaDataframe <- read.csv("VIA_Rides_202106_202201.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", sep= ";")


if(("fromstopID" %in% colnames(viaDataframe))==FALSE){
  #VSP-stops müssen den VIA Daten hinzugefügt werden
  ######################################

  # Daten der jeweiligen Haltestellen einlesen
  stops <- read.csv("kelheim-drt-stops-locations(1).csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8")
  #convert UTM32 Coordinates to Longitude and Latitude, Haltestellen Daten sind als X und Y Koordinaten (UTM 32) angegeben
  UTMCoord <- cbind(stops$X, stops$Y)
  v <- vect(UTMCoord, crs="+proj=utm +zone=32 +datum=WGS84  +units=m")
  y <- project(v, "+proj=longlat +datum=WGS84")
  lonlat <- geom(y)[, c("x", "y")]
  stops<-stops %>% mutate(stoplongitude=lonlat[,"x"],stoplatitude=lonlat[,"y"])


  ########################################
  # fügt zu den VIA Daten den Haltestellenwert von VSP hinzu

  viaDataframe <- viaDataframe  %>%
    rowwise() %>%
    mutate(fromstopID=as.integer(neareststop(Origin.Longitude, Origin.latitude)))

  viaDataframe <- viaDataframe  %>%
    rowwise() %>%
    mutate(tostop_ID = as.integer(neareststop(Destination.Longitude, Destination.latitude)))

  setwd("/Users/tomkelouisa/Documents/VSP")
  write.csv2(stops,"stop.csv",quote = FALSE)
  write.csv2(viaDataframe, "VIA_Rides_202106_202201.csv",quote = FALSE)

}
# zählt die Anzahl der Fahrten von einem Stop zum nächsten
countdrivenlinks(viaDataframe,"fromstopID","tostop_ID","Stop.ID","ViaStoptoStop.csv")

######################################
#shortest distance

neareststop <- function(longitude,latitude) {
  # calculate shortest distance between the VIA stops and the VSP stops
  bereich<-stops[stops$stoplatitude>latitude-0.003&stops$stoplatitude<latitude+0.003,]
  bereich<-bereich[bereich$stoplongitude>longitude-0.003&bereich$stoplongitude<longitude+0.003,]
  print(bereich)
  print(length(bereich$stoplatitude))
  if(length(bereich$stoplatitude)==0){
    # vergrößert den Bereich, falls in den Bereich [long+-0.003,lat+-0.003] noch keine Haltestelle reinfällt
    i<-0.004
    while(length(bereich$stoplatitude)==0){

      bereich<-stops[stops$stoplatitude>latitude-i&stops$stoplatitude<latitude+i,]
      bereich<-bereich[bereich$stoplongitude>longitude-i&bereich$stoplongitude<longitude+i,]
      i<-i+0.001
    }

  }
  bereich <- bereich %>%
    rowwise() %>%
    mutate(distance_m = as.double(distGeo(c(as.double(longitude), as.double(latitude)),
                                          c(as.double(stoplongitude), as.double(stoplatitude)))))
  print(bereich$distance_m)
  stopId <-bereich[which.min(bereich$distance_m),"Stop.ID"]

  return(stopId)


}


countdrivenlinks <- function(movements, fromstopID,tostop_ID,Stop.ID,csvname){

  sortedMovement <- movements[order(movements$fromstopID,movements$tostop_ID), ]

  #erstellt Vektoren / vll noch eone schönere Lösung möglich
  laenge <- length(movements$fromstopID)
  fromLink <-character(0)
  toLink <- character(0)
  anzahlFahrten <- character(0)
  fromstopIds <- character(0)
  tostopIds <- character(0)

  Fahrten <- 1 # gibt Anzahl der Fahrten zwischen zwei Links an
  connection <- c(sortedMovement[1,fromstopID],sortedMovement[1,tostop_ID]) # Vektor mit einem from und einem to Link drin

  #Iteration durch sorted Movement, wobei paarweise die Tuple (hier Vektoren, connection und newConnection) verglichen werden
  # sind sie identisch, wird Fahrten+1 gerechnet, ansonsten werden di eTuple abgespeichert und das nächste Tupel wird verglichen
  for(row in 2:laenge){
    newConnection <- c(sortedMovement[row,fromstopID],sortedMovement[row,tostop_ID]) # nächster Vektor mit einem from und einem to Link drin
    if (identical(connection,newConnection)){
      Fahrten <- Fahrten + 1
    }
    else{
      fromLink <- c(fromLink,connection[1])
      toLink <- c(toLink,connection[2])
      anzahlFahrten <- c(anzahlFahrten,Fahrten)
      Fahrten <- 1
      fromstopIds <- c(fromstopIds,as.character(stops$Stop.ID[which(stops$Stop.ID==connection[1],arr.ind=FALSE)]))
      tostopIds <- c(tostopIds,as.character(stops$Stop.ID[which(stops$Stop.ID==connection[2],arr.ind=FALSE)]))
      if (length(fromstopIds)!=length(tostopIds)){
        #bei nicht exitsierenden toStops
        tostopIds <- c(tostopIds,"NA")
      }
      print(connection)
      connection <- newConnection

    }
  }

  #mögliche letztes Tupel abfangen
  connection <- newConnection
  fromLink <- c(fromLink,connection[1])
  toLink <- c(toLink,connection[2])
  anzahlFahrten <- c(anzahlFahrten,Fahrten)
  fromstopIds <- c(fromstopIds,as.character(stops$Stop.ID[which(stops$Stop.ID==connection[1],arr.ind=FALSE)]))
  tostopIds <- c(tostopIds,as.character(stops$Stop.ID[which(stops$Stop.ID==connection[2],arr.ind=FALSE)]))


  # in Datadfame speichern und als csv Datei abspeichern
  if (Stop.ID=="Link.ID"){
    class.df <- data.frame(fromstopIds,tostopIds, fromLink,toLink,anzahlFahrten,stringsAsFactors = FALSE)
  }
  else{
    class.df <- data.frame(fromstopIds,tostopIds,anzahlFahrten,stringsAsFactors = FALSE)
  }




  setwd("/Users/tomkelouisa/Documents/VSP")

  write.csv2(class.df, csvname,quote = FALSE)
  #print(laenge)
  #print(sum(as.integer(class.df$anzahlFahrten)))
}

