package org.matsim.analysis.postAnalysis.scoring;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.matsim.analysis.KelheimMainModeIdentifier;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@CommandLine.Command(
        name = "analyze-output-scores",
        description = "Write traffic condition analysis and calculate congestion index of the network"
)
public class OutputPlansScoringAnalysis implements MATSimAppCommand {
    @CommandLine.Option(names = "--plans", description = "path to the output plans file", required = true)
    private String outputPlansFile;

    @CommandLine.Option(names = "--relevant-persons", description = "Path to the csv file with all relevant persons", defaultValue = "")
    private String relevantPersonsFile;

    public static void main(String[] args) {
        new OutputPlansScoringAnalysis().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        Population outputPopulation = PopulationUtils.readPopulation(outputPlansFile);
        MainModeIdentifier modeIdentifier = new KelheimMainModeIdentifier();

        List<Id<Person>> relevantPersons = new ArrayList<>();
        if (!relevantPersonsFile.equals("")) {
            List<String> relevantPersonsId = new ArrayList<>();
            try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(relevantPersonsFile)),
                    CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader())) {
                for (CSVRecord record : parser) {
                    relevantPersonsId.add(record.get(0));
                }
            }

            for (Person person : outputPopulation.getPersons().values()) {
                if (relevantPersonsId.contains(person.getId().toString())) {
                    relevantPersons.add(person.getId());
                }
            }
        } else {
            relevantPersons.addAll(outputPopulation.getPersons().values().stream().map(Identifiable::getId).collect(Collectors.toList()));
        }

        int numOfPersonsThatMayChangeMode = 0;
        int numOfPersonsConsidered = 0;
        List<Person> samplePersons = new ArrayList<>();
        for (Person person : outputPopulation.getPersons().values()) {
            if (!relevantPersons.contains(person.getId())) {
                continue;
            }
            numOfPersonsConsidered++;

            Plan selectedPlan = person.getSelectedPlan();
            double scoreOfSelectedPlan = selectedPlan.getScore();
            // identify modes of the trip in selected plan
            List<String> tripsModesOfSelectedPlan = new ArrayList<>();
            for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(selectedPlan)) {
                tripsModesOfSelectedPlan.add(modeIdentifier.identifyMainMode(trip.getTripElements()));
            }

            // Calculate exponential beta (denominator)
            double denominator = 0;
            for (Plan plan : person.getPlans()) {
                double delta = plan.getScore() - scoreOfSelectedPlan;
                denominator += Math.exp(delta);
            }

            // Analyze potential mode changes
            for (Plan plan : person.getPlans()) {
                double delta = plan.getScore() - scoreOfSelectedPlan;
                double probability = Math.exp(delta) / denominator;
                if (probability > 0.1) {
                    List<String> tripsModeOfAlternativePlan = new ArrayList<>();
                    for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {
                        tripsModeOfAlternativePlan.add(modeIdentifier.identifyMainMode(trip.getTripElements()));
                    }
                    boolean potentialModeChange = isTherePotentialModeChange(tripsModesOfSelectedPlan, tripsModeOfAlternativePlan);
                    if (potentialModeChange) {
                        numOfPersonsThatMayChangeMode++;
                        if (samplePersons.size() <= 3) {
                            samplePersons.add(person);
                        }
                        break;
                    }
                }
            }

        }

        System.out.println("There are " + numOfPersonsThatMayChangeMode + " persons that may change mode (probability >= 10%) in the next iteration");
        System.out.println("There are in total " + numOfPersonsConsidered + " persons living in the study area");
        System.out.println("The ratio is " + (double) numOfPersonsThatMayChangeMode / (double) numOfPersonsConsidered);

        for (Person person : samplePersons) {
            System.out.println("Sample person: person id =" + person.getId().toString());
            int counter = 1;
            for (Plan plan : person.getPlans()) {
                System.out.println("Plan " + counter + " score = " + plan.getScore());
                List<String> modes = new ArrayList<>();
                for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {
                    modes.add(modeIdentifier.identifyMainMode(trip.getTripElements()));
                }
                System.out.println(Arrays.toString(modes.toArray()));
                counter++;
            }
            System.out.println(" ");
        }

        return 0;
    }

    private boolean isTherePotentialModeChange(List<String> tripsModesOfSelectedPlan, List<String> tripsModeOfAlternativePlan) {
        for (int i = 0; i < tripsModesOfSelectedPlan.size(); i++) {
            if (!tripsModesOfSelectedPlan.get(i).equals(tripsModeOfAlternativePlan.get(i))) {
                return true;
            }
        }
        return false;
    }
}
