package org.matsim.analysis.preAnalysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.mutable.MutableInt;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.DefaultAnalysisMainModeIdentifier;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import picocli.CommandLine;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandLine.Command(
        name = "analyze-leg",
        description = "Analyze the trip data of the input plans"
)
public class ModeShareAnalysis implements MATSimAppCommand {
    @CommandLine.Option(names = "--plans", description = "Path to input population (plans) file", required = true)
    private String inputPlans;

    @CommandLine.Option(names = "--relevant-persons", description = "Path to the csv file with all relevant persons", defaultValue = "")
    private String relevantPersonsFile;

    @CommandLine.Option(names = "--output-folder", description = "Path to analysis output folder", required = true)
    private String outputFolder;

    @CommandLine.Option(names = "--distance-factor", description = "Multiply this factor to the euclidean distance to approximate network distance", defaultValue = "1.0")
    private double distanceFactor;

    private final String[] modes = new String[]{TransportMode.car, TransportMode.ride, TransportMode.pt, TransportMode.bike, TransportMode.walk};
    private final int[] distanceGroups = new int[]{0, 1, 2, 5, 10, 20}; // Corresponds to distance grouping: 0-1, 1-2, 2-5, 5-10, 10-20, 20+

    public static void main(String[] args) {
        new ModeShareAnalysis().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        MainModeIdentifier mainModeIdentifier = new DefaultAnalysisMainModeIdentifier();
        Population plans = PopulationUtils.readPopulation(inputPlans);
        List<Person> relevantPersons = new ArrayList<>();
        Map<String, Map<Integer, MutableInt>> modeCount = initializeModeCount();

        if (!relevantPersonsFile.equals("")) {
            List<String> relevantPersonsId = new ArrayList<>();
            try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(relevantPersonsFile)),
                    CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader())) {
                for (CSVRecord record : parser) {
                    relevantPersonsId.add(record.get(0));
                }
            }

            for (Person person : plans.getPersons().values()) {
                if (relevantPersonsId.contains(person.getId().toString())) {
                    relevantPersons.add(person);
                }
            }
        } else {
            relevantPersons.addAll(plans.getPersons().values());
        }

        double totalTrips = 0;
        double persons = 0;
        for (Person person : relevantPersons) {
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());
            for (TripStructureUtils.Trip trip : trips) {
                Coord fromCoord = trip.getOriginActivity().getCoord();
                Coord toCoord = trip.getDestinationActivity().getCoord();
                double euclideanDistance = CoordUtils.calcEuclideanDistance(fromCoord, toCoord);
                String mode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
                addTripToModeCount(modeCount, mode, euclideanDistance * distanceFactor);
                totalTrips++;
            }
            persons++;
        }
        System.out.println("There are " + (int) persons + " persons in the analysis");
        System.out.println("They made " + (int) totalTrips + " trips during the day");
        System.out.println("Average trips per person = " + totalTrips / persons);

        // Write results
        writeTotalModeShare(modeCount, totalTrips);
        writeTotalDistanceDistribution(modeCount, totalTrips);
        writeDetailedStatistics(modeCount, totalTrips);

        return 0;
    }

    private void writeTotalModeShare(Map<String, Map<Integer, MutableInt>> modeCount, double totalTrips) throws IOException {
        CSVPrinter csvWriter = new CSVPrinter(new FileWriter(outputFolder + "/total-mode-share.csv"), CSVFormat.DEFAULT);
        csvWriter.printRecord("mode", "number_of_trips", "share");
        for (String mode : modeCount.keySet()) {
            double sum = modeCount.get(mode).values().stream().mapToDouble(MutableInt::doubleValue).sum();
            double share = sum / totalTrips;
            csvWriter.printRecord(mode, (int) sum, share);
        }
        csvWriter.printRecord("total", (int) totalTrips, "1.0");
        csvWriter.close();
    }

    private void writeTotalDistanceDistribution(Map<String, Map<Integer, MutableInt>> modeCount, double totalTrips) throws IOException {
        CSVPrinter csvWriter = new CSVPrinter(new FileWriter(outputFolder + "/total-distance-distribution.csv"), CSVFormat.DEFAULT);
        csvWriter.printRecord("distance_group", "number_of_trips", "share");
        List<String> modes = new ArrayList<>(modeCount.keySet());
        for (int distanceGroup : distanceGroups) {
            double sum = 0;
            for (String mode : modes) {
                sum += modeCount.get(mode).get(distanceGroup).doubleValue();
            }
            double share = sum / totalTrips;
            String displayedDistanceGroup = convertToDisplayedDistanceGroup(distanceGroup);
            csvWriter.printRecord(displayedDistanceGroup, (int) sum, share);
        }
        csvWriter.printRecord("total", (int) totalTrips, "1.0");
        csvWriter.close();
    }

    private void writeDetailedStatistics(Map<String, Map<Integer, MutableInt>> modeCount, double totalTrips) throws IOException {
        String[] modes = new String[]{TransportMode.car, TransportMode.ride, TransportMode.pt, TransportMode.bike, TransportMode.walk};

        CSVPrinter csvWriter = new CSVPrinter(new FileWriter(outputFolder + "/normalized-detailed-statistics.csv"), CSVFormat.DEFAULT);
        csvWriter.printRecord("mode", "below 1km", "1km - 2km", "2km - 5km", "5km - 10km", "10km - 20km", "more than 20km");
        CSVPrinter csvWriter2 = new CSVPrinter(new FileWriter(outputFolder + "/detailed-statistics.csv"), CSVFormat.DEFAULT);
        csvWriter2.printRecord("mode", "below 1km", "1km - 2km", "2km - 5km", "5km - 10km", "10km - 20km", "more than 20km");

        for (String mode : modes) {
            List<String> countRow = new ArrayList<>();
            List<String> shareRow = new ArrayList<>();
            countRow.add(mode);
            shareRow.add(mode);
            for (int distanceGroup : distanceGroups) {
                double count = modeCount.get(mode).get(distanceGroup).doubleValue();
                double share = count / totalTrips;
                countRow.add(Integer.toString((int) count));
                shareRow.add(Double.toString(share));
            }
            csvWriter.printRecord(shareRow);
            csvWriter2.printRecord(countRow);
        }
        csvWriter.close();
        csvWriter2.close();
    }

    private String convertToDisplayedDistanceGroup(double distanceGroup) {
        if (distanceGroup == 0) {
            return "below 1km";
        }
        if (distanceGroup == 1) {
            return "1km - 2km";
        }
        if (distanceGroup == 2) {
            return "2km - 5km";
        }
        if (distanceGroup == 5) {
            return "5km - 10km";
        }
        if (distanceGroup == 10) {
            return "10km - 20km";
        }
        return "more than 20km";
    }

    private Map<String, Map<Integer, MutableInt>> initializeModeCount() {
        Map<String, Map<Integer, MutableInt>> modeCount = new HashMap<>();
        for (String mode : modes) {
            modeCount.put(mode, new HashMap<>());
            for (int distanceGroup : distanceGroups) {
                modeCount.get(mode).put(distanceGroup, new MutableInt());
            }
        }
        return modeCount;
    }

    private void addTripToModeCount(Map<String, Map<Integer, MutableInt>> modeCount, String mode, double distance) {
        if (distance < 1000) {
            modeCount.get(mode).get(0).increment();
        } else if (distance < 2000) {
            modeCount.get(mode).get(1).increment();
        } else if (distance < 5000) {
            modeCount.get(mode).get(2).increment();
        } else if (distance < 10000) {
            modeCount.get(mode).get(5).increment();
        } else if (distance < 20000) {
            modeCount.get(mode).get(10).increment();
        } else {
            modeCount.get(mode).get(20).increment();
        }
    }

}
