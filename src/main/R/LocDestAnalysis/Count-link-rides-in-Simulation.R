library(tidyverse)

######################
##INPUT##


# Path to stops file
# stopsPath <- "C:/Users/Simon/Documents/public-svn/matsim/scenarios/countries/de/kelheim/projects/KelRide/AVServiceAreas/input/kelheim-v2.0-drt-stops-locations.csv"
stopsPath <- "C:/Users/Simon/Documents/public-svn/matsim/scenarios/countries/de/kelheim/projects/KelRide/AVServiceAreas/input/Hohenpfahl-av-stops-locations.csv"
# path to run main output dir
# runDirectory <- "C:/Users/Simon/Documents/public-svn/matsim/scenarios/countries/de/kelheim/projects/KelRide/AVServiceAreas/output/KEXI-base-case/"
runDirectory <- "C:/Users/Simon/Documents/public-svn/matsim/scenarios/countries/de/kelheim/projects/KelRide/AVServiceAreas/output/KEXI-with-AV/AV-CORE-HOHENPFAHL/"

# mode to be analyzed. set either drt or av
mode <- "av"

##############################
## SCRIPT ##


outputDir <- paste(runDirectory, "analysis-stop-2-stop", sep = "") # the plots are going to be saved here
if(!file.exists(outputDir)){
  print("creating analysis sub-directory")
  dir.create(outputDir)  
}


#Daten Stopdaten einlesen
stops <- read.csv(stopsPath,
                  stringsAsFactors = FALSE,
                  header = TRUE,
                  encoding = "UTF-8")

fileEnding <- paste("*.drt_legs_", mode, ".csv", sep ="")

# Simulierte drt Daten einlesen
movements <- read.csv(list.files(paste(runDirectory, "ITERS/it.999/", sep=""), pattern = fileEnding, full.names = T, include.dirs = F),
                      stringsAsFactors = FALSE,
                      header = TRUE,
                      encoding = "UTF-8",
                      sep= ";")

#Sortiert erst Dataframe erst nach "fromLinkId" und dann nach "toLinkId"
sortedMovement <- movements[order(movements$fromLinkId,movements$toLinkId), ]

#erstellt Vektoren / vll noch eine schönere Lösung möglich
laenge <- length(movements$fromLinkId)
fromLink <-character(0)
toLink <- character(0)
anzahlFahrten <- character(0)
fromstopIds <- character(0)
tostopIds <- character(0)
toX <- character(0)
toY <- character(0)
fromX <- character(0)
fromY <- character(0)


Fahrten <- 1 # gibt Anzahl der Fahrten zwischen zwei Links an
connection <- c(sortedMovement[1,"fromLinkId"],sortedMovement[1,"toLinkId"]) # Vektor mit einem from und einem to Link drin

#Iteration durch sorted Movement, wobei paarweise die Tuple (hier Vektoren, connection und newConnection) verglichen werden
# sind sie identisch, wird Fahrten+1 gerechnet, ansonsten werden di eTuple abgespeichert und das nächste Tupel wird verglichen
for(row in 2:laenge){
  newConnection <- c(sortedMovement[row,"fromLinkId"],sortedMovement[row,"toLinkId"]) # nächster Vektor mit einem from und einem to Link drin
  if (identical(connection,newConnection)){
    Fahrten <- Fahrten + 1

    }
  else{
      fromLink <- c(fromLink,connection[1])
      toLink <- c(toLink,connection[2])
      anzahlFahrten <- c(anzahlFahrten,Fahrten)
      Fahrten <- 1
      fromstopIds <- c(fromstopIds,as.character(stops$Stop.ID[which(stops$Link.ID==connection[1],arr.ind=FALSE)]))
      tostopIds <- c(tostopIds,as.character(stops$Stop.ID[which(stops$Link.ID==connection[2],arr.ind=FALSE)]))
      toX <- c(toX,as.character(stops$X[which(stops$Link.ID==connection[2],arr.ind=FALSE)]))
      toY <- c(toY,as.character(stops$Y[which(stops$Link.ID==connection[2],arr.ind=FALSE)]))
      fromX <- c(fromX,as.character(stops$X[which(stops$Link.ID==connection[1],arr.ind=FALSE)]))
      fromY <- c(fromY,as.character(stops$Y[which(stops$Link.ID==connection[1],arr.ind=FALSE)]))
      if (length(fromstopIds)!=length(tostopIds)){
        #bei nicht exitsierenden toStops
        tostopIds <- c(tostopIds,"NA")
        toX <- c(toX,"NA")
        toY <- c(toY,"NA")
        #fromX <- c(fromX,"NA")
        #fromY <- c(fromY,"NA")
      }

      connection <- newConnection

    }
}

  #das letztes Tupel abfangen
  connection <- newConnection
  fromLink <- c(fromLink,connection[1])
  toLink <- c(toLink,connection[2])
  anzahlFahrten <- c(anzahlFahrten,Fahrten)
  fromstopIds <- c(fromstopIds,as.character(stops$Stop.ID[which(stops$Link.ID==connection[1],arr.ind=FALSE)]))
  tostopIds <- c(tostopIds,as.character(stops$Stop.ID[which(stops$Link.ID==connection[2],arr.ind=FALSE)]))
  toX <- c(toX,as.character(stops$X[which(stops$Link.ID==connection[2],arr.ind=FALSE)]))
  toY <- c(toY,as.character(stops$Y[which(stops$Link.ID==connection[2],arr.ind=FALSE)]))
  fromX <- c(fromX,as.character(stops$X[which(stops$Link.ID==connection[1],arr.ind=FALSE)]))
  fromY <- c(fromY,as.character(stops$Y[which(stops$Link.ID==connection[1],arr.ind=FALSE)]))


# in Datadfame speichern und als csv Datei abspeichern
class.df <- data.frame(fromstopIds,tostopIds, fromLink,toLink,fromX,fromY,toX,toY,anzahlFahrten,stringsAsFactors = FALSE)
class.smalldf <- data.frame(fromstopIds,tostopIds,anzahlFahrten,stringsAsFactors = FALSE) %>% 
  filter(!is.na(fromstopIds) & !is.na(tostopIds))


print(paste( "writing to ", outputDir, "/stop-2-stop-", mode, ".csv", sep=""))
write.csv2(class.smalldf,paste(outputDir, "/stop-2-stop-", mode, ".csv", sep=""),quote=FALSE, row.names=FALSE)
print(paste( "writing to ", outputDir, "/stop-2-stop-", mode, ".tsv", sep=""))
write.table(class.df,paste(outputDir, "/stop-2-stop-", mode, "-detailed.tsv", sep=""),quote=FALSE, sep="\t",col.names = NA,row.names = TRUE)



