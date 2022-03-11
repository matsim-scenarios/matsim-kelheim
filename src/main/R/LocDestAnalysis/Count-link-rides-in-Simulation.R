

setwd("/Users/tomkelouisa/Documents/VSP/")
#Daten Stopdaten einlesen
stops <- read.csv("kelheim-drt-stops-locations(1).csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8")
movements <- read.csv("KEXI-base-case.passingQ.250.drt_legs_drt.csv", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", sep= ";")

#Sortiert erst Dataframe erst nach "fromLinkId" und dann nach "toLinkId"
sortedMovement <- movements[order(movements$fromLinkId,movements$toLinkId), ]

#erstellt Vektoren / vll noch eone schönere Lösung möglich
laenge <- length(movements$fromLinkId)
fromLink <-character(0)
toLink <- character(0)
anzahlFahrten <- character(0)
fromstopIds <- character(0)
tostopIds <- character(0)



Fahrten <- 1 # gibt Anzahl der Fahrten zwischen zwei Links an
connection <- c(sortedMovement[1,"fromLinkId"],sortedMovement[1,"toLinkId"]) # Vektor mit einem from und einem to Link drin

#Iteration durch sorted Movement, wobei paarweise die Tuple (hier Vektoren, connection und newConnection) verglichen werden
# sind sie identisch, wird Fahrten+1 gerechnet, ansonsten werden di eTuple abgespeichert und das nächste Tupel wird verglichen
for(row in 2:laenge){
  newConnection <- c(sortedMovement[row,"fromLinkId"],sortedMovement[row,"toLinkId"]) # nächster Vektor mit einem from und einem to Link drin
  if (identical(connection,newConnection)){
    Fahrten <- Fahrten + 1
    print("Jujeee")
    }
  else{
      fromLink <- c(fromLink,connection[1])
      toLink <- c(toLink,connection[2])
      anzahlFahrten <- c(anzahlFahrten,Fahrten)
      Fahrten <- 1
      fromstopIds <- c(fromstopIds,as.character(stops$Stop.ID[which(stops$Link.ID==connection[1],arr.ind=FALSE)]))
      tostopIds <- c(tostopIds,as.character(stops$Stop.ID[which(stops$Link.ID==connection[2],arr.ind=FALSE)]))
      if (length(fromstopIds)!=length(tostopIds)){
        #bei nicht exitsierenden toStops
        tostopIds <- c(tostopIds,"NA")
      }
      print(connection)
      connection <- newConnection

    }
}
print(length(fromstopIds))
#mögliche letztes Tupel abfangen
if (Fahrten>=2){
  fromLink <- c(fromLink,connection[1])
  toLink <- c(toLink,connection[2])
  anzahlFahrten <- c(anzahlFahrten,Fahrten)
  fromstopIds <- c(fromstopIds,as.character(stops$Stop.ID[which(stops$Link.ID==connection[1],arr.ind=FALSE)]))
  tostopIds <- c(tostopIds,as.character(stops$Stop.ID[which(stops$Link.ID==connection[2],arr.ind=FALSE)]))
}
print(length(tostopIds))
# in Datadfame speichern und als csv Datei abspeichern
class.df <- data.frame(fromstopIds,tostopIds, fromLink,toLink,anzahlFahrten,stringsAsFactors = FALSE)
print(class.df)

setwd("/Users/tomkelouisa/Documents/VSP")

write.csv(class.df, "LinktoLinkAnzahl.csv",quote = FALSE)



