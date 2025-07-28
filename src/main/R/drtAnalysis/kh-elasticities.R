library(ggplot2) 
library(dplyr)
library(broom)
library(tidyr)
library(purrr)
library(car)
library(tidyverse)



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

################################################################################################
################################################################################################

if (stats_for_AV){
  # in order to (re-) create this file you need to run readValuesFromRunSummaries.R
  # and for that, you need to have checked out all the analysis (simwrapper) subfolders of the AV configurations -> meaning the average dashboard data over 5 seeds!
  input_file <- paste(mainDir, "results-av.csv", sep="")
} else {
  input_file <- paste(mainDir, "results-konvKEXI.csv", sep="")
}

##read input
transposed_result <- read.csv(input_file, check.names = FALSE, sep =",")

# serviceTimes umschreiben und faktorisieren
transposed_result$serviceTimes <- factor(ifelse(transposed_result$allDay == TRUE, "all-day", "9am - 4pm"),
                                         levels = c("all-day", "9am - 4pm"))

# Filter ggf. unplausible Daten vorher raus
transposed_result_clean <- transposed_result %>%
  filter(fleetSize < 150,
         str_detect(area, "Saal") # das Area-Encoding für die Konfigurations-Serie, die wir anschauen wollen ist KEXImSaal (grosses Gebiet) und SAR2023 (kleines Gebiet)
         #area == "ALLCITY" 
         | area == "SAR2023"
  ) %>% 
  # wir haben festgestellt, dass 2 Fzge im großen Bediengebiet zu unplausiblen Ergebnissen führen, weil zu viel rejected wird während innovation
  # deswegen sortieren wir diese Runs wieder aus
  filter( ! (fleetSize == 2 & str_detect(area, "Saal")) ) %>% 
  filter(`Avg. wait time` > 0, `Passengers (Pax)` > 0)

# Gruppierung definieren
transposed_result_clean <- transposed_result_clean %>%
  mutate(
    group_id = interaction(area, serviceTimes, speed, drop = TRUE),
    log_wait = log(`Avg. wait time`),
    log_pax = log(`Passengers (Pax)`)
  )

transposed_result_clean <- transposed_result_clean %>%
  mutate(area = recode(area,
                       "SAR2023" = "Area 2024",
                       "WIEKEXImSaal" = "Area All-City"))

# Regression innerhalb jeder Gruppe: log(Passengers) ~ log(Avg. wait time)
elasticities <- transposed_result_clean %>%
  group_by(group_id, area, serviceTimes, speed) %>%
  do(tidy(lm(log_pax ~ log_wait, data = .))) %>%
  filter(term == "log_wait") %>%
  rename(elasticity = estimate) %>%
  arrange(desc(abs(elasticity)))  # sortieren nach starker Wirkung

View(elasticities)

library(ggplot2)

# Balkenplot: Elastizität der Nachfrage vs. mittlere Wartezeit
ggplot(elasticities, aes(x = reorder(group_id, elasticity), y = elasticity, fill = area)) +
  geom_col() +
  coord_flip() +
  facet_grid(area ~ serviceTimes) +
  labs(
    title = "Elastizität der Nachfrage (Passengers) bezüglich der Wartezeit",
    x = "Gruppe (area × speed × serviceTimes)",
    y = "Elastizität (log-log-Koeffizient)",
    fill = "Area"
  ) +
  theme_minimal(base_size = 12)




##############################################


# Modell mit fleetSize als Kontrollvariable
model <- lm(log(`Passengers (Pax)`) ~ log(`Avg. wait time`) + fleetSize, data = transposed_result_clean)
summary(model)  # zeigt die Koeffizienten und Signifikanz

# Für den Plot berechnen wir die vorhergesagten Werte
transposed_result_clean$predicted_log_pax <- predict(model)

# Plot: Log-log-Beziehung mit Regressionslinie
ggplot(transposed_result_clean, aes(x = log(`Avg. wait time`), y = log(`Passengers (Pax)`))) +
  geom_point(aes(color = fleetSize), alpha = 0.7) +
  geom_line(aes(y = predicted_log_pax), color = "black", size = 1) +
  labs(
    title = "Zusammenhang zwischen Wartezeit und Nachfrage",
    subtitle = "Log-log-Skala, kontrolliert für fleetSize",
    x = "log(Avg. wait time)",
    y = "log(Passengers)",
    color = "Fleet Size"
  ) +
  theme_minimal(base_size = 13)

