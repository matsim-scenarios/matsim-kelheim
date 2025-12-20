package org.matsim.analysis.postAnalysis.accessbility;

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
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.application.ApplicationUtils;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityFromEvents;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.estimator.impl.DirectTripBasedDrtEstimator;
import org.matsim.contrib.drt.estimator.impl.distribution.NoDistribution;
import org.matsim.contrib.drt.estimator.impl.trip_estimation.ConstantRideDurationEstimator;
import org.matsim.contrib.drt.estimator.impl.waiting_time_estimation.ConstantWaitingTimeEstimator;
import org.matsim.contrib.drt.extension.dashboards.DrtDashboard;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.facilities.*;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Table;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.matsim.core.scenario.ScenarioUtils.createScenario;



// Time Req: 7 times (for pt) | 2 pois | 5 modes | 250m grid --- 10:52 - 11:16 (24 minutes)
// Time Req: 8 times (for pt) | 2 pois | 5 modes | 500m grid --- 12:19 - 12:30 (11 minutes)
// Time Req: 6 + 11 times (for pt) | 2 pois | 5 modes | 500m grid --- 12:37 - 13:00 (23 minutes)
// Time Req: 6 + 11 times (for pt) | 2 pois | 5 modes | 250m grid --- 14:50 - 15:55 (65 minutes)
// Time Req: 6 + 22 times (for pt) | 3 pois | 5 modes | 500 grid --- 16:21 - 17:27 (66 minutes)


/**
 * Run this class to calculate accessibility for the Kelheim scenario for different modes (including DRT). This class is meant to be run after
 * a simulation (offline).
 */
public class RunOfflineAccessibilityKelheim {

	private static final Logger log = LogManager.getLogger(RunOfflineAccessibilityKelheim.class);
	public static String stopsFileLand = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/kelheim-drt-accessibility-JB-master/input/drt-stops-land.xml";
	public static String stopsFileStadt = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/kelheim-v3.0/input/kelheim-v3.0-drt-stops.xml";
	public static String stopsFileStadtUndLand = "../public-svn/matsim/scenarios/countries/de/kelheim/kelheim-drt-accessibility-JB-master/input/drt-stops-stadt-und-land.xml";

	public static String stopsFileStadtUndLandUndNeustadt = "../public-svn/matsim/scenarios/countries/de/kelheim/kelheim-drt-accessibility-JB-master/input/drt-stops-stadt-und-land-und-neustadt.xml";

	public static String stopsFile;
	public static DrtConfigGroup drtConfigGroup;
	private static String outputDir;
	public static String coordinateSystem = "EPSG:25832";

	protected RunOfflineAccessibilityKelheim() {
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
		List<String> relevantPois = List.of("train_station", "amazon", "supermarket");
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
		List<Double> timesHour = List.of(7.5, 8.0, 8.5, 24.0);


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


		// BASE
		double waitingTime = 300;
		double slope = 1.22;
		double intercept = 177.5;
		double ascDrt = 0.0;
		outputDir = "../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/2025-11-19-amazon/";

//		// A1: low ASC,
//		double waitingTime = 0;
//		double slope = 1;
//		double intercept = 0;
//		double ascDrt = -2.5;
//		outputDir = "../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/2025-09-19-a1/";


		// A2:

//		double waitingTime = 300;
//		double slope = 1.22;
//		double intercept = 177.5;
//		double ascDrt = -2.5;
//		outputDir = "../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/2025-09-19-a2/";
////
//		// A3
//		double waitingTime = 15*60;
//		double slope = 3;
//		double intercept = 0;
//		double ascDrt = -2.5;
//		outputDir = "../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/2025-09-19-a3/";

		// B1:
//		double waitingTime = 0;
//		double slope = 1;
//		double intercept = 0;
//		double ascDrt = 0;
//		outputDir = "../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/2025-09-19-b1/";


//		// B2:
//		double waitingTime = 300;
//		double slope = 1.22;
//		double intercept = 177.5;
//		double ascDrt = 0;
//		outputDir = "../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/2025-09-19-b2/";
//
//
//		// B3:
//		double waitingTime = 15*60;
//		double slope = 3;
//		double intercept = 0;
//		double ascDrt = 0;
//		outputDir = "../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/2025-09-19-b3/";

//
//		// C1:
//		double waitingTime = 0;
//		double slope = 1;
//		double intercept = 0;
//		double ascDrt = 2.5;
//		outputDir = "../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/2025-09-19-c1/";

//
//		//C2:
//		double waitingTime = 300;
//		double slope = 1.22;
//		double intercept = 177.5;
//		double ascDrt = 2.5;
//		outputDir = "../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/2025-09-19-c2/";
//
//		// C3:
//		double waitingTime = 15*60;
//		double slope = 3;
//		double intercept = 0;
//		double ascDrt = 2.5;
//		outputDir = "../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/2025-09-19-c3/";


		FileUtils.copyDirectory(dirToCopy, new File(outputDir));

		DrtEstimator drtEstimator = new DirectTripBasedDrtEstimator.Builder()
			.setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(waitingTime))
			.setWaitingTimeDistributionGenerator(new NoDistribution())
			.setRideDurationEstimator(new ConstantRideDurationEstimator(slope, intercept))
			.setRideDurationDistributionGenerator(new NoDistribution())
			.build();


//		step2CalculateAccessibility(drtEstimator, ascDrt, relevantPois, accConfig);

