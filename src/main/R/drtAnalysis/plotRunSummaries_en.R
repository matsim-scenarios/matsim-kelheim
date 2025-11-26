library(ggplot2) 
library(dplyr)
library(tidyr)
library(stringr)
library(cowplot)
library(grid)

### this script needs input that is produced with readValuesFromRunSummaries.R (or gatherResults.sh on the cluster)

## the following path points to the runs-svn: https://data.vsp.tu-berlin.de/repos/runs-svn/KelRide/matsim-kelheim-v3.x/v3.1.1/output-KEXI-2.45-AV--0.0
## so you need to adjust it to your own local copy
mainDir <- "D:/runs-svn/KelRide/matsim-kelheim-v3.x/v3.1.1/output-KEXI-2.45-AV--0.0/"

## csv with information on waiting points. also sits on the cluster
wartepunkte_path <- paste(mainDir, "KelRide-Wartepunkte.csv", sep = "")
## local wartepunkty copy for backup
#wartepunkte_path <- "D:/Projekte/KelRide/AV-Service-Extension/untersucheNachfrageWartepunkte/KelRide-Wartepunkte.csv"

#set to true for AV and FALSE for conv. KEXI
stats_for_AV = TRUE

#########################################
#########################################
######## READ AND PREPARE DATA ##########      

if (stats_for_AV){
  # in order to (re-) create this file you need to run readValuesFromRunSummaries.R
  # and for that, you need to have checked out all the analysis (simwrapper) subfolders of the AV configurations -> meaning the average dashboard data over 5 seeds!
  input_file <- paste(mainDir, "results-av.csv", sep="")
} else {
  input_file <- paste(mainDir, "results-konvKEXI.csv", sep="")
}

##read input
transposed_result <- read.csv(input_file, check.names = FALSE, sep =",")
wartepunkte <- read.csv2(wartepunkte_path)



# serviceTimes umschreiben und faktorisieren
transposed_result$serviceTimes <- factor(ifelse(transposed_result$allDay == TRUE, "all-day", "9am - 4pm"),
                                         levels = c("all-day", "9am - 4pm"))

results <- transposed_result %>%
  gather(key = "parameter", value = "mean",
         -speed, -area, -fleetSize, -intermodal, -serviceTimes
         #, -Wartepunkte, -Wartepunkte_Namen
  )

results <- left_join(results, wartepunkte, join_by(area == Area.Key, fleetSize == Flottengroesse)) %>% 
  ## wir bennen um: 'konv. KEXI' --> 'conv. KEXI'
  #mutate(Bediengebiet = ifelse(Bediengebiet == "konv. KEXI", "conv. KEXI", Bediengebiet))
  ## wir bennen um: 'konv. KEXI' --> 'AV 2024' und 'konv. KEXI' --> 'Area All-2024'
  mutate(Bediengebiet = ifelse(Bediengebiet == "konv. KEXI", "Area All-City", Bediengebiet)) %>% 
  mutate(Bediengebiet = ifelse(Bediengebiet == "AV 2024", "Area 2024", Bediengebiet))


unique(results$parameter)


#########################################
#########################################
######## SAVING FUNCTION #############

save <- function(fileName){
  if (stats_for_AV){
    output_file_png <- paste(mainDir, "plots/AV/en-new/", fileName, "-AV.png", sep = "")
    output_file_pdf <- paste(mainDir, "plots/AV/en-new/", fileName, "-AV.pdf", sep = "")
  } else {
    output_file_png <- paste(mainDir, "plots/konvKEXI/en-new/", fileName, "-konvKEXI.png", sep = "")
    output_file_pdf <- paste(mainDir, "plots/konvKEXI/en-new/", fileName, "-konvKEXI.pdf", sep = "")
  }
  ggsave(filename = output_file_png,
         dpi = 600,
         width = 32, height = 24, units = "cm")
  ggsave(filename = output_file_pdf,
         #dpi = 600,
         width = 32, height = 24, units = "cm")
}