# Ohne Kontrolle
model_simple <- lm(log(`Passengers (Pax)`) ~ log(`Avg. wait time`), data = transposed_result_clean)

# Mit Kontrolle von speed
model_control <- lm(log(`Passengers (Pax)`) ~ log(`Avg. wait time`) + speed, data = transposed_result_clean)

# Vergleich
summary(model_simple)
summary(model_control)

# Kontrolliere die anderen 3 Variablen fleetSize + area + serviceTimes
model_control <- lm(log(`Passengers (Pax)`) ~ log(`Avg. wait time`) + fleetSize + area + serviceTimes, data = transposed_result_clean)
summary(model_control)

# Kontrolliere alle 4 variablen
model_control <- lm(log(`Passengers (Pax)`) ~ log(`Avg. wait time`) + fleetSize + area + serviceTimes + speed, data = transposed_result_clean)
summary(model_control)
vif(model_control)




# Mit fleetSize kontrolliert
elasticities_with_fleet <- transposed_result_clean %>%
  group_by(area, serviceTimes, speed) %>%
  do(tidy(lm(log(`Passengers (Pax)`) ~ log(`Avg. wait time`) + fleetSize, data = .))) %>%
  filter(term == "log(`Avg. wait time`)") %>%
  select(area, serviceTimes, speed, elasticity_with_fleetSize = estimate)

# Ohne fleetSize
elasticities_without_fleet <- transposed_result_clean %>%
  group_by(area, serviceTimes, speed) %>%
  do(tidy(lm(log(`Passengers (Pax)`) ~ log(`Avg. wait time`), data = .))) %>%
  filter(term == "log(`Avg. wait time`)") %>%
  select(area, serviceTimes, speed, elasticity_without_fleetSize = estimate)

# Zusammenführen beider Tabellen
elasticity_comparison <- left_join(elasticities_with_fleet, elasticities_without_fleet,
                                   by = c("area", "serviceTimes", "speed"))
# Ergebnis anzeigen
print(elasticity_comparison)

wait_elasticities_no_fleet <- transposed_result_clean %>%
  group_by(area, serviceTimes, speed) %>%
  do(tidy(lm(log(`Passengers (Pax)`) ~ log(`Avg. wait time`), data = .))) %>%
  filter(term == "log(`Avg. wait time`)") %>%
  select(area, serviceTimes, speed,
         estimate, std.error, statistic, p.value) %>%
  rename(
    elasticity = estimate,
    t_value = statistic
  ) %>%
  mutate(
    significance = case_when(
      p.value < 0.001 ~ "***",
      p.value < 0.01 ~ "**",
      p.value < 0.05 ~ "*",
      p.value < 0.1 ~ ".",
      TRUE ~ ""
    )
  )

print(wait_elasticities_no_fleet)




#######################################################
############### In-vehicle time

# Ohne Kontrolle
model_simple <- lm(log(`Passengers (Pax)`) ~ log(`Avg. in-vehicle time`), data = transposed_result_clean)

# Mit Kontrolle von speed
model_control <- lm(log(`Passengers (Pax)`) ~ log(`Avg. in-vehicle time`) + speed, data = transposed_result_clean)

# Vergleich
summary(model_simple)
summary(model_control)

# Kontrolliere die anderen 3 Variablen fleetSize + area + serviceTimes
model_control <- lm(log(`Passengers (Pax)`) ~ log(`Avg. in-vehicle time`) + fleetSize + area + serviceTimes, data = transposed_result_clean)
summary(model_control)

# Kontrolliere alle 4 variablen
model_control <- lm(log(`Passengers (Pax)`) ~ log(`Avg. in-vehicle time`) + fleetSize + area + serviceTimes + speed, data = transposed_result_clean)
summary(model_control)

#Vorsicht: mögliche Mediator­rolle von speed
#Höhere Geschwindigkeit verkürzt die Fahrzeit → beide Prädiktoren hängen logisch zusammen.

