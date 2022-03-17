library(geosphere)
library(tidyverse)
library(dplyr)
library(terra)

setwd("/Users/tomkelouisa/Documents/VSP/Kehlheim/src/main/R/LocDestAnalysis")
source("neareststop.R")
source("countdrivenstops.R")

setwd("/Users/tomkelouisa/Documents/VSP/Kehlheimfiles")
# Daten von VIA einlesen
#IOKIDataframe <- read.csv("IOKI_Rides_202006_202105.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", sep= ";")

stops <- read.csv("KEXI_Haltestellen_Liste_Kelheim.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", sep= ";")


if(("fromstopID" %in% colnames(IOKIDataframe))==FALSE){
  #VSP-stops müssen den VIA Daten hinzugefügt werden
  ########################################
  # fügt zu den VIA Daten den Haltestellenwert von VSP hinzu

  IOKIDataframe <- IOKIDataframe %>%
    rowwise() %>%
    mutate(fromstopID=as.integer(neareststop(Kalkulierter.Abfahrtsort..lon., Kalkulierter.Abfahrtsort..lat.,"Haltestellen.Nr.")))

  IOKIDataframe <- IOKIDataframe  %>%
    rowwise() %>%
    mutate(tostop_ID = as.integer(neareststop(Kalkulierter.Ankunftsort..lon., Kalkulierter.Ankunftsort..lat.,"Haltestellen.Nr.")))

  setwd("/Users/tomkelouisa/Documents/VSP/Kehlheimfiles")

  write.csv2(IOKIDataframe, "data.csv",quote = FALSE)
  write.table(IOKIDataframe,"IOKI-Origin-drt-count-Analysis.tsv",quote=FALSE, sep="\t",col.names = NA,row.names = TRUE)

}
countdrivenlinks(IOKIDataframe,"fromstopID","tostop_ID","Haltestellen.Nr.","IOKI-Origin-drt-count-Analysis.tsv")