#########################################
#########################################
######## PLOTTING FUNCTIONS #############  
plotByOperatingTimes <- function(parameterStr, yAxisLabel = parameterStr, scales = "free", show_legend = FALSE){
  
  # Filtern der Daten für die gewünschten Parameter
  plot_data <- data %>%
    filter(parameter == parameterStr)  
  
  # Funktion zum Anpassen der Farbenlabels (für Geschwindigkeit)
  speed_label_function <- function(value) {
    paste0(round(as.numeric(value) * 3.6, 0), " km/h")
  }
  
  if (stats_for_AV){
    plot_title <- paste(parameterStr)
  } else {
    plot_title <- paste("CONV. KEXI:", parameterStr)
  }
  
  plot <- ggplot(plot_data, aes(
    x = fleetSize,
    y = mean,
    color = factor(speed),
    linetype = Bediengebiet,
    group = interaction(speed, Bediengebiet)
  )) +
    geom_line(linewidth = 1.2) +
    geom_point(size = 4, aes(shape = Bediengebiet)) +
    
    facet_wrap(~serviceTimes, scales = scales) +
    
    labs(
      title = plot_title,
      x = "log(Fleet Size) [n]",
      y = yAxisLabel,
      color = "Speed",
      linetype = "Service Area",
      shape = "Service Area"
    ) +
    
    scale_color_discrete(labels = speed_label_function) +
    
    theme(
      plot.title = element_text(size = 32, face = "bold"),
      axis.title.x = element_text(size = 22, face = "bold"),
      axis.title.y = element_text(size = 22, face = "bold"),
      axis.text = element_text(size = 20, face = "bold"),
      legend.title = element_text(size = 24, face = "bold"),
      legend.text = element_text(size = 20),
      strip.text = element_text(size = 24, face = "bold")
    )
  
  if (show_legend) {
    plot <- plot + theme(legend.position = "bottom")
  } else {
    plot <- plot + theme(legend.position = "none")
  }
  
  return(plot)
}

plotByConfiguration <- function(parameterStr, yAxisLabel = parameterStr, scales = "free", show_legend = FALSE){
  
  # Filtern der Daten für die gewünschten Parameter
  plot_data <- data %>%
    filter(parameter == parameterStr) %>%
    mutate(
      speed_fct = factor(speed),
      speed_kmh = paste0(round(as.numeric(speed) * 3.6), " km/h"),
      facet_group = interaction(Bediengebiet, serviceTimes, sep = " | ")
    )
  
  if (stats_for_AV){
    plot_title <- paste(parameterStr)
  } else {
    plot_title <- paste("CONV. KEXI:", parameterStr)
  }
  
  plot <- ggplot(plot_data, aes(
    x = fleetSize,
    y = mean,
    color = speed_fct,
    linetype = Bediengebiet,
    group = interaction(speed, Bediengebiet)
  )) +
    geom_line(linewidth = 1.2) +
    geom_point(size = 4, aes(shape = Bediengebiet)) +
    
    facet_wrap(~facet_group, scales = scales) +
    
    labs(
      title = plot_title,
      x = "Fleet Size [n]",
      y = yAxisLabel,
      color = "Speed",
      linetype = "Service Area",
      shape = "Service Area"
    ) +
    
    scale_color_discrete(labels = function(x) paste0(round(as.numeric(x) * 3.6), " km/h")) +
    
    theme(
      plot.title = element_text(size = 32, face = "bold"),
      axis.title.x = element_text(size = 22, face = "bold"),
      axis.title.y = element_text(size = 22, face = "bold"),
      axis.text = element_text(size = 20, face = "bold"),
      legend.title = element_text(size = 24, face = "bold"),
      legend.text = element_text(size = 20),
      strip.text = element_text(size = 24, face = "bold")
    )
  
  if (show_legend) {
    plot <- plot + theme(legend.position = "bottom")
  } else {
    plot <- plot + theme(legend.position = "none")
  }
  
  return(plot)
}

plotByConfigurationLog <- function(parameterStr, yAxisLabel = parameterStr, scales = "free", show_legend = FALSE){
  
  # Filtern der Daten für die gewünschten Parameter
  plot_data <- data %>%
    filter(parameter == parameterStr) %>%
    mutate(
      speed_fct = factor(speed),
      speed_kmh = paste0(round(as.numeric(speed) * 3.6), " km/h"),
      facet_group = interaction(Bediengebiet, serviceTimes, sep = " | ")
    )
  
  if (stats_for_AV){
    plot_title <- paste(parameterStr)
  } else {
    plot_title <- paste("CONV. KEXI:", parameterStr)
  }
  
  plot <- ggplot(plot_data, aes(
    x = fleetSize,
    y = mean,
    color = speed_fct,
    linetype = Bediengebiet,
    group = interaction(speed, Bediengebiet)
  )) +
    geom_line(linewidth = 1.2) +
    geom_point(size = 4, aes(shape = Bediengebiet)) +
    
    facet_wrap(~facet_group, scales = scales) +
    
    labs(
      title = plot_title,
      x = "Fleet Size [n]",
      y = yAxisLabel,
      color = "Speed",
      linetype = "Service Area",
      shape = "Service Area"
    ) +
    
    scale_color_discrete(labels = function(x) paste0(round(as.numeric(x) * 3.6), " km/h")) +
    scale_x_log10() +
    scale_y_log10() +
    
    theme(
      plot.title = element_text(size = 32, face = "bold"),
      axis.title.x = element_text(size = 22, face = "bold"),
      axis.title.y = element_text(size = 22, face = "bold"),
      axis.text = element_text(size = 20, face = "bold"),
      legend.title = element_text(size = 24, face = "bold"),
      legend.text = element_text(size = 20),
      strip.text = element_text(size = 24, face = "bold")
    )
  
  if (show_legend) {
    plot <- plot + theme(legend.position = "bottom")
  } else {
    plot <- plot + theme(legend.position = "none")
  }
  
  return(plot)
}

