library(dplyr)
library(readr)
library(tidyr)
library(ggplot2)  # Für das Plotting mit ggplot2
library(plotly)

# Funktion zum Extrahieren der Parameter aus dem Ordnernamen
extract_parameters <- function(folder_name, speed) {
  # Extrahiere 'area'
  area <- strsplit(folder_name, '-')[[1]][1]
  
  # Extrahiere 'fleetSize'
  fleet_size_match <- regmatches(folder_name, regexpr("AV\\d+", folder_name))
  fleet_size <- as.numeric(gsub("AV", "", fleet_size_match))
  
  # Setze 'intermodal'
  intermodal <- grepl("intermodal", folder_name)
  
  # Setze 'allDay'
  all_day <- grepl("allDay", folder_name)
  
  return(list(speed = speed, area = area, fleetSize = fleet_size, intermodal = intermodal, allDay = all_day))
}

# Funktion zum Einlesen der CSV-Datei und Extrahieren der "mean"-Werte
read_stats <- function(folder_path, file_name) {
  csv_path <- file.path(folder_path, "analysis/drt-drt-av", file_name)
  
  if (file.exists(csv_path)) {
    df <- read_csv(csv_path)
    mean_values <- df %>% select(parameter, mean)
    return(mean_values)
  } else {
    return(NULL)
  }
}

