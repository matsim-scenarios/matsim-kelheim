package org.matsim.analysis.postAnalysis.drt;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math3.util.Precision;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.analysis.postAnalysis.traffic.TrafficAnalysis;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.common.util.DistanceUtils;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;

import java.io.FileWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.matsim.application.ApplicationUtils.globFile;

@CommandLine.Command(
		name = "analyze-drt-service",
		description = "Analyze DRT service quality"
)
public class DrtServiceQualityAnalysis implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(DrtServiceQualityAnalysis.class);
	private static final URL SHPFILE = IOUtils.resolveFileOrResource("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/projects/KelRide/AVServiceAreas/input/shp/kelheim-v2.0-drtZonalAnalysisSystem.shp");
	private static final String FEATURE_ORIGINS_ATTRIBUTE_NAME = "starts";
	private static final String FEATURE_DESTINATIONS_ATTRIBUTE_NAME = "ends";
	private static final String FEATURE_MEAN_WAIT_ATTRIBUTE_NAME = "meanWait";
	private static final String FEATURE_95PCT_WAIT_ATTRIBUTE_NAME = "95pctWait";
	@CommandLine.Option(names = "--directory", description = "path to matsim output directory", required = true)
	private Path directory;
	@CommandLine.Option(names = "--only-shape", defaultValue = "false", description = "only read drt legs file and write shp file")
	private boolean onlyShape;

	public static void main(String[] args) {
		new DrtServiceQualityAnalysis().execute(args);
	}

	@Override
	@SuppressWarnings("JavaNCSS")
	public Integer call() throws Exception {
		Path configPath = globFile(directory, "*output_config.*");
		Path networkPath = globFile(directory, "*output_network.*");
		Path eventPath = globFile(directory, "*output_events.*");
		Path outputFolder = Path.of(directory.toString() + "/analysis-drt-service-quality");

		if (!Files.exists(outputFolder)) {
			Files.createDirectory(outputFolder);
		}

		Config config = ConfigUtils.loadConfig(configPath.toString(), new MultiModeDrtConfigGroup(DrtWithExtensionsConfigGroup::new));
		int lastIteration = config.controler().getLastIteration();
		String runId = config.controler().getRunId();
		Path folderOfLastIteration = Path.of(directory.toString() + "/ITERS/it." + lastIteration);
		MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		List<String> modes = new ArrayList<>();
		for (DrtConfigGroup drtCfg : multiModeDrtConfigGroup.getModalElements()) {
			modes.add(drtCfg.getMode());
		}

		VehicleType vehicleTypeAv = VehicleUtils.createVehicleType(Id.create("av_type_for_route_calculation", VehicleType.class));
		vehicleTypeAv.setMaximumVelocity(5.0);
		Vehicle avVehicle = VehicleUtils.createVehicle(Id.create("dummy_av_vehicle", Vehicle.class), vehicleTypeAv);
		Network network = null;
		TravelTime travelTime = null;
		LeastCostPathCalculator router = null;
		if (!onlyShape) {
			network = NetworkUtils.readNetwork(networkPath.toString());
			travelTime = TrafficAnalysis.analyzeTravelTimeFromEvents(network, eventPath.toString());

			config.plansCalcRoute().setRoutingRandomness(0);
			TravelDisutility travelDisutility = new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, config)
					.createTravelDisutility(travelTime);
			router = new SpeedyALTFactory().
					createPathCalculator(network, travelDisutility, travelTime);
			// a quick fix for the AV speed calculation
		}

		for (String mode : modes) {
			Path tripsFile = globFile(folderOfLastIteration, "*drt_legs_" + mode + ".*");
			Path outputTripsPath = Path.of(outputFolder + "/" + mode + "_trips.tsv");
			Path outputStatsPath = Path.of(outputFolder + "/" + mode + "_KPI.tsv");

			List<Double> allWaitingTimes = new ArrayList<>();

			Map<SimpleFeature, ArrayList<Double>> shpWaitingTimes = null;
			Set<SimpleFeature> shpFeatures = new HashSet<>(ShapeFileReader.getAllFeatures(SHPFILE));
			for (SimpleFeature shpFeature : shpFeatures) {
				shpFeature.setAttribute(FEATURE_ORIGINS_ATTRIBUTE_NAME, 0.d);
				shpFeature.setAttribute(FEATURE_DESTINATIONS_ATTRIBUTE_NAME, 0.d);
			}
			shpWaitingTimes = shpFeatures.stream().collect(Collectors.toMap(feature -> feature, feature -> new ArrayList<Double>()));

			List<Double> onboardDelayRatios = new ArrayList<>();
			List<Double> detourDistanceRatios = new ArrayList<>();
			List<Double> euclideanDistances = new ArrayList<>();
			List<Double> directDistances = new ArrayList<>();
			List<Double> inVehicleTravelTimes = new ArrayList<>();
			List<Double> totalTravelTimes = new ArrayList<>();

			CSVPrinter tsvWriter = null;
			if (!onlyShape) {
				tsvWriter = new CSVPrinter(new FileWriter(outputTripsPath.toString()), CSVFormat.TDF);
				List<String> titleRow = Arrays.asList(
						"departure_time", "waiting_time", "in_vehicle_time", "total_travel_time",
						"est_direct_in_vehicle_time", "actual_travel_distance", "est_direct_drive_distance",
						"euclidean_distance", "onboard_delay_ratio", "detour_distance_ratio");
				tsvWriter.printRecord(titleRow);
			}

			int numOfTrips = 0;
			try (CSVParser parser = new CSVParser(Files.newBufferedReader(tripsFile),
					CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
				for (CSVRecord row : parser.getRecords()) {
					double waitingTime = Double.parseDouble(row.get(9));

					if (!onlyShape) {
						Link fromLink = network.getLinks().get(Id.createLinkId(row.get(3)));
						Link toLink = network.getLinks().get(Id.createLinkId(row.get(6)));
						double departureTime = Double.parseDouble(row.get(0));
						Vehicle vehicle = null;
						if (mode.equals("av")) {
							vehicle = avVehicle;
						}
						LeastCostPathCalculator.Path path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(),
								departureTime, null, vehicle);
						path.links.add(toLink);
						double estimatedDirectInVehicleTime = path.travelTime + travelTime.getLinkTravelTime(toLink, path.travelTime + departureTime, null, null) + 2;
						double estimatedDirectTravelDistance = path.links.stream().map(Link::getLength).mapToDouble(l -> l).sum();
						double actualInVehicleTime = Double.parseDouble(row.get(11));
						double totalTravelTime = waitingTime + actualInVehicleTime;
						double actualTravelDistance = Double.parseDouble(row.get(12));
						double euclideanDistance = DistanceUtils.calculateDistance(fromLink.getToNode().getCoord(), toLink.getToNode().getCoord());
						double onboardDelayRatio = actualInVehicleTime / estimatedDirectInVehicleTime - 1;
						double detourRatioDistance = actualTravelDistance / estimatedDirectTravelDistance - 1;

						allWaitingTimes.add(waitingTime);
						onboardDelayRatios.add(onboardDelayRatio);
						detourDistanceRatios.add(detourRatioDistance);
						euclideanDistances.add(euclideanDistance);
						directDistances.add(estimatedDirectTravelDistance);
						inVehicleTravelTimes.add(actualInVehicleTime);
						totalTravelTimes.add(totalTravelTime);

						List<String> outputRow = new ArrayList<>();
						outputRow.add(Double.toString(departureTime));
						outputRow.add(Double.toString(waitingTime));
						outputRow.add(Double.toString(actualInVehicleTime));
						outputRow.add(Double.toString(totalTravelTime));
						outputRow.add(Double.toString(estimatedDirectInVehicleTime));
						outputRow.add(Double.toString(actualTravelDistance));
						outputRow.add(Double.toString(estimatedDirectTravelDistance));
						outputRow.add(Double.toString(euclideanDistance));
						outputRow.add(Double.toString(onboardDelayRatio));
						outputRow.add(Double.toString(detourRatioDistance));

						tsvWriter.printRecord(outputRow);
					}

					//-------------spatial analysis
					Coord fromCoord = new Coord(Double.parseDouble(row.get(4)), Double.parseDouble(row.get(5)));
					Coord toCoord = new Coord(Double.parseDouble(row.get(7)), Double.parseDouble(row.get(8)));

					Set<SimpleFeature> originFeatures = getSimpleFeaturesContainingCoord(shpWaitingTimes.keySet(), fromCoord);
					//waiting time is monitored for the geometry containing the from coordinate
					if (originFeatures != null) {
						if (originFeatures.size() > 1) {
							log.warn("from coordinate " + fromCoord + " appears to be covered by several SimpleFeatures. It will be part of all of their statistics.\n" +
									"csv record = " + row);
						}
						for (SimpleFeature originFeature : originFeatures) {
							shpWaitingTimes.get(originFeature).add(waitingTime);
							originFeature.setAttribute(FEATURE_ORIGINS_ATTRIBUTE_NAME, (int) originFeature.getAttribute(FEATURE_ORIGINS_ATTRIBUTE_NAME) + 1);
						}
					}
					Set<SimpleFeature> destinationFeatures = getSimpleFeaturesContainingCoord(shpWaitingTimes.keySet(), toCoord);
					if (destinationFeatures != null) {
						for (SimpleFeature destinationFeature : destinationFeatures) {
							shpWaitingTimes.get(destinationFeature).add(waitingTime);
							destinationFeature.setAttribute(FEATURE_DESTINATIONS_ATTRIBUTE_NAME, (int) destinationFeature.getAttribute(FEATURE_DESTINATIONS_ATTRIBUTE_NAME) + 1);
						}
					}

					numOfTrips++;
				}
			}
			tsvWriter.close();

			if (!onlyShape) {

				CSVPrinter tsvWriterKPI = new CSVPrinter(new FileWriter(outputStatsPath.toString()), CSVFormat.TDF);
				List<String> titleRowKPI = Arrays.asList(
						"number_of_requests", "waiting_time_mean", "waiting_time_median", "waiting_time_95_percentile",
						"onboard_delay_ratio_mean", "detour_distance_ratio_mean", "trips_euclidean_distance_mean", "trips_direct_network_distance_mean",
						"in_vehicle_travel_time_mean", "total_travel_time_mean");
				tsvWriterKPI.printRecord(titleRowKPI);

//            List<Double> allWaitingTimes = waitingTimes.values().stream().flatMap(List::stream).collect(Collectors.toList());
				int meanWaitingTime = (int) allWaitingTimes.stream().mapToDouble(w -> w).average().orElse(-1);
				int medianWaitingTime = (int) StatUtils.percentile(allWaitingTimes.stream().mapToDouble(t -> t).toArray(), 50);
				int waitingTime95Percentile = (int) StatUtils.percentile(allWaitingTimes.stream().mapToDouble(t -> t).toArray(), 95);

				double meanDelayRatio = Precision.round(onboardDelayRatios.stream().mapToDouble(r -> r).average().orElse(-1), 2);
				double meanDetourDistanceRatio = Precision.round(detourDistanceRatios.stream().mapToDouble(d -> d).average().orElse(-1), 2);

				double meanEuclideanDistance = Precision.round(euclideanDistances.stream().mapToDouble(r -> r).average().orElse(-1), 2);
				double meanDirectNetworkDistance = Precision.round(directDistances.stream().mapToDouble(r -> r).average().orElse(-1), 2);
				double meanInVehicleTravelTime = Precision.round(inVehicleTravelTimes.stream().mapToDouble(r -> r).average().orElse(-1), 2);
				double meanTotalTravelTime = Precision.round(totalTravelTimes.stream().mapToDouble(r -> r).average().orElse(-1), 2);

				List<String> outputKPIRow = new ArrayList<>();
				outputKPIRow.add(Integer.toString(numOfTrips));
				outputKPIRow.add(Integer.toString(meanWaitingTime));
				outputKPIRow.add(Integer.toString(medianWaitingTime));
				outputKPIRow.add(Integer.toString(waitingTime95Percentile));
				outputKPIRow.add(Double.toString(meanDelayRatio));
				outputKPIRow.add(Double.toString(meanDetourDistanceRatio));
				outputKPIRow.add(Double.toString(meanEuclideanDistance));
				outputKPIRow.add(Double.toString(meanDirectNetworkDistance));
				outputKPIRow.add(Double.toString(meanInVehicleTravelTime));
				outputKPIRow.add(Double.toString(meanTotalTravelTime));

				tsvWriterKPI.printRecord(outputKPIRow);

				tsvWriterKPI.close();
			}

			//spatial analysis
			shpWaitingTimes.forEach((feature, waitingTimes) -> {
						feature.setAttribute(FEATURE_MEAN_WAIT_ATTRIBUTE_NAME, StatUtils.mean((waitingTimes.stream().mapToDouble(t -> t).toArray())));
						feature.setAttribute(FEATURE_95PCT_WAIT_ATTRIBUTE_NAME, StatUtils.percentile(waitingTimes.stream().mapToDouble(t -> t).toArray(), 95));
					}
			);
			ShapeFileWriter.writeGeometries(shpWaitingTimes.keySet(), outputFolder + "/" + mode + "_serviceZones_waitStats.shp");
		}
		return 0;
	}

	private Set<SimpleFeature> getSimpleFeaturesContainingCoord(Set<SimpleFeature> simpleFeatureSet, Coord coord) {
		return simpleFeatureSet.stream()
				.filter(feature -> ShpGeometryUtils.isCoordInGeometries(coord, List.of((Geometry) feature.getDefaultGeometry())))
				.collect(Collectors.toSet());
	}

}
