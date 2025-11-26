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

#transposed_result_clean <- transposed_result_clean %>%
#  mutate(area = recode(area,
#                       "SAR2023" = "Area 2024",
#                       "WIEKEXImSaal" = "Area All-City"))

transposed_result_clean <- transposed_result_clean %>%
  mutate(area = case_when(
    area == "SAR2023" ~ "Area 2024",
    area == "WIEKEXImSaal" ~ "Area All-City",
    TRUE ~ area
  ))

################################################################################################


##########
#### guppiert pro area und pro service times


compute_elasticities_per_area_times <- function(
    data,
    #time_vars = c("Avg. wait time", "Avg. in-vehicle time", "Avg. total travel time"),
    time_vars = c("Avg. wait time", "95th percentile wait time", "Avg. total travel time"),
    control_fleet = TRUE
) {
  data %>%
    group_by(area, serviceTimes) %>%
    group_split() %>%
    map_df(function(df_group) {
      map_df(time_vars, function(var) {
        # Modellformel dynamisch erstellen
        fmla_string <- if (control_fleet) {
          paste0("log(`Passengers (Pax)`) ~ log(`", var, "`) + log(fleetSize)")
        } else {
          paste0("log(`Passengers (Pax)`) ~ log(`", var, "`)")
        }
        fmla <- as.formula(fmla_string)
        
        cat("Modell für Gruppe:", unique(df_group$area), unique(df_group$serviceTimes), "\n")
        cat("  Variable:", var, "\n")
        cat("  Formel: ", fmla_string, "\n")
        
        # Werte prüfen
        pax <- df_group$`Passengers (Pax)`
        if (!var %in% colnames(df_group)) {
          cat("  ⚠️  Variable", var, "nicht im Datensatz – überspringe...\n\n")
          return(NULL)
        }
        var_values <- df_group[[var]]
        if (any(is.na(var_values)) | any(is.na(pax))) {
          cat("  🟡 NA-Werte gefunden – überspringe...\n\n")
          return(NULL)
        }
        if (min(var_values, pax, df_group$fleetSize, na.rm = TRUE) <= 0) {
          cat("  🔴 Nicht-positive Werte – überspringe...\n\n")
          return(NULL)
        }
        
        # Modell schätzen
        mod <- lm(fmla, data = df_group)
        summary_mod <- summary(mod)
        coefs <- tidy(mod)
        
        # Delta-Elastizität berechnen
        delta_log_pax <- log(pax[which.max(var_values)]) - log(pax[which.min(var_values)])
        delta_log_var <- log(max(var_values)) - log(min(var_values))
        approx_elast <- delta_log_pax / delta_log_var
        
        # Zeile für Zeitvariable
        time_row <- coefs %>%
          filter(str_detect(term, fixed(var))) %>%
          mutate(
            metric = var,
            area = unique(df_group$area),
            serviceTimes = unique(df_group$serviceTimes),
            r_squared = summary_mod$r.squared,
            adj_r_squared = summary_mod$adj.r.squared,
            N = nrow(df_group),
            signif = case_when(
              p.value < 0.001 ~ "***",
              p.value < 0.01 ~ "**",
              p.value < 0.05 ~ "*",
              p.value < 0.1 ~ ".",
              TRUE ~ ""
            ),
            label = paste0(round(estimate, 2), signif),
            y_label = paste0(var, " (", area, ", ", serviceTimes, ")"),
            delta_elasticity = approx_elast
          )
        
        # Zeile für fleetSize (optional)
        if (control_fleet) {
          fleet_row <- coefs %>%
            filter(term == "log(fleetSize)") %>%
            transmute(
              fleet_effect = estimate,
              fleet_se = std.error,
              fleet_signif = case_when(
                p.value < 0.001 ~ "***",
                p.value < 0.01 ~ "**",
                p.value < 0.05 ~ "*",
                p.value < 0.1 ~ ".",
                TRUE ~ ""
              )
            )
          # Kombinieren
          bind_cols(time_row, fleet_row)
        } else {
          time_row
        }
      })
    })
}


