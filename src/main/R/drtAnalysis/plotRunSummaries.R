library(ggplot2) 
library(dplyr)
library(tidyr)
library(stringr)

### this script needs input that is produced with readValuesFromRunSummaries.R
###which then needs to get manually copied into a file with German headers...

mainDir <- "E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0/"

#set to true for AV and FALSE for conv. KEXI
stats_for_AV = TRUE

################################################################################################
################################################################################################

if (stats_for_AV){
  input_file <- paste(mainDir, "results-av-deutsch.csv", sep="")
} else {
  input_file <- paste(mainDir, "results-konvKEXI-deutsch.csv", sep="")
}
  
  
transposed_result <- read.csv(input_file, check.names = FALSE, sep =",")
wartepunkte <- read.csv2("D:/Projekte/KelRide/AV-Service-Extension/untersucheNachfrageWartepunkte/KelRide-Wartepunkte.csv")

{
  ##Bediengebiete umkodieren
  #transposed_result <- transposed_result %>%
  #  mutate(Bediengebiet = recode(Bediengebiet, 
  #                               "SAR2023" = "AV 2024 (8WP)", 
  #                               "SAR2023_WM" = "AV 2024\n (WM)",
  #                               "SAR2023_DPWM" = "AV 2024\n (DP + WM)",
  #                               "ALLCITY" = "konv. KEXI\n (8-kein WP im Süden)", 
  #                               "WIEKEXI" = "konv. KEXI\n (9-nie WP am Bhf)",
  #                               "WIEKEXI_WM" = "konv. KEXI\n (WM)",
  #                               "WIEKEXI_DPWM" = "konv. KEXI\n (DP + WM)",
  #                               "WIEKEXImSaal" = "konv. KEXI\n (10-immer WP am Bhf)",
  #                               "ALLDEPOTklein" = "AV 2024\n (Depot einziger WP)",
  #                               "ALLDEPOTgross" = "konv. KEXI\n (Depot einziger WP)",),
  #         Wartepunkte = case_when(
  #           str_ends(Bediengebiet, "\\(WM\\)") ~ "1",
  #           str_ends(Bediengebiet, "einziger WP\\)") ~ "1",
  #           str_ends(Bediengebiet, "\\(DP \\+ WM\\)") ~ "2",
  #           #Bediengebiet == "AV 2024" ~ "<=8",
  #           #str_ends(Bediengebiet, "\\(kein WP im Süden\\)") ~ "<=8",
  #           #str_ends(Bediengebiet, "\\(nie WP am Bhf\\)") ~ "<=9",
  #           TRUE ~ "8-10"
  #           )
  #  )
}

# Betriebszeiten umschreiben und faktorisieren
transposed_result$Betriebszeiten <- factor(ifelse(transposed_result$Betriebszeiten == TRUE, "0h - 24h", "9h - 16h"),
                                           levels = c("9h - 16h", "0h - 24h"))

results <- transposed_result %>%
  gather(key = "parameter", value = "mean",
         -Geschwindigkeit, -Bediengebiet, -Flottengroeße, -Intermodal, -Betriebszeiten
         #, -Wartepunkte, -Wartepunkte_Namen
         )

results <- left_join(results, wartepunkte, join_by(Bediengebiet == Area.Key, Flottengroeße == Flottengroesse)) #%>%  
  #mutate(Bediengebiet = Bediengebiet.y)


unique(results$parameter)

##############################################################################################
save <- function(fileName){
  if (stats_for_AV){
    output_file <- paste(mainDir, "plots/AV/", fileName, "-AV.png", sep = "")
  } else {
    output_file <- paste(mainDir, "plots/konvKEXI/", fileName, "-konvKEXI.png", sep = "")
  }
  ggsave(filename = output_file,
         dpi = 600, width = 32, height = 18, units = "cm")
}


