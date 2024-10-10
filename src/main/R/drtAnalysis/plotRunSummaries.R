library(ggplot2) 
library(dplyr)
library(tidyr)

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

#Bediengebiete umkodieren
transposed_result <- transposed_result %>%
  mutate(Bediengebiet = recode(Bediengebiet, 
                               "ALLCITY" = "konv. KEXI\n schlechte Wartepunkte", 
                               "SAR2023" = "AV 2024", 
                               "WIEKEXI" = "konv. KEXI"))

# Betriebszeiten umschreiben und faktorisieren
transposed_result$Betriebszeiten <- factor(ifelse(transposed_result$Betriebszeiten == TRUE, "0h - 24h", "9h - 16h"),
                                           levels = c("9h - 16h", "0h - 24h"))

results <- transposed_result %>%
  gather(key = "parameter", value = "mean", -Geschwindigkeit, -Bediengebiet, -Flottengroeße, -Intermodal, -Betriebszeiten)


plotByConfiguration <- function(parameterStr, scales = "free"){
  
  # Filtern der Daten für die gewünschten Parameter
  plot_data <- results %>%
    filter(parameter == parameterStr,
           Intermodal == TRUE | Bediengebiet == "AV 2024")
  
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
  ggplot(plot_data, aes(x = Flottengroeße, y = mean, color = Bediengebiet, linetype = as.factor(Betriebszeiten), group = interaction(Bediengebiet, Betriebszeiten))) +
    geom_line(size = 1.2) +
    geom_point(size = 3,
               #aes(shape = as.factor(intermodal))
    ) +
    facet_wrap(~ Geschwindigkeit,
               labeller = labeller(Geschwindigkeit = label_function)
               ,scales = scales
    ) +
    labs(title = plot_title,
         x = "Flottengröße",
         y = parameterStr,
         color = "Bediengebiet",
         linetype = "Betriebszeiten"
         #,shape = "Intermodal"
    ) +
    #geom_text(aes(label = Flottengroeße), vjust = -1, hjust = 0.5, size = 3, color = "black") +
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

save <- function(fileName){
  if (stats_for_AV){
    output_file <- paste(mainDir, "plots/AV/", fileName, "-AV.png", sep = "")
  } else {
    output_file <- paste(mainDir, "plots/konvKEXI/", fileName, "-konvKEXI.png", sep = "")
  }
  ggsave(filename = output_file,
         dpi = 600, width = 32, height = 18, units = "cm")
}

unique(results$parameter)

results <- results %>%
  filter(Bediengebiet != "konv. KEXI\n schlechte Wartepunkte",
         Flottengroeße < 150)


###nachfrage
plotByConfiguration("Bediente Anfragen")
save("bedienteAnfragen")
plotByConfiguration("Anzahl Passagiere", "fixed")
save("passagiere")
plotByConfiguration("Mittl. Wartezeit [s]", "fixed")
save("wartezeit")
plotByConfiguration("Mittl. Gesamtreisezeit [s]", "fixed")


plotByConfiguration("Umwegfaktor", "fixed")
plotByConfiguration("Mittl. Reiseweite [km]", "fixed")
save("reiseweite")
plotByConfiguration("Anteil Leerkilometer", "fixed")
save("leerkilometer")


###betrieb
plotByConfiguration("Summe Fahrzeugkilometer [km]", "fixed")
save("fahrzeugkilometer")
plotByConfiguration("Besetzungsgrad [pax-km/v-km]", "fixed")
save("besetzungsgrad")
plotByConfiguration("Passagiere pro Fahrzeugkilometer", "fixed")
save("paxPerKM")
plotByConfiguration("Passagiere pro Fahrzeugstunde")
plotByConfiguration("Passagiere pro Fahrzeugstunde", "fixed")
save("paxPerVehHour")


plotByConfiguration("Gesamt Passagierkilometer [km]")
plotByConfiguration("Gesamt Passagierkilometer [km]", "fixed")

