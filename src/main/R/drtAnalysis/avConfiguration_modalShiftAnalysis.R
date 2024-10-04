library(sf)
library(matsim)
library(tidyverse)
library(plotly)


######################################################################################
####################### functions #######################################

#copied from matsim-r and modified
sankey <- function (trips_table1, trips_table2, show_onlychanges = FALSE, 
          unite_modes = character(0), united_name = "united") 
{
  trips_table1 <- process_rename_mainmodes(trips_table = trips_table1, 
                                           unite_modes = unite_modes, united_name = united_name)
  trips_table2 <- process_rename_mainmodes(trips_table = trips_table2, 
                                           unite_modes = unite_modes, united_name = united_name)
  joined <- as_tibble(inner_join(trips_table1, trips_table2 %>% 
                                   select(trip_id, main_mode), by = "trip_id") %>% dplyr::rename(base_mode = main_mode.x, 
                                                                                                 policy_mode = main_mode.y))
  if (show_onlychanges == TRUE) {
    joined <- joined %>% filter(base_mode != policy_mode)
  }
  joined <- joined %>% group_by(base_mode, policy_mode) %>% 
    count()
  modes = sort(unique(c(joined$base_mode, joined$policy_mode)))
  num_modes <- length(modes)
  joined$base_mode <- as.numeric(factor(joined$base_mode, 
                                        levels = modes,
                                        ordered = TRUE))
  joined$policy_mode <- as.numeric(factor(joined$policy_mode, 
                                          levels = modes,
                                          ordered = TRUE))
  palette <- colorRampPalette(c("blue", "yellow", "red"))(num_modes)
  fig <- plot_ly(type = "sankey",
                 orientation = "h",
                 node = list(label = c(modes,modes),
                             color = c(palette, palette),
                             pad = 15, thickness = 20,
                             line = list(color = "black", width = 0.5)),
                 link = list(source = joined$base_mode - 1,
                             target = joined$policy_mode + num_modes - 1,
                             value = joined$n)) %>% 
  layout(title = list(text = "Basic Sankey Diagram",
                      font = list(size = 24, weight = "bold"),  # Fettschrift und große Schriftgröße
                      x = 0.5,  # Zentriert den Titel horizontal
                      y = 0.95,  # Positioniert den Titel vertikal etwas weiter unten (0.95 = 95% der Höhe)
                      xref = "paper",
                      yref = "container"),
         margin = list(t = 90, r = 50, l = 50),  # Vergrößert den oberen Rand, um Platz für den Titel zu schaffen
         font = list(size = 18, weight = "bold"))
  fig
  return(fig)
}


######################################################################################
####################### INPUT #######################################

drtArea <- st_read("D:/public-svn/matsim/scenarios/countries/de/kelheim/shp/prepare-network/av-and-drt-area.shp")

no_kexi_trips <- read_output_trips("E:/matsim-kelheim/v3.0-release/output-base/25pct")

## Zielwert der KEXI Kalibrierung waren 159 Passagiere, erreichter Mittelwert über 5 Seeds 157.4 Passagiere.
## Seed 4 hat 155 rides und ist damit sehr repräsentativ für den Case "nur konv. KEXI" bzw. am nächsten dran am Durchschnitt aller 5 seeds.
## ein anderer Kandidat waere seed-3 mit 151 rides
nur_konv_trips <- read_output_trips("E:/matsim-kelheim/v3.0-release/output-KEXI/seed-4-kexi") %>% 
  mutate(main_mode = recode(main_mode, 
                            "av"  = "AV KEXI",
                            "drt" = "Konv. KEXI",
                            "pt_w_drt_used" = "KEXI + pt"))

## der case SAR2023 AV2 3.3mps ist die Kalibrierungsgrundlage für den AV im Jahr 2024
## mit dem Zielwert von 2,7 Buchungen pro Tag und 2,6 simulierten Buchungen über 5 seeds.
## im Schnitt haben die 5 seeds konventionelle KEXI Passagiere (JAR-Wechsel) :/ ..
## seed-1 hat 2 AV-Buchungen, 151
## seed-2 hat 3 AV-Buchungen, 144 konv. Pax
## seed-3 hat 3 AV-Buchungen, 154 konv. Pax
## seed-4 hat 3 AV-Buchungen, 145 konv. Pax
## seed-5 hat 2 AV-Buchungen, 170 konv. Pax

