package org.matsim.analysis.postAnalysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import picocli.CommandLine;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.matsim.application.ApplicationUtils.globFile;

/**
 * Analyse routes for agents which use a certain infrastructure segment.
 * 1) Retrieves agents which use the infrastructure (e.g. in base case)
 * 2) Save trips of those agents to tsv file for further R analysis.
 *
 * @author Simon Meinhardt (simei94)
 */
class BlockedInfrastructureRouteAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(BlockedInfrastructureRouteAnalysis.class);

	@CommandLine.Option(names = "--directory-base", description = "path to the directory of the simulation output for the base case", required = true)
	private Path directoryBase;

	@CommandLine.Option(names = "--directory-policy", description = "path to the directory of the simulation output for the policy case", required = true)
	private Path directoryPolicy;

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	Map<Id<Person>, Map<Integer, TripStructureUtils.Trip>> relevantTripsBase = new HashMap<>();

	public static void main(String[] args) {
		new BlockedInfrastructureRouteAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Geometry blockedInfrastructureArea = shp.getGeometry();

		Path networkPath = globFile(directoryBase, "*output_network.*");

		Path basePopulationPath = globFile(directoryBase, "*output_plans*");

		//output will be written into policy case folder
		Path outputFolder = Path.of(directoryPolicy.toString() + "/analysis-road-usage");

		if (!Files.exists(outputFolder)) {
			Files.createDirectory(outputFolder);
		}

		Network network = NetworkUtils.readNetwork(networkPath.toString());
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(basePopulationPath.toString());
		config.global().setCoordinateSystem("EPSG:25832");

		Population populationBase = ScenarioUtils.loadScenario(config).getPopulation();

		//get links, which are affected by blocked infrastructure
		List<String> blockedLinks = getBlockedLinks(network, blockedInfrastructureArea);

		relevantTripsBase = getTripsFromPlans(populationBase, blockedLinks);

		//writeResults
		String outputFile = outputFolder + "/" + "blocked_infrastructure_trip_comparison.tsv";
		CSVPrinter tsvPrinter = new CSVPrinter(new FileWriter(outputFile), CSVFormat.TDF);
		try {
			List<String> header = new ArrayList<>();
			header.add("person_id");
			header.add("trip_number");
			header.add("trip_id");

			tsvPrinter.printRecord(header);

			for (Map.Entry<Id<Person>, Map<Integer, TripStructureUtils.Trip>> entry : relevantTripsBase.entrySet()) {

				Integer tripNumber;

				for (Integer index : relevantTripsBase.get(entry.getKey()).keySet()) {

					tripNumber = index + 1;

					List<String> outputEntry = new ArrayList<>();
					outputEntry.add(entry.getKey().toString());
					outputEntry.add(tripNumber.toString());
					outputEntry.add(entry.getKey() + "_" + tripNumber);

					tsvPrinter.printRecord(outputEntry);
				}
			}
		} catch (IOException e) {
			log.log(Level.FATAL,e);
		} finally {
			tsvPrinter.close();
			log.log(Level.INFO, "Analysis output has been written to: {}", outputFile);
		}

		return 0;
	}

	private List<String> getBlockedLinks(Network network, Geometry geometry) {

		List<String> blockedLinks = new ArrayList<>();

		for (Link link : network.getLinks().values()) {
			if (!link.getId().toString().contains("pt_") && isInsideArea(link, geometry)) {
				blockedLinks.add(link.getId().toString());
			}
		}
		return blockedLinks;
	}

	private Map<Id<Person>, Map<Integer, TripStructureUtils.Trip>> getTripsFromPlans(Population population, List<String> blockedLinks) {

		Map<Id<Person>, Map<Integer, TripStructureUtils.Trip>> relevantTrips = new HashMap<>();

		if (relevantTripsBase.size() < 1) {
			//base case
			log.info("Analyzing legs on blocked infrastructure for base case population");

			for ( Person person : population.getPersons().values()) {
				//check if blocked link is part of leg-route

				List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());

				for (int i = 0; i < trips.size(); i++) {
					for (Leg leg : trips.get(i).getLegsOnly()) {
						for ( String linkId : blockedLinks ) {
							if (leg.getRoute().getRouteDescription().contains(linkId)) {
								relevantTrips.putIfAbsent(person.getId(), new HashMap<>());
								relevantTrips.get(person.getId()).put(i, trips.get(i));
								continue;
							}
						}
					}
				}
			}

		} else {
			//policy case
			//get corresponding trips to base case trips (where the now-blocked infrastructure is used)
			log.info("Analyzing legs on blocked infrastructure for policy case population");

			for (Map.Entry<Id<Person>, Map<Integer, TripStructureUtils.Trip>> entry : relevantTripsBase.entrySet()) {
				for ( Integer index : relevantTripsBase.get(entry.getKey()).keySet()) {
					relevantTrips.putIfAbsent(entry.getKey(), new HashMap<>());

					TripStructureUtils.Trip policyTrip = TripStructureUtils.getTrips(population.getPersons().get(entry.getKey()).getSelectedPlan()).get(index);

					relevantTrips.get(entry.getKey()).put(index, policyTrip);
				}
			}
		}

		return relevantTrips;
	}

	static boolean isInsideArea(Link link, Geometry geometry) {

		boolean isInsideArea = false;

		if (MGC.coord2Point(link.getToNode().getCoord()).within(geometry) ||
				MGC.coord2Point(link.getFromNode().getCoord()).within(geometry)) {
			isInsideArea = true;
		}

		return isInsideArea;
	}
}