		// Part 3: Create Dashboard
		step3CreateDashboard(relevantPois, accModes, mapCenterString);

	}







	private static EstimatorParameters step1GenerateParams() {

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



	private static void step2CalculateAccessibility(DrtEstimator drtEstimator, Double ascDrt, List<String> relevantPois, ConfigGroup accConfig) {

		// input files
		String eventsFile = ApplicationUtils.matchInput("output_events.xml.gz", Path.of(outputDir)).toString();
		String networkFile = ApplicationUtils.matchInput("output_network.xml.gz", Path.of(outputDir)).toString();
		String transportScheduleFile = ApplicationUtils.matchInput("output_transitSchedule.xml.gz", Path.of(outputDir)).toString();
		String poiFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/kelheim-drt-accessibility-JB-master/input/pois_complete.csv";



		if (stopsFile.equals(stopsFileStadtUndLand) && !Files.exists(Path.of(stopsFileStadtUndLand))) {

			mergeStadtUndLand();

		}

		if(stopsFile.equals(stopsFileStadtUndLandUndNeustadt) && !Files.exists(Path.of(stopsFileStadtUndLandUndNeustadt))){


			mergeNeustadt(networkFile);


		}




		// CONFIG
		//global
		final Config config = ConfigUtils.createConfig();

//		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);


		config.controller().setLastIteration(0);
//		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOutputDirectory(outputDir);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		config.routing().setRoutingRandomness(0.);

		config.global().setCoordinateSystem("EPSG:25832");

		config.network().setInputFile(networkFile);


		// transit
		config.transit().setTransitScheduleFile(transportScheduleFile);

		// default config: (see kelheim-1000-200) --> looks better, but with holes
		// search: 1000 (1km)
		// extension: 200 (200m)
		// maxBeelineWalkConnectionDistance 100.0

		// kelheim config:
		// search: 1000 (1km)
		// extension: 500 (500m)
		// maxBeelineWalkConnectionDistance 300.0

		//closest stop: (see kelheim-1-0)
		// search: 1 (1m)
		// extension: 0 (0m)

		// WAITING FOR PT:
		// default: -6 --> -12 (including time lost)
		// kelheim: -1.2 --> -7.2
		// neutral: 0.0
		config.scoring().setMarginalUtlOfWaitingPt_utils_hr(0.0);


//		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.defaultVehicle);


		config.transitRouter().setSearchRadius(1_000);
		config.transitRouter().setExtensionRadius(500);
		config.transitRouter().setMaxBeelineWalkConnectionDistance(300);

		//newC
//		config.transitRouter().setSearchRadius(30_000);
//		config.transitRouter().setExtensionRadius(500);
//		config.transitRouter().setMaxBeelineWalkConnectionDistance(300);

		// change walk speed to match kelheim scenario
		config.routing().getTeleportedModeParams().get(TransportMode.walk).setTeleportedModeSpeed(3.8 / 3.6);

		// change scoring default to match kelheim scenario
		ScoringConfigGroup.ModeParams drtParams = new ScoringConfigGroup.ModeParams(TransportMode.drt);
		drtParams.setConstant(ascDrt);
		drtParams.setMarginalUtilityOfDistance(-2.5E-4);
		drtParams.setMarginalUtilityOfTraveling(0.0);
		config.scoring().addModeParams(drtParams);

		//
//		ScoringConfigGroup.ModeParams carParams = config.scoring().getModes().get(TransportMode.car);;
//		carParams.setMarginalUtilityOfDistance(-2.5E-4);
//		config.scoring().addModeParams(carParams);

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

		drtConfigGroup.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().maxWalkDistance = 100000.;

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
		readPoiCsv(scenario.getActivityFacilities(), poiFile);

		ActivityFacilitiesFactory facilityFactory = scenario.getActivityFacilities().getFactory();
		Coordinate coordinateWgs84 = new Coordinate(12.008155054611294, 48.85333131246612);
		Coordinate coordinate25832;
		try {
			coordinate25832 = transformCoordinate(CRS.decode("EPSG:4326", true), CRS.decode("EPSG:25832"), coordinateWgs84);
		} catch (TransformException | FactoryException e) {
			throw new RuntimeException(e);
		}

		ActivityFacility logisticFac = facilityFactory.createActivityFacility(Id.create("logistic_facility", ActivityFacility.class), MGC.coordinate2Coord(coordinate25832));
		logisticFac.addActivityOption(facilityFactory.createActivityOption("logistic"));

		scenario.getActivityFacilities().addActivityFacility(logisticFac);


		ActivityFacility amazonFacility = facilityFactory.createActivityFacility(Id.create("amazon_facility", ActivityFacility.class), new Coord(715535.90,5410431.39));
		amazonFacility.addActivityOption(facilityFactory.createActivityOption("amazon"));
		scenario.getActivityFacilities().addActivityFacility(amazonFacility);

		AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder(scenario, eventsFile, relevantPois);

		// configure DRT Estimator with wait time, and ride time parameters



		builder.addDrtEstimator(drtEstimator);


		builder.build().run();

	}

	private static void mergeNeustadt(String networkFile) {
		assert (Files.exists(Path.of(stopsFileStadtUndLand)));
		Config config = ConfigUtils.createConfig();
//		config.network().setInputFile();
		Scenario scenarioUmland = createScenario(config);
		TransitScheduleReader transitScheduleReader = new TransitScheduleReader(scenarioUmland);
		transitScheduleReader.readFile(stopsFileStadtUndLand);
		TransitScheduleFactory tsf = scenarioUmland.getTransitSchedule().getFactory();


		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenarioUmland.getNetwork());
		networkReader.readFile(networkFile);


		Network subNetwork = NetworkUtils.createNetwork(config.network());
		new TransportModeNetworkFilter(scenarioUmland.getNetwork()).filter(subNetwork, Set.of(TransportMode.car));

		System.out.println("hello");
		try {
			// 1. Read CSV
			Table stops = Table.read().csv("/Users/jakob/git/public-svn/matsim/scenarios/countries/de/kelheim/kelheim-drt-accessibility-JB-master/input/kexi_bediengebiet___neustadt_a_d_donau.csv");

			// 2. Extract latitude and longitude columns
			IntColumn idCol = stops.intColumn("haltestellennummer");
			DoubleColumn latCol = stops.doubleColumn("Latitude");
			DoubleColumn lonCol = stops.doubleColumn("Longitude");

			// 3. Print or process them
			for (int i = 0; i < stops.rowCount(); i++) {
				Id<TransitStopFacility> id = Id.create(idCol.getInt(i) + "-neustadt", TransitStopFacility.class);

				// there is a single case of a Haltestellen Nummer not being unique (Number 345 occurs twice...)
				if (scenarioUmland.getTransitSchedule().getFacilities().containsKey(id)) {
					id =  Id.create(idCol.getInt(i) + "b-neustadt", TransitStopFacility.class);
				}


				double latitude = latCol.getDouble(i);
				double longitude = lonCol.getDouble(i);
				Coordinate coordinateWgs84 = new Coordinate(latitude, longitude);
				Coordinate coordinate25832 = transformCoordinate(CRS.decode("EPSG:4326", false), CRS.decode("EPSG:25832"), coordinateWgs84);

				Coord coord = CoordUtils.createCoord(coordinate25832);
				TransitStopFacility transitStopFacility = tsf.createTransitStopFacility(id, coord, false);

				transitStopFacility.setLinkId(NetworkUtils.getNearestLink(subNetwork, coord).getId());

				scenarioUmland.getTransitSchedule().addStopFacility(transitStopFacility);

			}

		} catch (TransformException | FactoryException e) {
			e.printStackTrace();
		}

		TransitScheduleWriter transitScheduleWriter = new TransitScheduleWriter(scenarioUmland.getTransitSchedule());
		transitScheduleWriter.writeFile(stopsFileStadtUndLandUndNeustadt);
	}

	private static void mergeStadtUndLand() {

		// read umland transit schedule
		Scenario scenarioUmland = createScenario(ConfigUtils.createConfig());
		TransitScheduleReader transitScheduleReader = new TransitScheduleReader(scenarioUmland);
		transitScheduleReader.readFile(stopsFileLand);


		Scenario scenarioCity = createScenario(ConfigUtils.createConfig());
		TransitScheduleReader transitScheduleReaderCity = new TransitScheduleReader(scenarioCity);
		transitScheduleReaderCity.readFile(stopsFileStadt);
		System.out.println(scenarioCity.getTransitSchedule().getFacilities().size());
		Set<Id<TransitStopFacility>> idsCity = scenarioCity.getTransitSchedule().getFacilities().keySet();


		for (Id<TransitStopFacility> id : idsCity) {
			TransitStopFacility stopCity = scenarioCity.getTransitSchedule().getFacilities().get(id);
			TransitStopFacility stopCityCopy = scenarioUmland.getTransitSchedule().getFactory().createTransitStopFacility(
				Id.create(id.toString() + "-city", TransitStopFacility.class),
				stopCity.getCoord(),
				stopCity.getIsBlockingLane()
			);
			stopCityCopy.setLinkId(stopCity.getLinkId());

			scenarioUmland.getTransitSchedule().addStopFacility(
				stopCityCopy
			);
		}

		TransitScheduleWriter transitScheduleWriter = new TransitScheduleWriter(scenarioUmland.getTransitSchedule());
		transitScheduleWriter.writeFile(stopsFileStadtUndLand);
	}

	private static void step3CreateDashboard(List<String> relevantPois, List<Modes4Accessibility> accModes, String mapCenterString) {

		final Config config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory(outputDir);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);


		//CONFIG
		config.controller().setLastIteration(0);
		config.controller().setWritePlansInterval(-1);
		config.controller().setWriteEventsInterval(-1);
		config.global().setCoordinateSystem("EPSG:25832");



		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.none);

		//simwrapper
		SimWrapperConfigGroup group = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		group.setSampleSize(0.001);
		if (mapCenterString != null) {
			group.defaultParams().setMapCenter(mapCenterString);
		}
		group.setDefaultDashboards(SimWrapperConfigGroup.Mode.disabled);


		coordinateSystem = config.global().getCoordinateSystem();
		SimWrapper sw = SimWrapper.create(config)
			.addDashboard(new OverviewDashboardHeart(relevantPois, coordinateSystem))
			.addDashboard(new AccessibilityDashboardHeart(coordinateSystem, relevantPois, Modes4Accessibility.car))
			.addDashboard(new AccessibilityDashboardHeart(coordinateSystem, relevantPois, Modes4Accessibility.pt))
			.addDashboard(new AccessibilityDashboardHeart(coordinateSystem, relevantPois, Modes4Accessibility.estimatedDrt));