wait_fleet_elasticity <- function(
    data,
    #time_vars = c("Avg. wait time", "Avg. in-vehicle time", "Avg. total travel time"),
    time_vars = c("Avg. wait time", "95th percentile wait time", "Avg. total travel time")
) {
  data %>%
    group_by(area, serviceTimes) %>%
    group_split() %>%
    map_df(function(df_group) {
      map_df(time_vars, function(var) {
        # Modellformel dynamisch erstellen
        fmla_string <- paste0("log(`", var, "`) ~  log(fleetSize)")
        fmla <- as.formula(fmla_string)
        
        cat("Modell für Gruppe:", unique(df_group$area), unique(df_group$serviceTimes), "\n")
        cat("  Variable:", var, "\n")
        cat("  Formel: ", fmla_string, "\n")
        
        # Werte prüfen
        pax <- df_group$`Passengers (Pax)`
        if (!var %in% colnames(df_group)) {
          cat("  ⚠️  Variable", var, "nicht im Datensatz – überspringe...\n\n")
          return(NULL)
        }
        var_values <- df_group[[var]]
        if (any(is.na(var_values)) | any(is.na(pax))) {
          cat("  🟡 NA-Werte gefunden – überspringe...\n\n")
          return(NULL)
        }
        if (min(var_values, pax, df_group$fleetSize, na.rm = TRUE) <= 0) {
          cat("  🔴 Nicht-positive Werte – überspringe...\n\n")
          return(NULL)
        }
        
        # Modell schätzen
        mod <- lm(fmla, data = df_group)
        summary_mod <- summary(mod)
        coefs <- tidy(mod)
        
        # Delta-Elastizität berechnen
        delta_log_pax <- log(pax[which.max(var_values)]) - log(pax[which.min(var_values)])
        delta_log_var <- log(max(var_values)) - log(min(var_values))
        approx_elast <- delta_log_pax / delta_log_var
        
        # Zeile für Zeitvariable
        time_row <- coefs %>%
          filter(str_detect(term, fixed(var))) %>%
          mutate(
            metric = var,
            area = unique(df_group$area),
            serviceTimes = unique(df_group$serviceTimes),
            r_squared = summary_mod$r.squared,
            adj_r_squared = summary_mod$adj.r.squared,
            N = nrow(df_group),
            signif = case_when(
              p.value < 0.001 ~ "***",
              p.value < 0.01 ~ "**",
              p.value < 0.05 ~ "*",
              p.value < 0.1 ~ ".",
              TRUE ~ ""
            ),
            label = paste0(round(estimate, 2), signif),
            y_label = paste0(var, " (", area, ", ", serviceTimes, ")"),
            delta_elasticity = approx_elast
          )
      
          time_row
        
      })
    })
}


# Mit Kontrolle von fleetSize
results_with_control <- compute_elasticities_per_area_times(transposed_result_clean, control_fleet = TRUE)

# Ohne Kontrolle von fleetSize
results_without_control <- compute_elasticities_per_area_times(transposed_result_clean, control_fleet = FALSE)
wait_fleet <- wait_fleet_elasticity(transposed_result_clean)

test <- transposed_result_clean %>% 
  filter(area == "Area All-City", allDay == TRUE)
fmla_string <- paste0("log(`Avg. wait time`) ~  log(fleetSize)")
fmla <- as.formula(fmla_string)
# Modell schätzen
mod <- lm(fmla, data = test)
summary(mod)

fmla_b_string <- paste0("log(`Passengers (Pax)`) ~  log(`Avg. wait time`)")
fmla_b <- as.formula(fmla_b_string)

mod_b <- lm(fmla_b, data = test)
summary(mod_b)

a <- coef(mod)[["log(fleetSize)"]]
b <- coef(mod_b)[["log(`Avg. wait time`)"]]

total_effect <- a * b
total_effect

####################################
####################################
## nun nehmen wir die wahrgenommene geschwindigkeit der kunden.

transposed_result_clean <- transposed_result_clean %>%
  mutate(
    'Avg. total speed' = `Avg. ride distance [km]` / (`Avg. total travel time` / 3600),       # Zeit in Stunden
    avg_speed_inveh = `Avg. ride distance [km]` / (`Avg. in-vehicle time` / 3600)
  )

time_vars = c("Avg. wait time", "Avg. total speed", "avg_speed_inveh")

results_with_control <- compute_elasticities_per_area_times(transposed_result_clean,
                                                            time_vars = time_vars,
                                                            control_fleet =  TRUE)
results_without_control <- compute_elasticities_per_area_times(transposed_result_clean,
                                                               time_vars = time_vars,
                                                               control_fleet = FALSE)
