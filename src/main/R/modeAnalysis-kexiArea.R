library(sf)
library(matsim)



drtArea <- st_read("D:/public-svn/matsim/scenarios/countries/de/kelheim/shp/prepare-network/av-and-drt-area.shp")

baseTrips <- read_output_trips("E:/matsim-kelheim/v3.0-release/output-base/25pct")
kexiTrips <- read_output_trips("E:/matsim-kelheim/v3.0-release/output-KEXI/seed-3-kexi")
largeAVTrips <- read_output_trips("E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0/AV-speed-mps-8.3/ALLCITY-AV100-intermodal/seed-1-ALLCITY")



base_filtered <- baseTrips %>% 
  process_filter_by_shape(shape_table = drtArea, crs = 25832)
kexi_filtered <- kexiTrips %>% 
  process_filter_by_shape(shape_table = drtArea, crs = 25832)
largeAV_filtered <- largeAVTrips %>% 
  process_filter_by_shape(shape_table = drtArea, crs = 25832)


#base
plot_mainmode_piechart(base_filtered)

#base vs KEXI
plot_compare_mainmode_barchart(base_filtered, kexi_filtered)
plot_compare_mainmode_sankey(base_filtered, kexi_filtered, show_onlychanges = TRUE)
plot_mainmode_piechart(kexi_filtered)


#base vs large AV
plot_compare_mainmode_barchart(base_filtered, largeAV_filtered)
plot_compare_mainmode_sankey(base_filtered, largeAV_filtered, show_onlychanges = TRUE)
plot_mainmode_piechart(largeAV_filtered)


