package org.matsim.analysis.postAnalysis.scoring;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


@CommandLine.Command(
		name = "analyze-output-scores",
		description = "Write traffic condition analysis and calculate congestion index of the network"
)
public class OutputPlansScoringAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(OutputPlansScoringAnalysis.class);

	@CommandLine.Option(names = "--plans", description = "path to the output plans file", required = true)
	private String outputPlansFile;

	@CommandLine.Option(names = "--relevant-persons", description = "Path to the csv file with all relevant persons", defaultValue = "")
	private String relevantPersonsFile;

	@CommandLine.Option(names = "--output", description = "Path to the output tsv file", required = true)
	private String outputTsvFileName;

	public static void main(String[] args) {
		new OutputPlansScoringAnalysis().execute(args);
	}

	@Override
	@SuppressWarnings({"JavaNCSS", "CyclomaticComplexity", "IllegalType"})
	public Integer call() throws Exception {
		Random rnd = new Random(1234);
		Population outputPopulation = PopulationUtils.readPopulation(outputPlansFile);
		MainModeIdentifier modeIdentifier = new KelheimMainModeIdentifier();

		List<Id<Person>> relevantPersons = new ArrayList<>();
		if (!relevantPersonsFile.equals("")) {
			List<String> relevantPersonsId = new ArrayList<>();
			try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(relevantPersonsFile)),
					CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader())) {
				for (CSVRecord row : parser) {
					relevantPersonsId.add(row.get(0));
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

		CSVPrinter tsvWriter = new CSVPrinter(new FileWriter(outputTsvFileName), CSVFormat.TDF);
		tsvWriter.printRecord("person-id", "plan 1 score", "plan 2 score", "plan 3 score", "plan 4 score",
				"plan 5 score", "max", "min", "average", "executed", "probability plan 1", "probability plan 2",
				"probability plan 3", "probability plan 4", "probability plan 5", "potential mode change");

		List<Person> samplePersons = new ArrayList<>();
		int counter = 0;
		for (Person person : outputPopulation.getPersons().values()) {
			if (!relevantPersons.contains(person.getId())) {
				continue;
			}
			Plan selectedPlan = person.getSelectedPlan();
			boolean potentialModeChange = false;
			List<Double> scores = new ArrayList<>();
			List<Double> probabilities = new ArrayList<>();
			double maxScore = Double.MAX_VALUE * -1;
			double minScore = Double.MAX_VALUE;
			double executedScore = selectedPlan.getScore();

			List<String> originalModes = new ArrayList<>();
			for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(selectedPlan)) {
				originalModes.add(modeIdentifier.identifyMainMode(trip.getTripElements()));
			}

			for (Plan plan : person.getPlans()) {
				List<String> modes = new ArrayList<>();
				double score = plan.getScore();
				scores.add(score);
				if (score > maxScore) {
					maxScore = score;
				}

				if (score < minScore) {
					minScore = score;
				}

				for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {
					modes.add(modeIdentifier.identifyMainMode(trip.getTripElements()));
				}

				if (isTherePotentialModeChange(originalModes, modes)) {
					potentialModeChange = true;
				}
			}
			double averageScore = scores.stream().mapToDouble(s -> s).average().orElse(Double.NaN);

			double denominator = 0;
			for (double score : scores) {
				denominator += Math.exp(score - maxScore);
			}
			for (double score : scores) {
				probabilities.add(Math.exp(score - maxScore) / denominator);
			}

			// prepare output row
			List<String> outputRow = new ArrayList<>();
			outputRow.add(person.getId().toString());
			for (double score : scores) {
				outputRow.add(Double.toString(score));
			}
			outputRow.add(Double.toString(maxScore));
			outputRow.add(Double.toString(minScore));
			outputRow.add(Double.toString(averageScore));
			outputRow.add(Double.toString(executedScore));
			for (double probability : probabilities) {
				outputRow.add(Double.toString(probability));
			}
			if (potentialModeChange) {
				outputRow.add("yes");
			} else {
				outputRow.add("no");
			}
			tsvWriter.printRecord(outputRow);

			// Show details of some plans in the console
			if (rnd.nextDouble() < 0.01 && counter < 10) {
				samplePersons.add(person);
				counter++;
			}
		}
		tsvWriter.close();

		log.info("There are in total {} persons in this analysis", relevantPersons.size());
		for (Person person : samplePersons) {
			log.info("Sample person: person id = {}", person.getId().toString());
			int printerCounter = 1;
			for (Plan plan : person.getPlans()) {
				log.info("Plan {} score = {}", printerCounter, plan.getScore());
				List<String> modes = new ArrayList<>();
				List<String> departureTimes = new ArrayList<>();
				for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {
					double departureTime = trip.getOriginActivity().getEndTime().seconds();
					departureTimes.add(Double.toString(departureTime));
					modes.add(modeIdentifier.identifyMainMode(trip.getTripElements()));
				}
				log.info(Arrays.toString(modes.toArray()));
				log.info(Arrays.toString(departureTimes.toArray()));
				printerCounter++;
			}
			log.info(" ");
		}

		return 0;
	}

	private boolean isTherePotentialModeChange(List<String> modesOfPlan1, List<String> modesOfPlan2) {
		for (int i = 0; i < modesOfPlan1.size(); i++) {
			if (!modesOfPlan1.get(i).equals(modesOfPlan2.get(i))) {
				return true;
			}
		}
		return false;
	}
}