## repraesentativ ist also vor allem seed-3
av_base_trips <- read_output_trips("E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0/AV-speed-mps-3.3/SAR2023-AV2/seed-3-SAR2023") %>% 
  mutate(main_mode = recode(main_mode, 
                            "av"  = "AV KEXI",
                            "drt" = "Konv. KEXI",
                            "pt_w_drt_used" = "KEXI + pt"))

## weil es hier um Policy Cases bzgl der !AV!-Auslegung geht und weil der AV-Base-Case oben recht präzise noch die Zahlen des konv. KEXI trifft,
## ist das unser Bezugsfall

########################################################################################
###### PROGNOSEFÄLLE

# SAR-AV2-mps3.3-allDay hat im Schnitt 11.2 AV-Buchungen und 138.4 KEXI-Passagiere
# seed-1 hat  10 / 143
# seed-2 hat  10 / 140
# seed-3 hat  9 / 125
# seed-4 hat  14 / 129
# seed-5 hat  13 / 155
av2_3.3mps_allDay_trips <- 
  read_output_trips("E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0/AV-speed-mps-3.3/SAR2023-AV2-allDay/seed-1-SAR2023-allDay") %>% 
  mutate(main_mode = recode(main_mode, 
                            "av"  = "AV KEXI",
                            "drt" = "Konv. KEXI",
                            "pt_w_drt_used" = "KEXI + pt"))

# wIEKEXI-AV2-mps3.3 hat im Schnitt 3.4 AV-Buchungen und 142 KEXI-Passagiere
# seed-1 hat  3 / 141
# seed-2 hat  1 / 132
# seed-3 hat  6 / 145
# seed-4 hat  3 / 150
# seed-5 hat  4 / 142
av2_3.3mps_largeArea_trips <- 
  read_output_trips("E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0/AV-speed-mps-3.3/WIEKEXI-AV2-intermodal/seed-5-WIEKEXI") %>% 
  mutate(main_mode = recode(main_mode, 
                            "av"  = "AV KEXI",
                            "drt" = "Konv. KEXI",
                            "pt_w_drt_used" = "KEXI + pt"))

# av2_8.3mps_trips hat im Schnitt 40 AV-Buchungen und 135 KEXI-Passagiere
# seed-1 hat  43 / 128
# seed-2 hat  35 / 135
# seed-3 hat  36 / 126
# seed-4 hat  35 / 149
# seed-5 hat  51 / 137
av2_8.3mps_trips <- 
  read_output_trips("E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0/AV-speed-mps-8.3//SAR2023-AV2/seed-1-SAR2023") %>% 
  mutate(main_mode = recode(main_mode, 
                            "av"  = "AV KEXI",
                            "drt" = "Konv. KEXI",
                            "pt_w_drt_used" = "KEXI + pt"))


# der groeßte AV case mit aktueller geschwindigkeit. Im schnitt haben wir 247.4 simulierte AV-Buchungen und 125 konv. KEXI-Passagiere.
# seed-5 hat 245 / 124
av100_3.3mps_allDay_largeArea_trips <- 
  read_output_trips("E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0/AV-speed-mps-3.3/WIEKEXI-AV100-intermodal-allDay/seed-5-WIEKEXI-allDay") %>% 
  mutate(main_mode = recode(main_mode, 
                            "av"  = "AV KEXI",
                            "drt" = "Konv. KEXI",
                            "pt_w_drt_used" = "KEXI + pt"))


# der groeßte AV case überhaupt. Im schnitt haben wir 1105.4 simulierte AV-Buchungen und 136.2 konv. KEXI-Passagiere.
# seed-5 hat 1082 / 138
# seed-4 hat 1114 / 146
# seed-4 hat 1144 / 138
# seed-2 hat 1100 / 126
# seed-1 hat 1087 / 133
av100_8.3mps_allDay_largeArea_trips <- 
  read_output_trips("E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0/AV-speed-mps-8.3/WIEKEXI-AV100-intermodal-allDay/seed-2-WIEKEXI-allDay") %>% 
  mutate(main_mode = recode(main_mode, 
                            "av"  = "AV KEXI",
                            "drt" = "Konv. KEXI",
                            "pt_w_drt_used" = "KEXI + pt"))

