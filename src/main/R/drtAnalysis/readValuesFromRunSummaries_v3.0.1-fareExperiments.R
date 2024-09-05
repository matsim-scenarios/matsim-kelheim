library(dplyr)
library(readr)
library(tidyr)
library(ggplot2)  # Für das Plotting mit ggplot2
library(plotly)

# Funktion zum Extrahieren der Parameter aus dem Ordnernamen
extract_parameters <- function(folder_name) {
  # Extrahiere 'area'
  fares <- strsplit(folder_name, '-')[[1]][2]
  
  return(list(fares = fares))
}

# Funktion zum Einlesen der CSV-Datei und Extrahieren der "mean"-Werte
read_stats <- function(folder_path, file_name) {
  csv_path <- file.path(folder_path, "analysis/drt-drt", file_name)
  
  if (file.exists(csv_path)) {
    df <- read_csv(csv_path)
    mean_values <- df %>% select(parameter, mean)
    return(mean_values)
  } else {
    return(NULL)
  }
}

# Hauptfunktion zum Iterieren durch Unterordner
process_folders <- function(main_folder) {
  # Liste aller Unterordner im Hauptordner
  subfolders <- list.dirs(main_folder, recursive = FALSE, full.names = FALSE)
  
  # Initialisiere eine Liste zum Speichern der Ergebnisse
  results <- list()
  
  # Iteriere durch alle Unterordner
  for (subfolder in subfolders) {
    parameters <- extract_parameters(subfolder)
    full_path <- file.path(main_folder, subfolder)
    
    demand_mean_values <- read_stats(full_path, "avg_demand_stats.csv")
    supply_mean_values <- read_stats(full_path, "avg_supply_stats.csv")
    
    if (!is.null(demand_mean_values) || !is.null(supply_mean_values)) {
      if (!is.null(demand_mean_values)) {
        demand_mean_values <- demand_mean_values %>% 
          mutate(type = "demand",
                 fares = parameters$fares)
      }
      
      if (!is.null(supply_mean_values)) {
        supply_mean_values <- supply_mean_values %>% 
          mutate(type = "supply",
                 fares = parameters$fares)
      }
      
      combined_values <- bind_rows(demand_mean_values, supply_mean_values)
      results[[subfolder]] <- combined_values
    }
  }
  
  # Kombiniere alle Ergebnisse in eine Tabelle
  final_result <- bind_rows(results)
  return(final_result)
}

#############

mainDir <- "D:/Projekte/KelRide/runs/v3.0.1-fare-experiments/output-KEXI-kexi"
mainDir <- "E:/matsim-kelheim/v3.0.1-fare-experiments/output-KEXI-kexi/"

#speeds <- list(3.3, 5, 8.3)
#results <- list()
#for (speed in speeds) {
#  main_folder <- paste(mainDir, "AV-speed-mps-", speed, "/", sep="")
#  runResults <- process_folders(mainDir)  
#  results[[speed]] <- runResults
#}

#results <- bind_rows(results)


results <- process_folders(mainDir)

# Transponiere die Tabelle, um Parameter als Spalten zu setzen
transposed_result <- results %>%
  select(fares, parameter, mean) %>%
  spread(key = parameter, value = mean)


# Ergebnisse ausgeben
print(results)
print(transposed_result)

write_csv(transposed_result, paste(mainDir, "results.csv", sep=""))



save <- function(fileName){
  ggsave(filename = paste(mainDir, "plots/", fileName, ".png", sep = ""),
         dpi = 600, width = 32, height = 18, units = "cm")
}


###########################
plotByConfiguration <- function(parameterStr){
  
  # Filtern der Daten für die gewünschten Parameter
  plot_data <- results %>%
    filter(parameter == parameterStr)
  
  # Erstellen des Facet-Plots
  ggplot(plot_data, aes(x = fares, y = mean)) +
    #geom_line(size = 1.2) +
    geom_point(size = 3,
               #aes(shape = as.factor(intermodal))
    ) +
    #facet_wrap(~ speed,
    #           scales = "free"
    #) +
    labs(title = paste(parameterStr, "by fare system (conv. KEXI)"),
         x = "Fare System",
         y = parameterStr,
         #color = "Area",
         #linetype = "All Day"
         #,shape = "Intermodal"
    ) +
    #theme_dark() +
    theme(
      plot.title = element_text(size = 20, face = "bold"),  # Titelgröße anpassen
      axis.title.x = element_text(size = 18),  # X-Achsentitelgröße anpassen
      axis.title.y = element_text(size = 18),  # Y-Achsentitelgröße anpassen
      axis.text = element_text(size = 14),  # Achsentextgröße anpassen
      legend.title = element_text(size = 18),  # Legendentitelgröße anpassen
      legend.text = element_text(size = 14),  # Legendtextgröße anpassen
      strip.text = element_text(size = 18, face = "bold")  # Facet-Textgröße anpassen
    )
}

