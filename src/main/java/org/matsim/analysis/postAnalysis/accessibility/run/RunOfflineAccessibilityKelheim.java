package org.matsim.analysis.postAnalysis.accessibility.run;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.matsim.analysis.postAnalysis.accessibility.AccessibilityDashboardHeart;
import org.matsim.analysis.postAnalysis.accessibility.OverviewDashboardHeart;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.ApplicationUtils;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityFromEvents;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.estimator.impl.DirectTripBasedDrtEstimator;
import org.matsim.contrib.drt.estimator.impl.distribution.NoDistribution;
import org.matsim.contrib.drt.estimator.impl.trip_estimation.ConstantRideDurationEstimator;
import org.matsim.contrib.drt.estimator.impl.waiting_time_estimation.ConstantWaitingTimeEstimator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.*;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Run this class to calculate accessibility for the Kelheim scenario for different modes (including DRT). This class is meant to be run after
 * a simulation (offline).
 */
public class RunOfflineAccessibilityKelheim {

	private static final Logger log = LogManager.getLogger(RunOfflineAccessibilityKelheim.class);

	public static String stopsFile = "../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/_data/1_processed/drt_stops/drt_stops.xml";
	public static String facilitiesFile = "../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/_data/1_processed/osm_supermarkets_buffer5km/pois.xml";

	public static DrtConfigGroup drtConfigGroup;
	public static String coordinateSystem = "EPSG:25832";

	protected RunOfflineAccessibilityKelheim() {
		// should not be instantiated
		throw new UnsupportedOperationException();
	}

	public static void main(String[] args) throws FactoryException, TransformException, IOException {

		// CONFIGURATION
		// what POIs will are being examined
		List<String> relevantPois = List.of("train_station");

		// What times will we calculate accessibilty
//		List<Double> timesHour = DoubleStream.iterate(0, i -> i <= 24., i -> i + 0.5).boxed().toList();
		List<Double> timesHour = List.of(7.5);

		// For what modes
		List<Modes4Accessibility> accModes = List.of( Modes4Accessibility.estimatedDrt);
		// What parameters will be used for DRT Estimator
		double waitingTime = 300;
		double slope = 1.22;
		double intercept = 177.5;
		double ascDrt = 0.0;
		// With what directory are we working? Following code makes a copy, so as to leave original directory intact.
		File dirToCopy = new File("../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/0000-kelheim-scratch");
		String outputDir = "../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/2026-01-08-a/";
		FileUtils.copyDirectory(dirToCopy, new File(outputDir));

		// CONFIG

		AccessibilityConfigGroup accConfig = new AccessibilityConfigGroup();
		accConfig.setTileSize_m(500);
		accConfig.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromShapeFile);
		accConfig.setShapeFileCellBasedAccessibility("input/shp/lk-kelheim/lk-kelheim.shp");
		accConfig.setTimeOfDay(new ArrayList<>(timesHour.stream().map(t -> t * 60 * 60).toList()));
		for (Modes4Accessibility mode : Modes4Accessibility.values()) {
			accConfig.setComputingAccessibilityForMode(mode, accModes.contains(mode));
		}
		String mapCenterString = "11.87632,48.81992";



		// Part 1: Generate Parameters for Estimator
// commented out because estimator params are configured at beginning of script
//		EstimatorParameters estimatorParameters = step1GenerateParams();

		DrtEstimator drtEstimator = new DirectTripBasedDrtEstimator.Builder()
			.setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(waitingTime))
			.setWaitingTimeDistributionGenerator(new NoDistribution())
			.setRideDurationEstimator(new ConstantRideDurationEstimator(slope, intercept))
			.setRideDurationDistributionGenerator(new NoDistribution())
			.build();

		// Part 2: Calculate Accessibility
		step2CalculateAccessibility(drtEstimator, ascDrt, relevantPois, accConfig, outputDir);

		// Part 3: Create Dashboard
