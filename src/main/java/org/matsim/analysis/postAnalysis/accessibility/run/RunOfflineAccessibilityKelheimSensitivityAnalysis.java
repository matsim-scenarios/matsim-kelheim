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
import org.matsim.contrib.drt.run.DrtConfigGroup;

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
	public static String stopsFileLand = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/kelheim-drt-accessibility-JB-master/input/drt-stops-land.xml";
	public static String stopsFileStadt = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/kelheim-v3.0/input/kelheim-v3.0-drt-stops.xml";
	public static String stopsFileStadtUndLand = "../public-svn/matsim/scenarios/countries/de/kelheim/kelheim-drt-accessibility-JB-master/input/drt-stops-stadt-und-land.xml";

	public static String stopsFileStadtUndLandUndNeustadt = "../public-svn/matsim/scenarios/countries/de/kelheim/kelheim-drt-accessibility-JB-master/input/drt-stops-stadt-und-land-und-neustadt.xml";

	public static String stopsFile;
	public static DrtConfigGroup drtConfigGroup;
	private static String outputDir;
	public static String coordinateSystem = "EPSG:25832";

	protected RunOfflineAccessibilityKelheimSensitivityAnalysis() {
		// should not be instantiated
		throw new UnsupportedOperationException();
	}

	public static void main(String[] args) throws FactoryException, TransformException, IOException {

//		if (args.length == 0) {
//			outputDir = "../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/2025-09-18-b-doubleDetour/";
//		} else if (args.length == 1) {
//			outputDir = args[0];
//		} else {
//			throw new IllegalArgumentException("Please provide the output directory as an argument.");
//		}

		// CONFIGURATION
		List<String> relevantPois = List.of("supermarket");
//		List<String> relevantPois = List.of("train_station", "amazon", "supermarket");
//		List<String> relevantPois = List.of("train_station");

		stopsFile = stopsFileStadtUndLandUndNeustadt;

		AccessibilityConfigGroup accConfig = new AccessibilityConfigGroup();

		accConfig.setTileSize_m(500);

		String mapCenterString;
//		{
//			accConfig.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromBoundingBox);
//
//			Coordinate leftBottomWgs84 = new Coordinate(11.805, 48.81);
//			Coordinate topRightWgs84 = new Coordinate(12.095, 48.994);
//			Coordinate leftBottom = transformCoordinate(CRS.decode("EPSG:4326", true), CRS.decode("EPSG:25832"), leftBottomWgs84);
//			Coordinate rightTop = transformCoordinate(CRS.decode("EPSG:4326", true), CRS.decode("EPSG:25832"), topRightWgs84);
//			mapCenterString = (leftBottomWgs84.x + topRightWgs84.x) / 2 + "," + (leftBottomWgs84.y + topRightWgs84.y) / 2;
//
////			//--
		double left = 689951.8456988378893584;
		double bottom = 5384612.2985062776133418;
		double right = 729756.0493708059657365;
		double top = 5433939.7461467878893018;
		accConfig.setBoundingBoxLeft(left);
		accConfig.setBoundingBoxBottom(bottom);
		accConfig.setBoundingBoxRight(right);
		accConfig.setBoundingBoxTop(top);
		mapCenterString = "11.87632,48.81992";

//
//		}

		{
			accConfig.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromShapeFile);
			accConfig.setShapeFileCellBasedAccessibility("input/shp/lk-kelheim/lk-kelheim.shp");
		}


//		List<Double> timesHour = List.of(5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5, 10., 24.);
//
//		List<Double> timesHour = List.of(7.5, 8.0, 8.5, 24.0);
		List<Double> timesHour = List.of(7.5);


		List<Double> timesSeconds = new ArrayList<>(timesHour.stream().map(t -> t * 60 * 60).toList());

//
//		List<Double> timesHour = List.of(4.0, 8.0, 12.0, 16.0, 20.0, 24.0);
//
//
//		List<Double> timesSeconds = new ArrayList<>(timesHour.stream().map(t -> t * 60 * 60).toList());
//
//		for (int min = 5; min < 60; min += 5) {
//
//			timesSeconds.add(8.0 * 60 * 60 + min * 60);
//			timesSeconds.add(9.0 * 60 * 60 + min * 60);
//
//		}
		accConfig.setTimeOfDay(timesSeconds);

		List<Modes4Accessibility> accModes = List.of(Modes4Accessibility.teleportedWalk, Modes4Accessibility.pt, Modes4Accessibility.car, Modes4Accessibility.estimatedDrt);
//		List<Modes4Accessibility> accModes = List.of(Modes4Accessibility.pt, Modes4Accessibility.car, Modes4Accessibility.estimatedDrt);
//				List<Modes4Accessibility> accModes = List.of(Modes4Accessibility.pt);

		for (Modes4Accessibility mode : Modes4Accessibility.values()) {
			accConfig.setComputingAccessibilityForMode(mode, accModes.contains(mode));
		}

//		List<Modes4Accessibility> accModes = List.of(Modes4Accessibility.estimatedDrt);


		// Part 1: Generate Parameters for Estimator
		// Part 2: Calculate Accessibility

//		EstimatorParameters estimatorParameters = step1GenerateParams();


		File dirToCopy = new File("../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/0000-kelheim-scratch");


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

				outputDir = "../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/2025-11-19-" + asc + "-" + los + "/";

				FileUtils.copyDirectory(dirToCopy, new File(outputDir));
				DrtEstimator drtEstimator = new DirectTripBasedDrtEstimator.Builder()
					.setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(waitingTime))
					.setWaitingTimeDistributionGenerator(new NoDistribution())
					.setRideDurationEstimator(new ConstantRideDurationEstimator(slope, intercept))
					.setRideDurationDistributionGenerator(new NoDistribution())
					.build();

				step2CalculateAccessibility(drtEstimator, ascDrt, relevantPois, accConfig);

				// Part 3: Create Dashboard
				step3CreateDashboard(relevantPois, accModes, mapCenterString);


			}
		}
	}

}
