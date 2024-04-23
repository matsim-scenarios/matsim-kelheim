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

package org.matsim.analysis.emissions;

import org.matsim.application.prepare.network.CreateGeoJsonNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Links;
import org.matsim.simwrapper.viz.Table;
import org.matsim.simwrapper.viz.XYTime;

/**
 * this is basically equivalent to the standard emissions dashboard
 * but calls the matsim-kelheim-specific emissions analysis class
 * {@code KelheimOfflineAirPollutionAnalysisByEngineInformation}
 * which has specific network and vehicle type attributes.
 */
public class KelheimEmissionsDashboard implements Dashboard{
	public KelheimEmissionsDashboard() {
	}

	/**
	 * Produces the dashboard.
	 */
	public void configure(Header header, Layout layout) {
		header.title = "Emissions";
		header.description = "Shows the emissions footprint and spatial distribution.";
		layout.row("links")
			.el(Table.class, (viz, data) -> {
				viz.title = "Emissions";
				viz.description = "by pollutant";
				viz.dataset = data.compute(KelheimOfflineAirPollutionAnalysisByEngineInformation.class, "emissions_total.csv", new String[0]);
				viz.enableFilter = false;
				viz.showAllRows = true;
				viz.width = 1.0;
			})
			.el(Links.class, (viz, data) -> {
				viz.title = "Emissions per Link per Meter";
				viz.description = "Displays the emissions for each link per meter.";
				viz.height = 12.0;
				viz.datasets.csvFile = data.compute(KelheimOfflineAirPollutionAnalysisByEngineInformation.class, "emissions_per_link_per_m.csv", new String[0]);
				viz.network = data.compute(CreateGeoJsonNetwork.class, "network.geojson", new String[0]);
				viz.display.color.columnName = "CO2_TOTAL [g/m]";
				viz.display.color.dataset = "csvFile";
				viz.display.width.scaleFactor = 1;
				viz.display.width.columnName = "CO2_TOTAL [g/m]";
				viz.display.width.dataset = "csvFile";
				viz.center = data.context().getCenter();
				viz.width = 3.0;
		});
		layout.row("second").el(XYTime.class, (viz, data) -> {
			viz.title = "CO₂ Emissions";
			viz.description = "per day";
			viz.height = 12.0;
			viz.file = data.compute(KelheimOfflineAirPollutionAnalysisByEngineInformation.class, "emissions_grid_per_day.xyt.csv", new String[0]);
		});
	}
}
