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
    theme_dark() +
    theme(
      plot.title = element_text(size = 16, face = "bold"),  # Titelgröße anpassen
      axis.title.x = element_text(size = 14),  # X-Achsentitelgröße anpassen
      axis.title.y = element_text(size = 14),  # Y-Achsentitelgröße anpassen
      axis.text = element_text(size = 12),  # Achsentextgröße anpassen
      legend.title = element_text(size = 14),  # Legendentitelgröße anpassen
      legend.text = element_text(size = 12),  # Legendtextgröße anpassen
      strip.text = element_text(size = 12)  # Facet-Textgröße anpassen
    )
  
}

unique(results$parameter)
plotByConfiguration("Rides")
plotByConfiguration("Avg. wait time")
plotByConfiguration("Avg. ride distance [km]")
plotByConfiguration("Empty ratio")
plotByConfiguration("Total vehicle mileage [km]")
plotByConfiguration("Avg. fare [MoneyUnit]" )
plotByConfiguration("Pax per veh-km")

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
  geom_text(aes(label = fares), vjust = -1, hjust = 0.5, size = 3, color = "white") +
  #facet_wrap(~ speed, scales = "free") +
  labs(title = "Handled Requests by Avg. Wait Time and Fare System (conv. KEXI)",
       x = "Avg. Wait Time",
       y = "Handled Requests",
       #color = "Area",
       #linetype = "All Day"
       #,shape = "Intermodal"
  ) +
  theme_dark() +
  theme(
    plot.title = element_text(size = 16, face = "bold"),  # Titelgröße anpassen
    axis.title.x = element_text(size = 14),  # X-Achsentitelgröße anpassen
    axis.title.y = element_text(size = 14),  # Y-Achsentitelgröße anpassen
    axis.text = element_text(size = 12),  # Achsentextgröße anpassen
    legend.title = element_text(size = 14),  # Legendentitelgröße anpassen
    legend.text = element_text(size = 12),  # Legendtextgröße anpassen
    strip.text = element_text(size = 12)  # Facet-Textgröße anpassen
  )

# Plot anzeigen
print(facet_plot)

