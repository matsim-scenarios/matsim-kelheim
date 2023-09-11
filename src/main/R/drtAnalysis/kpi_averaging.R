#####libraries####
library(stringr)

#####global variables####
path_to_data <- "runs"


##### Collect all folder names####

folders_list<-list.files(path_to_data,full.names = TRUE)

folders_seeded = list()

for(i in 1:length(folders_list)){
  
  case_name = tail(str_split(folders_list[i],"-")[[1]],n = 1)
  
  if(!case_name %in% names(folders_seeded)){
    folders_seeded[[case_name]] = folders_list[i]
  }else{
    folders_seeded[[case_name]] = append(folders_seeded[[case_name]],folders_list[i]) 
  }
}

#####Reading and averaging av tables#####


folders_av_averaged_table = list()

for(case_name in names(folders_seeded)){
  
  
  
  for(folder in folders_seeded[[case_name]]){
    files_list<- list.files(paste0(folder,"/analysis-drt-service-quality"),full.names = TRUE)
    av_KPI_file <- files_list[grepl(pattern = "av_KPI.tsv",files_list)]
    cat("processing ",av_KPI_file," \r\n")
    av_KPI_table <- read.delim(av_KPI_file)
    
    if(!case_name %in% names(folders_av_averaged_table)){
      folders_av_averaged_table[[case_name]] = av_KPI_table
    }else{
      folders_av_averaged_table[[case_name]] = folders_av_averaged_table[[case_name]]+av_KPI_table
    }
    
  }
  
  folders_av_averaged_table[[case_name]] = folders_av_averaged_table[[case_name]]/length(folders_seeded[[case_name]])
  
  
}

print(folders_av_averaged_table)

#####Write averaged av tables####

dir_output_name <- paste(path_to_data, "results_kpi_av/", sep="")
print("writing to " + dir_output_name)
dir.create(dir_output_name)

for(case_name in names(folders_av_averaged_table)){
  write.table(folders_av_averaged_table[[case_name]],paste0(dir_output_name, "result_av_",case_name,".tsv"),quote = FALSE,row.names = FALSE)
}



#####Reading and averaging drt tables####



folders_drt_averaged_table = list()

for(case_name in names(folders_seeded)){
  
  
  
  for(folder in folders_seeded[[case_name]]){
    files_list<- list.files(paste0(folder,"/analysis-drt-service-quality"),full.names = TRUE)
    drt_KPI_file <- files_list[grepl(pattern = "drt_KPI.tsv",files_list)]
    cat("processing ",drt_KPI_file," \r\n")
    drt_KPI_table <- read.delim(drt_KPI_file)
    
    if(!case_name %in% names(folders_drt_averaged_table)){
      folders_drt_averaged_table[[case_name]] = drt_KPI_table
    }else{
      folders_drt_averaged_table[[case_name]] = folders_drt_averaged_table[[case_name]]+drt_KPI_table
    }
    
  }
  
  folders_drt_averaged_table[[case_name]] = folders_drt_averaged_table[[case_name]]/length(folders_seeded[[case_name]])
  
  
}

print(folders_drt_averaged_table)

#####Write averaged drt tables####

output_dir_name <- paste(path_to_data, "/results_kpi_drt/", sep="")
print(paste("writing to " , output_dir_name, sep=""))
dir.create(output_dir_name)

for(case_name in names(folders_drt_averaged_table)){
  write.table(folders_drt_averaged_table[[case_name]],paste0(output_dir_name, "result_drt_", case_name,".tsv"),quote = FALSE,row.names = FALSE)
}