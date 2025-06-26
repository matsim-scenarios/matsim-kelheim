# Pakete laden
#library(ggplot2)
#library(dplyr)
library(tidyverse)
library(readr)
library(ggokabeito) 


drtPlot<- function(base, runId, iteration, description = "", save = TRUE){
  # Dateien einlesen  
  legs_path <- paste(base, "/ITERS/it.", iteration, "/", runId, ".", iteration, ".drt_legs_av.csv",
                     sep = "")
  rejections_path <- paste(base, "/ITERS/it.", iteration, "/", runId, ".", iteration, ".drt_rejections_av.csv",
                           sep = "")
  
  legs <- read_delim(legs_path,
                     delim = ";",
                     locale = locale(decimal_mark = "."))
  
  rejections <- read_delim(rejections_path,
                           delim = ";",
                           locale = locale(decimal_mark = "."))
  
  if (nrow(rejections) == 0) {
    rejections <- data.frame(time = numeric(0))  # Leeres DataFrame mit erwarteter Spalte
  }

  # Binning-Funktion für 15 Minuten (15 * 60 Sekunden = 900 Sekunden)
  bin_size <- 1800  # 15 Minuten in Sekunden
  
  # Binning für submitted (submissionTime)
  submitted_bins <- legs %>%
    mutate(time_bin = floor(submissionTime / bin_size) * bin_size) %>%
    group_by(time_bin) %>%
    summarise(count = n(), .groups = "drop") %>%
    mutate(type = "submitted")
  
  # Binning für departureTime
  departure_bins <- legs %>%
    mutate(realDeparture = departureTime + waitTime) %>% 
    mutate(time_bin = floor(realDeparture / bin_size) * bin_size) %>%
    group_by(time_bin) %>%
    summarise(count = n(), .groups = "drop") %>%
    mutate(type = "departed")
  
  # Binning für rejections (time)
  rejected_bins <- rejections %>%
    mutate(time_bin = floor(time / bin_size) * bin_size) %>%
    group_by(time_bin) %>%
    summarise(count = n(), .groups = "drop") %>%
    mutate(type = "rejected")
  
  # Daten kombinieren
  all_bins <- bind_rows(submitted_bins, departure_bins, rejected_bins)
  
  # Zeit in Stunden umrechnen für Plot
  all_bins <- all_bins %>%
    mutate(time_hour = time_bin / 3600)
  
  # Kumulierte Summen berechnen
  all_bins_cumulative <- all_bins %>%
    arrange(type, time_bin) %>%
    group_by(type) %>%
    mutate(cum_count = cumsum(count)) %>%
    ungroup()
  
  fixed_levels <- c("submitted", "departed", "rejected")
  all_bins_cumulative$type <- factor(all_bins_cumulative$type,
                                     levels = fixed_levels)
  
  #my_colors <- c(
  #  submitted = "#41aaff",  # blau
  #  rejected = "#ff4a41",  # orange
  #  departed = "#41f1a1"   # grün
  #)
  
  # Plot: Kumulierte Werte
  p <- ggplot(all_bins_cumulative, aes(x = time_hour, y = cum_count, color = type)) +
    #geom_vline(xintercept = c(9, 16), color = "purple", linetype = "dashed", size = 1) +  # vertikale Linien
    geom_line(size = 1) +

    scale_x_continuous(breaks = seq(0, 36, by = 3), ,
                       limits = c(0, 36)) +  # X-Achse: alle 3h, 0 bis 36 uhr
    scale_y_continuous(breaks = seq(0, 150, by = 25), ,
                       limits = c(0, 150)) +  # X-Achse: alle 3h, 0 bis 36 uhr
    # Feste Farbspezifikation:
    #scale_color_manual(
    #  name   = "Group",
    #  values = my_colors,
    #  limits = names(my_colors),  # stellt sicher, dass A, B, C immer in dieser Reihenfolge auftauchen
    #  drop   = FALSE              # behält alle Levels auch ohne Datenpunkte
    #) +
    # hier ggokabeito einsetzen:
    scale_color_okabe_ito(
      name   = "Group",
      limits = fixed_levels,
      drop   = FALSE
    ) +
    labs(title = paste("Cumulative DRT Requests over Day Time. Iteration=", iteration, sep=""),
         subtitle = description,
         x = "Time (hours since simulation start)",
         y = "Cumulative number of requests",
         color = "Group") +
    theme(
      plot.title = element_text(size = 24, face = "bold"),  # Titelgröße anpassen
      plot.subtitle = element_text(size = 24),
      axis.title.x = element_text(size = 22, face = "bold"),  # X-Achsentitelgröße anpassen
      axis.title.y = element_text(size = 22, face = "bold"),  # Y-Achsentitelgröße anpassen
      axis.text = element_text(size = 20, face = "bold"),  # Achsentextgröße anpassen
      legend.title = element_text(size = 24, face = "bold"),  # Legendentitelgröße anpassen
      legend.text = element_text(size = 20),  # Legendtextgröße anpassen
      strip.text = element_text(size = 24, face = "bold")  # Facet-Textgröße anpassen
    )
  
  # Nur vertikale Linien hinzufügen, wenn runId nicht "allDay" enthält
  if (!grepl("allDay", runId)) {
    p <- p + geom_vline(xintercept = c(9, 16), color = "darkred", linetype = "dashed", size = 1)
  }
  
  p <- p + geom_point(size = 3)
  
  print(p)
  
  if ( save ==TRUE){
    output_file <- paste("E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0/plots/AV/en/", runId, "/", "requestsOverTime_iter", iteration, "-AV.pdf", sep = "")
    ggsave(filename = output_file,
           #dpi = 600,
           width = 32, height = 18, units = "cm")
  }
  
}


