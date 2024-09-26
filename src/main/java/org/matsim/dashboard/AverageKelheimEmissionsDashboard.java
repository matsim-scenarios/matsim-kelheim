/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.dashboard;

import org.matsim.analysis.postAnalysis.emissions.EmissionsPostProcessingAverageAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Data;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.*;

import java.util.ArrayList;
import java.util.List;

import static org.matsim.dashboard.AverageKelheimNoiseDashboard.*;

/**
 * Average emissions dashboard for several runs with the same config but a different random seed.
 */
public class AverageKelheimEmissionsDashboard implements Dashboard {
	private final List<String> dirs;
	private final Integer noRuns;
	private final String pathToCsvBase;

	public AverageKelheimEmissionsDashboard(List<String> dirs, Integer noRuns) {
		this.dirs = dirs;
		this.noRuns = noRuns;
		this.pathToCsvBase = null;
	}

	public AverageKelheimEmissionsDashboard(List<String> dirs, Integer noRuns, String pathToBaseRun) {
		this.dirs = dirs;
		this.noRuns = noRuns;

		if (pathToBaseRun == null || pathToBaseRun.equals("null")){
			this.pathToCsvBase = null;
		} else {
			if (!pathToBaseRun.endsWith("/")) {
				pathToBaseRun += "/";
			}
			this.pathToCsvBase = pathToBaseRun + "analysis/emissions/emissions_per_link_per_m.csv";
		}
	}

	private String postProcess(Data data, String outputFile) {
//		args for analysis have to be: list of paths to run dirs + drt modes / folder names
		List<String> args = new ArrayList<>(List.of("--input-runs", String.join(",", dirs), "--no-runs", noRuns.toString()));

		return data.compute(EmissionsPostProcessingAverageAnalysis.class, outputFile, args.toArray(new String[0]));
	}

	/**
	 * Produces the dashboard.
	 */
	public void configure(Header header, Layout layout) {
		header.title = "Average Air Pollution";
		header.description = "Shows the average air pollution and spatial distribution for several simulation runs.";

		String linkDescription = "Displays the emissions for each link per meter. Be aware that emission values are provided in the simulation sample size!";
		if (pathToCsvBase != null){
			linkDescription += String.format("%n Base is %s", pathToCsvBase);
		}
		String finalLinkDescription = linkDescription;

		layout.row("links")
			.el(Table.class, (viz, data) -> {
				viz.title = "Emissions";
				viz.description = "by pollutant. Total values are scaled from the simulation sample size to 100%.";
				viz.dataset = postProcess(data, "mean_emissions_total.csv");
				viz.enableFilter = false;
				viz.showAllRows = true;
				viz.width = 1.0;
			})
			/*
			 *  Commented out link panel, because the MapPlot can show a legend and seems to be the development head.
			 *  However, it doesn't seem to have the pointer to the base case
			 */
			/*.el(Links.class, (viz, data) -> {
				viz.title = "Emitted Pollutant in Gram per Meter";
				viz.description = finalLinkDescription;
				viz.height = 12.0;
				viz.datasets.csvFile = postProcess(data, "mean_emissions_per_link_per_m.csv");
				viz.datasets.csvBase = pathToCsvBase == null ? "" : Path.of(this.dirs.get(0)).getParent().relativize(Path.of(pathToCsvBase)).toString();
				viz.network = new CreateAverageDashboards().copyVizNetwork(dirs, ".avro");
				viz.display.color.columnName = "CO2_TOTAL [g/m]";
				viz.display.color.dataset = "csvFile";
				viz.display.width.scaleFactor = 100;
				viz.display.width.columnName = "CO2_TOTAL [g/m]";
				viz.display.width.dataset = "csvFile";
				viz.center = data.context().getCenter();
				viz.width = 1.0;
			})*/
			.el(MapPlot.class, (viz, data) -> {
				viz.title = "Emitted Pollutant in Gram per Meter";
				viz.description = finalLinkDescription;
				viz.height = 12.0;
				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
				viz.setShape(new CreateAverageDashboards().copyVizNetwork(dirs, ".avro"), "id");
				viz.addDataset("emissions_per_m", postProcess(data, "mean_emissions_per_link_per_m.csv"));
				viz.display.lineColor.dataset = "emissions_per_m";
				viz.display.lineColor.columnName = "CO2_TOTAL [g/m]";
				viz.display.lineColor.join = "linkId";
				viz.display.lineColor.setColorRamp(ColorScheme.Oranges, 8, false, "35, 45, 55, 65, 75, 85, 95");
				viz.display.lineWidth.dataset = "emissions_per_m";
				viz.display.lineWidth.columnName = "CO2_TOTAL [g/m]";
				viz.display.lineWidth.scaleFactor = 100d;
				viz.display.lineWidth.join = "linkId";
				viz.width = 3.0;
			});

		layout.row("second").el(GridMap.class, (viz, data) -> {
			viz.title = "CO₂ Emissions";
			viz.description = "per day. Be aware that CO2 values are provided in the simulation sample size!";
			setGridMapStandards(viz);
			viz.file = postProcess(data, "mean_emissions_grid_per_day.xyt.csv");
		});
		layout.row("third")
			.el(GridMap.class, (viz, data) -> {
				viz.title = "CO₂ Emissions";
				viz.description = "per hour. Be aware that CO2 values are provided in the simulation sample size!";
				setGridMapStandards(viz);
				viz.file = postProcess(data, "mean_emissions_grid_per_hour.csv");
			});
	}

	private static void setGridMapStandards(GridMap viz) {
		viz.projection = "EPSG:25832";
		viz.setColorRamp(new double[]{30, 40, 50, 60, 70}, new String[]{DARK_BLUE, LIGHT_BLUE, YELLOW, SAND, ORANGE, RED});
		viz.height = 12.0;
	}
}