# SAR-AV50-mps3.3 hat im Schnitt 64.8 AV-Buchungen und 138 KEXI-Passagiere
# seed-1 hat  65 / 148
# seed-2 hat  65 / 138
# seed-3 hat  66 / 128
# seed-4 hat  61 / 134
# seed-5 hat  67 / 142
av50_3.3mps_trips <- 
  read_output_trips("E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0/AV-speed-mps-3.3/SAR2023-AV50/seed-2-SAR2023") %>% 
  mutate(main_mode = recode(main_mode, 
                            "av"  = "AV KEXI",
                            "drt" = "Konv. KEXI",
                            "pt_w_drt_used" = "KEXI + pt"))

# wIEKEXI-AV50-mps8.3-allDay hat im Schnitt 1093.4 AV-Buchungen und 141.6 KEXI-Passagiere
# seed-1 hat  1081 / 148
# seed-2 hat  1096 / 153
# seed-3 hat  1099 / 143
# seed-4 hat  1099 / 130
# seed-5 hat  1092 / 134
av50_8.3mps_largeArea_allDay_trips <- 
  read_output_trips("E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0/AV-speed-mps-8.3/WIEKEXI-AV50-intermodal-allDay/seed-3-WIEKEXI-allDay") %>% 
  mutate(main_mode = recode(main_mode, 
                            "av"  = "AV KEXI",
                            "drt" = "Konv. KEXI",
                            "pt_w_drt_used" = "KEXI + pt"))

######################## FILTERN ######################
########################################################



######################## VARIANTE 1: FILTERE DRT TRIPS AUS DEN POLICY CASES. DANN PLOTTE MODAL SHIFT
######################## (WO KOMMEN DIE DRT TRIPS HER ??)
########################################################

drt_modes <- c ("drt", "av", "pt_w_drt_used", "AV KEXI", "KEXI + pt", "Konv. KEXI")

av_base_trips_drt <- av_base_trips %>% 
  filter(main_mode %in% drt_modes)

av2_3.3mps_allDay_trips_drt <- av2_3.3mps_allDay_trips %>% 
  filter(main_mode %in% drt_modes)

av2_3.3mps_largeArea_trips_drt <- av2_3.3mps_largeArea_trips %>% 
  filter(main_mode %in% drt_modes)

av2_8.3mps_trips_drt <- av2_8.3mps_trips %>% 
  filter(main_mode %in% drt_modes)

av50_3.3mps_trips_drt <- av50_3.3mps_trips %>% 
  filter(main_mode %in% drt_modes)

av100_3.3mps_allDay_largeArea_trips_drt <- av100_3.3mps_allDay_largeArea_trips %>% 
  filter(main_mode %in% drt_modes)

av100_8.3mps_allDay_largeArea_trips_drt <- av100_8.3mps_allDay_largeArea_trips %>% 
  filter(main_mode %in% drt_modes)

av50_8.3mps_largeArea_allDay_trips_drt <- av50_8.3mps_largeArea_allDay_trips %>% 
  filter(main_mode %in% drt_modes)


###################################
####PLOTS
sankey(no_kexi_trips, av_base_trips_drt) %>% 
  layout(title = "Basisfall (ohne KEXI)\n vs. Status Quo Mai 2024")

sankey(no_kexi_trips, av2_3.3mps_largeArea_trips_drt) %>% 
  layout(title = "Basisfall (ohne KEXI)\n vs. Vergrößerung Bediengebiet")

#sankey(av_base_trips, av2_3.3mps_largeArea_trips_drt) %>% 
#  layout(title = "Status Quo (Mai 2024)\n vs. Vergrößerung Bediengebiet")

sankey(no_kexi_trips, av2_8.3mps_trips_drt) %>% 
  layout(title = "Basisfall (ohne KEXI)\n vs. Beschleunigung auf 30 km/h")

#sankey(av_base_trips, av2_8.3mps_trips_drt) %>% 
#  layout(title = "Status Quo (Mai 2024)\n vs. Beschleunigung auf 30 km/h")

