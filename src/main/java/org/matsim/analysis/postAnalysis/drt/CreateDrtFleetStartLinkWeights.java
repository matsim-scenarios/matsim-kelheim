package org.matsim.analysis.postAnalysis.drt;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Creates population-based weights for reproducible DRT fleet start-link placement. */
@CommandLine.Command(name = "create-drt-fleet-start-link-weights",
	description = "Assign population home locations to their nearest DRT-stop link.",
	mixinStandardHelpOptions = true, showDefaultValues = true)
public class CreateDrtFleetStartLinkWeights implements MATSimAppCommand {
	private static final Logger LOG = LogManager.getLogger(CreateDrtFleetStartLinkWeights.class);

	@CommandLine.Option(names = "--population", required = true, description = "Population plans file.")
	private Path populationFile;

	@CommandLine.Option(names = "--network", required = true, description = "MATSim network file.")
	private Path networkFile;

	@CommandLine.Option(names = "--drt-stops", required = true, description = "Transit schedule containing the DRT stops.")
	private Path drtStopsFile;

	@CommandLine.Option(names = "--output", required = true, description = "Output CSV or CSV.GZ file.")
	private Path outputFile;

	public static void main(String[] args) {
		new CreateDrtFleetStartLinkWeights().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile.toString());
		new PopulationReader(scenario).readFile(populationFile.toString());
		new TransitScheduleReader(scenario).readFile(drtStopsFile.toString());

		Map<Id<Link>, Candidate> candidates = new LinkedHashMap<>();
		scenario.getTransitSchedule().getFacilities().values().stream()
			.sorted(Comparator.comparing(stop -> stop.getId().toString()))
			.forEach(stop -> addCandidate(stop, scenario, candidates));
		if (candidates.isEmpty()) {
			throw new IllegalArgumentException("No DRT stops with valid network links found in " + drtStopsFile);
		}

		long assignedPersons = 0;
		long skippedPersons = 0;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Coord home = findHomeCoord(person, scenario);
			if (home == null) {
				skippedPersons++;
				continue;
			}
			Candidate nearest = candidates.values().stream()
				.min(Comparator.comparingDouble(candidate -> squaredDistance(home, candidate.coord)))
				.orElseThrow();
			nearest.population++;
			assignedPersons++;
		}
		if (assignedPersons == 0) {
			throw new IllegalArgumentException("No population home coordinates could be resolved from " + populationFile);
		}

		CSVFormat format = CSVFormat.DEFAULT.builder()
			.setDelimiter(';')
			.setHeader("linkId", "population", "weight")
			.build();
		try (BufferedWriter bufferedWriter = IOUtils.getBufferedWriter(outputFile.toString());
			 CSVPrinter writer = new CSVPrinter(bufferedWriter, format)) {
			for (Candidate candidate : candidates.values()) {
				writer.printRecord(candidate.linkId, candidate.population,
					candidate.population / (double)assignedPersons);
			}
		}

		LOG.info("Wrote {} DRT start-link weights for {} persons to {}; skipped {} persons without a home coordinate",
			candidates.size(), assignedPersons, outputFile, skippedPersons);
		return 0;
	}

	private static void addCandidate(TransitStopFacility stop, Scenario scenario,
								 Map<Id<Link>, Candidate> candidates) {
		if (stop.getLinkId() == null) {
			LOG.warn("Ignoring DRT stop {} without a link id", stop.getId());
			return;
		}
		Link link = scenario.getNetwork().getLinks().get(stop.getLinkId());
		if (link == null) {
			throw new IllegalArgumentException("DRT stop " + stop.getId() + " references missing link " + stop.getLinkId());
		}
		candidates.putIfAbsent(link.getId(), new Candidate(link.getId(), stop.getCoord()));
	}

	private static Coord findHomeCoord(Person person, Scenario scenario) {
		Object homeX = person.getAttributes().getAttribute("home_x");
		Object homeY = person.getAttributes().getAttribute("home_y");
		if (homeX instanceof Number x && homeY instanceof Number y) {
			return new Coord(x.doubleValue(), y.doubleValue());
		}
		if (person.getSelectedPlan() == null) {
			return null;
		}
		for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
			if (element instanceof Activity activity && activity.getType().startsWith("home")) {
				if (activity.getCoord() != null) {
					return activity.getCoord();
				}
				if (activity.getLinkId() != null) {
					Link link = scenario.getNetwork().getLinks().get(activity.getLinkId());
					return link == null ? null : link.getCoord();
				}
			}
		}
		return null;
	}

	private static double squaredDistance(Coord first, Coord second) {
		double dx = first.getX() - second.getX();
		double dy = first.getY() - second.getY();
		return dx * dx + dy * dy;
	}

	private static final class Candidate {
		private final Id<Link> linkId;
		private final Coord coord;
		private long population;

		private Candidate(Id<Link> linkId, Coord coord) {
			this.linkId = linkId;
			this.coord = coord;
		}
	}
}
