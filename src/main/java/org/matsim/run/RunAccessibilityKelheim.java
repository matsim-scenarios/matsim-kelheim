/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.run;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.AccessibilityUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;

/**
 * @author nagel
 */
final public class RunAccessibilityKelheim {
	// do not change name of class; matsim book refers to it.  kai, dec'14

	private static final Logger LOG = LogManager.getLogger(RunAccessibilityKelheim.class);




	public static void main(String[] args) {
//		if (args.length==0 || args.length>1) {
//			throw new RuntimeException("No config.xml file provided. The config file needs to reference a network file and a facilities file.") ;
//		}
//		Config config = ConfigUtils.loadConfig("input/v3.1/kelheim-v3.1-config.xml");
//		Config config = ConfigUtils.loadConfig("input/acc-config.xml");
		// this config only contains activity & mode params ; other input files need to be specified.
		Config config = ConfigUtils.loadConfig("input/v3.0-release/output-KEXI-adequate-vehicles/seed-1-adequate-vehicles/kexi-seed1-adequate-vehicles.output_config-jr-edit.xml");
				config.network().setInputFile("kexi-seed1-adequate-vehicles.output_network.xml.gz");

		config.plans().setInputFile("kexi-seed1-adequate-vehicles.output_plans.xml.gz");

		config.transit().setTransitScheduleFile("kexi-seed1-adequate-vehicles.output_transitSchedule.xml.gz");

		config.global().setCoordinateSystem("EPSG:25832");

		// Now we try loading the full output config from a kelheim run.
//		Config config = ConfigUtils.createConfig();
//		ConfigUtils.loadConfig(config, "input/v3.0-release/output-KEXI-adequate-vehicles/seed-1-adequate-vehicles/kexi-seed1-adequate-vehicles.output_config.xml");

		// or just
//		Config config = ConfigUtils.createConfig();



		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(0);




		// Because of following error: java.lang.RuntimeException: java.util.concurrent.ExecutionException: java.lang.RuntimeException: java.lang.RuntimeException: you cannot use the randomzing travel disutility without person.  If you need this without a person, set sigma to zero. If you are loading a scenario from a config, set the routingRandomness in the plansCalcRoute config group to zero.
		config.routing().setRoutingRandomness(0);

		// Accessibility Config Group:
		AccessibilityConfigGroup accConfig = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class ) ;
//		accConfig.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromShapeFile);
//		accConfig.setShapeFileCellBasedAccessibility("/Users/jakob/Downloads/solid_gitter/solid_gitter.shp");

		//train station 715041.71, 5420617.28
		double trainStationX = 715041.71;
		double trainStationY = 5420617.28;
		double tileSize = 250;
		double num_rows = 10;

		accConfig.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromBoundingBox);
		accConfig.setBoundingBoxLeft(trainStationX - num_rows*tileSize - tileSize/2);
		accConfig.setBoundingBoxRight(trainStationX + num_rows*tileSize + tileSize/2);
		accConfig.setBoundingBoxBottom(trainStationY - num_rows*tileSize - tileSize/2);
		accConfig.setBoundingBoxTop(trainStationY + num_rows*tileSize + tileSize/2);
		accConfig.setTileSize_m((int) tileSize);
		accConfig.setTimeOfDay(14 * 60 * 60.);
		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, false); // works
		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.car, false); // works
//		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.bike, false); // doesn't work!!!
		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.pt, true); // works
//		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.walk, true); //TODO: walk doesn't work, maybe since it is a teleported mode?
//		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.estimatedDrt, true);


		//TODO: implement closest accessibility type
//		accConfig.setAccessibilityMeasureType(AccessibilityConfigGroup.AccessibilityMeasureType.closest);



		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		ActivityFacilitiesFactory af = scenario.getActivityFacilities().getFactory();

		// add opportunity facility
		ActivityFacility fac1 = af.createActivityFacility(Id.create("xxx", ActivityFacility.class), new Coord(trainStationX, trainStationY));
		ActivityOption ao = af.createActivityOption("train station");
		fac1.addActivityOption(ao);
		scenario.getActivityFacilities().addActivityFacility(fac1);
		run(scenario);
	}


	public static void run(final Scenario scenario) {
		List<String> activityTypes = AccessibilityUtils.collectAllFacilityOptionTypes(scenario);
		LOG.info("The following activity types were found: " + activityTypes);

		Controler controler = new Controler(scenario);
		for (final String actType : activityTypes) { // Add an overriding module for each activity type.
			final AccessibilityModule module = new AccessibilityModule();
			module.setConsideredActivityType(actType);
			controler.addOverridingModule(module);
		}
		controler.run();
	}
}
