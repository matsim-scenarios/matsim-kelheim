library(matsim)
library(dplyr)
library(stringr)
library(fs) # Für die Verzeichnisoperationen

cluster_mainDir <- "E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0/AV-speed-mps-3.3/"
output_mainDir <- "D:/Projekte/KelRide/AV-Service-Extension/untersucheNachfrageWartepunkte/12kmh/"

run_name <- "ALLDEPOTklein-AV2"

# Pfad zum Hauptordner für die Seeds
seed_dir <- paste0(cluster_mainDir, run_name, "/")

# Suche nach allen Ordnern, die mit "seed-" beginnen
seed_folders <- list.files(seed_dir, pattern = "^seed-", full.names = TRUE)

# Lese und filtere Daten für alle gefundenen Ordner
allSeeds <- seed_folders %>% 
  purrr::map_df(~ read_output_trips(.x) %>% filter(main_mode == "av"))

# Pfad zum Output-Ordner erstellen, falls nicht vorhanden
output_dir <- paste0(output_mainDir, run_name, "/")
if (!dir_exists(output_dir)) {
  dir_create(output_dir)
}

# Konvertiere wait_time von HH:MM:SS in Sekunden und füge die Spalte wait_time_s hinzu
allSeeds <- allSeeds %>% 
  mutate(wait_time_s = as.numeric(lubridate::period_to_seconds(lubridate::hms(wait_time))))

# Schreibe die Ergebnisse in eine CSV-Datei
write.csv(allSeeds, quote = FALSE, file = paste0(output_dir, run_name, "-trips-allSeeds.csv"))
