library(sf)
library(tmap)
library(tidyverse)
library(units)
# devtools::install_github("matsim-vsp/matsim-r", ref="jr_bug_fixes", force = TRUE)
devtools::load_all("~/git/matsim-r")
library(matsim)

# global options
tmap_mode("view")
tmap_options(check.and.fix = TRUE)

# read in base and policy cases
base_path <- "~/git/runs-svn/KelRide/matsim-kelheim-v2.0/1-Base-Case-With-Conventional-KEXI/output-ASC-0.15-dist-0.00006-intermodal-kexi-seed_1111/"
policy_path <- "~/git/runs-svn/KelRide/matsim-kelheim-v2.0/2-AV-Service-Area-And-Fleet-Size/runs/output-ASC-0.15-dist-0.00006-2_av-seed1111-CORE/"

persons_base <- readPersonsTable(base_path)
persons_policy <- readPersonsTable(policy_path)

trips_base <- readTripsTable(base_path)
trips_policy <- readTripsTable(policy_path)

shp <- st_read("~/git/matsim-kelheim/scenarios/input/shp/dilutionArea.shx") %>% 
  st_set_crs(25832)

persons_joined <- join_base_and_policy_persons(persons_base, persons_policy)

persons_joined_sf <- transform_persons_sf(persons_joined, filter_shp = shp, first_act_type_filter = "home")

joined_hex <- persons_attributes_on_hex_grid(persons_joined_sf, shp)

joined_centroids <- joined_hex %>% st_centroid()

tm_shape(joined_centroids)+
  tm_bubbles(col = "score_diff", size = "pop_density")


st_write(joined_centroids, "~/git/simwrapper-example-project/data/kelheim-ex/score_diff_points.shp")

st_write(joined_hex, "~/git/simwrapper-example-project/data/kelheim-ex/score_diff_hex.shp")

st_write(joined_centroids %>% st_buffer(1000), "~/git/simwrapper-example-project/data/kelheim-ex/score_diff_polygon.shp")



# where do the people live who lose the most
tm_shape(joined %>% filter(score_diff < -1)) +
  tm_dots(col = "red",size = 0.01, alpha = 0.5)

# where do the people live who win the most
tm_shape(joined %>% filter(score_diff > 1)) +
  tm_dots(col = "green",size = 0.01, alpha = 0.5)

# what about other variables 
ggplot(joined) +
  geom_histogram(aes(score_diff)) +
  xlim(-10,10) +
  geom_histogram(data = joined %>% filter(carAvail == "always"), aes(score_diff), fill = "red") +
  geom_histogram(data = joined %>% filter(sim_ptAbo == "full"), aes(score_diff), fill = "green") 

# car availability
ggplot(joined) +
  geom_violin(aes(income,score_diff, col = carAvail)) +
  ylim(-10,10)

ggplot(joined) +
  geom_smooth(aes(income, score_diff, col = carAvail)) 

# pt subscription 
ggplot(joined) +
  geom_violin(aes(income,score_diff, col = sim_ptAbo)) +
  ylim(-10,10)

ggplot(joined) +
  geom_smooth(aes(income,score_diff, col = sim_ptAbo)) 


### old

# pop density
tm_shape(joined_hex) +
  tm_polygons(col = "cnt", alpha = 0.75)

#income
ggplot(joined) +
  geom_density(aes(income))

tm_shape(joined_hex) +
  tm_polygons(col = "income", alpha = 0.75)

#carAvail
tm_shape(joined_hex) +
  tm_polygons(col = "carAvail", alpha = 0.75)

#pt
tm_shape(joined_hex) +
  tm_polygons(col = "sim_ptAbo", alpha = 0.75)


#income by carAvail
ggplot(joined) +
  geom_density(aes(income, col = carAvail))

#income by pt
ggplot(joined) +
  geom_density(aes(income, col = sim_ptAbo))

tm_shape(joined) +
  tm_dots(col = "income", size = 0.001)


# dist to center
# ggplot(joined) +
#   geom_point(aes(dist,score_diff, col = carAvail), size = 0.0000001) +
#   ylim(-10,10)
# 
# ggplot(joined) +
#   geom_smooth(aes(dist,score_diff)) 