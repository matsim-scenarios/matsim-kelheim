library(tidyverse)
library(sf)


join_base_and_policy <- function(basePerson, policyPersons, shp, crs = "EPSG:25832"){
  # merge 2 data sets; filter to only include people starting at home; 
  # filter people in shapefile
  joined <- basePersons %>%
    left_join(policyPersons %>% dplyr::select(person,executed_score), by = "person") %>%
    separate(first_act_type, sep = "_", into = c("first_act_type", "typical_duration")) %>%
    filter(first_act_type == "home") %>% 
    st_as_sf(coords = c("first_act_x", "first_act_y"), crs = crs) %>% 
    st_intersection(shp) %>% 
    dplyr::select(person, score_base = executed_score.x, score_policy = executed_score.y, income, carAvail, sim_ptAbo) %>% 
    mutate(score_diff = score_policy - score_base)
  
  return(joined)
}

create_hex_grid <- function(joined, shp, n = 20){
  hex_grid <- st_make_grid(shp, square = F, n = n) %>% 
    st_as_sf() %>% 
    mutate(id = row_number())
  
  joined_hex <- hex_grid %>% 
    st_join(joined) %>%
    group_by(id) %>% 
    summarise(cnt = n(),
              income = mean(income, na.rm = T),
              carAvail = sum(carAvail=="always") / n(),
              sim_ptAbo = sum(sim_ptAbo=="full") / n(),
              score_base = mean(score_base, na.rm = TRUE),
              score_policy = mean(score_policy, na.rm = TRUE),
              score_diff = mean(score_diff, na.rm = TRUE)) %>% 
    mutate(area = st_area(.) %>% units::set_units(km^2)) %>% 
    mutate(pop_density = cnt / area) %>% 
    filter(!is.na(score_base))
  
  return(joined_hex)
  
}
