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

import org.matsim.analysis.postAnalysis.emissions.KelheimOfflineAirPollutionAnalysisByEngineInformation;
import org.matsim.application.prepare.network.CreateGeoJsonNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.GridMap;
import org.matsim.simwrapper.viz.Links;
import org.matsim.simwrapper.viz.Table;

import static org.matsim.dashboard.AverageKelheimNoiseDashboard.*;
import static org.matsim.dashboard.AverageKelheimNoiseDashboard.RED;

/**
 * this is basically equivalent to the standard emissions dashboard
 * but calls the matsim-kelheim-specific emissions analysis class
 * {@code KelheimOfflineAirPollutionAnalysisByEngineInformation}
 * which has specific network and vehicle type attributes.
 */
public class KelheimEmissionsDashboard implements Dashboard{
	private final String pathToCsvBase;

	public KelheimEmissionsDashboard() {
		this.pathToCsvBase = null;
	}

	public KelheimEmissionsDashboard(String pathToBaseRun) {
		if (!pathToBaseRun.endsWith("/")) {
			pathToBaseRun += "/";
		}
		this.pathToCsvBase = pathToBaseRun + "analysis/emissions/emissions_per_link_per_m.csv";
	}

	/**
	 * Produces the dashboard.
	 */
	public void configure(Header header, Layout layout) {

		header.title = "Air Pollution";
		header.description = "Shows the air pollution and spatial distribution.";

		String linkDescription = "Displays the emitted pair pollutants for each link per meter. Be aware that pollutant values are provided in the simulation sample size!";
		if (pathToCsvBase != null){
			linkDescription += String.format("%n Base is %s", pathToCsvBase);
		}
		String finalLinkDescription = linkDescription;

		layout.row("links")
			.el(Table.class, (viz, data) -> {
				viz.title = "Air Pollution";
				viz.description = "by pollutant. Total values are scaled from the simulation sample size to 100%.";
				viz.dataset = data.compute(KelheimOfflineAirPollutionAnalysisByEngineInformation.class, "emissions_total.csv", new String[0]);
				viz.enableFilter = false;
				viz.showAllRows = true;
				viz.width = 1.0;
			})
			.el(Links.class, (viz, data) -> {
				viz.title = "Emitted Pollutant in Gram per Meter";
				viz.description = finalLinkDescription;
				viz.height = 12.0;
				viz.datasets.csvFile = data.compute(KelheimOfflineAirPollutionAnalysisByEngineInformation.class, "emissions_per_link_per_m.csv", new String[0]);
				viz.datasets.csvBase = this.pathToCsvBase;
				viz.network = data.compute(CreateGeoJsonNetwork.class, "network.geojson", new String[0]);
				viz.display.color.columnName = "CO2_TOTAL [g/m]";
				viz.display.color.dataset = "csvFile";
				//TODO how to set color ramp??
//				viz.display.color.setColorRamp(Plotly.ColorScheme.RdBu, 5, true);
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
			viz.projection = "EPSG:25832";
			viz.file = data.compute(KelheimOfflineAirPollutionAnalysisByEngineInformation.class, "emissions_grid_per_day.xyt.csv", new String[0]);
			viz.setColorRamp(new double[]{30, 40, 50, 60, 70}, new String[]{DARK_BLUE, LIGHT_BLUE, YELLOW, SAND, ORANGE, RED});

		});
		layout.row("third")
			.el(GridMap.class, (viz, data) -> {
				viz.title = "CO₂ Emissions";
				viz.description = "per hour. Be aware that CO2 values are provided in the simulation sample size!";
				viz.height = 12.;
				viz.projection = "EPSG:25832";
				viz.file = data.compute(KelheimOfflineAirPollutionAnalysisByEngineInformation.class, "emissions_grid_per_hour.csv");
				viz.setColorRamp(new double[]{30, 40, 50, 60, 70}, new String[]{DARK_BLUE, LIGHT_BLUE, YELLOW, SAND, ORANGE, RED});
			});
	}
}
