package org.matsim.analysis.postAnalysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.geotools.MGC;
import picocli.CommandLine;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CommandLine.Command(
        name = "analyze-traffic",
        description = "Write traffic condition"
)
public class TrafficAnalysis implements MATSimAppCommand {
    @CommandLine.Option(names = "--network", description = "path to network file", required = true)
    private String networkFile;

    @CommandLine.Option(names = "--events", description = "path to events file", required = true)
    private String eventsFile;

    @CommandLine.Option(names = "--output", description = "output path", required = true)
    private String output;

    @CommandLine.Option(names = "--interval", description = "observation interval (in seconds). Default is 900 seconds (15 min)", defaultValue = "900")
    private double timeInterval;

    @CommandLine.Mixin
    private ShpOptions shp = new ShpOptions();

    private static final Logger log = LogManager.getLogger(TrafficAnalysis.class);

    public static void main(String[] args) {
        new TrafficAnalysis().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        Network network = NetworkUtils.readTimeInvariantNetwork(networkFile);
        TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder(network);
        TravelTimeCalculator travelTimeCalculator = builder.build();

        // event reader add event handeler travelTimeCalculator
        log.info("Begin analyzing travel time from events file...");
        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(travelTimeCalculator);
        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
        eventsReader.readFile(eventsFile);

        // Actual TravelTime based on the events file
        TravelTime travelTime = travelTimeCalculator.getLinkTravelTimes();

        Geometry studyArea = null;
        if (shp.getShapeFile() != null && !shp.getShapeFile().toString().equals("")) {
            studyArea = shp.getGeometry();
        }

        log.info("Begin writing out results...");
        log.info("There are in total " + network.getLinks().size() + " links in the network");
        Map<Double, List<Double>> networkSpeedRatiosMap = new HashMap<>();
        double networkCongestionIndex = 0;
        double totalLength = 0;
        List<String> titleRow = new ArrayList<>();
        titleRow.add("link_id");
        titleRow.add("congestion_index");
        titleRow.add("average_daily_speed");
        for (double i = 0; i < 86400; i += timeInterval) {
            networkSpeedRatiosMap.put(i, new ArrayList<>());
            titleRow.add(Double.toString(i));
        }

        CSVPrinter csvWriter = new CSVPrinter(new FileWriter(output), CSVFormat.DEFAULT);
        csvWriter.printRecord(titleRow);

        int processed = 0;
        for (Link link : network.getLinks().values()) {
            processed += 1;
            if (processed % 10000 == 0) {
                log.info("Processing: " + processed + " links have been processed");
            }
            if (!link.getAllowedModes().contains("car")) {
                continue;
            }

            if (link.getLength() < 10) {
                continue; // Links that are too short can produce extreme values and make the analysis not accurate
            }

            boolean isLinkWithinStudyArea = MGC.coord2Point(link.getToNode().getCoord()).within(studyArea) ||
                    MGC.coord2Point(link.getFromNode().getCoord()).within(studyArea);
            if (!isLinkWithinStudyArea && studyArea != null) {
                continue;
            }

            List<Double> linksSpeedRatios = new ArrayList<>();
            double counter = 0;
            double congestedPeriods = 0;
            for (double t = 0; t < 86400; t += timeInterval) {
                double freeSpeedTravelTime = Math.floor(link.getLength() / link.getFreespeed()) + 1;
                double actualTravelTime = travelTime.getLinkTravelTime(link, t, null, null);
                double speedRatio = freeSpeedTravelTime / actualTravelTime;
                if (speedRatio > 1) {
                    speedRatio = 1;
                }
                networkSpeedRatiosMap.get(t).add(speedRatio);
                linksSpeedRatios.add(speedRatio);
                counter++;
                if (speedRatio <= 0.5) {
                    congestedPeriods++;
                }
            }
            double linkDailyAverageSpeed = linksSpeedRatios.stream().mapToDouble(s -> s).average().orElse(-1);
            double linkCongestionIndex = linkDailyAverageSpeed * (1 - congestedPeriods / counter);
            List<String> outputRow = new ArrayList<>();
            outputRow.add(link.getId().toString());
            outputRow.add(Double.toString(linkCongestionIndex));
            outputRow.add(Double.toString(linkDailyAverageSpeed));
            outputRow.addAll(linksSpeedRatios.stream().map(s -> Double.toString(s)).collect(Collectors.toList()));
            csvWriter.printRecord(outputRow);

            networkCongestionIndex += linkCongestionIndex * link.getLength();
            totalLength += link.getLength();
        }

        // final row (whole network)
        networkCongestionIndex = networkCongestionIndex / totalLength;
        List<Double> networkSpeedRatios = new ArrayList<>();
        for (Double t : networkSpeedRatiosMap.keySet()) {
            double averageSpeedRatioForTimePeriod = networkSpeedRatiosMap.get(t).stream().mapToDouble(s -> s).average().orElse(-1);
            networkSpeedRatios.add(averageSpeedRatioForTimePeriod);
        }

        List<String> lastRow = new ArrayList<>();
        lastRow.add("full_network");
        lastRow.add(Double.toString(networkCongestionIndex));
        lastRow.add(Double.toString(networkSpeedRatios.stream().mapToDouble(s -> s).average().orElse(-1)));
        lastRow.addAll(networkSpeedRatios.stream().map(s -> Double.toString(s)).collect(Collectors.toList()));
        csvWriter.printRecord(lastRow);
        csvWriter.close();

        return 0;
    }
}
