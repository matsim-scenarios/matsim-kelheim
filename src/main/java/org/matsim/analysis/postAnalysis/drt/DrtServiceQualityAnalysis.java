package org.matsim.analysis.postAnalysis.drt;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math.stat.StatUtils;
import org.matsim.analysis.postAnalysis.traffic.TrafficAnalysis;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.common.util.DistanceUtils;
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
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import picocli.CommandLine;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.matsim.application.ApplicationUtils.globFile;

@CommandLine.Command(
        name = "analyze-drt-service",
        description = "Analyze DRT service quality"
)
public class DrtServiceQualityAnalysis implements MATSimAppCommand {
    @CommandLine.Option(names = "--directory", description = "path to network file", required = true)
    private Path directory;

    public static void main(String[] args) {
        new DrtServiceQualityAnalysis().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        Path configPath = globFile(directory, "*output_config.*");
        Path networkPath = globFile(directory, "*output_network.*");
        Path eventPath = globFile(directory, "*output_events.*");
        Path outputFolder = Path.of(directory.toString() + "/analysis-drt-service-quality");

        if (!Files.exists(outputFolder)) {
            Files.createDirectory(outputFolder);
        }

        Config config = ConfigUtils.loadConfig(configPath.toString());
        int lastIteration = config.controler().getLastIteration();
        String runId = config.controler().getRunId();
        Path folderOfLastIteration = Path.of(directory.toString() + "/ITERS/it." + lastIteration);
        MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
        List<String> modes = new ArrayList<>();
        for (DrtConfigGroup drtCfg : multiModeDrtConfigGroup.getModalElements()) {
            modes.add(drtCfg.getMode());
        }

        Network network = NetworkUtils.readNetwork(networkPath.toString());
        TravelTime travelTime = TrafficAnalysis.analyzeTravelTimeFromEvents(network, eventPath.toString());
        config.plansCalcRoute().setRoutingRandomness(0);
        TravelDisutility travelDisutility = new RandomizingTimeDistanceTravelDisutilityFactory
                (TransportMode.car, config).createTravelDisutility(travelTime);
        LeastCostPathCalculator router = new SpeedyALTFactory().
                createPathCalculator(network, travelDisutility, travelTime);

        // a quick fix for the AV speed calculation
        VehicleType vehicleTypeAv = VehicleUtils.createVehicleType(Id.create("av_type_for_route_calculation", VehicleType.class));
        vehicleTypeAv.setMaximumVelocity(5.0);
        Vehicle avVehicle = VehicleUtils.createVehicle(Id.create("dummy_av_vehicle", Vehicle.class), vehicleTypeAv);

        for (String mode : modes) {
            Path tripsFile = globFile(folderOfLastIteration, "*drt_legs_" + mode + ".*");
            Path outputTripsPath = Path.of(outputFolder + "/" + mode + "_trips.tsv");
            Path outputStatsPath = Path.of(outputFolder + "/" + mode + "_KPI.tsv");

            List<Double> waitingTimes = new ArrayList<>();
            List<Double> onboardDelayRatios = new ArrayList<>();
            List<Double> detourDistanceRatios = new ArrayList<>();
            List<Double> euclideanDistances = new ArrayList<>();
            List<Double> directDistances = new ArrayList<>();

            CSVPrinter tsvWriter = new CSVPrinter(new FileWriter(outputTripsPath.toString()), CSVFormat.TDF);
            List<String> titleRow = Arrays.asList
                    ("departure_time", "waiting_time", "in_vehicle_time", "total_travel_time",
                            "est_direct_in_vehicle_time", "actual_travel_distance", "est_direct_drive_distance",
                            "euclidean_distance", "onboard_delay_ratio", "detour_distance_ratio");
            tsvWriter.printRecord(titleRow);

            int numOfTrips = 0;
            try (CSVParser parser = new CSVParser(Files.newBufferedReader(tripsFile),
                    CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
                for (CSVRecord record : parser.getRecords()) {
                    Link fromLink = network.getLinks().get(Id.createLinkId(record.get(3)));
                    Link toLink = network.getLinks().get(Id.createLinkId(record.get(6)));
                    double departureTime = Double.parseDouble(record.get(0));
                    Vehicle vehicle = null;
                    if (mode.equals("av")) {
                        vehicle = avVehicle;
                    }
                    LeastCostPathCalculator.Path path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getToNode(),
                            departureTime, null, vehicle);
                    double estimatedDirectInVehicleTime = path.travelTime;
                    double estimatedDirectTravelDistance = path.links.stream().map(Link::getLength).mapToDouble(l -> l).sum();
                    double waitingTime = Double.parseDouble(record.get(9));
                    double actualInVehicleTime = Double.parseDouble(record.get(11));
                    double totalTravelTime = waitingTime + actualInVehicleTime;
                    double actualTravelDistance = Double.parseDouble(record.get(12));
                    double euclideanDistance = DistanceUtils.calculateDistance(fromLink.getToNode().getCoord(), toLink.getToNode().getCoord());
                    double onboardDelayRatio = actualInVehicleTime / estimatedDirectInVehicleTime - 1;
                    double detourRatioDistance = actualTravelDistance / estimatedDirectTravelDistance - 1;

                    waitingTimes.add(waitingTime);
                    onboardDelayRatios.add(onboardDelayRatio);
                    detourDistanceRatios.add(detourRatioDistance);
                    euclideanDistances.add(euclideanDistance);
                    directDistances.add(estimatedDirectTravelDistance); //TODO there is currently discrepancy between DRT legs and calculated route in the post analysis

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

                    numOfTrips++;
                }
            }
            tsvWriter.close();

            CSVPrinter tsvWriterKPI = new CSVPrinter(new FileWriter(outputStatsPath.toString()), CSVFormat.TDF);
            List<String> titleRowKPI = Arrays.asList
                    ("number_of_requests", "waiting_time_mean", "waiting_time_median", "waiting_time_95_percentile",
                            "onboard_delay_ratio_mean", "detour_distance_ratio_mean", "trips_euclidean_distance_mean", "trips_direct_network_distance_mean");
            tsvWriterKPI.printRecord(titleRowKPI);

            int meanWaitingTime = (int) waitingTimes.stream().mapToDouble(w -> w).average().orElse(-1);
            int medianWaitingTime = (int) StatUtils.percentile(waitingTimes.stream().mapToDouble(t -> t).toArray(), 50);
            int waitingTime95Percentile = (int) StatUtils.percentile(waitingTimes.stream().mapToDouble(t -> t).toArray(), 95);

            DecimalFormat formatter = new DecimalFormat("0.00");
            String meanDelayRatio = formatter.format(onboardDelayRatios.stream().mapToDouble(r -> r).average().orElse(-1));
            String meanDetourDistanceRatio = formatter.format(detourDistanceRatios.stream().mapToDouble(d -> d).average().orElse(-1));

            String meanEuclideanDistance = formatter.format(euclideanDistances.stream().mapToDouble(r -> r).average().orElse(-1));
            String meanDirectNetworkDistance = formatter.format(directDistances.stream().mapToDouble(r -> r).average().orElse(-1));

            List<String> outputKPIRow = new ArrayList<>();
            outputKPIRow.add(Integer.toString(numOfTrips));
            outputKPIRow.add(Integer.toString(meanWaitingTime));
            outputKPIRow.add(Integer.toString(medianWaitingTime));
            outputKPIRow.add(Integer.toString(waitingTime95Percentile));
            outputKPIRow.add(meanDelayRatio);
            outputKPIRow.add(meanDetourDistanceRatio);
            outputKPIRow.add(meanEuclideanDistance);
            outputKPIRow.add(meanDirectNetworkDistance);  //TODO read this value carefully

            tsvWriterKPI.printRecord(outputKPIRow);

            tsvWriterKPI.close();
        }
        return 0;
    }
}