# Hauptfunktion zum Iterieren durch Unterordner
process_folders <- function(main_folder, speed) {
  # Liste aller Unterordner im Hauptordner
  subfolders <- list.dirs(main_folder, recursive = FALSE, full.names = FALSE)
  
  # Initialisiere eine Liste zum Speichern der Ergebnisse
  results <- list()
  
  # Iteriere durch alle Unterordner
  for (subfolder in subfolders) {
    parameters <- extract_parameters(subfolder, speed)
    full_path <- file.path(main_folder, subfolder)
    
    demand_mean_values <- read_stats(full_path, "avg_demand_stats.csv")
    supply_mean_values <- read_stats(full_path, "avg_supply_stats.csv")
    
    if (!is.null(demand_mean_values) || !is.null(supply_mean_values)) {
      if (!is.null(demand_mean_values)) {
        demand_mean_values <- demand_mean_values %>% 
          mutate(type = "demand",
                 speed = parameters$speed,
                 area = parameters$area,
                 fleetSize = parameters$fleetSize,
                 intermodal = parameters$intermodal,
                 allDay = parameters$allDay)
      }
      
      if (!is.null(supply_mean_values)) {
        supply_mean_values <- supply_mean_values %>% 
          mutate(type = "supply",
                 speed = parameters$speed,
                 area = parameters$area,
                 fleetSize = parameters$fleetSize,
                 intermodal = parameters$intermodal,
                 allDay = parameters$allDay)
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

mainDir <- "D:/Projekte/KelRide/runs/v3.1.1/output-KEXI-2.45-AV--0.0/"
#mainDir <- "//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0/"
speeds <- list(3.3, 5, 8.3)

results <- list()
for (speed in speeds) {
  main_folder <- paste(mainDir, "AV-speed-mps-", speed, "/", sep="")
  runResults <- process_folders(main_folder, speed)  
  results[[speed]] <- runResults
}

results <- bind_rows(results)


# Transponiere die Tabelle, um Parameter als Spalten zu setzen
transposed_result <- results %>%
  select(speed, area, fleetSize, intermodal, allDay, parameter, mean) %>%
  spread(key = parameter, value = mean)


# Ergebnisse ausgeben
print(results)
print(transposed_result)

write_csv(transposed_result, paste(mainDir, "results.csv", sep=""))

#####################################################################
######PLOTS####

# Filtern der Daten für die gewünschten Parameter
plot_data <- results %>%
  filter(parameter %in% c("Handled Requests"))

# Erstellen des interaktiven dreidimensionalen Plots
plot <- plot_ly(plot_data, 
                x = ~speed, 
                y = ~fleetSize, 
                z =  ~mean, 
                color = ~area, 
                type = "scatter3d", 
                mode = "markers",
                marker = list(size = 5)) %>%
  add_markers() %>%
  layout(title = "Handled Requests by speed, area and Fleet Size",
         scene = list(xaxis = list(title = "Speed"),
                      yaxis = list(title = "Fleet Size"),
                      zaxis = list(title = "Handled Requests")))

# Plot anzeigen
plot

# Erstellen des interaktiven dreidimensionalen Plots mit Mesh
plot <- plot_ly(plot_data) %>%
  add_mesh(x = ~speed, 
           y = ~fleetSize, 
           z = ~mean,
           color = ~area,
           opacity = 0.6,  # Opazität der Flächen
           text = ~paste("Area:", area),
           colors = c("#1f77b4", "#ff7f0e", "#2ca02c", "#d62728", "#9467bd", "#8c564b", "#e377c2", "#7f7f7f", "#bcbd22", "#17becf"),  # Farben für jede Area
           showscale = TRUE) %>%
  layout(title = "Handled Requests by speed, fleetSize and area",
         scene = list(xaxis = list(title = "Speed"),
                      yaxis = list(title = "Fleet Size"),
                      zaxis = list(title = "Mean Handled Requests")))

# Plot anzeigen
plot

mesh_data <- plot_data %>%
  group_by(area) %>%
  summarise(speed = mean(speed),
            fleetSize = mean(fleetSize),
            mean = mean(mean)) %>%
  arrange(area)

# Erstellen des interaktiven dreidimensionalen Plots mit Meshes für jede Area
plot <- plot_ly(plot_data) %>%
  add_trace(x = ~speed, 
            y = ~fleetSize, 
            z = ~mean,
            color = ~area,
            type = "scatter3d",
            mode = "markers",
            marker = list(size = 5)) %>%
  add_trace(data = mesh_data,
            x = ~speed,
            y = ~fleetSize,
            z = ~mean,
            color = ~area,
            type = "mesh3d",
            opacity = 0.6,  # Opazität der Flächen
            colorscale = "Viridis",  # Farbskala für die Flächen
            showscale = TRUE) %>%
  layout(title = "Handled Requests by speed, fleetSize and area",
         scene = list(xaxis = list(title = "Speed"),
                      yaxis = list(title = "Fleet Size"),
                      zaxis = list(title = "Mean Handled Requests")))

# Plot anzeigen
plot


###########################
plotByConfiguration <- function(parameterStr){
  
  # Filtern der Daten für die gewünschten Parameter
  plot_data <- results %>%
    filter(parameter == parameterStr,
           intermodal == TRUE | area == "SAR2023")
  
  # Erstellen des Facet-Plots
  ggplot(plot_data, aes(x = fleetSize, y = mean, color = area, linetype = as.factor(allDay), group = interaction(area, allDay))) +
    geom_line(size = 1.2) +
    geom_point(size = 3,
               #aes(shape = as.factor(intermodal))
               ) +
    facet_wrap(~ speed,
               scales = "free"
               ) +
    labs(title = paste(parameterStr, "by Fleet Size, Speed, Area and Service Hours"),
         x = "Fleet Size",
         y = parameterStr,
         color = "Area",
         linetype = "All Day"
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
plotByConfiguration("Handled Requests")
plotByConfiguration("Avg. wait time")
plotByConfiguration("Avg. ride distance [km]")
plotByConfiguration("Empty ratio")
plotByConfiguration("Total vehicle mileage [km]")
plotByConfiguration("Avg. fare [MoneyUnit]" )
plotByConfiguration("Pax per veh-km")

  
#####################
##Zusammenhang wait time und Nachfrage

  handled_requests_data <- results %>%
    filter(parameter == "Handled Requests") %>%
    select(area, speed, fleetSize, allDay, mean, intermodal) %>%
    rename(handled_requests = mean)
  
  avg_wait_time_data <- results %>%
    filter(parameter == "Avg. wait time") %>%
    select(area, speed, fleetSize, allDay, mean, intermodal) %>%
    rename(avg_wait_time = mean)
  
  # Zusammenführen der Daten
  plot_data <- left_join(handled_requests_data, avg_wait_time_data, by = c("area", "speed", "fleetSize", "allDay", "intermodal"))
  
  # Erstellen des Facet-Plots
  facet_plot <- ggplot(plot_data, aes(x = avg_wait_time, y = handled_requests, color = area, linetype = as.factor(allDay), group = interaction(area, allDay))) +
    geom_line(size = 1.2) +
    geom_point(size = 3
               #,aes(shape = as.factor(intermodal))
               ) +
    facet_wrap(~ speed, scales = "free") +
    labs(title = "Handled Requests by Avg. Wait Time, Speed, Area, and All Day",
         x = "Avg. Wait Time",
         y = "Handled Requests",
         color = "Area",
         linetype = "All Day"
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
  