//		step3CreateDashboard(relevantPois, accModes, mapCenterString);

	}







	private static EstimatorParameters step1GenerateParams(String outputDir) {

		DoubleList inVehicleTravelTime = new DoubleArrayList();
		DoubleList directTravelDistance_m = new DoubleArrayList();

		double n = 0.;
		double waitTimeSum = 0.;

		Path filePath = Path.of(outputDir + "kexi-seed1-ASC-2.45.output_drt_legs_drt.csv");
		try (CSVParser parser = new CSVParser(new BufferedReader(new InputStreamReader(Files.newInputStream(filePath))),
			CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {

			for (CSVRecord csvRecord : parser) {
				n++;
				waitTimeSum += Double.parseDouble(csvRecord.get("waitTime"));
				inVehicleTravelTime.add(Double.parseDouble(csvRecord.get("inVehicleTravelTime")));
				directTravelDistance_m.add(Double.parseDouble(csvRecord.get("directTravelDistance_m")));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		double waitTime_s = waitTimeSum / n;
		log.info("Wait time (seconds):" + waitTime_s);
		log.info("Wait time (minutes):" + waitTime_s/60);

		SimpleRegression regression = new SimpleRegression(true);

		for (int i = 0; i < n; i++) {
			regression.addData(directTravelDistance_m.getDouble(i), inVehicleTravelTime.getDouble(i));
		}

		// Get the intercept and slope
		double intercept = regression.getIntercept();
		double slope = regression.getSlope();


		log.info("Intercept: " + intercept);
		log.info("Slope: " + slope);
		log.info("R2: " + regression.getRSquare());

		return new EstimatorParameters(intercept, slope, waitTime_s);
	}



	static void step2CalculateAccessibility(DrtEstimator drtEstimator, Double ascDrt, List<String> relevantPois, ConfigGroup accConfig, String outputDir) {

		// input files
		String eventsFile = ApplicationUtils.matchInput("output_events.xml.gz", Path.of(outputDir)).toString();
		String networkFile = ApplicationUtils.matchInput("output_network.xml.gz", Path.of(outputDir)).toString();
		String transportScheduleFile = ApplicationUtils.matchInput("output_transitSchedule.xml.gz", Path.of(outputDir)).toString();



		// CONFIG
		//global
		final Config config = ConfigUtils.createConfig();


		config.controller().setLastIteration(0);
		config.controller().setOutputDirectory(outputDir);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		config.routing().setRoutingRandomness(0.);

		config.global().setCoordinateSystem(coordinateSystem);

		config.network().setInputFile(networkFile);


		// transit
		config.transit().setTransitScheduleFile(transportScheduleFile);
		config.scoring().setMarginalUtlOfWaitingPt_utils_hr(0.0);


		config.transitRouter().setSearchRadius(1_000);
		config.transitRouter().setExtensionRadius(500);
		config.transitRouter().setMaxBeelineWalkConnectionDistance(300);


		// change walk speed to match kelheim scenario
		config.routing().getTeleportedModeParams().get(TransportMode.walk).setTeleportedModeSpeed(3.8 / 3.6);

		// change scoring default to match kelheim scenario
		ScoringConfigGroup.ModeParams drtParams = new ScoringConfigGroup.ModeParams(TransportMode.drt);
		drtParams.setConstant(ascDrt);
		drtParams.setMarginalUtilityOfDistance(-2.5E-4);
		drtParams.setMarginalUtilityOfTraveling(0.0);
		config.scoring().addModeParams(drtParams);


		ScoringConfigGroup.ModeParams walkParams = config.scoring().getModes().get(TransportMode.walk);
		walkParams.setMarginalUtilityOfTraveling(0.0);
		config.scoring().addModeParams(walkParams);

		// accessibility config

		config.addModule(accConfig);

		// drt config
		ConfigUtils.addOrGetModule( config, DvrpConfigGroup.class );

		drtConfigGroup = new DrtConfigGroup();
		drtConfigGroup.setOperationalScheme(DrtConfigGroup.OperationalScheme.stopbased);
		drtConfigGroup.setTransitStopFile(stopsFile);

		drtConfigGroup.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().setMaxWalkDistance(100000.);

		MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup();
		multiModeDrtConfigGroup.addParameterSet(drtConfigGroup);
		config.addModule(multiModeDrtConfigGroup);
		config.addModule(drtConfigGroup);

		// SCENARIO

		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);

		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (!link.getId().toString().startsWith("pt_")) {
				Set<String> modes = new HashSet<>(link.getAllowedModes());
				modes.add(TransportMode.walk);
				modes.add(TransportMode.bike);
				link.setAllowedModes(modes);
			}

		}


		// add pois to scenario as facilities
		new MatsimFacilitiesReader(scenario).readFile(facilitiesFile);

		AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder(scenario, eventsFile, relevantPois);

		builder.setDrtEstimator(drtEstimator);


		builder.build().run();

	}



	static void step3CreateDashboard(List<String> relevantPois, List<Modes4Accessibility> accModes, String mapCenterString, String outputDir) {

		final Config config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory(outputDir);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);


		//CONFIG
		config.controller().setLastIteration(0);
		config.controller().setWritePlansInterval(-1);
		config.controller().setWriteEventsInterval(-1);
		config.global().setCoordinateSystem(coordinateSystem);



		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.none);

		//simwrapper
		SimWrapperConfigGroup group = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		group.setSampleSize(0.001);
		if (mapCenterString != null) {
			group.defaultParams().setMapCenter(mapCenterString);
		}
u		group.setDefaultDashboards(SimWrapperConfigGroup.DefaultDashboardsMode.disabled);


		SimWrapper sw = SimWrapper.create(config)
			.addDashboard(new OverviewDashboardHeart(relevantPois, coordinateSystem))
			.addDashboard(new AccessibilityDashboardHeart(coordinateSystem, relevantPois, Modes4Accessibility.car))
			.addDashboard(new AccessibilityDashboardHeart(coordinateSystem, relevantPois, Modes4Accessibility.pt))
			.addDashboard(new AccessibilityDashboardHeart(coordinateSystem, relevantPois, Modes4Accessibility.estimatedDrt));



		boolean append = false;
		try {
			sw.generate(Path.of(outputDir), append);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		sw.run(Path.of(outputDir));

	}





	/**
	 * This class holds the parameters for the DRT estimator, which are generated in step 1.
	 */
	public static final class EstimatorParameters {
		private final double intercept;
		private final double slope;
		private final double waitTime;

		public EstimatorParameters(double value1, double value2, double value3) {
			this.intercept = value1;
			this.slope = value2;
			this.waitTime = value3;
		}

		public double getIntercept() {
			return intercept;
		}

		public double getSlope() {
			return slope;
		}

		public double getWaitTime() {
			return waitTime;
		}
	}


}
