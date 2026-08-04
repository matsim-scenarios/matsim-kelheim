library(tidyverse)
library(sf)

#each section is meant to run separately, wd is set and data loaded within each section. The overall pattern is always the same, but file/column names change.
#there are three shape files needed for the calculations in east Kelheim:

buildings <- st_read("C:/Users/J/Documents/Thesis/shapefiles/Buildings_LK.shp") %>% 
  st_transform(crs = 25832) %>% 
  mutate(built = "yes")
study_area <-  st_read("C:/Users/J/Documents/Thesis/shapefiles/LK_Ost.shp") %>% 
  st_transform(crs=25832)
grid <- st_read("C:/Users/J/Documents/Thesis/shapefiles/ek_grid.shp")
grid <- grid[study_area,]
grid_buildings <-grid[buildings,]

#### TOWN KELHEIM - PT ALL DAY ####

#Stadt Kelheim
setwd("~/Thesis/accessibilities/final/stadt_model1")
accessibilities_files <- list.files(pattern = "doctor", full.names = TRUE)

##Create list of data frame names
names_long <-substr(accessibilities_files, 3, 18)
names_shrt <-substr(accessibilities_files, 15, 18)

#OstKelheim wait =20 Supermarket
setwd("~/Thesis/accessibilities/final/ek_model1_w20")
accessibilities_files <- list.files(pattern = "supermarkt", full.names = TRUE)

##Create list of data frame names without the ".csv" part 
names_long <-substr(accessibilities_files, 3, 29)
names_shrt <-substr(accessibilities_files, 26, 29)

###Load all files
for(i in 1:length(names_long)){
  
  filepath <- file.path(paste0(names_long[i],".csv"))
  assign(paste0("acc",names_shrt[i]), read.csv(filepath))
}

day_pt <- select(acc0862, -c("time", "car_accessibility", "estimatedDrt_accessibility")) %>% 
  left_join(select(acc0943, c("id", "pt_accessibility")), by = join_by(id), suffix = c(".0862", ".0943")) %>%
  left_join(.,select(acc1009, c("id", "pt_accessibility")), by = join_by(id) ) %>%
  rename(pt_accessibility.1009 = pt_accessibility) %>% 
  left_join(.,select(acc1189, c("id", "pt_accessibility")), by = join_by(id)) %>%
  rename(pt_accessibility.1189 = pt_accessibility) %>%
  left_join(.,select(acc1369, c("id", "pt_accessibility")), by = join_by(id)) %>% 
  rename(pt_accessibility.1369 = pt_accessibility)%>%
  left_join(.,select(acc1398, c("id", "pt_accessibility")), by = join_by(id)) %>% 
  rename(pt_accessibility.1398 = pt_accessibility)%>%
  left_join(.,select(acc1426, c("id", "pt_accessibility")), by = join_by(id)) %>% 
  rename(pt_accessibility.1426 = pt_accessibility) %>%
  left_join(.,select(acc1594, c("id", "pt_accessibility")), by = join_by(id)) %>% 
  rename(pt_accessibility.1594 = pt_accessibility)%>%
  left_join(.,select(acc1643, c("id", "pt_accessibility")), by = join_by(id)) %>% 
  rename(pt_accessibility.1643 = pt_accessibility)

