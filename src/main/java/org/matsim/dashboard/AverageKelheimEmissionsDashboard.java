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

import org.matsim.analysis.postAnalysis.EmissionsPostProcessingAverageAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Data;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.GridMap;
import org.matsim.simwrapper.viz.Links;
import org.matsim.simwrapper.viz.Table;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

		if (!pathToBaseRun.endsWith("/")) {
			pathToBaseRun += "/";
		}
		this.pathToCsvBase = pathToBaseRun + "analysis/emissions/emissions_per_link_per_m.csv";
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
		header.title = "Average Emissions";
		header.description = "Shows the average emissions footprint and spatial distribution for several simulation runs.";

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
			.el(Links.class, (viz, data) -> {
				viz.title = "Emissions per Link per Meter";
				viz.description = finalLinkDescription;
				viz.height = 12.0;
				viz.datasets.csvFile = postProcess(data, "mean_emissions_per_link_per_m.csv");
				viz.datasets.csvBase = Path.of(this.dirs.get(0)).getParent().relativize(Path.of(pathToCsvBase)).toString();
				viz.network = new CreateAverageDashboards().copyGeoJsonNetwork(dirs);
				viz.display.color.columnName = "CO2_TOTAL [g/m]";
				viz.display.color.dataset = "csvFile";
				viz.display.width.scaleFactor = 100;
				viz.display.width.columnName = "CO2_TOTAL [g/m]";
				viz.display.width.dataset = "csvFile";
				viz.center = data.context().getCenter();
				viz.width = 3.0;
		});
		layout.row("second").el(GridMap.class, (viz, data) -> {
			viz.title = "CO₂ Emissions";
			viz.description = "per day. Be aware that CO2 values are provided in the simulation sample size!";
			viz.height = 12.0;
			viz.file = postProcess(data, "mean_emissions_grid_per_day.xyt.csv");
		});
		layout.row("third")
			.el(GridMap.class, (viz, data) -> {
				viz.title = "CO₂ Emissions";
				viz.description = "per hour. Be aware that CO2 values are provided in the simulation sample size!";
				viz.height = 12.;
				viz.file = postProcess(data, "mean_emissions_grid_per_hour.csv");
			});
	}
}
