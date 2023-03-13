

neareststop <- function(longitude,latitude,stopIDcolumn) {
  # calculate shortest distance between the VIA stops and the VSP stops
  print("long/Lat")
  print(longitude)
  print(latitude)
  bereich<-stops[stops[['lat']]>latitude-0.003&stops[['lat']]<latitude+0.003,]
  # bereich<-bereich[bereich$lon>longitude-0.003&bereich$lon<longitude+0.003,]
  bereich<-bereich[bereich[['lon']]>longitude-0.003&bereich[['lon']]<longitude+0.003,]
  print("Bereich")
  print(bereich)
  print(length(bereich[['lat']]))
  if(length(bereich[['lon']])==0){
    # vergrößert den Bereich, falls in den Bereich [long+-0.003,lat+-0.003] noch keine Haltestelle reinfällt
    i<-0.004
    while(length(bereich[['lat']])==0){
      bereich<-stops[stops[['lat']]>latitude-i&stops[['lat']]<latitude+i,]
      bereich<-bereich[bereich[['lon']]>longitude-i&bereich[['lon']]<longitude+i,]
      i<-i+0.001
    }

  }
  bereich <- bereich %>%
    rowwise() %>%
    mutate(distance_m = as.double(distGeo(c(as.double(longitude), as.double(latitude)),
                                          c(as.double(lon), as.double(lat)))))
  print(bereich$distance_m)
  # stopId <-bereich[which.min(bereich$distance_m), stopIDcolumn]
  stopId <- bereich$Haltestellen.Nr.[bereich$distance_m==min(bereich$distance_m)]
  # stopId <- grep(stopIDcolumn, colnames(bereich))[bereich$distance_m==min(bereich$distance_m)]
# [bereich$distance_m==min(bereich$distance_m)]
  print(stopId)

  return(stopId)


}