#############################
#############################
######## Filter DATA ########
      
      
      data <- results %>%
        filter(fleetSize < 150,
               str_detect(area, "Saal") # das Area-Encoding für die Konfigurations-Serie, die wir anschauen wollen ist KEXImSaal (grosses Gebiet) und SAR2023 (kleines Gebiet)
               #area == "ALLCITY" 
               | area == "SAR2023"
        ) %>% 
        # wir haben festgestellt, dass 2 Fzge im großen Bediengebiet zu unplausiblen Ergebnissen führen, weil zu viel rejected wird während innovation
        # deswegen sortieren wir diese Runs wieder aus
        filter( ! (fleetSize == 2 & str_detect(area, "Saal")) 
        )

#############################
#############################
######## PLOTTING############      


      ###passengers
      plotByConfiguration("Handled Requests", show_legend = TRUE)
      #save("singleConfig-handledRequests")
      plotByConfiguration("Passengers (Pax)", yAxisLabel = "Passengers [n]", scales = "fixed", show_legend = TRUE) +
        labs(title = "Nr. of AMoD passengers"
        )
      save("singleConfig-pax")
      plotByConfigurationLog("Passengers (Pax)", yAxisLabel = "Passengers [n]", scales = "fixed", show_legend = TRUE) +
        labs(title = "Nr. of AMoD passengers"
        )
      save("singleConfig-pax-log")
      plotByConfiguration("Passengers (Pax)", yAxisLabel = "Passengers [n]")
      #save("singleConfig-pax_yAxesDiff")
      
      ###waiting/travel time
      plotByConfigurationLog("Avg. wait time", yAxisLabel = "Avg. wait time [s]", scales = "fixed", show_legend =  TRUE)
      save("singleConfig-waitTime-log")
      plotByConfiguration("Avg. wait time", yAxisLabel = "Avg. wait time [s]", scales = "fixed", show_legend =  TRUE)
      save("singleConfig-waitTime")
      plotByConfiguration("95th percentile wait time", yAxisLabel = "95th percentile wait time [s]", scales = "fixed")
      #save("singleConfig-waitTime-p95")
      plotByConfiguration("Avg. total travel time", yAxisLabel = "Avg. total travel time [s]", scales =  "fixed", show_legend = TRUE)
      save("singleConfig-totalTravelTime")
      plotByConfiguration("Avg. in-vehicle time", yAxisLabel = "Avg. in-vehicle travel time [s]", scales =  "fixed", show_legend = TRUE)
      save("singleConfig-inVehTime")
      plotByConfigurationLog("Avg. in-vehicle time", yAxisLabel = "Avg. in-vehicle travel time [s]", scales =  "fixed", show_legend = TRUE)
      save("singleConfig-inVehTime-log")
      
      ###distance
      plotByConfiguration("Avg. ride distance [km]", scales = "fixed", show_legend = TRUE) +
        labs(title = "Avg. customer ride distance"
        )
      save("singleConfig-rideDistance")
      plotByConfigurationLog("Avg. ride distance [km]", scales = "fixed", show_legend = TRUE) +
        labs(title = "Avg. customer ride distance"
        )
      save("singleConfig-rideDistance-log")
      
      plotByConfiguration("Avg. direct distance [km]", scales = "fixed", show_legend = FALSE) +
        labs(title = "Avg. customer direct distance [km]"
        )
      save("singleConfig-directDistance")
      
      plotByConfigurationLog("Avg. direct distance [km]", scales = "fixed", show_legend = FALSE) +
        labs(title = "Avg. customer direct distance [km]"
        )
      save("singleConfig-directDistance-log")
      
      plotByConfiguration("Empty ratio", scales = "fixed") +
        labs(title = "Ratio of empty vehicle mileage"
        )
      save("singleConfig-emptyRatio")
      plotByConfiguration("Detour ratio", scales = "fixed") +
        labs(title = "Total vehicle mileage / Avg. direct customer distance"
        )
      #save("singleConfig-detourRatio")
      
      ###betrieb
      plotByConfiguration("Total vehicle mileage [km]", scales = "fixed")
      save("singleConfig-vehicleMileage")
      plotByConfiguration("Occupancy rate [pax-km/v-km]", scales = "fixed")
      save("singleConfig-occupancyRate")
      plotByConfiguration("Pax per veh-km", scales = "fixed", show_legend = FALSE) +
        labs(title = "Nr. of passengers per vehicle-km"
        ) +
        scale_y_continuous(labels = function(x) sprintf("%.2f", x))
      save("singleConfig-paxPerKM-en")
      
      plotByConfiguration("Pax per veh-h")
      plotByConfiguration("Pax per veh-h", scales = "fixed", show_legend = TRUE) +
        labs(title = "Nr. of passengers per vehicle-hour"
        )
      save("singleConfig-paxPerVehHour")
      
      plotByConfiguration("Total pax distance [km]", scales = "fixed")
      save("singleConfig-paxKM-en")
      