#Prüfe Multikollinearität (z. B. Variance Inflation Factor).
#library(car)
vif(model_control)

model_nospeed <- lm(log(`Passengers (Pax)`) ~ log(`Avg. in-vehicle time`) + fleetSize + area + serviceTimes, data = transposed_result_clean)
summary(model_nospeed)
vif(model_nospeed)

#library(dplyr)
#library(broom)
#library(purrr)

# Modelle je Gebiet
model_by_area <- transposed_result_clean %>%
  group_by(area) %>%
  group_split() %>%
  map_df(function(df_area) {
    mod <- lm(log(`Passengers (Pax)`) ~ log(`Avg. in-vehicle time`) + fleetSize + serviceTimes, data = df_area)
    
    # Modellzusammenfassung
    summary_mod <- summary(mod)
    
    # VIFs
    vif_values <- vif(mod)
    
    # tidy() für Koeffizienten
    coeff <- tidy(mod) %>%
      filter(term == "log(`Avg. in-vehicle time`)") %>%
      mutate(
        r_squared = summary_mod$r.squared,
        adj_r_squared = summary_mod$adj.r.squared,
        vif_inveh = vif_values["log(`Avg. in-vehicle time`)"],
        vif_fleetsize = vif_values["fleetSize"],
        vif_serviceTimes = vif_values["serviceTimes9am - 4pm"],
        area = unique(df_area$area),
        N = nrow(df_area)
      )
    
    return(coeff)
  })

# Ausgabe anzeigen
print(model_by_area)


# Mit Kontrolle für fleetSize
inveh_elasticities_with_fleet <- transposed_result_clean %>%
  group_by(area, serviceTimes, speed) %>%
  do(tidy(lm(log(`Passengers (Pax)`) ~ log(`Avg. in-vehicle time`) + fleetSize, data = .))) %>%
  filter(term == "log(`Avg. in-vehicle time`)") %>%
  select(area, serviceTimes, speed, elasticity_with_fleetSize = estimate)

# Ohne Kontrolle
inveh_elasticities_without_fleet <- transposed_result_clean %>%
  group_by(area, serviceTimes, speed) %>%
  do(tidy(lm(log(`Passengers (Pax)`) ~ log(`Avg. in-vehicle time`), data = .))) %>%
  filter(term == "log(`Avg. in-vehicle time`)") %>%
  select(area, serviceTimes, speed, elasticity_without_fleetSize = estimate)

# Zusammenführen
inveh_elasticity_comparison <- left_join(inveh_elasticities_with_fleet,
                                         inveh_elasticities_without_fleet,
                                         by = c("area", "serviceTimes", "speed"))

# Ergebnis anzeigen
print(inveh_elasticity_comparison)


library(dplyr)
library(broom)

inveh_elasticities_no_fleet <- transposed_result_clean %>%
  group_by(area, serviceTimes, speed) %>%
  do(tidy(lm(log(`Passengers (Pax)`) ~ log(`Avg. in-vehicle time`), data = .))) %>%
  filter(term == "log(`Avg. in-vehicle time`)") %>%
  select(area, serviceTimes, speed,
         estimate, std.error, statistic, p.value) %>%
  rename(
    elasticity = estimate,
    t_value = statistic
  ) %>%
  mutate(
    significance = case_when(
      p.value < 0.001 ~ "***",
      p.value < 0.01 ~ "**",
      p.value < 0.05 ~ "*",
      p.value < 0.1 ~ ".",
      TRUE ~ ""
    )
  )

print(inveh_elasticities_no_fleet)


#######################################################
############### total travel time

# Ohne Kontrolle
model_simple <- lm(log(`Passengers (Pax)`) ~ log(`Avg. total travel time`), data = transposed_result_clean)

# Mit Kontrolle von speed
model_control <- lm(log(`Passengers (Pax)`) ~ log(`Avg. total travel time`) + speed, data = transposed_result_clean)

# Vergleich
summary(model_simple)
summary(model_control)

# Kontrolliere die anderen 3 Variablen fleetSize + area + serviceTimes
model_control <- lm(log(`Passengers (Pax)`) ~ log(`Avg. total travel time`) + fleetSize + area + serviceTimes, data = transposed_result_clean)
summary(model_control)
vif(model_control)

