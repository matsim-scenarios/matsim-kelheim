

library(tidyverse)
library(lubridate)
library(sf)

# setwd("C:/Users/chris/Development/matsim-scenarios/matsim-kelheim/src/main/R")

# trip distance groups
levels = c("0 - 1000", "1000 - 2000", "2000 - 5000", "5000 - 10000", "10000 - 20000", "20000+")
breaks = c(0, 1000, 2000, 5000, 10000, 20000, Inf)

shape <- st_read("../../../../shared-svn/projects/KelRide/matsim-input-files/20210521_kehlheim/dilutionArea.shp", crs=25832)

f <- "../../../output/output-kelheim-25pct"
sim_scale <- 4

persons <- read_delim(Sys.glob(file.path(f, "*.output_persons.csv.gz")) , delim = ";", trim_ws = T, 
                     col_types = cols(
                       person = col_character(),
                       good_type = col_integer()
                     )) %>%
          st_as_sf(coords = c("first_act_x", "first_act_y"), crs = 25832) %>%
          st_filter(shape)

trips <- read_delim(Sys.glob(file.path(f, "*.output_trips.csv.gz")) , delim = ";", trim_ws = T, 
                    col_types = cols(
                      person = col_character()
                    )) %>%
        filter(main_mode!="freight") %>%
        semi_join(persons) %>%
        mutate(dist_group = cut(traveled_distance, breaks=breaks, labels=levels))


sim <- trips %>%
  group_by(dist_group, main_mode) %>%
  summarise(trips=n()) %>%
  mutate(mode = fct_relevel(main_mode, "walk", "bike", "pt", "ride", "car")) %>%
  mutate(source = "sim")


ggplot(sim, aes(fill=mode, y=trips, x=dist_group)) +
  labs(subtitle = "Simulated scenario", x="distance [m]") +
  geom_bar(position="stack", stat="identity")

#g <- arrangeGrob(p1, p2, ncol = 2)
#ggsave(filename = "modal-split.png", path = ".", g,
#       width = 15, height = 5, device='png', dpi=300)


# Combined plot

total <- bind_rows(srv, sim)

ggplot(total, aes(fill=mode, y=scaled_trips, x=source)) +
  labs(subtitle = paste("Kelheim scenario", f), x="distance [m]") +
  geom_bar(position="stack", stat="identity", width = 0.5) +
  facet_wrap(dist_order, nrow = 1)


# Needed for adding short distance trips

sim_sum <- sum(sim$trips)
sim_aggr <- sim %>%
  group_by(dist_group) %>%
  summarise(share=sum(trips) / sim_sum)

# Needed share of trips
tripShare <- 0.19
shortDistance <- sum(filter(sim, dist_group=="0 - 1000")$trips)
numTrips = (shortDistance - sim_sum * tripShare) / (tripShare - 1)



