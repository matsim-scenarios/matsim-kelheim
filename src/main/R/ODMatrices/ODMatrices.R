if(require("matsim")){
  install.packages("matsim")
  library(matsim)
}
path_to_base = "052"
path_to_av = "av"
path_to_calibrun = "calibrun"
path_to_result = "another_test"
crs = 25832

if(!file.exists(path_to_result)){
  dir.create(path_to_result)
}

####################################################################################################
#Reading calibrun runs
calibrun_1111 = readTripsTable(paste0(path_to_calibrun,"/1111_kexi.output_trips.csv.gz"))
calibrun_1234 = readTripsTable(paste0(path_to_calibrun,"/1234_kexi.output_trips.csv.gz"))
calibrun_2222 = readTripsTable(paste0(path_to_calibrun,"/2222_kexi.output_trips.csv.gz"))
calibrun_4711 = readTripsTable(paste0(path_to_calibrun,"/4711_kexi.output_trips.csv.gz"))
calibrun_5678 = readTripsTable(paste0(path_to_calibrun,"/5678_kexi.output_trips.csv.gz"))

#Generate ODMatrices
calibrun_1111_od = deriveODMatrix(calibrun_1111,shapePath = shape_path,crs = crs,colnames = "name", outer = TRUE)
calibrun_1234_od = deriveODMatrix(calibrun_1234,shapePath = shape_path,crs = crs,colnames = "name", outer = TRUE)
calibrun_2222_od = deriveODMatrix(calibrun_2222,shapePath = shape_path,crs = crs,colnames = "name", outer = TRUE)
calibrun_4711_od = deriveODMatrix(calibrun_4711,shapePath = shape_path,crs = crs,colnames = "name", outer = TRUE)
calibrun_5678_od = deriveODMatrix(calibrun_5678,shapePath = shape_path,crs = crs,colnames = "name", outer = TRUE)

# Take sum and average of all matrices
calibrun_od_sum = calibrun_1111_od+calibrun_1234_od+calibrun_2222_od+calibrun_4711_od+calibrun_5678_od
calibrun_od_result = calibrun_od_sum/5

#Save result
if(!file.exists(paste0(path_to_result,"/kexi-only"))){
  dir.create(paste0(path_to_result,"/kexi-only"))
  }
write.table(calibrun_od_result,paste0(paste0(path_to_result,"/kexi-only"),"/ODMatrix_kexi_only.csv"),row.names = FALSE,sep = ";")

####################################################################################################
##BAUERNSIEDLUNG from AV##
bauer_1111 = readTripsTable(paste0(path_to_av,"/1111BAUER_kelheim-v2.0-25pct-av.output_trips.csv.gz"))
bauer_1234 = readTripsTable(paste0(path_to_av,"/1234BAUER_kelheim-v2.0-25pct-av.output_trips.csv.gz"))
bauer_2222 = readTripsTable(paste0(path_to_av,"/2222BAUER_kelheim-v2.0-25pct-av.output_trips.csv.gz"))
bauer_4711 = readTripsTable(paste0(path_to_av,"/4711BAUER_kelheim-v2.0-25pct-av.output_trips.csv.gz"))
bauer_5678 = readTripsTable(paste0(path_to_av,"/5678BAUER_kelheim-v2.0-25pct-av.output_trips.csv.gz"))

#Generate ODMatrices
bauer_1111_od = deriveODMatrix(bauer_1111,shape_path,crs,colnames = "name", outer = TRUE)
bauer_1234_od = deriveODMatrix(bauer_1234,shape_path,crs,colnames = "name", outer = TRUE)
bauer_2222_od = deriveODMatrix(bauer_2222,shape_path,crs,colnames = "name", outer = TRUE)
bauer_4711_od = deriveODMatrix(bauer_4711,shape_path,crs,colnames = "name", outer = TRUE)
bauer_5678_od = deriveODMatrix(bauer_5678,shape_path,crs,colnames = "name", outer = TRUE)

# Take sum and average of all matrices
bauer_od_sum = bauer_1111_od+bauer_1234_od+bauer_2222_od+bauer_4711_od+bauer_5678_od
bauer_od_result = bauer_od_sum/5

