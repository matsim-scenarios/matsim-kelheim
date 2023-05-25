package org.matsim.analysis.postAnalysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.matsim.application.ApplicationUtils.globFile;

/**
 * Analyse routes for agents which use a certain infrastructure segment.
 * 1) Retrieves agents which use the infrastructure (e.g. in base case)
 * 2) Searches for the (new) routes of the agents in a policy case
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

	Map<Id<Person>, Map<Integer, Leg>> relevantLegsBase = new HashMap<>();

	public static void main(String[] args) {
		new BlockedInfrastructureRouteAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Geometry blockedInfrastructureArea = shp.getGeometry();

		Path networkPath = globFile(directoryBase, "*output_network.*");

		Path basePopulationPath = globFile(directoryBase, "*output_plans*");

		Path policyPopulationPath = globFile(directoryBase, "*output_plans*");
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

		config.plans().setInputFile(policyPopulationPath.toString());
		Population populationPolicy = ScenarioUtils.loadScenario(config).getPopulation();

		//get links, which are affected by blocked infrastructure
		List<String> blockedLinks = getBlockedLinks(network, blockedInfrastructureArea);

		relevantLegsBase = getLegsFromPlans(populationBase, blockedLinks);
		Map<Id<Person>, Map<Integer, Leg>> relevantLegsPolicy = getLegsFromPlans(populationPolicy, blockedLinks);

		//writeResults
		String outputFile = outputFolder + "/" + "blocked_infrastructure_leg_comparison.tsv";
		CSVPrinter tsvPrinter = new CSVPrinter(new FileWriter(outputFile), CSVFormat.TDF);
		List<String> header = new ArrayList<>();
		header.add("person_id");
		header.add("tt_s_base");
		header.add("tt_s_policy");
		header.add("dist_m_base");
		header.add("dist_m_policy");
		header.add("mode_base");
		header.add("mode_policy");
		header.add("startLink_base");
		header.add("startLink_policy");
		header.add("endLink_base");
		header.add("endLink_policy");
		header.add("route_description_base");
		header.add("route_description_policy");

		tsvPrinter.printRecord(header);

		for (Id<Person> personId : relevantLegsBase.keySet()) {

			for (Integer index : relevantLegsBase.get(personId).keySet()) {

				Map<Integer, Leg> basePersonStats = relevantLegsBase.get(personId);
				Map<Integer, Leg> policyPersonStats = relevantLegsPolicy.get(personId);

				List<String> entry = new ArrayList<>();
				entry.add(personId.toString());
				entry.add(String.valueOf(basePersonStats.get(index).getRoute().getTravelTime().seconds()));
				entry.add(String.valueOf(policyPersonStats.get(index).getRoute().getTravelTime().seconds()));
				entry.add(String.valueOf(basePersonStats.get(index).getRoute().getDistance()));
				entry.add(String.valueOf(policyPersonStats.get(index).getRoute().getDistance()));
				entry.add(basePersonStats.get(index).getMode());
				entry.add(policyPersonStats.get(index).getMode());
				entry.add(basePersonStats.get(index).getRoute().getStartLinkId().toString());
				entry.add(policyPersonStats.get(index).getRoute().getStartLinkId().toString());
				entry.add(basePersonStats.get(index).getRoute().getEndLinkId().toString());
				entry.add(policyPersonStats.get(index).getRoute().getEndLinkId().toString());
				entry.add(basePersonStats.get(index).getRoute().getRouteDescription());
				entry.add(policyPersonStats.get(index).getRoute().getRouteDescription());

				tsvPrinter.printRecord(entry);
			}
		}
		tsvPrinter.close();

		log.info("Analysis output has been written to: " + outputFile);

		return 0;
	}

	private List<String> getBlockedLinks(Network network, Geometry geometry) {

		List<String> blockedLinks = new ArrayList<>();

		for (Link link : network.getLinks().values()) {
			if (!link.getId().toString().contains("pt_")) {
				if (isInsideArea(link, geometry)) {
					blockedLinks.add(link.getId().toString());
				}
			}
		}
		return blockedLinks;
	}

	private Map<Id<Person>, Map<Integer, Leg>> getLegsFromPlans(Population population, List<String> blockedLinks) {

		Map<Id<Person>, Map<Integer, Leg>> relevantLegs = new HashMap<>();

		if (relevantLegsBase.size() < 1) {
			//base case
			log.info("Analyzing legs on blocked infrastructure for base case population");

			for ( Person person : population.getPersons().values()) {
				//check if blocked link is part of leg-route
				for ( Leg leg : TripStructureUtils.getLegs(person.getSelectedPlan())) {
					for ( String linkId : blockedLinks ) {
						if (leg.getRoute().getRouteDescription().contains(linkId)) {
							relevantLegs.putIfAbsent(person.getId(), new HashMap<>());
							relevantLegs.get(person.getId()).put(person.getSelectedPlan().getPlanElements().indexOf(leg), leg);
							continue;
						}
					}
				}
			}
		} else {
			//policy case
			//get corresponding legs to base case legs (where the now-blocked infrastructure is used)
			log.info("Analyzing legs on blocked infrastructure for policy case population");

			for (Id<Person> personId : relevantLegsBase.keySet()) {
				for ( Integer index : relevantLegsBase.get(personId).keySet()) {
					relevantLegs.putIfAbsent(personId, new HashMap<>());
					Leg policyLeg = (Leg) population.getPersons().get(personId).getSelectedPlan().getPlanElements().get(index);
					relevantLegs.get(personId).put(population.getPersons().get(personId).getSelectedPlan().getPlanElements().indexOf(policyLeg),
							policyLeg);
				}
			}
		}

		return relevantLegs;
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