#####################################################################################
##############################
########## PLOT ausgangsvergleich
########
plotByConfiguration <- function(parameterStr, scales = "free"){
  
  # Filtern der Daten für die gewünschten Parameter
  plot_data <- data %>%
    filter(parameter == parameterStr)  
  
  # Funktion zum Anpassen der Facet-Labels
  label_function <- function(value) {
    paste(round(as.numeric(value) * 3.6, 0), "km/h")
  }
  
  if (stats_for_AV){
    plot_title <- paste("AV KEXI:",
                        parameterStr,
                        "nach AV-Konfiguration\n (Geschwindigkeit, Flottengröße, Bediengebiet und Betriebszeiten)")
  } else {
    plot_title <- paste("KONV. KEXI:",
                        parameterStr,
                        "nach AV-Konfiguration\n (Geschwindigkeit, Flottengröße, Bediengebiet und Betriebszeiten)")
  }
  
  # Erstellen des Facet-Plots
  ggplot(plot_data, aes(x = Flottengroeße, y = mean,
                        color = Bediengebiet.y,
                        linetype = Betriebszeiten,
                        group = interaction(Bediengebiet, Betriebszeiten))) +
    geom_line(linewidth = 0.8) +
    geom_point(size = 4
               , aes(shape = Betriebszeiten)
    )+
    
    facet_wrap(~ Geschwindigkeit,
               labeller = labeller(Geschwindigkeit = label_function)
               ,scales = scales
    ) +
    
    #scale_x_log10() +
    #scale_y_log10() +
    
    labs(title = plot_title,
         x = "Flottengröße",
         y = parameterStr,
         color = "Bediengebiet",
         linetype = "Betriebszeiten",
         shape = "Betriebszeiten"
    ) +
    
    #geom_text(aes(label = Flottengroeße), vjust = -1, hjust = 0.5, size = 3, color = "black") +
    #geom_text(aes(label = Betriebszeiten), vjust = -1, hjust = 0.5, size = 3, color = "black") +
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

#####################################################################################

if(FALSE){
  
  data <- results %>%
    filter(Flottengroeße < 200,
           str_detect(Bediengebiet, "Saal")
           | Bediengebiet == "SAR2023"
    )
  
  ###nachfrage
  plotByConfiguration("Bediente Anfragen")
  save("singleConfig-bedienteAnfragen")
  plotByConfiguration("Anzahl Passagiere", "fixed")
  save("singleConfig-passagiere")
  plotByConfiguration("Anzahl Passagiere")
  save("singleConfig-passagiere_yAxesDiff")
  plotByConfiguration("Mittl. Wartezeit [s]", "fixed")
  save("singleConfig-wartezeit")
  plotByConfiguration("95. Perzentil Wartezeit", "fixed")
  plotByConfiguration("Mittl. Gesamtreisezeit [s]", "fixed")
  
  plotByConfiguration("Mittl. Reiseweite [km]", "fixed")
  save("singleConfig-reiseweite")
  plotByConfiguration("Anteil Leerkilometer", "fixed")
  save("singleConfig-leerkilometer")
  plotByConfiguration("Umwegfaktor", "fixed")
  
  ###betrieb
  plotByConfiguration("Summe Fahrzeugkilometer [km]", "fixed")
  save("singleConfig-fahrzeugkilometer")
  plotByConfiguration("Besetzungsgrad [pax-km/v-km]", "fixed")
  save("singleConfig-besetzungsgrad")
  plotByConfiguration("Passagiere pro Fahrzeugkilometer", "fixed")
  save("singleConfig-paxPerKM")
  plotByConfiguration("Passagiere pro Fahrzeugstunde")
  plotByConfiguration("Passagiere pro Fahrzeugstunde", "fixed")
  save("singleConfig-paxPerVehHour")
  
  plotByConfiguration("Gesamt Passagierkilometer [km]", "fixed")
  save("singleConfig-paxKM")
  
}

#############################################
###########################################################################
############################################
#### plotte mit mehr Konfigurationen für kleine Stützstellen


##########################


########
plotWithRibbon <- function(parameterStr, scales = "free"){
  
  # Filtern der Daten für die gewünschten Parameter
  plot_data <- data %>%
    filter(parameter == parameterStr
           #,Intermodal == TRUE | Bediengebiet == "AV 2024"
    )
  
  print(names(plot_data))
  
  ribbon_data <- plot_data %>% 
  group_by(Flottengroeße, Geschwindigkeit, Bediengebiet.y, Betriebszeiten) %>%
  summarise(Min = min(mean, na.rm = TRUE), 
            Max = max(mean, na.rm = TRUE), 
            Median = median(mean, na.rm = TRUE)) %>%
  ungroup()
  
  #print(head(plot_data))
  
  # Funktion zum Anpassen der Facet-Labels
  label_function <- function(value) {
    paste(round(as.numeric(value) * 3.6, 0), "km/h")
  }
  
  if (stats_for_AV){
    plot_title <- paste("AV KEXI:",
                        parameterStr,
                        "nach AV-Konfiguration\n (Geschwindigkeit, Flottengröße, Bediengebiet und Betriebszeiten)")
  } else {
    plot_title <- paste("KONV. KEXI:",
                        parameterStr,
                        "nach AV-Konfiguration\n (Geschwindigkeit, Flottengröße, Bediengebiet und Betriebszeiten)")
  }
  
  # Erstellen des Facet-Plots
  ggplot(plot_data, aes(x = Flottengroeße, y = mean,
                        color = Bediengebiet,
                        linetype = Betriebszeiten,
                        group = interaction(Bediengebiet, Betriebszeiten))) +
    
    geom_line(linewidth = 0.8) +
    geom_point(size = 4
               , aes(shape = Betriebszeiten)
    )+
    
    # Fläche zwischen min und max markieren
    geom_ribbon(data = ribbon_data, aes(x = Flottengroeße, ymin = Min, ymax = Max,
                                        group = interaction(Bediengebiet.y, Betriebszeiten),
                                        fill = Bediengebiet.y), 
                alpha = 0.2,  # Transparenz
                inherit.aes = FALSE, # Verhindert Konflikte mit globalem Mapping
                color = NA) + # Keine Randlinien
    
    facet_wrap(~ Geschwindigkeit,
               labeller = labeller(Geschwindigkeit = label_function)
               ,scales = scales
    ) +
    
    #scale_x_log10() +
    #scale_y_log10() +
    
    labs(title = plot_title,
         x = "Flottengröße",
         y = parameterStr,
         color = "Bediengebiet",
         linetype = "Betriebszeiten",
         shape = "Betriebszeiten"
    ) +
    
    #geom_text(aes(label = Flottengroeße), vjust = -1, hjust = 0.5, size = 3, color = "black") +
    geom_text(aes(label = Wartepunkte_Namen), vjust = -1, hjust = 0.5, size = 3, color = "black") +
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

#####################################################################################

data <- results %>%
  filter(#Bediengebiet != "konv. KEXI\n schlechte Wartepunkte",
    #Bediengebiet == "AV 2024",
    #Wartepunkte == "8-10",
    #str_starts(Bediengebiet, "AV 2024"),
    #str_starts(Bediengebiet, "konv."),
    str_starts(Bediengebiet, "WIE"),
    Betriebszeiten == "9h - 16h",
    #Betriebszeiten == "0h - 24h",
    Flottengroeße <= 10,
    
    str_detect(Bediengebiet, "Saal")
    | str_count(Wartepunkte_Namen, ",") == Flottengroeße -1,
    
    #& ! str_detect(Bediengebiet, "SAR")
    #         | str_detect(Bediengebiet, "SAR"),
    
    Geschwindigkeit != 5.0
  )

###nachfrage
#plotWithRibbon("Bediente Anfragen")
#save("ribbon-bedienteAnfragen")
#plotWithRibbon("Anzahl Passagiere", "fixed")
#save("ribbon-passagiere")
#plotWithRibbon("Anzahl Passagiere")
#save("ribbon-passagiere_yAxesDiff")
plotWithRibbon("Mittl. Wartezeit [s]", "fixed")
#save("ribbon-wartezeit")
#plotWithRibbon("95. Perzentil Wartezeit", "fixed")
plotWithRibbon("Mittl. Gesamtreisezeit [s]", "fixed")

plotWithRibbon("Mittl. Reiseweite [km]", "fixed")
#save("ribbon-reiseweite")
plotWithRibbon("Anteil Leerkilometer", "fixed")
#save("ribbon-leerkilometer")
plotWithRibbon("Umwegfaktor", "fixed")

###betrieb
plotWithRibbon("Summe Fahrzeugkilometer [km]", "fixed")
#save("ribbon-fahrzeugkilometer")
plotWithRibbon("Besetzungsgrad [pax-km/v-km]", "fixed")
#save("ribbon-besetzungsgrad")
plotWithRibbon("Passagiere pro Fahrzeugkilometer", "fixed")
#save("ribbon-paxPerKM")
plotWithRibbon("Passagiere pro Fahrzeugstunde")
plotWithRibbon("Passagiere pro Fahrzeugstunde", "fixed")
#save("ribbon-paxPerVehHour")


plotWithRibbon("Gesamt Passagierkilometer [km]")
plotWithRibbon("Gesamt Passagierkilometer [km]", "fixed")

