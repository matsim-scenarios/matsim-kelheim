countdrivenlinks <- function(movements, fromstopID,tostop_ID,tsvname){

  sortedMovement <- movements[order(movements$fromstopID,movements$tostop_ID), ]

  #erstellt Vektoren / vll noch eone schönere Lösung möglich
  laenge <- length(movements$fromstopID)
  fromLink <-character(0)
  toLink <- character(0)
  anzahlFahrten <- character(0)
  fromstopIds <- character(0)
  tostopIds <- character(0)
  tolat <- character(0)
  tolon <- character(0)
  fromlat <- character(0)
  fromlon <- character(0)

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
      fromstopIds <- c(fromstopIds,as.character(connection[1]))
      tostopIds <- c(tostopIds,as.character(connection[2]))
      tolat <- c(tolat,as.character(stops$lat[which(stops$Haltestellen.Nr.==connection[2],arr.ind=FALSE)]))
      tolon <- c(tolon,as.character(stops$lon[which(stops$Haltestellen.Nr.==connection[2],arr.ind=FALSE)]))
      fromlat <- c(fromlat,as.character(stops$lat[which(stops$Haltestellen.Nr.==connection[1],arr.ind=FALSE)]))
      fromlon <- c(fromlon,as.character(stops$lon[which(stops$Haltestellen.Nr.==connection[1],arr.ind=FALSE)]))

      connection <- newConnection

    }
  }

  #mögliche letztes Tupel abfangen
  connection <- newConnection
  fromLink <- c(fromLink,connection[1])
  toLink <- c(toLink,connection[2])
  anzahlFahrten <- c(anzahlFahrten,Fahrten)
  fromstopIds <- c(fromstopIds,as.character(connection[1]))
  tostopIds <- c(tostopIds,as.character(connection[2]))
  tolat <- c(tolat,as.character(stops$lat[which(stops$Haltestellen.Nr.==connection[2],arr.ind=FALSE)]))
  tolon <- c(tolon,as.character(stops$lon[which(stops$Haltestellen.Nr.==connection[2],arr.ind=FALSE)]))
  fromlat <- c(fromlat,as.character(stops$lat[which(stops$Haltestellen.Nr.==connection[1],arr.ind=FALSE)]))
  fromlon <- c(fromlon,as.character(stops$lon[which(stops$Haltestellen.Nr.==connection[1],arr.ind=FALSE)]))
  #fromstopIds <- c(fromstopIds,as.character(stops[,Stop.ID[which(stops[,Stop.ID==connection[1]],arr.ind=FALSE)]]))
  #tostopIds <- c(tostopIds,as.character(stops[,Stop.ID[which(stops[,Stop.ID==connection[2]],arr.ind=FALSE)]]))


  # in Datadfame speichern und als csv Datei abspeichern

  class.df <- data.frame(fromstopIds,tostopIds,fromlat,fromlon,tolat,tolon,anzahlFahrten,stringsAsFactors = FALSE)





  setwd(filePath)


  write.table(class.df,tsvname,quote=FALSE, sep="\t",col.names = NA,row.names = TRUE)

}