day_drt <- select(acc0862, -c("time", "car_accessibility", "pt_accessibility")) %>% 
  left_join(select(acc0943, c("id", "estimatedDrt_accessibility")), by = join_by(id), suffix = c(".0862", ".0943")) %>%
  left_join(.,select(acc1009, c("id", "estimatedDrt_accessibility")), by = join_by(id) ) %>%
  rename(estimatedDrt_accessibility.1009 = estimatedDrt_accessibility) %>% 
  left_join(.,select(acc1189, c("id", "estimatedDrt_accessibility")), by = join_by(id)) %>%
  rename(estimatedDrt_accessibility.1189 = estimatedDrt_accessibility) %>%
  left_join(.,select(acc1369, c("id", "estimatedDrt_accessibility")), by = join_by(id)) %>% 
  rename(estimatedDrt_accessibility.1369 = estimatedDrt_accessibility)%>%
  left_join(.,select(acc1398, c("id", "estimatedDrt_accessibility")), by = join_by(id)) %>% 
  rename(estimatedDrt_accessibility.1398 = estimatedDrt_accessibility)%>%
  left_join(.,select(acc1426, c("id", "estimatedDrt_accessibility")), by = join_by(id)) %>% 
  rename(estimatedDrt_accessibility.1426 = estimatedDrt_accessibility) %>%
  left_join(.,select(acc1594, c("id", "estimatedDrt_accessibility")), by = join_by(id)) %>% 
  rename(estimatedDrt_accessibility.1594 = estimatedDrt_accessibility)%>%
  left_join(.,select(acc1643, c("id", "estimatedDrt_accessibility")), by = join_by(id)) %>% 
  rename(estimatedDrt_accessibility.1643 = estimatedDrt_accessibility)

day_car <- select(acc0862, -c("time", "estimatedDrt_accessibility", "pt_accessibility")) %>% 
  left_join(select(acc0943, c("id", "car_accessibility")), by = join_by(id), suffix = c(".0862", ".0943")) %>%
  left_join(.,select(acc1009, c("id", "car_accessibility")), by = join_by(id) ) %>%
  rename(car_accessibility.1009 = car_accessibility) %>% 
  left_join(.,select(acc1189, c("id", "car_accessibility")), by = join_by(id)) %>%
  rename(car_accessibility.1189 = car_accessibility) %>%
  left_join(.,select(acc1369, c("id", "car_accessibility")), by = join_by(id)) %>% 
  rename(car_accessibility.1369 = car_accessibility)%>%
  left_join(.,select(acc1398, c("id", "car_accessibility")), by = join_by(id)) %>% 
  rename(car_accessibility.1398 = car_accessibility)%>%
  left_join(.,select(acc1426, c("id", "car_accessibility")), by = join_by(id)) %>% 
  rename(car_accessibility.1426 = car_accessibility) %>%
  left_join(.,select(acc1594, c("id", "car_accessibility")), by = join_by(id)) %>% 
  rename(car_accessibility.1594 = car_accessibility)%>%
  left_join(.,select(acc1643, c("id", "car_accessibility")), by = join_by(id)) %>% 
  rename(car_accessibility.1643 = car_accessibility)

day_pt <- day_pt %>% 
  rowwise() %>% 
  mutate(
    mean = mean(c_across(pt_accessibility.0862:pt_accessibility.1643)),
    min = min(c_across(pt_accessibility.0862:pt_accessibility.1643)),
    max = max(c_across(pt_accessibility.0862:pt_accessibility.1643)))

day_drt <- day_drt %>% 
  rowwise() %>% 
  mutate(
    mean_DRT = mean(c_across(estimatedDrt_accessibility.0862:estimatedDrt_accessibility.1643)),
    min_DRT = min(c_across(estimatedDrt_accessibility.0862:estimatedDrt_accessibility.1643)),
    max_DRT = max(c_across(estimatedDrt_accessibility.0862:estimatedDrt_accessibility.1643)))

day_car <- day_car %>% 
  rowwise() %>% 
  mutate(
    mean = mean(c_across(car_accessibility.0862:car_accessibility.1643)),
    min = min(c_across(car_accessibility.0862:car_accessibility.1643)),
    max = max(c_across(car_accessibility.0862:car_accessibility.1643)))

write.csv(day_pt, "day_pt.csv")
write.csv(day_drt, "day_drt.csv")
write.csv(day_car, "day_car.csv")

avg_range_day_pt <- day_pt %>% 
  pivot_longer(cols = starts_with("pt"), values_to = "acc", names_to = "time") %>% 
  group_by(time) %>% 
  summarise(mean= mean(acc),
            min = min(acc),
            max = max(acc)) %>% 
  mutate(times_h = c("08:37","09:25","10:05","11:53","13:41","13:58","14:15","15:56","16:25")) %>% 
  pivot_longer(cols=c("mean", "min", "max"), values_to = "acc", names_to = "type")