#Save result
if(!file.exists(paste0(path_to_result,"/kexi-with-av"))){
  dir.create(paste0(path_to_result,"/kexi-with-av"))
}
if(!file.exists(paste0(path_to_result,"/kexi-with-av","/BAUERNSIEDLUNG"))){
  dir.create(paste0(path_to_result,"/kexi-with-av","/BAUERNSIEDLUNG"))
}
write.table(bauer_od_result,paste0(path_to_result,"/kexi-with-av","/BAUERNSIEDLUNG","/ODMatrix_bauernsiedlung.csv"),row.names = FALSE,sep = ";")

####################################################################################################
#CORE from AV
CORE_1111 = readTripsTable(paste0(path_to_av,"/1111CORE_kelheim-v2.0-25pct-av.output_trips.csv.gz"))
CORE_1234 = readTripsTable(paste0(path_to_av,"/1234CORE_kelheim-v2.0-25pct-av.output_trips.csv.gz"))
CORE_2222 = readTripsTable(paste0(path_to_av,"/2222CORE_kelheim-v2.0-25pct-av.output_trips.csv.gz"))
CORE_4711 = readTripsTable(paste0(path_to_av,"/4711CORE_kelheim-v2.0-25pct-av.output_trips.csv.gz"))
CORE_5678 = readTripsTable(paste0(path_to_av,"/5678CORE_kelheim-v2.0-25pct-av.output_trips.csv.gz"))

#Generate ODMatrices
CORE_1111_od = deriveODMatrix(CORE_1111,shape_path,crs,colnames = "name", outer = TRUE)
CORE_1234_od = deriveODMatrix(CORE_1234,shape_path,crs,colnames = "name", outer = TRUE)
CORE_2222_od = deriveODMatrix(CORE_2222,shape_path,crs,colnames = "name", outer = TRUE)
CORE_4711_od = deriveODMatrix(CORE_4711,shape_path,crs,colnames = "name", outer = TRUE)
CORE_5678_od = deriveODMatrix(CORE_5678,shape_path,crs,colnames = "name", outer = TRUE)

# Take sum and average of all matrices
CORE_od_sum = CORE_1111_od+CORE_1234_od+CORE_2222_od+CORE_4711_od+CORE_5678_od
CORE_od_result = CORE_od_sum/5

#Save result
if(!file.exists(paste0(path_to_result,"/kexi-with-av"))){
  dir.create(paste0(path_to_result,"/kexi-with-av"))
}
if(!file.exists(paste0(path_to_result,"/kexi-with-av","/CORE"))){
  dir.create(paste0(path_to_result,"/kexi-with-av","/CORE"))
}
write.table(CORE_od_result,paste0(path_to_result,"/kexi-with-av","/CORE","/ODMatrix_CORE.csv"),row.names = FALSE,sep = ";")


####################################################################################################
#CORE WITH SHOPS from AV
CWS_1111 = readTripsTable(paste0(path_to_av,"/1111CWS_kelheim-v2.0-25pct-av.output_trips.csv.gz"))
CWS_1234 = readTripsTable(paste0(path_to_av,"/1234CWS_kelheim-v2.0-25pct-av.output_trips.csv.gz"))
CWS_2222 = readTripsTable(paste0(path_to_av,"/2222CWS_kelheim-v2.0-25pct-av.output_trips.csv.gz"))
CWS_4711 = readTripsTable(paste0(path_to_av,"/4711CWS_kelheim-v2.0-25pct-av.output_trips.csv.gz"))
CWS_5678 = readTripsTable(paste0(path_to_av,"/5678CWS_kelheim-v2.0-25pct-av.output_trips.csv.gz"))

#Generate ODMatrices
CWS_1111_od = deriveODMatrix(CWS_1111,shape_path,crs,colnames = "name", outer = TRUE)
CWS_1234_od = deriveODMatrix(CWS_1234,shape_path,crs,colnames = "name", outer = TRUE)
CWS_2222_od = deriveODMatrix(CWS_2222,shape_path,crs,colnames = "name", outer = TRUE)
CWS_4711_od = deriveODMatrix(CWS_4711,shape_path,crs,colnames = "name", outer = TRUE)
CWS_5678_od = deriveODMatrix(CWS_5678,shape_path,crs,colnames = "name", outer = TRUE)

