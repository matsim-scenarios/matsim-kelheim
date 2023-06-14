library(tidyverse)
# library(matsim)
# library(ggalluvial)
# library(ggplot2)

setwd("Y:/net/ils/matsim-kelheim/run-roadBlock/output/")
tsvFile <- "analysis-road-usage/blocked_infrastructure_leg_comparison.tsv"

maximiliansbruecke <- "output-casekelheim-v2.0-network-with-pt_blocked-Maximiliansbruecke.xml.gz"

person32245 <- read.csv2(paste0(maximiliansbruecke, "-seed1111/analysis-road-usage/person32445.tsv"), stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", sep="\t")

legs_32445 <- person32245 %>% 
# TODO split policy and base



trips_1111_base_av <- read.csv2(paste0(maximiliansbruecke, "-seed1111/", tsvFile), stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", sep="\t")

ride_trips_1111 <- trips_1111_base_av %>% 
  filter(mode_base == "ride")

equal_trips_1111 <- trips_1111_base_av %>% 
  filter(dist_m_base == dist_m_policy)
