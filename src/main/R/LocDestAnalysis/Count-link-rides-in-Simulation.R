library(tidyverse)

######################
# dies sind die Variablen, die man für den eigenen Gebrauch anpassen muss
filePath <- "/Users/tomkelouisa/Documents/VSP/Kehlheimfiles" # hier sollen die beiden folgenden Files liegen
simulationfilename <- "KEXI-base-case.passingQ.250.drt_legs_drt.csv" # Filename der Realdaten
haltestellenFile <- "kelheim-drt-stops-locations(1).csv" # Filename der Haltestellenliste
nameAnalyseFile <- "Simulation-drt-Analyse-Anzahl.tsv" # Filename des Analyseoutputfiles, in tsv
csvfilename <- "stop2stoprides.csv"
# wenn die obigen Daten eingegeben sind, kann das Programm gestartet werden und es wird das outputfile im filePath erstellt
##############################


setwd(filePath)
#Daten Stopdaten einlesen
stops <- read.csv("kelheim-drt-stops-locations(1).csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8")
# Simulierte drt Daten einlesen
movements <- read.csv(simulationfilename, stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", sep= ";")

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
class.smalldf <- data.frame(fromstopIds,tostopIds,anzahlFahrten,stringsAsFactors = FALSE)

setwd(filePath)

write.csv2(class.smalldf,csvfilename,quote=FALSE, row.names=FALSE)
write.table(class.df,nameAnalyseFile,quote=FALSE, sep="\t",col.names = NA,row.names = TRUE)



