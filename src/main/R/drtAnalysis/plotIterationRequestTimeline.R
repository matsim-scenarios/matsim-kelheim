# Pakete laden
library(ggplot2)
library(dplyr)
library(readr)



drtPlot<- function(base, runId, iteration){
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
  # 
  # # Plot erzeugen
  # ggplot(all_bins, aes(x = time_hour, y = count, color = type)) +
  #   geom_vline(xintercept = c(9, 16), color = "purple", linetype = "dashed", size = 1) +  # vertikale Linien
  #   geom_line(size = 1) +
  #   geom_point() +
  #   scale_x_continuous(breaks = seq(0, 36, by = 3), ,
  #                       limits = c(0, 36)) +  # X-Achse: alle 3h, 0 bis 36 uhr
  #   labs(title = paste("DRT Requests über die Zeit Iteration=", iteration, sep=""),
  #        x = "Zeit (Stunden seit Simulationsstart)",
  #        y = "Anzahl Requests",
  #        color = "Typ") +
  #   theme(
  #     plot.title = element_text(size = 32, face = "bold"),  # Titelgröße anpassen
  #     axis.title.x = element_text(size = 22, face = "bold"),  # X-Achsentitelgröße anpassen
  #     axis.title.y = element_text(size = 22, face = "bold"),  # Y-Achsentitelgröße anpassen
  #     axis.text = element_text(size = 20, face = "bold"),  # Achsentextgröße anpassen
  #     legend.title = element_text(size = 24, face = "bold"),  # Legendentitelgröße anpassen
  #     legend.text = element_text(size = 20),  # Legendtextgröße anpassen
  #     strip.text = element_text(size = 24, face = "bold")  # Facet-Textgröße anpassen
  #   )
  
  # Kumulierte Summen berechnen
  all_bins_cumulative <- all_bins %>%
    arrange(type, time_bin) %>%
    group_by(type) %>%
    mutate(cum_count = cumsum(count)) %>%
    ungroup()
  
  # Plot: Kumulierte Werte
  p <- ggplot(all_bins_cumulative, aes(x = time_hour, y = cum_count, color = type)) +
    #geom_vline(xintercept = c(9, 16), color = "purple", linetype = "dashed", size = 1) +  # vertikale Linien
    geom_line(size = 1) +
    geom_point(size = 3) +
    scale_x_continuous(breaks = seq(0, 36, by = 3), ,
                       limits = c(0, 36)) +  # X-Achse: alle 3h, 0 bis 36 uhr
    scale_y_continuous(breaks = seq(0, 150, by = 25), ,
                       limits = c(0, 150)) +  # X-Achse: alle 3h, 0 bis 36 uhr
    labs(title = paste("Kumulative DRT Requests über die Zeit. \n", runId, "\t Iteration=", iteration, sep=""),
         x = "Zeit (Stunden seit Simulationsstart)",
         y = "Kumulierte Anzahl Requests",
         color = "Typ") +
    theme(
      plot.title = element_text(size = 24, face = "bold"),  # Titelgröße anpassen
      axis.title.x = element_text(size = 22, face = "bold"),  # X-Achsentitelgröße anpassen
      axis.title.y = element_text(size = 22, face = "bold"),  # Y-Achsentitelgröße anpassen
      axis.text = element_text(size = 20, face = "bold"),  # Achsentextgröße anpassen
      legend.title = element_text(size = 24, face = "bold"),  # Legendentitelgröße anpassen
      legend.text = element_text(size = 20),  # Legendtextgröße anpassen
      strip.text = element_text(size = 24, face = "bold")  # Facet-Textgröße anpassen
    )
  
  # Nur vertikale Linien hinzufügen, wenn runId nicht "allDay" enthält
  if (!grepl("allDay", runId)) {
    p <- p + geom_vline(xintercept = c(9, 16), color = "purple", linetype = "dashed", size = 1)
  }
  
  print(p)
  
}


base <- "E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0-allIters/AV-speed-mps-3.3/WIEKEXImSaal-AV2-intermodal/seed-2-WIEKEXImSaal"
runId <- "AV2-MPS3.3-WIEKEXImSaal-seed2"

drtPlot(base, runId, iteration = 50)
drtPlot(base, runId, iteration = 100)
drtPlot(base, runId, iteration = 150)
drtPlot(base, runId, iteration = 200)
drtPlot(base, runId, iteration = 300)
drtPlot(base, runId, iteration = 400)
drtPlot(base, runId, iteration = 500)
drtPlot(base, runId, iteration = 600)
drtPlot(base, runId, iteration = 700)
drtPlot(base, runId, iteration = 800)
drtPlot(base, runId, iteration = 850)
drtPlot(base, runId, iteration = 900)
drtPlot(base, runId, iteration = 950)
drtPlot(base, runId, iteration = 1000)

base <- "E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0-allIters/AV-speed-mps-8.3/WIEKEXImSaal-AV2-intermodal-allDay/seed-2-WIEKEXImSaal-allDay"
runId <- "AV2-MPS8.3-WIEKEXImSaal-allDay-seed2"

drtPlot(base, runId, iteration = 50)
drtPlot(base, runId, iteration = 100)
drtPlot(base, runId, iteration = 150)
drtPlot(base, runId, iteration = 200)
drtPlot(base, runId, iteration = 300)
drtPlot(base, runId, iteration = 400)
drtPlot(base, runId, iteration = 500)
drtPlot(base, runId, iteration = 600)
drtPlot(base, runId, iteration = 700)
drtPlot(base, runId, iteration = 800)
drtPlot(base, runId, iteration = 850)
drtPlot(base, runId, iteration = 900)
drtPlot(base, runId, iteration = 950)
drtPlot(base, runId, iteration = 1000)

base <- "E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0-allIters/AV-speed-mps-3.3/WIEKEXImSaal-AV2-intermodal-allDay/seed-2-WIEKEXImSaal-allDay"
runId <- "AV2-MPS3.3-WIEKEXImSaal-allDay-seed2"

drtPlot(base, runId, iteration = 50)
drtPlot(base, runId, iteration = 100)
drtPlot(base, runId, iteration = 150)
drtPlot(base, runId, iteration = 200)
drtPlot(base, runId, iteration = 300)
drtPlot(base, runId, iteration = 400)
drtPlot(base, runId, iteration = 500)
drtPlot(base, runId, iteration = 600)
drtPlot(base, runId, iteration = 700)
drtPlot(base, runId, iteration = 800)
drtPlot(base, runId, iteration = 850)
drtPlot(base, runId, iteration = 900)
drtPlot(base, runId, iteration = 950)
drtPlot(base, runId, iteration = 1000)