sankey(no_kexi_trips, av2_3.3mps_allDay_trips_drt) %>% 
  layout(title = "Basisfall (ohne KEXI)\n vs. Ganztägiger Betrieb")

#sankey(av_base_trips, av2_3.3mps_allDay_trips_drt) %>% 
#  layout(title = "Status Quo (Mai 2024)\n vs. Ganztägiger Betrieb")

sankey(no_kexi_trips, av50_3.3mps_trips_drt) %>% 
  layout(title = "Basisfall (ohne KEXI)\n vs. Große Flotte (50 AV)")

#sankey(av_base_trips, av50_3.3mps_trips_drt) %>% 
#  layout(title = "Status Quo (Mai 2024)\n vs. Große Flotte (50 AV)")

sankey(no_kexi_trips, av50_8.3mps_largeArea_allDay_trips_drt) %>% 
  layout(title = "Basisfall (ohne KEXI)\n vs. Alle Maßnahmen")

sankey(av_base_trips, av50_8.3mps_largeArea_allDay_trips_drt) %>% 
  layout(title = "Status Quo (Mai 2024)\n vs. Alle Maßnahmen")





sankey(av_base_trips, av100_3.3mps_allDay_largeArea_trips_drt)
sankey(no_kexi_trips, av100_3.3mps_allDay_largeArea_trips_drt)


######################## VARIANTE 2: Räumliches filtern nach Eimnzugsgebiet
########################################################
#filter trips auf einzugsgebiet des konventionellen KEXI = Stadtgebiet.
no_kexi_trips_spatial <- no_kexi_trips %>% 
  process_filter_by_shape(shape_table = drtArea, crs = 25832)

nur_konv_trips_spatial <- nur_konv_trips %>% 
  process_filter_by_shape(shape_table = drtArea, crs = 25832)

av_base_trips_spatial <- av_base_trips %>% 
  process_filter_by_shape(shape_table = drtArea, crs = 25832)

av100_3.3mps_allDay_largeArea_trips_spatial <- av100_3.3mps_allDay_largeArea_trips %>% 
  process_filter_by_shape(shape_table = drtArea, crs = 25832)

av100_8.3mps_allDay_largeArea_trips_spatial <- av100_8.3mps_allDay_largeArea_trips %>% 
  process_filter_by_shape(shape_table = drtArea, crs = 25832)




##################################################
###PLOTS

#kein KEXI
p <- plot_mainmode_piechart(no_kexi_trips)
p <- p %>% layout(title = "Kein KEXI")
p

#AV-Base
p <- plot_mainmode_barchart(av_base_trips)
p <- p %>% layout(title = "AV Base Case")
p

#av100_3.3mps_allDay_largeArea
p <- plot_mainmode_piechart(av100_3.3mps_allDay_largeArea_trips)
p <- p %>% layout(title = "av100_3.3mps_allDay_largeArea")
p

#av100_8.3mps_allDay_largeArea
p <- plot_mainmode_piechart(av100_8.3mps_allDay_largeArea_trips)
p <- p %>% layout(title = "av100_8.3mps_allDay_largeArea")
p


####TODO:
#filter for trips that are drt or AV in the policy and then display sankey for those only.

#av100_3.3mps_allDay_largeArea VERSUS av_base
plot_compare_mainmode_barchart(av_base_trips, av100_3.3mps_allDay_largeArea_trips)
plot_compare_mainmode_sankey(av_base_trips, av100_3.3mps_allDay_largeArea_trips, show_onlychanges = TRUE)

#av100_8.3mps_allDay_largeArea VERSUS av_base
plot_compare_mainmode_barchart(av_base_trips, av100_8.3mps_allDay_largeArea_trips)
plot_compare_mainmode_sankey(av_base_trips, av100_8.3mps_allDay_largeArea_trips, show_onlychanges = TRUE)


#base vs large AV
plot_compare_mainmode_barchart(base_filtered, largeAV_filtered)
plot_compare_mainmode_sankey(base_filtered, largeAV_filtered, show_onlychanges = TRUE)
plot_mainmode_piechart(largeAV_filtered)

matsim::plot_map_trips(kexi_filtered, crs = 25832)