# Kontrolliere alle 4 variablen
model_control <- lm(log(`Passengers (Pax)`) ~ log(`Avg. total travel time`) + fleetSize + area + serviceTimes + speed, data = transposed_result_clean)
summary(model_control)
vif(model_control)

model_total_by_area <- transposed_result_clean %>%
  group_by(area) %>%
  group_split() %>%
  map_df(function(df_area) {
    mod <- lm(log(`Passengers (Pax)`) ~ log(`Avg. total travel time`) + fleetSize + serviceTimes, data = df_area)
    
    # Zusammenfassung & VIFs
    summary_mod <- summary(mod)
    vif_values <- vif(mod)
    
    # Koef. extrahieren
    tidy(mod) %>%
      filter(term == "log(`Avg. total travel time`)") %>%
      mutate(
        r_squared = summary_mod$r.squared,
        adj_r_squared = summary_mod$adj.r.squared,
        vif_total = vif_values["log(`Avg. total travel time`)"],
        vif_fleet = vif_values["fleetSize"],
        vif_service = vif_values["serviceTimes9am - 4pm"],
        area = unique(df_area$area),
        N = nrow(df_area)
      )
  })

print(model_total_by_area)


################################################################
##### Vergleichsplot


# Zeitmetriken, die wir vergleichen wollen
time_vars <- c("Avg. wait time", "Avg. in-vehicle time", "Avg. total travel time")

# Dynamische Modellerstellung
elasticity_results <- map_df(time_vars, function(var) {
  transposed_result_clean %>%
    group_by(area) %>%
    group_split() %>%
    map_df(function(df_area) {
      # dynamische Formel
      fmla <- as.formula(paste0("log(`Passengers (Pax)`) ~ log(`", var, "`) + fleetSize + serviceTimes"))
      
      # Modell schätzen
      mod <- lm(fmla, data = df_area)
      summary_mod <- summary(mod)
      vif_vals <- vif(mod)
      
      # Koeffizienten extrahieren
      tidy(mod) %>%
        filter(term == paste0("log(`", var, "`)")) %>%
        mutate(
          metric = var,
          area = unique(df_area$area),
          r_squared = summary_mod$r.squared,
          adj_r_squared = summary_mod$adj.r.squared,
          N = nrow(df_area),
          vif_metric = vif_vals[term],
          signif = case_when(
            p.value < 0.001 ~ "***",
            p.value < 0.01 ~ "**",
            p.value < 0.05 ~ "*",
            p.value < 0.1 ~ ".",
            TRUE ~ ""
          ),
          label = paste0(round(estimate, 2), signif),
          y_label = paste0(var, " (", area, ")")
        )
    })
})

# Plot erstellen
ggplot(elasticity_results, aes(x = estimate, y = fct_rev(y_label), fill = area)) +
  geom_col(width = 0.6, alpha = 0.7) +
  geom_errorbarh(aes(xmin = estimate - std.error, xmax = estimate + std.error), height = 0.2) +
  geom_text(aes(label = label), hjust = ifelse(elasticity_results$estimate > 0, -0.1, 1),
            vjust = ifelse(elasticity_results$estimate > 0, -0.5, -0.5),
            size = 8,
            angle = 90) +
  geom_vline(xintercept = 0, linetype = "dashed") +
  labs(
    title = "Nachfrageelastizitäten nach Zeitkomponente und Gebiet",
    subtitle = "Fehlerbalken = Standardfehler, Sterne = Signifikanzniveau",
    x = "log-log Elastizität",
    y = NULL,
    fill = "Gebiet"
  ) +
  theme_minimal(base_size = 16) +     # Basisschriftgröße erhöhen
  theme(
    legend.position = "bottom",
    text = element_text(face = "bold"),         # Alles fett
    axis.text = element_text(size = 18, face = "bold"),
    axis.title = element_text(size = 22, face = "bold"),
    plot.title = element_text(size = 24, face = "bold"),
    plot.subtitle = element_text(size = 22, face = "bold"),
    legend.text = element_text(size = 18, face = "bold"),
    legend.title = element_text(size = 16, face = "bold")
  )

