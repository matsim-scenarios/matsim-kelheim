library(tidyverse)
library(modelr)
#options(na.action = na.warn)

results <- read.csv(file = '/Users/luchengqi/Documents/MATSimScenarios/Kelheim/output/kelheim-case-study/run01/drt-service-analysis.tsv', sep="\t")
mean_detour_ratio_time <- mean(results$detour_ratio_time)
mean_detour_ratio_distance <- mean(results$detour_ratio_distance)
waiting_time_mean <- mean(results$waiting_time)
waiting_time_median <- median(results$waiting_time)
waiting_time_95_percentile <- quantile(results$waiting_time, probs = c(0.95))[["95%"]]

distance_direct_mean = mean(results$est_direct_drive_distance)
distance_euclidean_mean = mean(results$euclidean_distance)

ggplot(results, aes(est_direct_in_vehicle_time, total_travel_time)) + 
  geom_point() + 
  xlim(0, 1800) + 
  ylim(0, 1800) + 
  geom_abline(size=1, colour="green", intercept = 60) + 
  geom_abline(size=0.5, colour = "orange", slope = 1.1, intercept = 660) +
  geom_abline(size=0.5, colour = "red", slope = 1.25, intercept = 960) +
  ggtitle("DRT Service Quality Plot") +
  theme(plot.title = element_text(hjust = 0.5)) +
  xlab("Estimated Direct Drive Time") + 
  ylab("Actual Total Travel Time")

