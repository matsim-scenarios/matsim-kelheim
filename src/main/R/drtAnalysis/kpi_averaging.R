#####libraries####
library(stringr)
library(tidyverse)

#####global variables####
path_to_data <- "path/to/data (folder of a specific case, with different seeds)"
stats = c("mean","median","sd" ,"max", "min")

##### Collect all folder names####
folders_list<-list.files(path_to_data,full.names = TRUE)
folders_seeded = list()

for(i in 1:length(folders_list)){
  if (endsWith(folders_list[i],".tsv")){
    next
  }
  
  case_name = tail(str_split(folders_list[i],"-")[[1]],n = 1)
  
  if(!case_name %in% names(folders_seeded)){
    folders_seeded[[case_name]] = folders_list[i]
  }else{
    folders_seeded[[case_name]] = append(folders_seeded[[case_name]],folders_list[i]) 
  }
}

#########################################
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
      folders_drt_averaged_table[[case_name]] = rbind(folders_drt_averaged_table[[case_name]],drt_KPI_table)
    }
    
  }

  tbl_colnames = c("stat",colnames(folders_drt_averaged_table[[case_name]]))
  result_tibble = tbl_colnames %>% purrr::map_dfc(setNames, object = list(numeric()))
  for(stat in stats){
    func = get(stat)
    new_row = c(stat)
    for(column in colnames(folders_drt_averaged_table[[case_name]])){
           new_row = append(new_row,func(folders_drt_averaged_table[[case_name]][[column]]))
    }
    
    result_tibble = rbind(result_tibble,new_row)
  }
  colnames(result_tibble) = tbl_colnames
  
  folders_drt_averaged_table[[case_name]] = result_tibble
  
}

print(folders_drt_averaged_table)

#Write averaged drt tables####
for(case_name in names(folders_drt_averaged_table)){
  write.table(folders_drt_averaged_table[[case_name]],paste0(path_to_data, "/kpi_summary_drt_", case_name, ".tsv"),quote = FALSE,row.names = FALSE)
}



#########################################
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
      folders_av_averaged_table[[case_name]] = rbind(folders_av_averaged_table[[case_name]],av_KPI_table)
    }
  }
  
  tbl_colnames = c("stat",colnames(folders_av_averaged_table[[case_name]]))
  result_tibble = tbl_colnames %>% purrr::map_dfc(setNames, object = list(numeric()))
  for(stat in stats){
    func = get(stat)
    new_row = c(stat)
    for(column in colnames(folders_av_averaged_table[[case_name]])){
      new_row = append(new_row,func(folders_av_averaged_table[[case_name]][[column]]))
    }
    result_tibble = rbind(result_tibble,new_row)
  }
  colnames(result_tibble) = tbl_colnames
  folders_av_averaged_table[[case_name]] = result_tibble
}

print(folders_av_averaged_table)

#####Write averaged av tables####
dir_output_name <- "results_kpi_av"
dir.create(dir_output_name)

for(case_name in names(folders_av_averaged_table)){
  write.table(folders_av_averaged_table[[case_name]],paste0(path_to_data, "/kpi_summary_av_",case_name,".tsv"),quote = FALSE,row.names = FALSE)
}