#############################################
#############################################
############################################
#### plotte mit mehr Konfigurationen für kleine Stützstellen


##########################


########
plotWithRibbon <- function(parameterStr, scales = "free", show_legend = FALSE){
  
  # Filtern der Daten für die gewünschten Parameter
  plot_data <- data %>%
    filter(parameter == parameterStr
           #,Intermodal == TRUE | Bediengebiet == "AV 2024"
    )
  
  print(names(plot_data))
  
  
  # Funktion zum Anpassen der Facet-Labels
  label_function <- function(value) {
    paste(round(as.numeric(value) * 3.6, 0), "km/h")
  }
  
  ribbon_data <- plot_data %>% 
  group_by(fleetSize, speed, Bediengebiet, serviceTimes) %>%
  summarise(Min = min(mean, na.rm = TRUE), 
            Max = max(mean, na.rm = TRUE), 
            Median = median(mean, na.rm = TRUE)) %>%
  ungroup()
 
  if (stats_for_AV){
    plot_title <- paste(#"AV KEXI:",
      parameterStr
      #,"by AV configuration\n (speed, fleet size, service area and times)"
    )
  } else {
    plot_title <- paste("CONV. KEXI:",
                        parameterStr
                        #, "by AV configuration\n (speed, fleet size, service area and times)"
    )
  }
  
  # Erstellen des Facet-Plots
  plot <- ggplot(plot_data, aes(x = fleetSize, y = mean,
                        color = Bediengebiet,
                        linetype = serviceTimes,
                        group = interaction(area, serviceTimes))) +
    geom_line(linewidth = 0.8) +
    geom_point(size = 2
               , aes(#shape = serviceTimes,
               color = #ifelse(str_detect(Wartepunkte_Namen, "BH"), "black", serviceTimes) 
                       Bediengebiet 
                 ) 
    )+
    
    
    # Fläche zwischen min und max markieren
    geom_ribbon(data = ribbon_data, aes(x = fleetSize, ymin = Min, ymax = Max,
                                        group = interaction(Bediengebiet, serviceTimes),
                                        fill = Bediengebiet), 
                alpha = 0.2,  # Transparenz
                inherit.aes = FALSE, # Verhindert Konflikte mit globalem Mapping
                color = NA) + # Keine Randlinien
    
    
    facet_wrap(~speed,
               labeller = labeller(speed = label_function)
               ,scales = scales
    ) +
    
    #scale_x_log10() +
    #scale_y_log10() +
    
    labs(title = plot_title,
         x = "Fleet Size [n]",
         y = parameterStr,
         color = "Service Area",
         linetype = "Service Times",
         #shape = "Service Times",
         fill = "Service Area"
    ) +
    
    scale_linetype_manual(values = c("9am - 4pm" = "solid", "all-day" = "dashed")) +
    scale_color_manual(values = c("Area 2024" = "lightcoral", "Area All-City" = "darkturquoise")) +
    scale_fill_manual(values = c("Area 2024" = "lightcoral", "Area All-City" = "darkturquoise")) +
    #scale_color_manual(values = c("9am - 4pm" = "lightcoral", "all-day" = "darkturquoise"))  + 
    #scale_fill_manual(values = c("9am - 4pm" = "lightcoral", "all-day" = "darkturquoise")) +
  
    
    #geom_text(aes(label = fleetSize), vjust = -1, hjust = 0.5, size = 3, color = "black") +
    #geom_text(aes(label = area), vjust = -1, hjust = 0.5, size = 2, color = "black") +
    
    #geom_text(data = filter(plot_data, str_detect(Wartepunkte_Namen, "BH")),
    #          aes(x = fleetSize + 1, y = mean - 2, label = Wartepunkte), 
    #          color = "black", size = 5, fontface = "bold") + 
    
    #geom_segment(data = filter(plot_data, str_detect(Wartepunkte_Namen, "BH")),
    #             aes(x = fleetSize + 0.5, xend = fleetSize + 0.1, 
    #                 y = mean - 2, yend = mean),
    #             arrow = arrow(length = unit(0.2, "cm")), color = "black") + 
    
    
    #geom_text(data = filter(plot_data, Wartepunkte == "7,2"),
    #          aes(x = fleetSize + 1, y = mean - 2, label = "WPs 7,2"), 
    #          color = "black", size = 5, fontface = "bold") + 
    
    #geom_segment(data = filter(plot_data, Wartepunkte == "7,2"),
    #             aes(x = fleetSize + 0.5, xend = fleetSize + 0.1, 
    #                 y = mean - 2, yend = mean),
    #             arrow = arrow(length = unit(0.2, "cm")), color = "black") + 
    
    #theme_dark() +
    
    theme(
      plot.title = element_text(size = 32, face = "bold"),  # Titelgröße anpassen
      axis.title.x = element_text(size = 22, face = "bold"),  # X-Achsentitelgröße anpassen
      axis.title.y = element_text(size = 22, face = "bold"),  # Y-Achsentitelgröße anpassen
      axis.text = element_text(size = 20, face = "bold"),  # Achsentextgröße anpassen
      legend.title = element_text(size = 24, face = "bold"),  # Legendentitelgröße anpassen
      legend.text = element_text(size = 20),  # Legendtextgröße anpassen
      strip.text = element_text(size = 24, face = "bold")  # Facet-Textgröße anpassen
    )

  
  # Falls die Legende aktiviert ist, positioniere sie unten
  if (show_legend) {
    plot <- plot + theme(legend.position = "bottom")
  } else {
    plot <- plot + theme(legend.position = "none")
  }
  
  return(plot)
  
}

