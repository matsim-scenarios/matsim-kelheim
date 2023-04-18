library(sf)
library(tmap)
library(tidyverse)
library(units)
devtools::install_github("matsim-vsp/matsim-r", ref="jr_bug_fixes", force = TRUE)
library(matsim)
# devtools::load_all("~/git/matsim-r")

# load utils
source("~/git/matsim-kelheim/src/main/R/utils_jr.R")

# global options
tmap_mode("view")
tmap_options(check.and.fix = TRUE)

# read in base and policy cases
baseCaseDirectory <- "~/git/net/ils/matsim-kelheim/calibration/runs/052"
policyCaseDirectory <- "~/git/net/ils/matsim-kelheim/kelheim-case-study/v2.0/KEXI-with-av/output-ASC-0.15-dist-0.00006-5_av-seed5678-BAUERNSIEDLUNG"
baseCaseDirectory <- "/Users/jakob/base"
policyCaseDirectory <- "/Users/jakob/policy"

basePersons <- readPersonsTable(baseCaseDirectory)
policyPersons <- readPersonsTable(policyCaseDirectory)

shp <- st_read("~/git/matsim-kelheim/scenarios/input/shp/dilutionArea.shx") %>% 
  st_set_crs(25832)

joined <- join_base_and_policy(basePersons,policyPersons, shp)

joined_hex <- create_hex_grid(joined, shp)

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


# score difference plot

ggplot(joined) + 
  geom_boxplot(aes(score_diff))


tm_shape(joined_hex) +
  tm_polygons(col = "score_diff", alpha = 0.7)


to_save <- joined_hex %>% st_centroid()

tm_shape(to_save)+
  tm_bubbles(col = "score_diff", size = "pop_density")

st_write(to_save, "~/git/simwrapper-example-project/data/kelheim-ex/points.shp")

st_write(joined_hex, "~/git/simwrapper-example-project/data/kelheim-ex/points2.shp")

st_write(to_save %>% st_buffer(100), "~/git/simwrapper-example-project/data/kelheim-ex/points_poly.shp")



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



# dist to center
# ggplot(joined) +
#   geom_point(aes(dist,score_diff, col = carAvail), size = 0.0000001) +
#   ylim(-10,10)
# 
# ggplot(joined) +
#   geom_smooth(aes(dist,score_diff)) 