//		for (String poi : relevantPois) {
//			sw.addDashboard(new AccessibilityDashboardHeart(config.global().getCoordinateSystem(), poi, accModes));
//		}


		boolean append = false;
		try {
			sw.generate(Path.of(outputDir), append);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		sw.run(Path.of(outputDir));






	}


	private static Coordinate transformCoordinate(CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS, Coordinate sourceCoordinate) throws TransformException, FactoryException {

		// Create transform
		boolean lenient = true;
		MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, lenient);

		// Create coordinate
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
		Point sourcePoint = geometryFactory.createPoint(sourceCoordinate);

		// Transform
		Point targetPoint = (Point) org.geotools.geometry.jts.JTS.transform(sourcePoint, transform);

		return targetPoint.getCoordinate();
	}

	private static void readPoiCsv(ActivityFacilities activityFacilities, String filePath) {

		ActivityFacilitiesFactory af = activityFacilities.getFactory();
		HttpURLConnection connection;
		try {
			connection = (HttpURLConnection) new URL(filePath).openConnection();
			connection.setRequestMethod("GET");
		} catch (ProtocolException | MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		try (CSVParser parser = new CSVParser(new BufferedReader(new InputStreamReader(connection.getInputStream())),

			CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader())) {

			for (CSVRecord csvRecord : parser) {

				String id = csvRecord.get("id");
				double x = Double.parseDouble(csvRecord.get("x"));
				double y = Double.parseDouble(csvRecord.get("y"));
				String type = csvRecord.get("type");
				ActivityFacility fac = af.createActivityFacility(Id.create(id, ActivityFacility.class), new Coord(x, y));
				ActivityOption ao = af.createActivityOption(type);
				fac.addActivityOption(ao);
				activityFacilities.addActivityFacility(fac);


			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
