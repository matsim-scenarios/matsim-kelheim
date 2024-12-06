1. Import .csv file of all ride requests into data_prep.R, save data_plausible as rides_clean.csv
2. Use r5r.R to calculate shortest path distances for KEXI data and create linear regression models, save data frame including distances as rides_dist.csv
	-this requires the network in .pbf format
3. KEXI_analysis.R: analyse rides_clean.csv and rides_dist.csv
4. Accessibility_analysis.R: accessibility runs over time can be analysed here. Looking back this is very hard coded, it only works for the specific time slots and adjustments need to be made if file names change.