results_comparison <- bind_rows(
  results_without_control %>% mutate(control = "without control"),
  results_with_control %>% mutate(control = "control log(fleetSize)")
) %>%
  #filter(metric == "Avg. wait time" | metric == "avg_speed_total") %>%  # Nur Wartezeit
  mutate(
    label = paste0(round(estimate, 2), signif),
    ymin = estimate - std.error,
    ymax = estimate + std.error,
    y_group = fct_rev(y_label)  # für sortierte Achse
  )


#fleet_effect_labels <- results_comparison %>%
#  filter(control == "control log(fleetSize)") %>%
#  group_by(area, serviceTimes) %>%
#  summarise(
#    fleet_label = paste0("fleetSize effect: ", round(first(fleet_effect), 2), first(fleet_signif)),
#    .groups = "drop"
#  ) %>%
#  mutate(
#    x = 0,       # Mittelwert auf X-Achse
#    y = 2.5      # Oberhalb der höchsten Metrikanzahl – ggf. anpassen
#  )

fleet_effects <- results_comparison %>%
  filter(control == "control log(fleetSize)") %>%
  transmute(
    term = "fleetSize",
    estimate = fleet_effect,
    std.error = fleet_se,
    p.value = NA,  # Optional
    metric = "Fleet size",
    area,
    serviceTimes,
    r_squared,
    adj_r_squared,
    N,
    signif = fleet_signif,
    label = paste0(round(fleet_effect, 2), fleet_signif),
    y_label = paste0("Fleet size (", area, ", ", serviceTimes, ")"),
    delta_elasticity = NA_real_,
    control = control,
    ymin = fleet_effect * fleet_se,
    ymax = fleet_effect * fleet_se,
    y_group = paste0("Fleet size (", area, ", ", serviceTimes, ")")
  )


results_plotdata <- bind_rows(results_comparison, fleet_effects)

ggplot(results_plotdata, aes(x = estimate, y = metric, fill = control)) +
  geom_col(width = 0.6, position = position_dodge(width = 0.7)) +
  geom_errorbarh(aes(xmin = ymin, xmax = ymax),
                 height = 0.2,
                 position = position_dodge(width = 0.7)) +
  geom_text(aes(label = label),
            position = position_dodge(width = 0.7),
            hjust = ifelse(results_plotdata$estimate > 0, -0.1, 1.1),
            vjust = -0.3,
            size = 4.5,
            angle = 0) +
  geom_vline(xintercept = 0, linetype = "dashed") +
  facet_grid(serviceTimes ~ area) +
  labs(
    title = "Comparison of Elasticity Estimates",
    subtitle = "With and without controlling for fleet size\nError bars = standard errors, stars indicate significance levels",
    x = "log-log elasticity estimate",
    y = NULL,
    fill = "Model specification"
  ) +
  theme_minimal(base_size = 15) +
  theme(
    legend.position = "bottom",
    #text = element_text(face = "bold"),
    axis.text = element_text(size = 14),
    legend.text = element_text(size = 13),
    plot.title = element_text(size = 18, face = "bold"),
    plot.subtitle = element_text(size = 14, face = "bold"),
    strip.text = element_text(size = 18, face = "bold")
  ) 

#+
#  geom_text(
#    data = fleet_effect_labels,
#    aes(x = x, y = y, label = fleet_label),
#    inherit.aes = FALSE,
#    hjust = 0,
#    vjust = 1,
#    size = 4,
#    fontface = "italic"
#  )






# Nur Modelle mit Kontrolle
fleet_effects <- results_comparison %>%
  filter(!is.na(fleet_effect)) %>%
  transmute(
    estimate = fleet_effect,
    std.error = fleet_se,
    metric = "fleetSize",
    control = control,
    area = area,
    serviceTimes = serviceTimes,
    r_squared = r_squared,
    adj_r_squared = adj_r_squared,
    N = N,
    signif = fleet_signif,
    label = paste0(round(fleet_effect, 2), fleet_signif),
    y_label = paste0("fleetSize (", area, ", ", serviceTimes, ")"),
    ymin = fleet_effect - fleet_se,
    ymax = fleet_effect + fleet_se,
    y_group = y_label
  )


# Filter z. B. nach avg_speed_total
speed_results <- results_with_control %>%
  filter(metric == "Avg. total speed")

