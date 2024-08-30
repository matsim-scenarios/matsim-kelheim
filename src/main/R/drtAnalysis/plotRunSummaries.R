library(ggplot2) 
library(dplyr)
library(tidyr)

mainDir <- "E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0/"

transposed_result <- read.csv(paste(mainDir, "results-deutsch.csv", sep=""), check.names = FALSE)
#names(transposed_result) <- make.names(names(transposed_result), unique = TRUE, allow_ = FALSE)

# Betriebszeiten umschreiben
transposed_result$Betriebszeiten <- ifelse(transposed_result$Betriebszeiten == TRUE, "0h - 24h", "9h - 16h")


results <- transposed_result %>%
  gather(key = "parameter", value = "mean", -Geschwindigkeit, -Bediengebiet, -Flottengroeße, -Intermodal, -Betriebszeiten)


plotByConfiguration <- function(parameterStr){
  
  # Filtern der Daten für die gewünschten Parameter
  plot_data <- results %>%
    filter(parameter == parameterStr,
           Intermodal == TRUE | Bediengebiet == "AV 2024")
  
  # Funktion zum Anpassen der Facet-Labels
  label_function <- function(value) {
    paste(value, "m/s")
  }
  
  # Erstellen des Facet-Plots
  ggplot(plot_data, aes(x = Flottengroeße, y = mean, color = Bediengebiet, linetype = as.factor(Betriebszeiten), group = interaction(Bediengebiet, Betriebszeiten))) +
    geom_line(size = 1.2) +
    geom_point(size = 3,
               #aes(shape = as.factor(intermodal))
    ) +
    facet_wrap(~ Geschwindigkeit,
               labeller = labeller(Geschwindigkeit = label_function)
               ,scales = "free"
    ) +
    labs(title = paste(parameterStr, "nach Geschwindigkeit, Flottengröße, Bediengebiet und Betriebszeiten"),
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

unique(results$parameter)
plotByConfiguration("Bediente Anfragen")
plotByConfiguration("Mittl. Wartezeit")
plotByConfiguration("Avg. ride distance [km]")
plotByConfiguration("Empty ratio")
plotByConfiguration("Total vehicle mileage [km]")
plotByConfiguration("Avg. fare [MoneyUnit]" )
plotByConfiguration("Pax per veh-km")