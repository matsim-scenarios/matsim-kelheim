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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.emissions.KelheimEmissionsDashboard;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.drt.extension.dashboards.DrtDashboardProvider;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.dashboard.NoiseDashboard;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@CommandLine.Command(
	name = "simwrapper",
	description = "Run emission analysis and create SimWrapper dashboard for existing run output."
)
final class KelheimSimWrapperRunner implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(KelheimSimWrapperRunner.class);

	@CommandLine.Parameters(arity = "1..*", description = "Path to run output directories for which emission dashboards are to be generated.")
	private List<Path> inputPaths;

	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();

	@CommandLine.Option(names = "--base", description = "Optional. " +
		"Relative path (from each! output directory provided) to main output folder for the base MATSim run. " +
		"Will be used to compare emissions per link.")
	private String baseRun;

	@CommandLine.Option(names = "--emissions", defaultValue = "false", description = "create emissions dashboard")
	private boolean emissions;

	@CommandLine.Option(names = "--noise", defaultValue = "false", description = "create noise dashboard")
	private boolean noise;

	@CommandLine.Option(names = "--drt", defaultValue = "false", description = "create DRT + AV dashboards")
	private boolean drt;


	private KelheimSimWrapperRunner(){
	}

	@Override
	public Integer call() throws Exception {

		if (!drt && !emissions && !noise){
			throw new IllegalArgumentException("you have not configured any dashboard to be created! Use command line parameters!");
		}

		for (Path runDirectory : inputPaths) {
			log.info("Running on {}", runDirectory);

			//this is to avoid overriding
//			renameExistingDashboardYAMLs(runDirectory);

			Path configPath = ApplicationUtils.matchInput("config.xml", runDirectory);
			Config config = ConfigUtils.loadConfig(configPath.toString());
			SimWrapper sw = SimWrapper.create(config);

			SimWrapperConfigGroup simwrapperCfg = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
			if (shp.isDefined()){
				//not sure if this is the best way to go, might be that the shape file would be automatically read by providing the --shp command line option
				simwrapperCfg.defaultParams().shp = shp.getShapeFile().toString();
			}
			//skip default dashboards
			simwrapperCfg.defaultDashboards = SimWrapperConfigGroup.Mode.disabled;
			simwrapperCfg.defaultParams().mapCenter = "48.91265,11.89223";

			//add dashboards according to command line parameters
			if (emissions){
				if (baseRun != null){
					sw.addDashboard(new KelheimEmissionsDashboard(baseRun));
				} else {
					sw.addDashboard(new KelheimEmissionsDashboard());
				}
			}
			if (drt){
				new DrtDashboardProvider().getDashboards(config, sw).forEach(sw::addDashboard);
			}
			if (noise){
				sw.addDashboard(new NoiseDashboard());
			}

			try {
				sw.generate(runDirectory, true);
				sw.run(runDirectory);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		return 0;
	}

	public static void main(String[] args) {
		new KelheimSimWrapperRunner().execute(args);

	}

}
