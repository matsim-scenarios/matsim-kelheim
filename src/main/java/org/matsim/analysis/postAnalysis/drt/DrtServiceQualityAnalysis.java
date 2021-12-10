package org.matsim.analysis.postAnalysis.drt;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.matsim.analysis.postAnalysis.traffic.TrafficAnalysis;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.common.util.DistanceUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import picocli.CommandLine;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @CommandLine.Option(names = "--drt-modes", description = "drt modes in the scenario. Separate with , ", defaultValue = "drt")
    private String drtModes;

    public static void main(String[] args) {
        new DrtServiceQualityAnalysis().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        String[] modes = drtModes.split(",");
        Path configPath = globFile(directory, "*output_config.*");
        Path networkPath = globFile(directory, "*output_network.*");
        Path eventPath = globFile(directory, "*output_events.*");
        Path outputFolder = Path.of(directory.toString() + "/drt-service-quality-analysis");

        if (!Files.exists(outputFolder)) {
            Files.createDirectory(outputFolder);
        }

        Config config = ConfigUtils.loadConfig(configPath.toString());
        int lastIteration = config.controler().getLastIteration();
        String runId = config.controler().getRunId();
        Path folderOfLastIteration = Path.of(directory.toString() + "/ITERS/it." + lastIteration);


        Network network = NetworkUtils.readTimeInvariantNetwork(networkPath.toString());
        TravelTime travelTime = TrafficAnalysis.analyzeTravelTimeFromEvents(network, eventPath.toString());
        config.plansCalcRoute().setRoutingRandomness(0);
        TravelDisutility travelDisutility = new RandomizingTimeDistanceTravelDisutilityFactory
                (TransportMode.car, config).createTravelDisutility(travelTime);
        LeastCostPathCalculator router = new SpeedyALTFactory().
                createPathCalculator(network, travelDisutility, travelTime);


        for (String mode : modes) {
            Path tripsFile = globFile(folderOfLastIteration, "*drt_legs_" + mode + ".*");
            Path outputTripsPath = Path.of(outputFolder.toString() + "/" + mode + "_trips.tsv");
            Path outputStatsPath = Path.of(outputFolder.toString() + "/" + mode + "_KPI.tsv");

            CSVPrinter tsvWriter = new CSVPrinter(new FileWriter(outputTripsPath.toString()), CSVFormat.TDF);
            List<String> titleRow = Arrays.asList
                    ("departure_time", "waiting_time", "in_vehicle_time", "total_travel_time",
                            "est_direct_in_vehicle_time", "actual_travel_distance", "est_direct_drive_distance",
                            "euclidean_distance", "detour_ratio_time", "detour_ratio_distance");
            tsvWriter.printRecord(titleRow);
            try (CSVParser parser = new CSVParser(Files.newBufferedReader(tripsFile),
                    CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
                for (CSVRecord record : parser.getRecords()) {
                    Link fromLink = network.getLinks().get(Id.createLinkId(record.get(3)));
                    Link toLink = network.getLinks().get(Id.createLinkId(record.get(6)));
                    double departureTime = Double.parseDouble(record.get(0));
                    LeastCostPathCalculator.Path path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getToNode(),
                            departureTime, null, null);

                    double estimatedDirectInVehicleTime = path.travelTime;
                    double estimatedDirectTravelDistance = path.links.stream().map(Link::getLength).mapToDouble(l -> l).sum();
                    double waitingTime = Double.parseDouble(record.get(9));
                    double actualInVehicleTime = Double.parseDouble(record.get(11));
                    double totalTravelTime = waitingTime + actualInVehicleTime;
                    double actualTravelDistance = Double.parseDouble(record.get(12));
                    double euclideanDistance = DistanceUtils.calculateDistance(fromLink.getToNode().getCoord(), toLink.getToNode().getCoord());
                    double detourRatioTime = actualInVehicleTime / estimatedDirectInVehicleTime;
                    double detourRatioDistance = actualTravelDistance / estimatedDirectTravelDistance;

                    List<String> outputRow = new ArrayList<>();
                    outputRow.add(Double.toString(departureTime));
                    outputRow.add(Double.toString(waitingTime));
                    outputRow.add(Double.toString(actualInVehicleTime));
                    outputRow.add(Double.toString(totalTravelTime));
                    outputRow.add(Double.toString(estimatedDirectInVehicleTime));
                    outputRow.add(Double.toString(actualTravelDistance));
                    outputRow.add(Double.toString(estimatedDirectTravelDistance));
                    outputRow.add(Double.toString(euclideanDistance));
                    outputRow.add(Double.toString(detourRatioTime));
                    outputRow.add(Double.toString(detourRatioDistance));

                    tsvWriter.printRecord(outputRow);
                }
            }
            tsvWriter.close();
        }
        return 0;
    }
}