# Kombiniere beide
plot_data <- bind_rows(speed_results, fleet_effects)

library(ggplot2)
library(forcats)

ggplot(plot_data, aes(x = estimate, y = fct_rev(y_group), fill = metric)) +
  geom_col(width = 0.6, alpha = 0.75, position = position_dodge2()) +
  geom_errorbarh(aes(xmin = ymin, xmax = ymax), height = 0.2) +
  geom_text(aes(label = label),
            hjust = ifelse(plot_data$estimate > 0, -0.1, 1.1),
            size = 4) +
  geom_vline(xintercept = 0, linetype = "dashed") +
  labs(
    title = "Elastizitäten und Effekt der Flottengröße",
    subtitle = "Fehlerbalken = Standardfehler, Sterne = Signifikanzniveau",
    x = "log-log Effekt / Elastizität",
    y = NULL,
    fill = "Variable"
  ) +
  theme_minimal(base_size = 14) +
  theme(
    legend.position = "bottom",
    axis.text.y = element_text(face = "bold"),
    axis.text.x = element_text(face = "bold"),
    plot.title = element_text(face = "bold"),
    plot.subtitle = element_text(face = "italic")
  )




######################
############################
#################################
# alter code
#####################


########################################################
# Gruppierung definieren
transposed_result_clean <- transposed_result_clean %>%
  mutate(
    group_id = interaction(area, serviceTimes, speed, drop = TRUE),
    log_wait = log(`Avg. wait time`),
    log_pax = log(`Passengers (Pax)`)
  )

## Regression innerhalb jeder Gruppe: log(Passengers) ~ log(Avg. wait time)
#elasticities <- transposed_result_clean %>%
#  group_by(group_id, area, serviceTimes, speed) %>%
#  do(tidy(lm(log_pax ~ log_wait, data = .))) %>%
#  filter(term == "log_wait") %>%
#  rename(elasticity = estimate) %>%
#  arrange(desc(abs(elasticity)))  # sortieren nach starker Wirkung

#View(elasticities)

## Balkenplot: Elastizität der Nachfrage vs. mittlere Wartezeit
#ggplot(elasticities, aes(x = reorder(group_id, elasticity), y = elasticity, fill = area)) +
#  geom_col() +
#  coord_flip() +
#  facet_grid(area ~ serviceTimes) +
#  labs(
#    title = "Elastizität der Nachfrage (Passengers) bezüglich der Wartezeit",
#    x = "Gruppe (area × speed × serviceTimes)",
#    y = "Elastizität (log-log-Koeffizient)",
#    fill = "Area"
#  ) +
#  theme_minimal(base_size = 12)

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

#Vorsicht: mögliche Mediatorrolle von speed
#Höhere Geschwindigkeit verkürzt die Fahrzeit → beide Prädiktoren hängen logisch zusammen.

#Prüfe Multikollinearität (z. B. Variance Inflation Factor).
#library(car)
vif(model_control)

## sehr hohe vif werte für area und in-veh time

model_nospeed <- lm(log(`Passengers (Pax)`) ~ log(`Avg. in-vehicle time`) + fleetSize + area + serviceTimes, data = transposed_result_clean)
summary(model_nospeed)
vif(model_nospeed)

# vif(area) immer noch > 5. daher aufspaltung per area

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
time_vars <- c("Avg. wait time", "Avg. in-vehicle time", "Avg. total travel time", "95th percentile wait time")

