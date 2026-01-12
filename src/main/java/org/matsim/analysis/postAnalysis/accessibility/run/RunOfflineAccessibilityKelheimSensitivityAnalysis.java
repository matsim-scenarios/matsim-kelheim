package org.matsim.analysis.postAnalysis.accessibility.run;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.estimator.impl.DirectTripBasedDrtEstimator;
import org.matsim.contrib.drt.estimator.impl.distribution.NoDistribution;
import org.matsim.contrib.drt.estimator.impl.trip_estimation.ConstantRideDurationEstimator;
import org.matsim.contrib.drt.estimator.impl.waiting_time_estimation.ConstantWaitingTimeEstimator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.matsim.analysis.postAnalysis.accessibility.run.RunOfflineAccessibilityKelheim.step2CalculateAccessibility;
import static org.matsim.analysis.postAnalysis.accessibility.run.RunOfflineAccessibilityKelheim.step3CreateDashboard;

/**
 * Run this class to calculate accessibility for the Kelheim scenario for different modes (including DRT). This class is meant to be run after
 * a simulation (offline).
 */
public class RunOfflineAccessibilityKelheimSensitivityAnalysis {

	private static final Logger log = LogManager.getLogger(RunOfflineAccessibilityKelheimSensitivityAnalysis.class);
	public static String coordinateSystem = "EPSG:25832";

	protected RunOfflineAccessibilityKelheimSensitivityAnalysis() {
		// should not be instantiated
		throw new UnsupportedOperationException();
	}

	public static void main(String[] args) throws FactoryException, TransformException, IOException {

		// CONFIGURATION
		// what POIs will are being examined
		List<String> relevantPois = List.of("train_station","logistic","supermarket");

		// What times will we calculate accessibilty
		List<Double> timesHour = List.of(7.5);

		// For what modes
		List<Modes4Accessibility> accModes = List.of( Modes4Accessibility.estimatedDrt);

		// With what directory are we working? Following code makes a copy, so as to leave original directory intact.
		File dirToCopy = new File("../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/0000-kelheim-scratch");

		// CONFIG

		AccessibilityConfigGroup accConfig = new AccessibilityConfigGroup();
		accConfig.setTileSize_m(500);
		accConfig.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromShapeFile);
		accConfig.setShapeFileCellBasedAccessibility("input/shp/lk-kelheim/lk-kelheim.shp");
		accConfig.setTimeOfDay(new ArrayList<>(timesHour.stream().map(t -> t * 60 * 60).toList()));
		for (Modes4Accessibility mode : Modes4Accessibility.values()) {
			accConfig.setComputingAccessibilityForMode(mode, accModes.contains(mode));
		}


		for (String asc : List.of("a", "b", "c")) {
			for (String los : List.of("1", "2", "3")) {
				double ascDrt;
				switch (asc) {
					case "a":
						ascDrt = -2.5;
						break;
					case "b":
						ascDrt = 0;
						break;
					case "c":
						ascDrt = 2.5;
						break;
					default:
						throw new RuntimeException("Unknown case");
				}
				double waitingTime;
				double slope;
				double intercept;
				switch (los) {
					case "1":
						waitingTime = 0;
						slope = 1;
						intercept = 0;
						break;
					case "2":
						waitingTime = 300;
						slope = 1.22;
						intercept = 177.5;
						break;
					case "3":
						waitingTime = 15*60;
						slope = 3;
						intercept = 177.5;
						break;
					default:
						throw new RuntimeException("Unknown case");
				}

				String outputDir = "../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/2026-01-09-calibration/" + asc + "-" + los + "/";
				FileUtils.copyDirectory(dirToCopy, new File(outputDir));



				// Part 1: Generate Parameters for Estimator

				DrtEstimator drtEstimator = new DirectTripBasedDrtEstimator.Builder()
					.setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(waitingTime))
					.setWaitingTimeDistributionGenerator(new NoDistribution())
					.setRideDurationEstimator(new ConstantRideDurationEstimator(slope, intercept))
					.setRideDurationDistributionGenerator(new NoDistribution())
					.build();

				// Part 2: Calculate Accessibility
				log.info("Starting Accessibility Calculation for " + ascDrt + " and " + los);
				step2CalculateAccessibility(drtEstimator, ascDrt, relevantPois, accConfig, outputDir);


				// Part 3: Create Dashboard
				if(asc.equals("a") && los.equals("1")){
					step3CreateDashboard(relevantPois, accModes, "", outputDir);

				}



			}
		}
	}

}