# Funktion zur Verarbeitung einer einzelnen Iteration
enrich_iteration <- function(base, runId, iteration) {
  # Pfade zusammenbauen
  legs_path      <- file.path(base, "ITERS", paste0("it.", iteration), paste0(runId, ".", iteration, ".drt_legs_av.csv"))
  rejections_path <- file.path(base, "ITERS", paste0("it.", iteration), paste0(runId, ".", iteration, ".drt_rejections_av.csv"))
  
  # Daten einlesen
  legs       <- read_delim(legs_path, delim = ";", locale = locale(decimal_mark = "."))
  rejections <- read_delim(rejections_path, delim = ";", locale = locale(decimal_mark = "."))
  if (nrow(rejections) == 0) {
    rejections <- tibble(time = numeric(0))
  }
  
  # Binning-Parameter
  bin_size <- 1800  # 30 Minuten in Sekunden
  
  # submitted
  submitted_bins <- legs %>%
    mutate(time_bin = floor(submissionTime / bin_size) * bin_size) %>%
    count(time_bin, name = "count") %>%
    mutate(type = "submitted")
  
  # departed
  departure_bins <- legs %>%
    mutate(realDeparture = departureTime + waitTime,
           time_bin      = floor(realDeparture / bin_size) * bin_size) %>%
    count(time_bin, name = "count") %>%
    mutate(type = "departed")
  
  # rejected
  rejected_bins <- rejections %>%
    mutate(time_bin = floor(time / bin_size) * bin_size) %>%
    count(time_bin, name = "count") %>%
    mutate(type = "rejected")
  
  # Zusammenführen und kumulieren
  all_bins <- bind_rows(submitted_bins, departure_bins, rejected_bins) %>%
    mutate(time_hour = time_bin / 3600) %>%
    arrange(type, time_bin) %>%
    group_by(type) %>%
    mutate(cum_count = cumsum(count)) %>%
    ungroup() %>%
    mutate(
      iteration = iteration,
      type      = factor(type, levels = c("submitted", "departed", "rejected"))
    )
  
  return(all_bins)
}

####################################################################################
base <- "E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0-allIters/AV-speed-mps-8.3/WIEKEXImSaal-AV2-intermodal-allDay/seed-2-WIEKEXImSaal-allDay"
runId <- "AV2-MPS8.3-WIEKEXImSaal-allDay-seed2"
description <- "2 AV - 30 km/h - large area - allDay"


base <- "E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0-allIters/AV-speed-mps-3.3/WIEKEXImSaal-AV2-intermodal-allDay/seed-2-WIEKEXImSaal-allDay"
runId <- "AV2-MPS3.3-WIEKEXImSaal-allDay-seed2"
description <- "2 AV - 12 km/h - large area - all day"

base <- "E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0-allIters/AV-speed-mps-3.3/WIEKEXImSaal-AV2-intermodal/seed-2-WIEKEXImSaal"
runId <- "AV2-MPS3.3-WIEKEXImSaal-seed2"
description <- "2 AV - 12 km/h - large area - part day"

## only for stacked bar plot
base <- "E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0/AV-speed-mps-3.3/SAR2023-AV2/seed-2-SAR2023"
runId <- "AV2-MPS3.3-SAR2023-seed2"
description <- "2 AV - 12 km/h - small area - part day"

########################################################################################

#drtPlot(base, runId, iteration = 50, description)
#drtPlot(base, runId, iteration = 100, description)
#drtPlot(base, runId, iteration = 150, description)
#drtPlot(base, runId, iteration = 200, description)
#drtPlot(base, runId, iteration = 300, description)
#drtPlot(base, runId, iteration = 400, description)
#drtPlot(base, runId, iteration = 500, description)
#drtPlot(base, runId, iteration = 600, description)
#drtPlot(base, runId, iteration = 700, description)
#drtPlot(base, runId, iteration = 800, description)
#drtPlot(base, runId, iteration = 850, description)
#drtPlot(base, runId, iteration = 900, description)
#drtPlot(base, runId, iteration = 950, description)
#drtPlot(base, runId, iteration = 1000, description)


