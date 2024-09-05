library(ggplot2) 
library(dplyr)
library(tidyr)

mainDir <- "E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0/"

transposed_result <- read.csv(paste(mainDir, "results-deutsch.csv", sep=""), check.names = FALSE, sep =";")
#names(transposed_result) <- make.names(names(transposed_result), unique = TRUE, allow_ = FALSE)

# Betriebszeiten umschreiben
transposed_result$Betriebszeiten <- factor(ifelse(transposed_result$Betriebszeiten == TRUE, "0h - 24h", "9h - 16h"),
                                           levels = c("9h - 16h", "0h - 24h"))


###
#in Realität haben wir eine avg gruppengr0eße von 1.7 gemessen, diese aber nicht simuliert.
# wir rechnen die jetzt im nachhinein wieder drauf.
## --> machen wir jetzt im skript, dass die daten rausschreibt (readValuesFromRunSammaries.R)

#transposed_result <- transposed_result %>% 
#  mutate(`Anzahl Passagiere` = `Bediente Anfragen` * 1.7,
#         `Gesamt Passagierkilometer [km]` = `Gesamt Passagierkilometer [km]` * 1.7) %>% 
#  mutate(`Passagiere pro Fahrzeug` = `Anzahl Passagiere` / Fahrzeuge,
#         `Passagiere pro Fahrzeugkilometer` = `Anzahl Passagiere` / `Summe Fahrzeugkilometer [km]`,
#         `Passagiere pro Fahrzeugstunde` = `Anzahl Passagiere` / `Summe Fzg.-Betriebsstunden`,
#         `Besetzungsgrad [pax-km/v-km]` = `Gesamt Passagierkilometer [km]` / `Summe Fahrzeugkilometer [km]`)#


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
    labs(title = paste(parameterStr, "nach Geschwindigkeit, Flottengröße,\nBediengebiet und Betriebszeiten"),
         x = "Flottengröße",
         y = parameterStr,
         color = "Bediengebiet",
         linetype = "Betriebszeiten"
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

save <- function(fileName){
  ggsave(filename = paste(mainDir, "plots/", fileName, ".png", sep = ""),
         dpi = 600, width = 32, height = 18, units = "cm")
}

unique(results$parameter)

###nachfrage
plotByConfiguration("Bediente Anfragen")
save("bedienteAnfragen")
plotByConfiguration("Anzahl Passagiere", "fixed")
save("passagiere")
plotByConfiguration("Mittl. Wartezeit [s]", "fixed")
save("wartezeit")
plotByConfiguration("Umwegfaktor", "fixed")
plotByConfiguration("Mittl. Reiseweite [km]", "fixed")
save("reiseweite")
plotByConfiguration("Anteil Leerkilometer", "fixed")
save("leerkilometer")


###betrieb
plotByConfiguration("Summe Fahrzeugkilometer [km]")
save("fahrzeugkilometer")
plotByConfiguration("Besetzungsgrad [pax-km/v-km]")
save("besetzungsgrad")
plotByConfiguration("Passagiere pro Fahrzeugkilometer", "fixed")
save("paxPerKM")
plotByConfiguration("Passagiere pro Fahrzeugstunde")
plotByConfiguration("Passagiere pro Fahrzeugstunde", "fixed")
x^xsave("paxPerVehHour")


plotByConfiguration("Gesamt Passagierkilometer [km]")
plotByConfiguration("Gesamt Passagierkilometer [km]", "fixed")

