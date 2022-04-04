library(geosphere)
library(tidyverse)
library(dplyr)
library(terra)

######################
# dies sind die Variablen, die man für den eigenen Gebrauch anpassen muss
programPath <- "/Users/tomkelouisa/Documents/VSP/Kehlheim/src/main/R/LocDestAnalysis" # hier sollen die Funktion countdrivenstops.R liegen
filePath <- "/Users/tomkelouisa/Documents/VSP/Kehlheimfiles" # hier sollen die beiden folgenden Files liegen
originfilename <- "IOKI_Rides_202006_202105.csv" # Filename der Realdaten
haltestellenFile <- "KEXI_Haltestellen_Liste_Kelheim.csv" # Filename der Haltestellenliste
nameAnalyseFile <- "IOKI-Origin-drt-count-Analysis-KEXI.tsv" # Filename des Analyseoutputfiles, in tsv
startdate <- "19-12-2020"  # wenn das ganze File ausgewertet werden soll, schreibe hier -1 rein
tage <- 1 # Anzahl an Tagen die ausgewertet werden sollen
csvfilename <- "stop2stopridesIOKI.csv"

setwd(programPath)

source("countdrivenstops.R")

setwd(filePath)
# Daten von IOKI einlesen
IOKIDataframe <- read.csv(originfilename, stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", sep= ";")


IOKIDataframe <- IOKIDataframe %>% mutate(Ankunftszeit = dmy_hms(Ankunftszeit))
# den Zeitbereich auswählen
if (startdate!=-1){
  d <- interval(dmy(startdate),dmy(startdate)+days(x=tage))
  IOKIDataframe <- IOKIDataframe %>% filter(Ankunftszeit %within% d)
}



stops <- read.csv(haltestellenFile, stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", sep= ";")


names(IOKIDataframe)[names(IOKIDataframe)=="Abfahrtsortsname"] <- "fromstopID"
names(IOKIDataframe)[names(IOKIDataframe)=="Ankunftsortsname"] <- "tostop_ID"
countdrivenlinks(IOKIDataframe,"fromstopID","tostop_ID",nameAnalyseFile)
