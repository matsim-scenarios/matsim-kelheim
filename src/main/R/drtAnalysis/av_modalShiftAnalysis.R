library(dplyr)
library(matsim)
library(ggalluvial)
library(ggplot2)
library(tibble)
library(alluvial)

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
base_12kmh_1111 <- base_12kmh_1111 + ggtitle("modal-shift_12kmh_1111")
base_12kmh_1111
base_9kmh_1111 <- plotModalShiftSankey(trips_1111_base_av, trips_1111_9kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_9kmh_1111 <- base_9kmh_1111 + ggtitle("modal-shift_9kmh_1111")
base_9kmh_1111
base_6kmh_1111 <- plotModalShiftSankey(trips_1111_base_av, trips_1111_6kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_6kmh_1111 <- base_6kmh_1111 + ggtitle("modal-shift_6kmh_1111")
base_6kmh_1111

#random seed 1234
trips_1234_base_av <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed1234-CORE") %>% 
  filter(main_mode == "av")
trips_1234_12kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed1234-bad-weather-1-CORE")
trips_1234_9kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed1234-bad-weather-2-CORE")
trips_1234_6kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed1234-bad-weather-3-CORE")

base_12kmh_1234 <- plotModalShiftSankey(trips_1234_base_av, trips_1234_12kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_12kmh_1234 <- base_12kmh_1234 + ggtitle("modal-shift_12kmh_1234")
base_12kmh_1234
base_9kmh_1234 <- plotModalShiftSankey(trips_1234_base_av, trips_1234_9kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_9kmh_1234 <- base_9kmh_1234 + ggtitle("modal-shift_9kmh_1234")
base_9kmh_1234
base_6kmh_1234 <- plotModalShiftSankey(trips_1234_base_av, trips_1234_6kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_6kmh_1234 <- base_6kmh_1234 + ggtitle("modal-shift_6kmh_1234")
base_6kmh_1234

#random seed 2222
trips_2222_base_av <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed2222-CORE") %>% 
  filter(main_mode == "av")
trips_2222_12kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed2222-bad-weather-1-CORE")
trips_2222_9kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed2222-bad-weather-2-CORE")
trips_2222_6kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed2222-bad-weather-3-CORE")

base_12kmh_2222 <- plotModalShiftSankey(trips_2222_base_av, trips_2222_12kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_12kmh_2222 <- base_12kmh_2222 + ggtitle("modal-shift_12kmh_2222")
base_12kmh_2222
base_9kmh_2222 <- plotModalShiftSankey(trips_2222_base_av, trips_2222_9kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_9kmh_2222 <- base_9kmh_2222 + ggtitle("modal-shift_9kmh_2222")
base_9kmh_2222
base_6kmh_2222 <- plotModalShiftSankey(trips_2222_base_av, trips_2222_6kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_6kmh_2222 <- base_6kmh_2222 + ggtitle("modal-shift_6kmh_2222")
base_6kmh_2222

#random seed 4711
trips_4711_base_av <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed4711-CORE") %>% 
  filter(main_mode == "av")
trips_4711_12kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed4711-bad-weather-1-CORE")
trips_4711_9kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed4711-bad-weather-2-CORE")
trips_4711_6kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed4711-bad-weather-3-CORE")

base_12kmh_4711 <- plotModalShiftSankey(trips_4711_base_av, trips_4711_12kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_12kmh_4711 <- base_12kmh_4711 + ggtitle("modal-shift_12kmh_4711")
base_12kmh_4711
base_9kmh_4711 <- plotModalShiftSankey(trips_4711_base_av, trips_4711_9kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_9kmh_4711 <- base_9kmh_4711 + ggtitle("modal-shift_9kmh_4711")
base_9kmh_4711
base_6kmh_4711 <- plotModalShiftSankey(trips_4711_base_av, trips_4711_6kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_6kmh_4711 <- base_6kmh_4711 + ggtitle("modal-shift_6kmh_4711")
base_6kmh_4711

#random seed 5678
trips_5678_base_av <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed5678-CORE") %>% 
  filter(main_mode == "av")
trips_5678_12kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed5678-bad-weather-1-CORE")
trips_5678_9kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed5678-bad-weather-2-CORE")
trips_5678_6kmh <- readTripsTable(pathToMATSimOutputDirectory = "output-ASC-0.15-dist-0.00006-5_av-seed5678-bad-weather-3-CORE")

base_12kmh_5678 <- plotModalShiftSankey(trips_5678_base_av, trips_5678_12kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_12kmh_5678 <- base_12kmh_5678 + ggtitle("modal-shift_12kmh_5678")
base_12kmh_5678
base_9kmh_5678 <- plotModalShiftSankey(trips_5678_base_av, trips_5678_9kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_9kmh_5678 <- base_9kmh_5678 + ggtitle("modal-shift_9kmh_5678")
base_9kmh_5678
base_6kmh_5678 <- plotModalShiftSankey(trips_5678_base_av, trips_5678_6kmh, dump.output.to = "C:/Users/Simon/Desktop/wd/2023-03-28")
base_6kmh_5678 <- base_6kmh_5678 + ggtitle("modal-shift_6kmh_5678")
base_6kmh_5678

########################## avg values 6kmh ######################################################

# 1111 no ride
# 1234 no car, no ride
# 2222 no av, no ride
# 4711 no ride
# 5678 no drt no ride

mod_base_6kmh_1234 <- base_6kmh_1234$data %>% 
  select(policy_mode,n) %>% 
  ungroup() %>% 
  add_row(base_mode="av", policy_mode="car", n=0) %>% 
  add_row(base_mode="av", policy_mode="ride", n=0) %>% 
  select(policy_mode,n) %>% 
  arrange(policy_mode)

mod_base_6kmh_2222 <- base_6kmh_2222$data %>% 
  select(policy_mode,n) %>% 
  ungroup() %>% 
  add_row(base_mode="av", policy_mode="av", n=0) %>% 
  add_row(base_mode="av", policy_mode="ride", n=0) %>% 
  select(policy_mode,n) %>% 
  arrange(policy_mode)

mod_base_6kmh_5678 <- base_6kmh_5678$data %>% 
  select(policy_mode,n) %>% 
  ungroup() %>% 
  add_row(base_mode="av", policy_mode="drt", n=0) %>% 
  add_row(base_mode="av", policy_mode="ride", n=0) %>% 
  select(policy_mode,n) %>% 
  arrange(policy_mode)

mod_base_6kmh_1111 <- base_6kmh_1111$data %>% 
  select(policy_mode,n) %>%
  ungroup() %>% 
  add_row(base_mode="av", policy_mode="ride", n=0) %>%
  select(policy_mode,n) %>% 
  arrange(policy_mode)

mod_base_6kmh_4711 <- base_6kmh_4711$data %>% 
  select(policy_mode,n) %>%
  ungroup() %>% 
  add_row(base_mode="av", policy_mode="ride", n=0) %>%
  select(policy_mode,n) %>% 
  arrange(policy_mode)

six_kmh <- data.frame(base_mode=c("av","av","av","av","av","av","av"),
                      policy_mode=c("av","bike","car","drt","pt","ride","walk")) %>% 
  arrange(policy_mode) %>% 
  add_column(n_1111 = mod_base_6kmh_1111$n) %>% 
  add_column(n_1234 = mod_base_6kmh_1234$n) %>% 
  add_column(n_2222 = mod_base_6kmh_2222$n) %>% 
  add_column(n_4711 = mod_base_6kmh_4711$n) %>% 
  add_column(n_5678 = mod_base_6kmh_5678$n) %>% 
  mutate(rel_1111 = n_1111 / sum(n_1111),
         rel_1234 = n_1234 / sum(n_1234),
         rel_2222 = n_2222 / sum(n_2222),
         rel_4711 = n_4711 / sum(n_4711),
         rel_5678 = n_5678 / sum(n_5678))

means6 <- rowMeans(six_kmh %>% select(rel_1111,rel_1234,rel_2222,rel_4711,rel_5678))

six_kmh <- six_kmh %>% 
  mutate(avg_rel = means6) %>% 
  mutate(policy_mode = paste0(policy_mode, " (", round(avg_rel,digits=2),")"))

mean_modal_shift_6kmh <- alluvial(six_kmh[1:2],
                 freq = six_kmh$avg_rel,
                 border = NA,
                 axis_labels = c("Base Mode", "Policy Mode"))
mtext("Modal shift of AV users 6 km/h (mean over all simulation runs)", 3, line=3, font=2)

piechart6 <- ggplot(six_kmh, aes(x="", y=avg_rel, fill=policy_mode)) +
  geom_bar(stat="identity", width=1, color="white") +
  coord_polar("y", start=0) +
  theme_void() + 
  theme(legend.position="right") +
  theme(legend.title=element_text(size=17), 
        legend.text=element_text(size=15)) +
  # geom_text(aes(y = ypos, label = round(avg_rel,digits=2)), color = "white", size=3) +
  scale_fill_brewer(palette="Set1")
# scale_color_manual(values = colors)

piechart6

k########################## avg values 9kmh ######################################################

# 1111 no ride
# 1234 no car, no ride
# 2222 no ride
# 4711 no car, no ride
# 5678 no ride

mod_base_9kmh_1234 <- base_9kmh_1234$data %>% 
  select(policy_mode,n) %>% 
  ungroup() %>% 
  add_row(base_mode="av", policy_mode="car", n=0) %>% 
  add_row(base_mode="av", policy_mode="ride", n=0) %>%
  select(policy_mode,n) %>% 
  arrange(policy_mode)

mod_base_9kmh_4711 <- base_9kmh_4711$data %>% 
  select(policy_mode,n) %>% 
  ungroup() %>% 
  add_row(base_mode="av", policy_mode="car", n=0) %>% 
  add_row(base_mode="av", policy_mode="ride", n=0) %>%
  select(policy_mode,n) %>% 
  arrange(policy_mode)

mod_base_9kmh_2222 <- base_9kmh_2222$data %>% 
  select(policy_mode,n) %>% 
  ungroup() %>% 
  add_row(base_mode="av", policy_mode="ride", n=0) %>%
  select(policy_mode,n) %>% 
  arrange(policy_mode)

mod_base_9kmh_5678 <- base_9kmh_5678$data %>% 
  select(policy_mode,n) %>% 
  ungroup() %>% 
  add_row(base_mode="av", policy_mode="ride", n=0) %>%
  select(policy_mode,n) %>% 
  arrange(policy_mode)

mod_base_9kmh_1111 <- base_9kmh_1111$data %>% 
  select(policy_mode,n) %>% 
  ungroup() %>% 
  add_row(base_mode="av", policy_mode="ride", n=0) %>%
  select(policy_mode,n) %>% 
  arrange(policy_mode)

nine_kmh <- data.frame(base_mode=c("av","av","av","av","av","av","av"),
                       policy_mode=c("av","bike","car","drt","pt","ride","walk")) %>% 
  arrange(policy_mode) %>% 
  add_column(n_1111 = mod_base_9kmh_1111$n) %>% 
  add_column(n_1234 = mod_base_9kmh_1234$n) %>% 
  add_column(n_2222 = mod_base_9kmh_2222$n) %>% 
  add_column(n_4711 = mod_base_9kmh_4711$n) %>% 
  add_column(n_5678 = mod_base_9kmh_5678$n) %>% 
  mutate(rel_1111 = n_1111 / sum(n_1111),
         rel_1234 = n_1234 / sum(n_1234),
         rel_2222 = n_2222 / sum(n_2222),
         rel_4711 = n_4711 / sum(n_4711),
         rel_5678 = n_5678 / sum(n_5678))

means9 <- rowMeans(nine_kmh %>% select(rel_1111,rel_1234,rel_2222,rel_4711,rel_5678))

nine_kmh <- nine_kmh %>% 
  mutate(avg_rel = means9) %>% 
  mutate(policy_mode = paste0(policy_mode, " (", round(avg_rel,digits=2),")"))

mean_modal_shift_9kmh <- alluvial(nine_kmh[1:2],
                                  freq = nine_kmh$avg_rel,
                                  border = NA,
                                  axis_labels = c("Base Mode", "Policy Mode"))
mtext("Modal shift of AV users 9 km/h (mean over all simulation runs)", 3, line=3, font=2)

piechart9 <- ggplot(nine_kmh, aes(x="", y=avg_rel, fill=policy_mode)) +
  geom_bar(stat="identity", width=1, color="white") +
  coord_polar("y", start=0) +
  theme_void() + 
  theme(legend.position="right") +
  theme(legend.title=element_text(size=17), 
        legend.text=element_text(size=15)) +
  # geom_text(aes(y = ypos, label = round(avg_rel,digits=2)), color = "white", size=3) +
  scale_fill_brewer(palette="Set1")
# scale_color_manual(values = colors)

piechart9

########################## avg values 12kmh ######################################################

# 1111 no drt, no ride
# 1234 no car, no drt
# 2222 full
# 4711 no car
# 5678 no ride

mod_base_12kmh_1111 <- base_12kmh_1111$data %>% 
  select(policy_mode,n) %>% 
  ungroup() %>% 
  add_row(base_mode="av", policy_mode="drt", n=0) %>% 
  add_row(base_mode="av", policy_mode="ride", n=0) %>% 
  select(policy_mode,n) %>% 
  arrange(policy_mode)

mod_base_12kmh_1234 <- base_12kmh_1234$data %>% 
  select(policy_mode,n) %>% 
  ungroup() %>% 
  add_row(base_mode="av", policy_mode="drt", n=0) %>% 
  add_row(base_mode="av", policy_mode="car", n=0) %>% 
  select(policy_mode,n) %>% 
  arrange(policy_mode)

mod_base_12kmh_4711 <- base_12kmh_4711$data %>% 
  select(policy_mode,n) %>% 
  ungroup() %>%
  add_row(base_mode="av", policy_mode="car", n=0) %>% 
  select(policy_mode,n) %>% 
  arrange(policy_mode)

mod_base_12kmh_5678 <- base_12kmh_5678$data %>% 
  select(policy_mode,n) %>% 
  ungroup() %>%
  add_row(base_mode="av", policy_mode="ride", n=0) %>% 
  select(policy_mode,n) %>% 
  arrange(policy_mode)

twelve_kmh <- base_12kmh_2222$data %>% 
  ungroup() %>% 
  select(base_mode,policy_mode,n) %>% 
  rename("n_2222" = n) %>% 
  add_column(n_1234 = mod_base_12kmh_1234$n) %>%
  add_column(n_4711 = mod_base_12kmh_4711$n) %>%
  add_column(n_1111 = mod_base_12kmh_1111$n) %>% 
  add_column(n_5678 = mod_base_12kmh_5678$n) %>% 
  mutate(rel_1111 = n_1111 / sum(n_1111),
         rel_1234 = n_1234 / sum(n_1234),
         rel_2222 = n_2222 / sum(n_2222),
         rel_4711 = n_4711 / sum(n_4711),
         rel_5678 = n_5678 / sum(n_5678))

means12 <- rowMeans(twelve_kmh %>% select(rel_1111,rel_1234,rel_2222,rel_4711,rel_5678))

twelve_kmh <- twelve_kmh %>% 
  mutate(avg_rel = means12) %>% 
  mutate(policy_mode = paste0(policy_mode, " (", round(avg_rel,digits=2),")")) #%>% 
  # arrange(desc(policy_mode)) %>% 
  # mutate(ypos = cumsum(avg_rel)- 0.5*avg_rel )

mean_modal_shift_12kmh <- alluvial(twelve_kmh[1:2],
                                  freq = twelve_kmh$avg_rel,
                                  border = NA,
                                  axis_labels = c("Base Mode", "Policy Mode"))
mtext("Modal shift of AV users 12 km/h (mean over all simulation runs)", 3, line=3, font=2)

# colors <- c("av"="red","bike"="blue3","car"="green3","drt"="purple","pt"="orange","ride"="yellow2","walk"="brown")

piechart12 <- ggplot(twelve_kmh, aes(x="", y=avg_rel, fill=policy_mode)) +
  geom_bar(stat="identity", width=1, color="white") +
  coord_polar("y", start=0) +
  theme_void() + 
  theme(legend.position="right") +
  theme(legend.title=element_text(size=17), 
        legend.text=element_text(size=15)) +
  # geom_text(aes(y = ypos, label = round(avg_rel,digits=2)), color = "white", size=3) +
  scale_fill_brewer(palette="Set1")
  # scale_color_manual(values = colors)

piechart12

#########################################################################################################