unique(results$parameter)
plotByConfiguration("Rides")
plotByConfiguration("Avg. wait time")
#plotByConfiguration("Avg. ride distance [km]")
#plotByConfiguration("Empty ratio")
#plotByConfiguration("Total vehicle mileage [km]")
#plotByConfiguration("Avg. fare [MoneyUnit]" )
#plotByConfiguration("Pax per veh-km")

#####################
##Zusammenhang wait time und Nachfrage

handled_requests_data <- results %>%
  filter(parameter == "Rides") %>%
  select(fares, mean) %>%
  rename(handled_requests = mean)

avg_wait_time_data <- results %>%
  filter(parameter == "Avg. wait time") %>%
  select(fares, mean) %>%
  rename(avg_wait_time = mean)

# Zusammenführen der Daten
  plot_data <- left_join(handled_requests_data, avg_wait_time_data, by = c("fares"))

# Erstellen des Facet-Plots
facet_plot <- ggplot(plot_data, aes(x = avg_wait_time, y = handled_requests)) +
  #geom_line(size = 1.2) +
  geom_point(size = 3
             #,aes(shape = as.factor(intermodal))
  ) +
  geom_text(aes(label = fares), vjust = -1, hjust = 0.5, size = 6, color = "black") +
  #facet_wrap(~ speed, scales = "free") +
  labs(title = "Anzahl Passagiere nach durchschn. Wartezeit und Preisschema",
       x = "Durchschn. Wartezeit [s]",
       y = "# Passagiere",
       #color = "Area",
       #linetype = "All Day"
       #,shape = "Intermodal"
  ) +
  #theme_dark() +
  theme(
    plot.title = element_text(size = 20, face = "bold"),  # Titelgröße anpassen
    axis.title.x = element_text(size = 18),  # X-Achsentitelgröße anpassen
    axis.title.y = element_text(size = 18),  # Y-Achsentitelgröße anpassen
    axis.text = element_text(size = 14),  # Achsentextgröße anpassen
    legend.title = element_text(size = 18),  # Legendentitelgröße anpassen
    legend.text = element_text(size = 14),  # Legendtextgröße anpassen
    strip.text = element_text(size = 18, face = "bold")  # Facet-Textgröße anpassen
  )

# Plot anzeigen
print(facet_plot)
save("pax-over-avg-wait-time")

#####################
##Zusammenhang Durchschnittspreis und Nachfrage

# Filter für die beiden relevanten Parameter
rides_data <- results %>%
  filter(parameter == "Rides") %>%
  select(fares, mean) %>%
  rename(rides = mean)

avg_fare_data <- results %>%
  filter(parameter == "Avg. fare [MoneyUnit]") %>%
  select(fares, mean) %>%
  rename(avg_fare = mean)

# Zusammenführen der Daten
plot_data <- left_join(rides_data, avg_fare_data, by = c("fares"))

# Erstellen des Plots
fare_vs_rides_plot <- ggplot(plot_data, aes(x = avg_fare, y = rides)) +
  geom_point(size = 3) +
  geom_text(aes(label = fares), vjust = -1, hjust = 0.5, size = 6, color = "black") +
  labs(title = "Anzahl Passagiere nach Durchschnittspreis",
       x = "Durchschnittlicher Preis pro Fahrt [€]",
       y = "# Passagiere") +
  theme(
    plot.title = element_text(size = 20, face = "bold"),
    axis.title.x = element_text(size = 18),
    axis.title.y = element_text(size = 18),
    axis.text = element_text(size = 14),
    legend.title = element_text(size = 18),
    legend.text = element_text(size = 14),
    strip.text = element_text(size = 18, face = "bold")
  )

# Plot anzeigen
print(fare_vs_rides_plot)
save("pax-over-avg-fare")