ggplot(avg_range_day_pt)+
  geom_line(aes(x=factor(times_h), y=acc, color = type, group = type))

day <- st_as_sf(day, coords = c("xcoord", "ycoord"), crs = 25832 )
ggplot(day)+
  geom_sf(aes(color=mean))+
  scale_color_viridis_c()

ggplot(day)+
  geom_sf(aes(color=sd))+
  scale_color_viridis_c()



#### East Kelheim - SPORT ####

#OstKelheim wait =20 Sport
setwd("~/Thesis/accessibilities/final/ek_model1_w20")
accessibilities_files <- list.files(pattern = "sport", full.names = TRUE)

##Create list of data frame names without the ".csv" part
names_long <-substr(accessibilities_files, 3, 24)
names_shrt <-substr(accessibilities_files, 21, 24)

###Load all files
for(i in 1:length(names_long)){
  
  filepath <- file.path(paste0(names_long[i],".csv"))
  #placeholder <- read.csv(filepath)
  assign(paste0("acc",names_shrt[i]), read.csv(filepath))
}

day_pt <- select(acc1520, -c("time", "car_accessibility", "estimatedDrt_accessibility")) %>% 
  left_join(select(acc1585, c("id", "pt_accessibility")), by = join_by(id), suffix = c(".1520", ".1585")) %>%
  left_join(.,select(acc1678, c("id", "pt_accessibility")), by = join_by(id) ) %>%
  rename(pt_accessibility.1678 = pt_accessibility) %>% 
  left_join(.,select(acc1807, c("id", "pt_accessibility")), by = join_by(id)) %>%
  rename(pt_accessibility.1807 = pt_accessibility) %>%
  left_join(.,select(acc1961, c("id", "pt_accessibility")), by = join_by(id)) %>% 
  rename(pt_accessibility.1961 = pt_accessibility)

day_drt <- select(acc1520, -c("time", "car_accessibility", "pt_accessibility")) %>% 
  left_join(select(acc1585, c("id", "estimatedDrt_accessibility")), by = join_by(id), suffix = c(".1520", ".1585")) %>%
  left_join(.,select(acc1678, c("id", "estimatedDrt_accessibility")), by = join_by(id) ) %>%
  rename(estimatedDrt_accessibility.1678 = estimatedDrt_accessibility) %>% 
  left_join(.,select(acc1807, c("id", "estimatedDrt_accessibility")), by = join_by(id)) %>%
  rename(estimatedDrt_accessibility.1807 = estimatedDrt_accessibility) %>%
  left_join(.,select(acc1961, c("id", "estimatedDrt_accessibility")), by = join_by(id)) %>% 
  rename(estimatedDrt_accessibility.1961 = estimatedDrt_accessibility)

day_car <- select(acc1520, -c("time", "estimatedDrt_accessibility", "pt_accessibility")) %>% 
  left_join(select(acc1585, c("id", "car_accessibility")), by = join_by(id), suffix = c(".1520", ".1585")) %>%
  left_join(.,select(acc1678, c("id", "car_accessibility")), by = join_by(id) ) %>%
  rename(car_accessibility.1678 = car_accessibility) %>% 
  left_join(.,select(acc1807, c("id", "car_accessibility")), by = join_by(id)) %>%
  rename(car_accessibility.1807 = car_accessibility) %>%
  left_join(.,select(acc1961, c("id", "car_accessibility")), by = join_by(id)) %>% 
  rename(car_accessibility.1961 = car_accessibility)

day_pt <- day_pt %>% 
  rowwise() %>% 
  mutate(
    mean = mean(c_across(pt_accessibility.1520:pt_accessibility.1961)),
    min = min(c_across(pt_accessibility.1520:pt_accessibility.1961)),
    max = max(c_across(pt_accessibility.1520:pt_accessibility.1961)))

day_drt <- day_drt %>% 
  rowwise() %>% 
  mutate(
    mean_DRT = mean(c_across(estimatedDrt_accessibility.1520:estimatedDrt_accessibility.1961)),
    min_DRT = min(c_across(estimatedDrt_accessibility.1520:estimatedDrt_accessibility.1961)),
    max_DRT = max(c_across(estimatedDrt_accessibility.1520:estimatedDrt_accessibility.1961)))