# Dynamische Modellerstellung
elasticity_results <- map_df(time_vars, function(var) {
  transposed_result_clean %>%
    group_by(area) %>%
    group_split() %>%
    map_df(function(df_area) {
      # dynamische Formel
      fmla <- as.formula(paste0("log(`Passengers (Pax)`) ~ log(`", var, "`) + fleetSize"))
      
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


#########################################
#########################################
############## nur fleet size = 100

results_fleet_100 <- transposed_result_clean %>%
  filter(fleetSize == 100)

model_fleet100_total <- lm(log(`Passengers (Pax)`) ~ log(`Avg. total travel time`), data = results_fleet_100)
# Zusammenfassung
summary(model_fleet100_total)

# tidy-Output für übersichtliche Extraktion
tidy(model_fleet100_total)


### delta elasticity mit min und max pro speed gruppe
delta_elasticity <- results_fleet_100 %>%
  group_by(speed) %>%
  filter(n() >= 2) %>%  # nur Gruppen mit mindestens 2 Punkten
  summarise(
    travel_time_min = min(`Avg. total travel time`),
    travel_time_max = max(`Avg. total travel time`),
    passengers_min = `Passengers (Pax)`[which.min(`Avg. total travel time`)],
    passengers_max = `Passengers (Pax)`[which.max(`Avg. total travel time`)],
    .groups = "drop"
  ) %>%
  mutate(
    elasticity_approx = (log(passengers_max) - log(passengers_min)) /
      (log(travel_time_max) - log(travel_time_min))
  )

print(delta_elasticity)

### delta elasticity mit min und max pro area gruppe
delta_elasticity <- results_fleet_100 %>%
  group_by(area) %>%
  filter(n() >= 2) %>%  # nur Gruppen mit mindestens 2 Punkten
  summarise(
    travel_time_min = min(`Avg. total travel time`),
    travel_time_max = max(`Avg. total travel time`),
    passengers_min = `Passengers (Pax)`[which.min(`Avg. total travel time`)],
    passengers_max = `Passengers (Pax)`[which.max(`Avg. total travel time`)],
    .groups = "drop"
  ) %>%
  mutate(
    elasticity_approx = (log(passengers_max) - log(passengers_min)) /
      (log(travel_time_max) - log(travel_time_min))
  )

print(delta_elasticity)


# Datenbasis
df <- results_fleet_100
#df <- transposed_result_clean
#df <- transposed_result_clean

# Zwei Extrempunkte bestimmen (min/max total travel time)
travel_time_min <- min(df$`Avg. total travel time`)
travel_time_max <- max(df$`Avg. total travel time`)

passengers_min <- df$`Passengers (Pax)`[which.min(df$`Avg. total travel time`)]
passengers_max <- df$`Passengers (Pax)`[which.max(df$`Avg. total travel time`)]

# Approximate Elastizität berechnen
elasticity_approx <- (log(passengers_max) - log(passengers_min)) /
  (log(travel_time_max) - log(travel_time_min))

# Ergebnis anzeigen
cat("Approximierte Elastizität tot. travel time (ungruppiert):", round(elasticity_approx, 3), "\n")

# Funktion zur Berechnung der approximierten Elastizität
compute_delta_elasticity <- function(varname) {
  var <- df[[varname]]
  pax <- df$`Passengers (Pax)`
  
  delta_log_pax <- log(pax[which.max(var)]) - log(pax[which.min(var)])
  delta_log_var <- log(max(var)) - log(min(var))
  
  elasticity <- delta_log_pax / delta_log_var
  
  return(data.frame(
    time_component = varname,
    elasticity = round(elasticity, 3),
    min_var = round(min(var), 2),
    max_var = round(max(var), 2),
    min_pax = round(pax[which.min(var)], 0),
    max_pax = round(pax[which.max(var)], 0)
  ))
}

# Liste der Zeitkomponenten
time_vars <- c("Avg. wait time", "Avg. in-vehicle time", "Avg. total travel time")

# Für alle Zeitkomponenten berechnen
elasticity_table <- do.call(rbind, lapply(time_vars, compute_delta_elasticity))

# Ausgabe anzeigen
print(elasticity_table)


######################
library(dplyr)
library(broom)
library(purrr)


compute_elasticities <- function(data, time_vars = c("Avg. wait time", "Avg. in-vehicle time", "Avg. total travel time")) {
  map_df(time_vars, function(var) {
    # Modellformel: ohne Kontrollvariablen
    fmla <- as.formula(paste0("log(`Passengers (Pax)`) ~ log(`", var, "`) + area "))
    
    # Modell schätzen
    mod <- lm(fmla, data = data)
    summary_mod <- summary(mod)
    
    cat("Verwendetes Regressionsmodell:", paste(deparse(fmla), collapse = " "), "\n")
    
    # Werte für Extrem-Elastizität berechnen
    pax <- data$`Passengers (Pax)`
    var_values <- data[[var]]
    
    # Prüfe ob Werte > 0 (wichtig für log)
    if (min(pax, na.rm = TRUE) > 0 & min(var_values, na.rm = TRUE) > 0) {
      delta_log_pax <- log(pax[which.max(var_values)]) - log(pax[which.min(var_values)])
      delta_log_var <- log(max(var_values)) - log(min(var_values))
      approx_elast <- delta_log_pax / delta_log_var
    } else {
      approx_elast <- NA
    }
    
    
    # Ergebnis zusammenbauen
    tidy(mod) %>%
      filter(term == paste0("log(`", var, "`)")) %>%
      mutate(
        metric = var,
        r_squared = summary_mod$r.squared,
        adj_r_squared = summary_mod$adj.r.squared,
        N = nrow(data),
        signif = case_when(
          p.value < 0.001 ~ "***",
          p.value < 0.01 ~ "**",
          p.value < 0.05 ~ "*",
          p.value < 0.1 ~ ".",
          TRUE ~ ""
        ),
        label = paste0(round(estimate, 2), signif),
        delta_elasticity = approx_elast
      )
  })
}

# Auf vollständigem Datensatz

aa <- compute_elasticities(transposed_result_clean)

# Auf Teilmenge mit fleetSize == 100
bb <- compute_elasticities(results_fleet_100)



####################

compute_elasticities_per_area <- function(data, time_vars = c("Avg. wait time", "Avg. in-vehicle time", "Avg. total travel time")) {
  data %>%
    group_by(area) %>%
    group_split() %>%
    map_df(function(df_area) {
      map_df(time_vars, function(var) {
        # Modellformel: ohne Kontrollvariablen
        fmla <- as.formula(paste0("log(`Passengers (Pax)`) ~ log(`", var, "`) + log(speed)"))
        
        # Modell schätzen
        mod <- lm(fmla, data = df_area)
        summary_mod <- summary(mod)
        
        cat("Verwendetes Regressionsmodell:", paste(deparse(fmla), collapse = " "), "\n")
        
        # Werte für Extrem-Elastizität berechnen
        pax <- df_area$`Passengers (Pax)`
        var_values <- df_area[[var]]
        
        # Prüfe ob Werte > 0 (wichtig für log)
        if (min(pax, na.rm = TRUE) > 0 & min(var_values, na.rm = TRUE) > 0) {
          delta_log_pax <- log(pax[which.max(var_values)]) - log(pax[which.min(var_values)])
          delta_log_var <- log(max(var_values)) - log(min(var_values))
          approx_elast <- delta_log_pax / delta_log_var
        } else {
          approx_elast <- NA
        }
        
        # Ergebnis zusammenbauen
        tidy(mod) %>%
          filter(term == paste0("log(`", var, "`)")) %>%
          mutate(
            metric = var,
            r_squared = summary_mod$r.squared,
            adj_r_squared = summary_mod$adj.r.squared,
            N = nrow(df_area),
            area = unique(df_area$area),
            signif = case_when(
              p.value < 0.001 ~ "***",
              p.value < 0.01 ~ "**",
              p.value < 0.05 ~ "*",
              p.value < 0.1 ~ ".",
              TRUE ~ ""
            ),
            label = paste0(round(estimate, 2), signif),
            delta_elasticity = approx_elast
          )
      })
    })
}


# Ergebnisse anzeigen
areas <- compute_elasticities_per_area(transposed_result_clean)


##############################################################################





########################################
#########################################
######
## lokale elastizität gegenüber base case

# 1. Hole die Base Case Werte
base_case <- transposed_result_clean %>%
  filter(
    fleetSize == 2,
    speed == 3.3,
    area == "Area 2024",
    serviceTimes == "9am - 4pm"
  )

# 2. Prüfe, dass es genau einen Eintrag gibt
stopifnot(nrow(base_case) == 1)

# 3. Extrahiere Basiswerte
pax_base <- base_case$`Passengers (Pax)`
wait_base <- base_case$`Avg. wait time`
speed_total <- base_case$avg_speed_total


# 4. Berechne lokale Elastizität je Zeile
transposed_result_with_local_elast <- transposed_result_clean %>%
#  mutate(
#    local_elast_wait_base = (log(`Passengers (Pax)`) - log(pax_base)) /
#      (log(`Avg. wait time`) - log(wait_base))
#  )
  mutate(
    local_elast_wait_base = if_else(
      `Passengers (Pax)` > 0 & `Avg. wait time` > 0 & wait_base > 0 & pax_base > 0,
      (log(`Passengers (Pax)`) - log(pax_base)) / (log(`Avg. wait time`) - log(wait_base)),
      NA_real_
    )
)