#####################################################################################

data <- results %>%
  filter(#Bediengebiet != "conv. KEXI\n schlechte Wartepunkte",
    #str_starts(Bediengebiet, "conv."),
    str_starts(Bediengebiet, "Area All"),
    serviceTimes == "9am - 4pm",
    #serviceTimes == "all-day",
    fleetSize < 10,
    fleetSize > 2,
    
    # in AV2 ist DPWM 2x drin --> ALLCITY noch raus schmeißen !!!
    # außerdem die ganzen Runs mit 1 Wartepunkt = Depot rausschmeißen
    str_count(Wartepunkte_Namen, ",") == fleetSize -1,
    speed != 5.0
  )

## wir wollen dass die AllCity Plots nicht als Linie auftauchen.
data <- data %>% 
  mutate(area = case_when(
    area == "ALLCITY" & fleetSize == 2 ~ "allCity",  # Ersetze "NEUER_WERT" durch den gewünschten neuen Wert
    TRUE ~ area  # Alle anderen Werte bleiben unverändert
  )) %>% 
## wir bennen um: 'konv. KEXI' --> 'conv. KEXI'
  mutate(Bediengebiet = ifelse(Bediengebiet == "konv. KEXI", "conv. KEXI", Bediengebiet))


#unique(data$area)
###nachfrage
#plotWithRibbon("Handled Requests")
#save("ribbon-handledRequests")
#plotWithRibbon("Passengers (Pax)", "fixed")
#save("ribbon-pax")


plotWithRibbon("Passengers (Pax)") + 
  labs(title = "Nr. of AMoD passengers",
       y = "Passengers [n]",
  )


save("ribbon-pax_yAxesDiff")
#plotWithRibbon("Avg. wait time", "fixed", show_legend = TRUE)
#save("ribbon-waitTime-legend")

plotWithRibbon("Avg. wait time", "fixed") +
  labs(title = "Avg. wait time",
       y = "Avg. wait time [s]",
  )
save("ribbon-waitTime")

#plotWithRibbon("95th percentile wait time", "fixed")
plotWithRibbon("Avg. total travel time", "fixed")

plotWithRibbon("Avg. ride distance [km]", "fixed")
save("ribbon-rideDistance")
plotWithRibbon("Empty ratio", "fixed")
save("ribbon-emptyRation")
#plotWithRibbon("Detour ratio", "fixed")

###betrieb
#plotWithRibbon("Total vehicle mileage", "fixed")
#save("ribbon-vehicleMileage")
#plotWithRibbon("Occupancy rate [pax-km/v-km]", "fixed")
#save("ribbon-occupancyRate")
plotWithRibbon("Pax per veh-km", "fixed")
save("ribbon-paxPerKM-en")
plotWithRibbon("Pax per veh-h")
plotWithRibbon("Pax per veh-h", "fixed")
#save("ribbon-paxPerVehHou-enr")