###################
## facet plot

#iterations <- c(50, 100, 150, 200, 300, 400, 500, 600, 700, 800, 850, 900, 950, 1000)
iterations <- c(100, 200, 300, 400, 500, 600, 700, 800, 900, 1000)

# Alle Iterationen verarbeiten und zusammenführen
data_all <- map_dfr(iterations, ~ enrich_iteration(base, runId, .x))

# Facet-Plot erstellen
p <- ggplot(data_all, aes(x = time_hour, y = cum_count, color = type)) +
  geom_line(size = 1) +
  geom_point(size = 2) +
  scale_x_continuous(breaks = seq(0, 36, by = 9), limits = c(0, 36)) +
  scale_y_continuous(breaks = seq(0, 125, by = 50), limits = c(0, 125)) +
  scale_color_okabe_ito(
    name   = "Group",
    limits = c("submitted", "departed", "rejected"),
    drop   = FALSE
  ) +
  facet_wrap(~ iteration, ncol = 5) +
  labs(
    #title    = "Cumulative DRT Requests over Day Time",
    #subtitle = description,
    title = description,
    x        = "Time (hours since simulation start)",
    y        = "Cumulative number of requests",
    color    = "Group"
  ) +
  theme(
    plot.title       = element_text(size = 24, face = "bold"),
    plot.subtitle    = element_text(size = 20),
    axis.title.x     = element_text(size = 24, face = "bold"),
    #axis.text.x      = element_text(angle = 90, hjust = 1),
    axis.title.y     = element_text(size = 24, face = "bold"),
    axis.text        = element_text(size = 20, face = "bold"),
    legend.title     = element_text(size = 24, face = "bold"),
    legend.text      = element_text(size = 20, face = "bold"),
    strip.text       = element_text(size = 16, face = "bold"),
    legend.position = "bottom"
  )


# Optionale vertikale Linien global hinzufügen
if (!grepl("allDay", runId)) {
  p <- p + geom_vline(xintercept = c(9, 16), linetype = "dashed", size = 0.8)
} else {
  p <- p + geom_vline(xintercept = c(24), linetype = "dashed", size = 0.8)
}

# Plot anzeigen und speichern
print(p)
ggsave(
  filename = paste(runId, "-facetPlot.pdf", sep = ""),
  plot     = p,
  #dpi      = 800,
  width    = 30,
  height   = 20,
  units    = "cm",
  device   = "pdf",
  path     = file.path("E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0/plots/AV/en", runId)
)


##################################################
# 2) Stacked Bar Plot: rides vs rejections pro Iteration
# Datei einlesen
stats_path <- file.path(base, paste0(runId, ".drt_customer_stats_av.csv"))
stats      <- read_delim(stats_path, delim = ";", locale = locale(decimal_mark = "."))

# Daten transformieren für Stacked Bar
dat_bar <- stats %>%
  select(iteration, rides, rejections) %>%
  pivot_longer(cols = c(rides, rejections), names_to = "type", values_to = "count") %>%
  mutate(type = factor(type, levels = c("rides", "rejections")))

# Plot erstellen
gg_bar <- ggplot(dat_bar, aes(x = factor(iteration), y = count, fill = type)) +
  geom_bar(stat = "identity") +
  scale_fill_okabe_ito(name = "Type", limits = c("rides", "rejections"), drop = FALSE) +
  #scale_x_continuous(breaks = seq(0, 1000, by = 50)
  #                   , limits = c(0, 1000)
  #                   ) +
  scale_x_discrete(breaks = as.character(seq(0, 1000, by = 50))) +
  
  labs(title = description,
       x = "Iteration",
       y = "Count",
       fill = "Type") +
  theme(
    plot.title       = element_text(size = 24, face = "bold"),
    plot.subtitle    = element_text(size = 20),
    axis.title.x     = element_text(size = 24, face = "bold"),
    axis.text.x      = element_text(angle = 45, hjust = 1),
    axis.title.y     = element_text(size = 24, face = "bold"),
    axis.text        = element_text(size = 20, face = "bold"),
    legend.title     = element_text(size = 24, face = "bold"),
    legend.text      = element_text(size = 20, face = "bold"),
    strip.text       = element_text(size = 16, face = "bold"),
    legend.position = "bottom"
  )

# Speichern und anzeigen
print(gg_bar)
ggsave(filename = paste(runId, "_stacked_rides_rejections.pdf"),
       plot = gg_bar,
       #dpi = 600,
       width = 30,
       height = 20,
       units = "cm",
       path     = file.path("E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0/plots/AV/en", runId)
       )