day_car <- day_car %>% 
  rowwise() %>% 
  mutate(
    mean = mean(c_across(car_accessibility.1520:car_accessibility.1961)),
    min = min(c_across(car_accessibility.1520:car_accessibility.1961)),
    max = max(c_across(car_accessibility.1520:car_accessibility.1961)))

write.csv(day_pt, "day_pt.csv")
write.csv(day_drt, "day_drt.csv")
write.csv(day_car, "day_car.csv")

avg_range_day_pt <- day_pt %>% 
  pivot_longer(cols = starts_with("pt"), values_to = "acc", names_to = "time") %>% 
  group_by(time) %>% 
  summarise(mean= mean(acc),
            min = min(acc),
            max = max(acc)) %>% 
  mutate(times_h = c("15:12","15:51","16:46","18:04","19:36")) %>% 
  pivot_longer(cols=c("mean", "min", "max"), values_to = "acc", names_to = "type")

ek_sport_drt <- st_as_sf(day_drt, coords = c("xcoord", "ycoord"), crs=25832)
ek_sport_pt <- st_as_sf(day_pt, coords = c("xcoord", "ycoord"), crs=25832)
ek_sport_car <- st_as_sf(day_car, coords = c("xcoord", "ycoord"), crs=25832)

grid_buildings <-grid[buildings,]

grid_buildings_drt <-st_join(grid_buildings,ek_sport_drt)
grid_buildings_pt <-st_join(grid_buildings,ek_sport_pt)
grid_buildings_car <-st_join(grid_buildings,ek_sport_car)

grid_buildings$mean_car <- grid_buildings_car$mean
grid_buildings$mean_pt <- grid_buildings_pt$mean
grid_buildings$mean_drt <- grid_buildings_drt$mean_DRT

grid_buildings <- st_join(grid_buildings, select(study_area, name))
grid_buildings <- grid_buildings %>% 
  pivot_longer(cols = c("mean_car", "mean_pt", "mean_drt"), values_to = "mean", names_to = "mode")

summ <- grid_buildings %>% 
  group_by(mode, name) %>% 
  summarize(min = min(mean), max = max(mean), 
            mean = mean(mean)) %>% 
  mutate(min = round(min,2),
         max = round(max,2),
         mean = round(mean,2)) %>% 
  st_drop_geometry()

ggplot(grid_buildings)+
  geom_histogram(aes(x = mean, fill = mode), alpha = 0.7, position = "identity")+
  scale_fill_viridis_d(labels= c("car", "DRT", "PT"))+
  facet_wrap(vars(name))+
  xlab("mean accessibility (utils)")


mean(grid_buildings_drt$mean_DRT)
mean(grid_buildings_pt$mean)
mean(grid_buildings_car$mean)

min(grid_buildings_drt$mean_DRT)
min(grid_buildings_pt$mean)
min(grid_buildings_car$mean)

max(grid_buildings_drt$mean_DRT)
max(grid_buildings_pt$mean)
max(grid_buildings_car$mean)


#### East Kelheim - INDEX ####
#OstKelheim wait =20 
setwd("~/Thesis/accessibilities/final/ek_model1_w20")
accessibilities_files <- list.files(pattern = "1200", full.names = TRUE)

##Create list of data frame names without the ".csv" part 
names_long <-substr(accessibilities_files, 3, 33)
names_shrt <-substr(accessibilities_files, 3, 7)

###Load all files
for(i in 1:length(names_long)){
  
  filepath <- file.path(names_long[i])
  #placeholder <- read.csv(filepath)
  assign(paste0("acc_",names_shrt[i]), read.csv(filepath))
}


