package org.matsim.analysis.postAnalysis.accessibility.run;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.postAnalysis.accessibility.AccessibilityDashboardHeart;
import org.matsim.analysis.postAnalysis.accessibility.OverviewDashboardHeart;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityFromEvents;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.estimator.impl.DirectTripBasedDrtEstimator;
import org.matsim.contrib.drt.estimator.impl.CsvServiceQualityDrtEstimator;
import org.matsim.contrib.drt.estimator.impl.distribution.NoDistribution;
import org.matsim.contrib.drt.estimator.impl.trip_estimation.ConstantRideDurationEstimator;
import org.matsim.contrib.drt.estimator.impl.waiting_time_estimation.ConstantWaitingTimeEstimator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.routing.DrtStopFacility;
import org.matsim.contrib.drt.routing.DrtStopFacilityImpl;
import org.matsim.contrib.drt.routing.DrtStopNetwork;
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
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableMap;

/**
 * Run this class to calculate accessibility for the Kelheim scenario for different modes (including DRT). This class is meant to be run after
 * a simulation (offline).
 */
@CommandLine.Command(
	name = "offline-accessibility",
	description = "Calculate Kelheim accessibility from a MATSim output directory.",
	mixinStandardHelpOptions = true,
	showDefaultValues = true
)
public class RunOfflineAccessibilityKelheim implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(RunOfflineAccessibilityKelheim.class);

	public static String stopsFile = "input/v3.1/expanded-service-area/drt_stops_landkreis.xml";
	public static String facilitiesFile = "input/v3.1/expanded-service-area/pois.xml";

	public static DrtConfigGroup drtConfigGroup;
	public static String coordinateSystem = "EPSG:25832";
	private static final String DEFAULT_POIS = "train_station,logistic,supermarket";
	private static final String DEFAULT_MODES = "estimatedDrt,teleportedWalk,pt,car";
	private static final String DEFAULT_TIMES = "21600,25200,28800,32400,36000,39600,43200,46800,50400,54000,57600,61200,64800,68400,72000,75600";
	private static final double DEFAULT_WAITING_TIME = 300;
	private static final double DEFAULT_SLOPE = 1.22;
	private static final double DEFAULT_INTERCEPT = 177.5;
	private static final double DEFAULT_ASC_DRT = 0.0;

	@CommandLine.Option(names = "--directory", required = true, description = "MATSim output directory.")
	private Path directory;

	@CommandLine.Option(names = "--pois", defaultValue = DEFAULT_POIS, description = "Comma-separated POI activity types.")
	private String pois;

	@CommandLine.Option(names = "--modes", defaultValue = DEFAULT_MODES, description = "Comma-separated accessibility modes.")
	private String modes;

	@CommandLine.Option(names = "--times", defaultValue = DEFAULT_TIMES, description = "Comma-separated times in seconds after midnight.")
	private String times;

	@CommandLine.Option(names = "--dashboard", description = "Calculate accessibility and generate dashboards afterwards.")
	private boolean dashboard;

	@CommandLine.Option(names = "--dashboard-only", description = "Generate dashboards from existing accessibility results without calculating accessibility.")
	private boolean dashboardOnly;

	@CommandLine.Option(names = "--use-default-drt-estimator", description = "Use the hard-coded DRT waiting-time and detour estimator instead of a service-probe file.")
	private boolean useDefaultDrtEstimator;

	public RunOfflineAccessibilityKelheim() {
	}

	public static void main(String[] args) {
		new RunOfflineAccessibilityKelheim().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		if (dashboard && dashboardOnly) {
			throw new IllegalArgumentException("--dashboard and --dashboard-only are mutually exclusive.");
		}
		if (!Files.isDirectory(directory)) {
			throw new IllegalArgumentException("MATSim output directory does not exist or is not a directory: " + directory);
		}

		List<String> relevantPois = parsePois(pois);
		List<Modes4Accessibility> accModes = parseModes(modes);
		List<Double> timesSeconds = parseTimes(times);
		String outputDir = directory.toString();

		if (!dashboardOnly) {
			AccessibilityConfigGroup accConfig = createAccessibilityConfig(timesSeconds, accModes);
			DrtEstimator drtEstimator = createDefaultDrtEstimator();
			DrtEstimator effectiveDrtEstimator = useDefaultDrtEstimator
				? drtEstimator
				: new CsvServiceQualityDrtEstimator(findServiceQualityProbe(directory).toString(), createStopNetwork());

			step2CalculateAccessibility(effectiveDrtEstimator, DEFAULT_ASC_DRT, relevantPois, accConfig, outputDir);
		}

		if (dashboard || dashboardOnly) {
			step3CreateDashboard(relevantPois, accModes, "11.87632,48.81992", outputDir);
		}

		return 0;
	}

	private static AccessibilityConfigGroup createAccessibilityConfig(List<Double> timesSeconds, List<Modes4Accessibility> accModes) {
		AccessibilityConfigGroup accConfig = new AccessibilityConfigGroup();
		accConfig.setTileSize_m(500);
		accConfig.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromShapeFile);
		accConfig.setShapeFileCellBasedAccessibility("input/shp/lk-kelheim/lk-kelheim.shp");
		accConfig.setTimeOfDay(new ArrayList<>(timesSeconds));
		accConfig.setWriteDrtStopPairs(false);
		for (Modes4Accessibility mode : Modes4Accessibility.values()) {
			accConfig.setComputingAccessibilityForMode(mode, accModes.contains(mode));
		}
		return accConfig;
	}

	private static DrtEstimator createDefaultDrtEstimator() {
		return new DirectTripBasedDrtEstimator.Builder()
			.setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(DEFAULT_WAITING_TIME))
			.setWaitingTimeDistributionGenerator(new NoDistribution())
			.setRideDurationEstimator(new ConstantRideDurationEstimator(DEFAULT_SLOPE, DEFAULT_INTERCEPT))
			.setRideDurationDistributionGenerator(new NoDistribution())
			.build();
	}

	static Path findServiceQualityProbe(Path directory) throws IOException {
		List<Path> probes;
		try (var files = Files.list(directory)) {
			probes = files.filter(Files::isRegularFile)
				.filter(path -> path.getFileName().toString().endsWith(".drt_service_quality_probes.csv")
					|| path.getFileName().toString().endsWith(".drt_service_quality_probes.csv.gz"))
				.toList();
		}
		if (probes.size() != 1) {
			throw new IllegalArgumentException("Expected exactly one service quality probe in " + directory + ", found " + probes.size()
				+ ". Supply --use-default-drt-estimator to use the hard-coded estimator.");
		}
		return probes.get(0);
	}

	static List<String> parsePois(String value) {
		List<String> result = parseCommaSeparated(value);
		if (result.isEmpty()) throw new IllegalArgumentException("--pois must not be empty.");
		return result;
	}

	static List<Modes4Accessibility> parseModes(String value) {
		try {
			List<Modes4Accessibility> result = parseCommaSeparated(value).stream().map(Modes4Accessibility::valueOf).toList();
			if (result.isEmpty()) throw new IllegalArgumentException("--modes must not be empty.");
			return result;
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid --modes value: " + value + ". Available values: " + List.of(Modes4Accessibility.values()), e);
		}
	}

	static List<Double> parseTimes(String value) {
		try {
			List<Double> result = parseCommaSeparated(value).stream().map(Double::parseDouble).toList();
			if (result.isEmpty() || result.stream().anyMatch(time -> time < 0)) {
				throw new IllegalArgumentException("--times must contain non-negative times.");
			}
			return result;
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid --times value: " + value, e);
		}
	}

	private static List<String> parseCommaSeparated(String value) {
		return java.util.Arrays.stream(value.split(","))
			.map(String::strip)
			.filter(item -> !item.isEmpty())
			.toList();
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
		String eventsFile = ApplicationUtils.matchInput("output_events.xml", Path.of(outputDir)).toString();
		String networkFile = ApplicationUtils.matchInput("output_network.xml", Path.of(outputDir)).toString();
		String transportScheduleFile = ApplicationUtils.matchInput("output_transitSchedule.xml", Path.of(outputDir)).toString();



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

	private static DrtStopNetwork createStopNetwork() {
		MutableScenario drtStopsScenario = (MutableScenario)ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(drtStopsScenario).readFile(stopsFile);
		ImmutableMap.Builder<org.matsim.api.core.v01.Id<DrtStopFacility>, DrtStopFacility> stops = ImmutableMap.builder();
		for (var stop : drtStopsScenario.getTransitSchedule().getFacilities().values()) {
			DrtStopFacility drtStop = new DrtStopFacilityImpl(
				org.matsim.api.core.v01.Id.create(stop.getId(), DrtStopFacility.class), stop.getLinkId(), stop.getCoord(), stop.getAttributes());
			stops.put(drtStop.getId(), drtStop);
		}
		ImmutableMap<org.matsim.api.core.v01.Id<DrtStopFacility>, DrtStopFacility> stopMap = stops.build();
		return () -> stopMap;
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
		group.setDefaultDashboards(SimWrapperConfigGroup.DefaultDashboardsMode.disabled);


		SimWrapper sw = SimWrapper.create(config)
			.addDashboard(new OverviewDashboardHeart(relevantPois, coordinateSystem));
		if (accModes.contains(Modes4Accessibility.car)) {
			sw.addDashboard(new AccessibilityDashboardHeart(coordinateSystem, relevantPois, Modes4Accessibility.car));
		}
		if (accModes.contains(Modes4Accessibility.pt)) {
			sw.addDashboard(new AccessibilityDashboardHeart(coordinateSystem, relevantPois, Modes4Accessibility.pt));
		}
		if (accModes.contains(Modes4Accessibility.estimatedDrt)) {
			sw.addDashboard(new AccessibilityDashboardHeart(coordinateSystem, relevantPois, Modes4Accessibility.estimatedDrt));
		}



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