#plotWithRibbon("Total pax distance [km]", "fixed")
#save("ribbon-paxKM-en")



#######################################################
##### TESTING

av2_12 <- data %>% 
  filter(fleetSize == 2, speed == 3.3)
unique(av2_12$area)

av2_12_avgWait <- av2_12 %>% 
  filter(parameter == "Avg. wait time") %>% 
  mutate(mean = as.numeric(mean))

av2_12_avgAvgWait <- mean(av2_12_avgWait$mean)

var(av2_12_avgWait$mean)
max(av2_12_avgWait$mean) / min(av2_12_avgWait$mean)

####
av2_30 <- data %>% 
  filter(fleetSize == 2, speed == 8.3)
unique(av2$area)

av2_30_avgWait <- av2_30 %>% 
  filter(parameter == "Avg. wait time") %>% 
  mutate(mean = as.numeric(mean))
av2_30_avgAvgWait <- mean(av2_30_avgWait$mean)

var(av2_30_avgWait$mean)
max(av2_30_avgWait$mean) / min(av2_30_avgWait$mean)

######
av5_12 <- data %>% 
  filter(fleetSize == 5, speed == 3.3)
unique(av5_12$area)

av5_12_avgWait <- av5_12 %>% 
  filter(parameter == "Avg. wait time") %>% 
  mutate(mean = as.numeric(mean))
av5_12_avgAvgWait <- mean(av5_12_avgAvgWait$mean)

var(av5_12_avgWait$mean)
max(av5_12_avgWait$mean) / min(av5_12_avgWait$mean)

#
av5_12_avgPax <- av5_12 %>% 
  filter(parameter == "Passengers (Pax)") %>% 
  mutate(mean = as.numeric(mean))
av5_12_avgAvgPax <- mean(av5_12_avgPax$mean)

var(av5_12_avgPax$mean)
max(av5_12_avgPax$mean) / min(av5_12_avgPax$mean)

####
av5_30 <- data %>% 
  filter(fleetSize == 5, speed == 8.3)
unique(av5_30$area)

av5_30_avgWait <- av5_30 %>% 
  filter(parameter == "Avg. wait time") %>% 
  mutate(mean = as.numeric(mean))
av5_30_avgAvgWait <- mean(av5_30_avgWait$mean)

var(av5_30_avgWait$mean)
max(av5_30_avgWait$mean) / min(av5_30_avgWait$mean)

#
av5_30_avgPax <- av5_30 %>% 
  filter(parameter == "Passengers (Pax)") %>% 
  mutate(mean = as.numeric(mean))
av5_30_avgAvgPax <- mean(av5_30_avgPax$mean)

var(av5_30_avgPax$mean)
max(av5_30_avgPax$mean) / min(av5_30_avgPax$mean)



#######
av8_12 <- data %>% 
  filter(fleetSize == 8, speed == 3.3)
unique(av8_12$area)

av8_12_avgWait <- av8_12 %>% 
  filter(parameter == "Avg. wait time") %>% 
  mutate(mean = as.numeric(mean))
av8_12_avgAvgWait <- mean(av8_12_avgWait$mean)

var(av8_12_avgWait$mean)
max(av8_12_avgWait$mean) / min(av8_12_avgWait$mean)

av8_12_avgPax <- av8_12 %>% 
  filter(parameter == "Passengers (Pax)") %>% 
  mutate(mean = as.numeric(mean))
av8_12_avgAvgPax <- mean(av8_12_avgPax$mean)

var(av8_12_avgPax$mean)
max(av8_12_avgPax$mean) / min(av8_12_avgPax$mean)


##
av8_30 <- data %>% 
  filter(fleetSize == 8, speed == 8.3)
unique(av8_30$area)

av8_30_avgWait <- av8_30 %>% 
  filter(parameter == "Avg. wait time") %>% 
  mutate(mean = as.numeric(mean))
av8_30_avgAvgWait <- mean(av8_30_avgWait$mean)

var(av8_30_avgWait$mean)
max(av8_30_avgWait$mean) / min(av8_30_avgWait$mean)

#
av8_30_avgPax <- av8_30 %>% 
  filter(parameter == "Passengers (Pax)") %>% 
  mutate(mean = as.numeric(mean))
av8_30_avgAvgPax <- mean(av8_30_avgPax$mean)

var(av8_30_avgPax$mean)
max(av8_30_avgPax$mean) / min(av8_30_avgPax$mean)




#########################################################
####
# 