#glue the columns together to create one dataframe per mode with one column per time/activity and cut to size of grid_buildings
all_pt <- select(acc_ameni, -c("time", "car_accessibility", "estimatedDrt_accessibility")) %>% 
  left_join(select(acc_docto, c("id", "pt_accessibility")), by = join_by(id), suffix = c(".ameni", ".docto")) %>%
  left_join(.,select(acc_pharm, c("id", "pt_accessibility")), by = join_by(id) ) %>%
  rename(pt_accessibility.pharm = pt_accessibility) %>% 
  # left_join(.,select(acc_churc, c("id", "pt_accessibility")), by = join_by(id)) %>% #CHURCH COMMENT OUT IF NOT NEEDED
  #rename(pt_accessibility.churc = pt_accessibility) %>% #CHURCH COMMENT OUT IF NOT NEEDED
  left_join(.,select(acc_sport, c("id", "pt_accessibility")), by = join_by(id)) %>%
  rename(pt_accessibility.sport = pt_accessibility) %>%
  left_join(.,select(acc_super, c("id", "pt_accessibility")), by = join_by(id)) %>% 
  rename(pt_accessibility.super = pt_accessibility) %>% 
  st_as_sf(coords = c("xcoord", "ycoord"), crs=25832)
all_pt <- all_pt[grid_buildings,] #cut to size

all_drt <- select(acc_ameni, -c("time", "car_accessibility", "pt_accessibility")) %>% 
  left_join(select(acc_docto, c("id", "estimatedDrt_accessibility")), by = join_by(id), suffix = c(".ameni", ".docto")) %>%
  left_join(.,select(acc_pharm, c("id", "estimatedDrt_accessibility")), by = join_by(id) ) %>%
  rename(estimatedDrt_accessibility.pharm = estimatedDrt_accessibility) %>% 
  # left_join(.,select(acc_churc, c("id", "estimatedDrt_accessibility")), by = join_by(id)) %>% #CHURCH COMMENT OUT IF NOT NEEDED
  #rename(estimatedDrt_accessibility.churc = estimatedDrt_accessibility) %>% #CHURCH COMMENT OUT IF NOT NEEDED
  left_join(.,select(acc_sport, c("id", "estimatedDrt_accessibility")), by = join_by(id)) %>%
  rename(estimatedDrt_accessibility.sport = estimatedDrt_accessibility) %>%
  left_join(.,select(acc_super, c("id", "estimatedDrt_accessibility")), by = join_by(id)) %>% 
  rename(estimatedDrt_accessibility.super = estimatedDrt_accessibility)%>% 
  st_as_sf(coords = c("xcoord", "ycoord"), crs=25832)
all_drt <- all_drt[grid_buildings,]

all_car <- select(acc_ameni, -c("time", "estimatedDrt_accessibility", "pt_accessibility")) %>% 
  left_join(select(acc_docto, c("id", "car_accessibility")), by = join_by(id), suffix = c(".ameni", ".docto")) %>%
  left_join(.,select(acc_pharm, c("id", "car_accessibility")), by = join_by(id) ) %>%
  rename(car_accessibility.pharm = car_accessibility) %>% 
  #left_join(.,select(acc_churc, c("id", "car_accessibility")), by = join_by(id)) %>% #CHURCH COMMENT OUT IF NOT NEEDED
  #rename(car_accessibility.churc = car_accessibility) %>% #CHURCH COMMENT OUT IF NOT NEEDED
  left_join(.,select(acc_sport, c("id", "car_accessibility")), by = join_by(id)) %>%
  rename(car_accessibility.sport = car_accessibility) %>%
  left_join(.,select(acc_super, c("id", "car_accessibility")), by = join_by(id)) %>% 
  rename(car_accessibility.super = car_accessibility)%>% 
  st_as_sf(coords = c("xcoord", "ycoord"), crs=25832)
all_car <- all_car[grid_buildings,]

#create additional columns with mean/min/max
all_pt <- all_pt %>%
  rowwise() %>% 
  mutate(
    mean = mean(c_across(pt_accessibility.ameni:pt_accessibility.super)),
    median = median(c_across(pt_accessibility.ameni:pt_accessibility.super)),
    min = min(c_across(pt_accessibility.ameni:pt_accessibility.super)),
    max = max(c_across(pt_accessibility.ameni:pt_accessibility.super)))


