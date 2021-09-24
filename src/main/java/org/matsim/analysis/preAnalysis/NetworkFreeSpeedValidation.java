package org.matsim.analysis.preAnalysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.analysis.vsp.traveltimedistance.HereMapsRouteValidator;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import picocli.CommandLine;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


@CommandLine.Command(
        name = "analyze-network-free-speed",
        description = "Analyze the free speed of network against online API"
)
public class NetworkFreeSpeedValidation implements MATSimAppCommand {

    @CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to network file")
    private Path networkPath;

    @CommandLine.Option(names = "--output-folder", description = "Path to output folder", required = true)
    private Path outputFolder;

    @CommandLine.Option(names = "--api", description = "Online API used. Choose from: [here, googleMap(TODO)]", defaultValue = "here")
    private String onlineAPI;

    @CommandLine.Option(names = "--api-key", description = "API key. You can apply for a free API key on their website", required = true)
    private String apiKey;

    @CommandLine.Option(names = "--trips", description = "Number of trips to validate", defaultValue = "500")
    private int trips;

    @CommandLine.Mixin
    private ShpOptions shp = new ShpOptions();

    @CommandLine.Mixin
    private CrsOptions crs = new CrsOptions();


    private static final Logger log = LogManager.getLogger(NetworkFreeSpeedValidation.class);
    private final Random rnd = new Random(1234);

    public static void main(String[] args) {
        new NetworkFreeSpeedValidation().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        if (!Files.exists(networkPath)) {
            log.error("Input population does not exist: {}", networkPath);
            return 2;
        }

        Network network = NetworkUtils.readNetwork(networkPath.toString());
        List<Link> links = network.getLinks().values().stream().
                filter(l -> l.getAllowedModes().contains(TransportMode.car)).
                collect(Collectors.toList());
        int numOfLinks = links.size();

        // Create router
        FreeSpeedTravelTime travelTime = new FreeSpeedTravelTime();
        LeastCostPathCalculatorFactory fastAStarLandmarksFactory = new SpeedyALTFactory();
        OnlyTimeDependentTravelDisutilityFactory disutilityFactory = new OnlyTimeDependentTravelDisutilityFactory();
        TravelDisutility travelDisutility = disutilityFactory.createTravelDisutility(travelTime);
        LeastCostPathCalculator router = fastAStarLandmarksFactory.createPathCalculator(network, travelDisutility,
                travelTime);

        // Read shapefile if presents
        List<Link> linksInsideShp = new ArrayList<>();
        List<Link> outsideLinks = new ArrayList<>();
        if (shp.getShapeFile() != null) {
            Geometry geometry = shp.getGeometry();
            for (Link link : links) {
                if (MGC.coord2Point(link.getToNode().getCoord()).within(geometry)) {
                    linksInsideShp.add(link);
                }
            }
            outsideLinks.addAll(links);
            outsideLinks.removeAll(linksInsideShp);
        }

        // Choose random trips to validate
        CSVPrinter csvWriter = new CSVPrinter(new FileWriter(outputFolder.toString() + "/results.csv"), CSVFormat.DEFAULT);
        csvWriter.printRecord("trip_number", "trip_category", "from_x", "from_y", "to_x", "to_y", "simulated_travel_time", "validated_travel_time");
        int counter = 0;
        CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(crs.getInputCRS(), TransformationFactory.WGS84);
        HereMapsRouteValidator validator = new HereMapsRouteValidator(outputFolder.toString(), apiKey, "2021-01-01", transformation);
        Link fromLink;
        Link toLink;
        String tripType;

        while (counter < trips) {
            if (!linksInsideShp.isEmpty()) {
                int numOfLinksInsideShp = linksInsideShp.size();
                int numOfOutsideLinks = outsideLinks.size();

                if (counter < 0.6 * trips) {
                    fromLink = linksInsideShp.get(rnd.nextInt(numOfLinksInsideShp));
                    toLink = linksInsideShp.get(rnd.nextInt(numOfLinksInsideShp));
                    tripType = "inside";
                } else if (counter < 0.9 * trips) {
                    fromLink = linksInsideShp.get(rnd.nextInt(numOfLinksInsideShp));
                    toLink = outsideLinks.get(rnd.nextInt(numOfOutsideLinks));
                    tripType = "cross-border";
                } else {
                    fromLink = outsideLinks.get(rnd.nextInt(numOfOutsideLinks));
                    toLink = outsideLinks.get(rnd.nextInt(numOfOutsideLinks));
                    tripType = "outside";
                }
            } else {
                fromLink = links.get(rnd.nextInt(numOfLinks));
                toLink = links.get(rnd.nextInt(numOfLinks));
                tripType = "unknown";
            }

            if (!fromLink.getToNode().getId().equals(toLink.getToNode().getId())) {
                String detailedFile = outputFolder + "/detailed-record/trip" + counter + ".json.gz";
                Coord fromCorrd = fromLink.getToNode().getCoord();
                Coord toCoord = toLink.getToNode().getCoord();
                double validatedTravelTime = validator.getTravelTime
                        (fromCorrd, toCoord, 1, detailedFile).getFirst();
                if (validatedTravelTime < 60){
                    continue;
                }
                double simulatedTravelTime = router.calcLeastCostPath
                        (fromLink.getToNode(), toLink.getToNode(), 0, null, null).travelTime;
                csvWriter.printRecord(Integer.toString(counter), tripType, Double.toString(fromCorrd.getX()),
                        Double.toString(fromCorrd.getY()), Double.toString(toCoord.getX()),
                        Double.toString(toCoord.getY()), Double.toString(simulatedTravelTime),
                        Double.toString(validatedTravelTime));
                counter++;
            }
        }

        csvWriter.close();
        return 0;
    }
}