data <- results %>%
  filter(#Bediengebiet != "conv. KEXI\n schlechte Wartepunkte",
    #Bediengebiet == "AV 2024",
    #Wartepunkte == "8-10",
    #str_starts(Bediengebiet, "AV 2024"),
    str_starts(Bediengebiet, "conv."),
    #str_ends(area, "Saal"),
    serviceTimes == "9am - 4pm",
    #serviceTimes == "all-day",
    fleetSize < 50,
    
    # in AV2 ist DPWM 2x drin --> ALLCITY noch raus schmeißen !!!
    # außerdem die ganzen Runs mit 1 Wartepunkt = Depot rausschmeißen
    
    #str_count(Wartepunkte_Namen, ",") == fleetSize -1,
    #area == "ALLCITY",
    
    
    #& ! str_detect(Bediengebiet, "SAR")
    #         | str_detect(Bediengebiet, "SAR"),
    
    #speed != 5.0
  )



plotWithRibbon("Avg. wait time", "fixed") +
  labs(title = "Avg. wait time",
       y = "Avg. wait time [s]",
  )





########
plotWithRibbon2 <- function(parameterStr, scales = "free", show_legend = FALSE){
  
  # Filtern der Daten für die gewünschten Parameter
  plot_data <- data %>%
    filter(parameter == parameterStr
           #,Intermodal == TRUE | Bediengebiet == "AV 2024"
    )
  
  print(names(plot_data))
  
  
  # Funktion zum Anpassen der Facet-Labels
  label_function <- function(value) {
    paste(round(as.numeric(value) * 3.6, 0), "km/h")
  }
  
  ribbon_data <- plot_data %>% 
    mutate(hatBhf = str_detect(Wartepunkte_Namen, "BH")) %>% 
    group_by(fleetSize, speed, serviceTimes, hatBhf) %>%
    summarise(Min = min(mean, na.rm = TRUE), 
              Max = max(mean, na.rm = TRUE), 
              Median = median(mean, na.rm = TRUE)) %>%
    ungroup()
  
  if (stats_for_AV){
    plot_title <- paste(#"AV KEXI:",
      parameterStr
      #,"by AV configuration\n (speed, fleet size, service area and times)"
    )
  } else {
    plot_title <- paste("CONV. KEXI:",
                        parameterStr
                        #, "by AV configuration\n (speed, fleet size, service area and times)"
    )
  }
  
  # Erstellen des Facet-Plots
  plot <- ggplot(plot_data, aes(x = fleetSize, y = mean,
                                color = Bediengebiet,
                                linetype = serviceTimes,
                                group = interaction(area, serviceTimes))) +
    geom_line(linewidth = 0.8) +
    geom_point(size = 2
               , aes(shape = serviceTimes,
                     color = ifelse(str_detect(Wartepunkte_Namen, "BH"), "red", "blue") ) 
    )+
    
    
    # Fläche zwischen min und max markieren
    geom_ribbon(data = ribbon_data, aes(x = fleetSize, ymin = Min, ymax = Max,
                                        group = interaction(serviceTimes, hatBhf),
                                        fill = hatBhf), 
                alpha = 0.1,  # Transparenz
                inherit.aes = FALSE, # Verhindert Konflikte mit globalem Mapping
                color = NA) + # Keine Randlinien
    
    
    facet_wrap(~speed,
               labeller = labeller(speed = label_function)
               ,scales = scales
    ) +
    
    #scale_x_log10() +
    #scale_y_log10() +
    
    labs(title = plot_title,
         x = "Fleet Size",
         y = parameterStr,
         color = "Hat WP am Bhf",
         linetype = "Service Times",
         shape = "Service Times",
         fill = "Service Area"
    ) +
    
    scale_linetype_manual(values = c("9am - 4pm" = "solid", "all-day" = "dashed")) +
    #scale_color_manual(values = c("AV 2024" = "lightcoral", "conv. KEXI" = "darkturquoise"))  + 
    #scale_fill_manual(values = c("AV 2024" = "lightcoral", "conv. KEXI" = "darkturquoise")) +
    
    
    #geom_text(aes(label = fleetSize), vjust = -1, hjust = 0.5, size = 3, color = "black") +
    #geom_text(aes(label = Wartepunkte_Namen), vjust = -1, hjust = 0.5, size = 2, color = "black") +
    
    #geom_text(data = filter(str_detect(Wartepunkte_Namen, "BH")),
    #          aes(x = fleetSize + 1, y = mean - 2, label = Wartepunkte_Namen), 
    #          color = "black", size = 5, fontface = "bold") + 
    
    #geom_segment(data = filter(str_detect(Wartepunkte_Namen, "BH")),
    #             aes(x = fleetSize + 0.5, xend = fleetSize + 0.1, 
    #                 y = mean - 2, yend = mean),
    #             arrow = arrow(length = unit(0.2, "cm")), color = "black") + 
    
    
    #geom_text(data = filter(plot_data, Wartepunkte == "7,2"),
    #          aes(x = fleetSize + 1, y = mean - 2, label = "WPs 7,2"), 
    #          color = "black", size = 5, fontface = "bold") + 
    
    #geom_segment(data = filter(plot_data, Wartepunkte == "7,2"),
    #             aes(x = fleetSize + 0.5, xend = fleetSize + 0.1, 
    #                 y = mean - 2, yend = mean),
    #             arrow = arrow(length = unit(0.2, "cm")), color = "black") + 
    
    #theme_dark() +
    
    theme(
      plot.title = element_text(size = 32, face = "bold"),  # Titelgröße anpassen
      axis.title.x = element_text(size = 22, face = "bold"),  # X-Achsentitelgröße anpassen
      axis.title.y = element_text(size = 22, face = "bold"),  # Y-Achsentitelgröße anpassen
      axis.text = element_text(size = 20, face = "bold"),  # Achsentextgröße anpassen
      legend.title = element_text(size = 24, face = "bold"),  # Legendentitelgröße anpassen
      legend.text = element_text(size = 20),  # Legendtextgröße anpassen
      strip.text = element_text(size = 24, face = "bold")  # Facet-Textgröße anpassen
    )
  
  
  # Falls die Legende aktiviert ist, positioniere sie unten
  if (show_legend) {
    plot <- plot + theme(legend.position = "bottom")
  } else {
    plot <- plot + theme(legend.position = "none")
  }
  
  return(plot)
  
}


