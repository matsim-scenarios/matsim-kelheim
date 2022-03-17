countdrivenlinks <- function(movements, fromstopID,tostop_ID,Stop.ID,tsvname){

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
      fromstopIds <- c(fromstopIds,as.character(connection[1]))
      tostopIds <- c(tostopIds,as.character(connection[2]))
      #fromstopIds <- c(fromstopIds,as.character(stops[,Stop.ID[which(stops[,Stop.ID==connection[1]],arr.ind=FALSE)]]))
      #tostopIds <- c(tostopIds,as.character(stops$Stop.ID[which(stops$Stop.ID==connection[2],arr.ind=FALSE)]))
      #tostopIds <- c(tostopIds,as.character(stops[,Stop.ID[which(stops[,Stop.ID==connection[2]],arr.ind=FALSE)]]))
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
  fromstopIds <- c(fromstopIds,as.character(connection[1]))
  tostopIds <- c(tostopIds,as.character(connection[2]))
  #fromstopIds <- c(fromstopIds,as.character(stops[,Stop.ID[which(stops[,Stop.ID==connection[1]],arr.ind=FALSE)]]))
  #tostopIds <- c(tostopIds,as.character(stops[,Stop.ID[which(stops[,Stop.ID==connection[2]],arr.ind=FALSE)]]))


  # in Datadfame speichern und als csv Datei abspeichern
  if (Stop.ID=="Link.ID"){
    class.df <- data.frame(fromstopIds,tostopIds, fromLink,toLink,anzahlFahrten,stringsAsFactors = FALSE)
  }
  else{
    class.df <- data.frame(fromstopIds,tostopIds,anzahlFahrten,stringsAsFactors = FALSE)
  }




  setwd("/Users/tomkelouisa/Documents/VSP/Kehlheimfiles")

  #write.csv2(class.df, csvname,quote = FALSE)
  write.table(class.df,tsvname,quote=FALSE, sep="\t",col.names = NA,row.names = TRUE)
  #print(laenge)
  #print(sum(as.integer(class.df$anzahlFahrten)))
}
