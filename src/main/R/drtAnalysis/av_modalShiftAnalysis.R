library(dplyr)
library(matsim)
library(ggalluvial)
library(ggplot2)

# this is a script to compare trips / main_modes of av users in a base case to their corresponding mode in a policy case with reduced av max speed
# some sankey plots are produced.

setwd("Y:/net/ils/matsim-kelheim/kelheim-case-study/v2.0/caseStudy-badWeather/")

#random seed 1111
trips_1111_base_av <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed1111-CORE") %>% 
  filter(main_mode == "av")
trips_1111_12kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed1111-bad-weather-1-CORE")
trips_1111_9kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed1111-bad-weather-2-CORE")
trips_1111_6kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed1111-bad-weather-3-CORE")

base_12kmh_1111 <- plotModalShiftSankey(trips_1111_base_av, trips_1111_12kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_12kmh_1111 <- base_12kmh_1111 + ggtitle("base_12kmh_1111")
base_12kmh_1111
base_9kmh_1111 <- plotModalShiftSankey(trips_1111_base_av, trips_1111_9kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_9kmh_1111 <- base_9kmh_1111 + ggtitle("base_9kmh_1111")
base_9kmh_1111
base_6kmh_1111 <- plotModalShiftSankey(trips_1111_base_av, trips_1111_6kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_6kmh_1111 <- base_6kmh_1111 + ggtitle("base_6kmh_1111")
base_6kmh_1111

#random seed 1234
trips_1234_base_av <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed1234-CORE") %>% 
  filter(main_mode == "av")
trips_1234_12kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed1234-bad-weather-1-CORE")
trips_1234_9kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed1234-bad-weather-2-CORE")
trips_1234_6kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed1234-bad-weather-3-CORE")

base_12kmh_1234 <- plotModalShiftSankey(trips_1234_base_av, trips_1234_12kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_12kmh_1234 <- base_12kmh_1234 + ggtitle("base_12kmh_1234")
base_12kmh_1234
base_9kmh_1234 <- plotModalShiftSankey(trips_1234_base_av, trips_1234_9kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_9kmh_1234 <- base_9kmh_1234 + ggtitle("base_9kmh_1234")
base_9kmh_1234
base_6kmh_1234 <- plotModalShiftSankey(trips_1234_base_av, trips_1234_6kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_6kmh_1234 <- base_6kmh_1234 + ggtitle("base_6kmh_1234")
base_6kmh_1234

#random seed 2222
trips_2222_base_av <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed2222-CORE") %>% 
  filter(main_mode == "av")
trips_2222_12kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed2222-bad-weather-1-CORE")
trips_2222_9kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed2222-bad-weather-2-CORE")
trips_2222_6kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed2222-bad-weather-3-CORE")

base_12kmh_2222 <- plotModalShiftSankey(trips_2222_base_av, trips_2222_12kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_12kmh_2222 <- base_12kmh_2222 + ggtitle("base_12kmh_2222")
base_12kmh_2222
base_9kmh_2222 <- plotModalShiftSankey(trips_2222_base_av, trips_2222_9kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_9kmh_2222 <- base_9kmh_2222 + ggtitle("base_9kmh_2222")
base_9kmh_2222
base_6kmh_2222 <- plotModalShiftSankey(trips_2222_base_av, trips_2222_6kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_6kmh_2222 <- base_6kmh_2222 + ggtitle("base_6kmh_2222")
base_6kmh_2222

#random seed 4711
trips_4711_base_av <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed4711-CORE") %>% 
  filter(main_mode == "av")
trips_4711_12kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed4711-bad-weather-1-CORE")
trips_4711_9kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed4711-bad-weather-2-CORE")
trips_4711_6kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed4711-bad-weather-3-CORE")

base_12kmh_4711 <- plotModalShiftSankey(trips_4711_base_av, trips_4711_12kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_12kmh_4711 <- base_12kmh_4711 + ggtitle("base_12kmh_4711")
base_12kmh_4711
base_9kmh_4711 <- plotModalShiftSankey(trips_4711_base_av, trips_4711_9kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_9kmh_4711 <- base_9kmh_4711 + ggtitle("base_9kmh_4711")
base_9kmh_4711
base_6kmh_4711 <- plotModalShiftSankey(trips_4711_base_av, trips_4711_6kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_6kmh_4711 <- base_6kmh_4711 + ggtitle("base_6kmh_4711")
base_6kmh_4711

#random seed 5678
trips_5678_base_av <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed5678-CORE") %>% 
  filter(main_mode == "av")
trips_5678_12kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed5678-bad-weather-1-CORE")
trips_5678_9kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed5678-bad-weather-2-CORE")
trips_5678_6kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed5678-bad-weather-3-CORE")

base_12kmh_5678 <- plotModalShiftSankey(trips_5678_base_av, trips_5678_12kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_12kmh_5678 <- base_12kmh_5678 + ggtitle("base_12kmh_5678")
base_12kmh_5678
base_9kmh_5678 <- plotModalShiftSankey(trips_5678_base_av, trips_5678_9kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_9kmh_5678 <- base_9kmh_5678 + ggtitle("base_9kmh_5678")
base_9kmh_5678
base_6kmh_5678 <- plotModalShiftSankey(trips_5678_base_av, trips_5678_6kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_6kmh_5678 <- base_6kmh_5678 + ggtitle("base_6kmh_5678")
base_6kmh_5678