all_drt <- all_drt %>% 
  rowwise() %>% 
  mutate(
    mean_DRT = mean(c_across(estimatedDrt_accessibility.ameni:estimatedDrt_accessibility.super)),
    median_DRT = median(c_across(estimatedDrt_accessibility.ameni:estimatedDrt_accessibility.super)),
    min_DRT = min(c_across(estimatedDrt_accessibility.ameni:estimatedDrt_accessibility.super)),
    max_DRT = max(c_across(estimatedDrt_accessibility.ameni:estimatedDrt_accessibility.super)))

all_car <- all_car %>% 
  rowwise() %>% 
  mutate(
    mean = mean(c_across(car_accessibility.ameni:car_accessibility.super)),
    median = median(c_across(car_accessibility.ameni:car_accessibility.super)),
    min = min(c_across(car_accessibility.ameni:car_accessibility.super)),
    max = max(c_across(car_accessibility.ameni:car_accessibility.super)))

grid_buildings_meanmodes <- grid_buildings %>% 
  left_join(st_drop_geometry(select(all_drt, c("id", "mean_DRT", "median_DRT"))), by = "id") %>% 
  left_join(st_drop_geometry(select(all_pt, c("id", "mean", "median"))), by = "id") %>% 
  left_join(st_drop_geometry(select(all_car, c("id", "mean", "median"))), by = "id", suffix = c("_PT", "_car"))

grid_buildings_meanmodes <- st_join(grid_buildings_meanmodes, select(study_area, name))
grid_buildings_meanmodes <- grid_buildings_meanmodes %>% 
  pivot_longer(cols = c("mean_car", "mean_PT", "mean_DRT"), values_to = "mean", names_to = "mode")

ggplot(grid_buildings_meanmodes)+
  geom_histogram(aes(x = mean, fill = mode), alpha = 0.7, position = "identity")+
  scale_fill_viridis_d(labels= c("car", "DRT", "PT"))+
  facet_wrap(vars(name))+
  xlim(-12,1)+
  xlab("mean accessibility (utils)")

grid_buildings_medianmodes <- grid_buildings_meanmodes %>% 
  pivot_longer(cols = c("median_car", "median_PT", "median_DRT"), values_to = "median", names_to = "mode")

ggplot(grid_buildings_medianmodes)+
  geom_histogram(aes(x = median, fill = mode), alpha = 0.7, position = "identity")+
  scale_fill_viridis_d(labels= c("car", "DRT", "PT"))+
  facet_wrap(vars(name))+
  xlim(-12,1)+
  xlab("median accessibility (utils)")

#### East Kelheim - BUILDINGS####
#OstKelheim wait =20 
setwd("~/Thesis/accessibilities/final/ek_model1_w20")
#load data
data_buildings <- read.csv("buildings_3vh1_w20_1200.csv") %>% 
  st_as_sf(coords= c("xcoord", "ycoord"), crs=25832)
data_buildings <- data_buildings[grid_buildings,]

grid_buildings_meanmodes <- st_join(data_buildings, select(study_area, name)) %>% 
  drop_na(name)
grid_buildings_meanmodes <- grid_buildings_meanmodes %>% 
  pivot_longer(cols = c("car_accessibility", "pt_accessibility", "estimatedDrt_accessibility"), values_to = "mean", names_to = "mode")

ggplot(grid_buildings_meanmodes)+
  geom_histogram(aes(x = mean, fill = mode), alpha = 0.7, position = "identity")+
  scale_fill_viridis_d(labels= c("car", "DRT", "PT"))+
  facet_wrap(vars(name))+
  xlim(-12,1)+
  xlab("mean accessibility (utils)")

grid_buildings_medianmodes <- grid_buildings_meanmodes %>% 
  pivot_longer(cols = c("median_car", "median_PT", "median_DRT"), values_to = "median", names_to = "mode")

ggplot(grid_buildings_medianmodes)+
  geom_histogram(aes(x = median, fill = mode), alpha = 0.7, position = "identity")+
  scale_fill_viridis_d(labels= c("car", "DRT", "PT"))+
  facet_wrap(vars(name))+
  xlim(-12,1)+
  xlab("median accessibility (utils)")
