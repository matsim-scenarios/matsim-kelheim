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
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;

/**
 * @author nagel
 */
final public class RunAccessibility {
	// do not change name of class; matsim book refers to it.  kai, dec'14

	private static final Logger LOG = LogManager.getLogger(RunAccessibility.class);


	// use contribs/accessibility/examples/RunAccessibilityExample/config.xml



	public static void main(String[] args) {
//		if (args.length==0 || args.length>1) {
//			throw new RuntimeException("No config.xml file provided. The config file needs to reference a network file and a facilities file.") ;
//		}
//		Config config = ConfigUtils.loadConfig("input/v3.1/kelheim-v3.1-config.xml");
		Config config = ConfigUtils.loadConfig("input/acc-config.xml");
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
//		config.controller().setLastIteration(0);
//		config.plans().setInputFile(null);

		AccessibilityConfigGroup accConfig = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class ) ;
		accConfig.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromShapeFile);
		accConfig.setShapeFileCellBasedAccessibility("/Users/jakob/Downloads/solid_gitter/solid_gitter.shp");
		accConfig.setTileSize_m(500);
		accConfig.setTimeOfDay(14 * 60 * 60.);
		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);


		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
//		scenario.getPopulation().getPersons().clear();


		// add facilities
		ActivityFacilitiesFactory af = scenario.getActivityFacilities().getFactory();
		ActivityFacility fac1 = af.createActivityFacility(Id.create("xxx", ActivityFacility.class), new Coord(710934.0800000001,5422522.85));
		ActivityOption ao = af.createActivityOption("shop");
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