plotWithRibbon2("Avg. wait time", "fixed", show_legend = TRUE) +
  labs(title = "Avg. wait time",
       y = "Avg. wait time [s]",
  )





###########################################
#####
#Travel time components

componentData <- results %>% 
  filter (area %in% c(
    "SAR2023"
    ,"WIEKEXImSaal"
    ),
          #parameter %in% c("Avg. wait time", "Avg. in-vehicle time"))
          parameter %in% c("Avg. direct distance [km]", "Avg. ride distance [km]"))

label_function <- function(value) {
  paste(round(as.numeric(value) * 3.6, 0), "km/h")
}

ggplot(componentData, aes(x = fleetSize, y = mean, fill = parameter)) +
  geom_bar(stat = "identity") +
  labs(x = "Flottengröße", y = "Durchschnittswerte", fill = "Parameter") +
  facet_wrap(~speed,
             labeller = labeller(speed = label_function)
             ,scales = "free"
  ) +
  theme_minimal()

ratioData <- componentData %>%
  group_by(fleetSize, speed, Bediengebiet, serviceTimes) %>%
  pivot_wider(names_from = parameter, values_from = mean) %>%  # Parameter zu Spalten machen
  #mutate(ratio = `Avg. in-vehicle time` / `Avg. wait time`) %>%
  mutate(ratio = `Avg. ride distance [km]` / `Avg. direct distance [km]`) %>%
  ungroup()  # Falls keine weitere Gruppierung benötigt wird
  

# Linienplot mit Stützstellen
ggplot(ratioData, aes(x = fleetSize, y = ratio,
                      color = Bediengebiet,
                      linetype = serviceTimes,
                      group = interaction(area, serviceTimes)) ) +
  geom_line(linewidth = 1.2) +
  geom_point(size = 4
             , aes(shape = serviceTimes)
  )+
  labs(x = "Flottengröße",
       y = "Ratio (In-Vehicle Time / Wait Time)",
       color = "Geschwindigkeit",
       plot_title = "Ratio In-Veh Time / Wait Time") +
  facet_wrap(~speed, labeller = labeller(speed = label_function), scales = "fixed") +
  scale_linetype_manual(values = c("9am - 4pm" = "solid", "all-day" = "dashed")) +
  scale_color_manual(values = c("AV 2024" = "lightcoral", "conv. KEXI" = "darkturquoise"))  + 
  
  theme(
    plot.title = element_text(size = 32, face = "bold"),  # Titelgröße anpassen
    axis.title.x = element_text(size = 22, face = "bold"),  # X-Achsentitelgröße anpassen
    axis.title.y = element_text(size = 22, face = "bold"),  # Y-Achsentitelgröße anpassen
    axis.text = element_text(size = 20, face = "bold"),  # Achsentextgröße anpassen
    legend.title = element_text(size = 24, face = "bold"),  # Legendentitelgröße anpassen
    legend.text = element_text(size = 20),  # Legendtextgröße anpassen
    legend.position = "none",
    strip.text = element_text(size = 24, face = "bold")  # Facet-Textgröße anpassen
  )

#save("singleConfig-travelTimeRatio-convKEXI")
save("singleConfig-travelTimeRatio-AV2024")