# Take sum and average of all matrices
CWS_od_sum = CWS_1111_od+CWS_1234_od+CWS_2222_od+CWS_4711_od+CWS_5678_od
CWS_od_result = CWS_od_sum/5

#Save result
if(!file.exists(paste0(path_to_result,"/kexi-with-av"))){
  dir.create(paste0(path_to_result,"/kexi-with-av"))
}
if(!file.exists(paste0(path_to_result,"/kexi-with-av","/CWS"))){
  dir.create(paste0(path_to_result,"/kexi-with-av","/CWS"))
}
write.table(CWS_od_result,paste0(path_to_result,"/kexi-with-av","/CWS","/ODMatrix_CWS.csv"),row.names = FALSE,sep = ";")



####################################################################################################
#HOHENPFAHL from AV
HOH_1111 = readTripsTable(paste0(path_to_av,"/1111HOH_kelheim-v2.0-25pct-av.output_trips.csv.gz"))
HOH_1234 = readTripsTable(paste0(path_to_av,"/1234HOH_kelheim-v2.0-25pct-av.output_trips.csv.gz"))
HOH_2222 = readTripsTable(paste0(path_to_av,"/2222HOH_kelheim-v2.0-25pct-av.output_trips.csv.gz"))
HOH_4711 = readTripsTable(paste0(path_to_av,"/4711HOH_kelheim-v2.0-25pct-av.output_trips.csv.gz"))
HOH_5678 = readTripsTable(paste0(path_to_av,"/5678HOH_kelheim-v2.0-25pct-av.output_trips.csv.gz"))

#Generate ODMatrices
HOH_1111_od = deriveODMatrix(HOH_1111,shape_path,crs,colnames = "name", outer = TRUE)
HOH_1234_od = deriveODMatrix(HOH_1234,shape_path,crs,colnames = "name", outer = TRUE)
HOH_2222_od = deriveODMatrix(HOH_2222,shape_path,crs,colnames = "name", outer = TRUE)
HOH_4711_od = deriveODMatrix(HOH_4711,shape_path,crs,colnames = "name", outer = TRUE)
HOH_5678_od = deriveODMatrix(HOH_5678,shape_path,crs,colnames = "name", outer = TRUE)

# Take sum and average of all matrices
HOH_od_sum = HOH_1111_od+HOH_1234_od+HOH_2222_od+HOH_4711_od+HOH_5678_od
HOH_od_result = HOH_od_sum/5

#Save result
if(!file.exists(paste0(path_to_result,"/kexi-with-av"))){
  dir.create(paste0(path_to_result,"/kexi-with-av"))
}
if(!file.exists(paste0(path_to_result,"/kexi-with-av","/HOHENPFAHL"))){
  dir.create(paste0(path_to_result,"/kexi-with-av","/HOHENPFAHL"))
}
write.table(CWS_od_result,paste0(path_to_result,"/kexi-with-av","/HOHENPFAHL","/ODMatrix_HOHENPFAHL.csv"),row.names = FALSE,sep = ";")


#######################################################
#Create Delta tables

delta_calibrun_bauernsiedlung = calibrun_od_result - bauer_od_result
delta_calibrun_core = calibrun_od_result - CORE_od_result
delta_calibrun_cws = calibrun_od_result - CWS_od_result
delta_calibrun_hoh = calibrun_od_result - HOH_od_result

#Save result
if(!file.exists(paste0(path_to_result,"/delta"))){
  dir.create(paste0(path_to_result,"/delta"))
}

write.table(delta_calibrun_bauernsiedlung,paste0(path_to_result,"/delta","/ODMatrix_kexi_only-BAUERNSIEDLUNDG.csv"),row.names = FALSE,sep = ";")
write.table(delta_calibrun_core,paste0(path_to_result,"/delta","/ODMatrix_kexi_only-CORE.csv"),row.names = FALSE,sep = ";")
write.table(delta_calibrun_cws,paste0(path_to_result,"/delta","/ODMatrix_kexi_only-CORE_WITH_SHOP.csv"),row.names = FALSE,sep = ";")
write.table(delta_calibrun_hoh,paste0(path_to_result,"/delta","/ODMatrix_kexi_only-HOHENPFAHL.csv"),row.names = FALSE,sep = ";")



