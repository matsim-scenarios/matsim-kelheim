package org.matsim.run.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Replaces home-only adult plans with reproducible hot-deck counterfactual daily plans.
 */
@CommandLine.Command(
	name = "generate-counterfactual-immobile-plans",
	description = "Generate mobile counterfactual plans for immobile adult agents"
)
public class GenerateCounterfactualImmobilePlans implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(GenerateCounterfactualImmobilePlans.class);
	public static final String COUNTERFACTUAL_TARGET = "counterfactual_immobile_target";
	public static final String SYNTHETIC_PLAN = "counterfactual_synthetic_plan";
	public static final String DONOR_PERSON_ID = "counterfactual_donor_person_id";
	public static final String MATCH_LEVEL = "counterfactual_donor_matching_level";
	public static final String GENERATION_SEED = "counterfactual_generation_seed";
	public static final String GENERATION_RUN = "counterfactual_generation_run";
	private static final int MINIMUM_COHORT_SIZE = 30;

	@CommandLine.Option(names = "--input-population", required = true, description = "Input MATSim population path or URL")
	private String inputPopulation;

	@CommandLine.Option(names = "--output-population", required = true, description = "Output MATSim population")
	private Path outputPopulation;

	@CommandLine.Option(names = "--network", description = "Network path or URL used to validate activity links; required when enabled")
	private String networkFile;

	@CommandLine.Option(names = "--study-area-shp", description = "Study-area shapefile; required when enabled")
	private Path studyAreaShp;

	@CommandLine.Option(names = "--enabled", defaultValue = "true", description = "Enable counterfactual generation")
	private boolean enabled;

	@CommandLine.Option(names = "--seed", defaultValue = "4711", description = "Seed for reproducible donor selection")
	private long seed;

	@CommandLine.Option(names = "--generation-run", defaultValue = "default", description = "Label stored with generated plans")
	private String generationRun;

	@CommandLine.Option(names = "--minimum-target-age", defaultValue = "18", description = "Minimum age of target and donor persons")
	private int minimumTargetAge;

	public static void main(String[] args) {
		new GenerateCounterfactualImmobilePlans().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		if (!isUrl(inputPopulation) && !Files.exists(Path.of(inputPopulation))) {
			throw new IllegalArgumentException("Input population does not exist: " + inputPopulation);
		}
		Population population = PopulationUtils.readPopulation(inputPopulation);
		if (!enabled) {
			writePopulation(population);
			log.info("Counterfactual generation disabled; copied population without changes.");
			return 0;
		}
		if (networkFile == null || (!isUrl(networkFile) && !Files.exists(Path.of(networkFile)))) {
			throw new IllegalArgumentException("An existing --network is required when counterfactual generation is enabled.");
		}
		if (studyAreaShp == null || !Files.exists(studyAreaShp)) {
			throw new IllegalArgumentException("An existing --study-area-shp is required when counterfactual generation is enabled.");
		}

		Network network = NetworkUtils.readNetwork(networkFile);
		Geometry studyArea = new ShpOptions(studyAreaShp.toString(), null, null).getGeometry();
		List<? extends Person> persons = population.getPersons().values().stream()
			.sorted(Comparator.comparing(person -> person.getId().toString()))
			.toList();

		List<Target> targets = persons.stream()
			.map(person -> toTarget(person, network))
			.filter(target -> target != null)
			.toList();
		List<Donor> donors = persons.stream()
			.map(person -> toDonor(person, network))
			.filter(donor -> donor != null)
			.toList();
		Map<String, List<Location>> locationsByActivityType = buildDestinationInventory(donors, network, studyArea);
		donors = donors.stream().filter(donor -> donor.activities().stream()
			.allMatch(activity -> isHome(activity) || locationsByActivityType.containsKey(activity.getType())))
			.toList();
		if (!targets.isEmpty() && donors.isEmpty()) {
			throw new IllegalStateException("No eligible mobile adult donors with study-area destinations found.");
		}

		SplittableRandom random = new SplittableRandom(seed);
		Set<MatchingCohort> warnedSmallCohorts = new HashSet<>();
		PopulationFactory factory = population.getFactory();
		for (Target target : targets) {
			DonorMatch donorMatch = selectDonor(target.person(), donors, random, warnedSmallCohorts);
			Plan syntheticPlan = createSyntheticPlan(factory, target, donorMatch.donor(), locationsByActivityType, network, random);
			Person person = target.person();
			person.getPlans().clear();
			person.addPlan(syntheticPlan);
			person.setSelectedPlan(syntheticPlan);
			PersonUtils.setCarAvail(person, "never");
			person.getAttributes().putAttribute(COUNTERFACTUAL_TARGET, true);
			person.getAttributes().putAttribute(DONOR_PERSON_ID, donorMatch.donor().person().getId().toString());
			person.getAttributes().putAttribute(MATCH_LEVEL, donorMatch.level());
			person.getAttributes().putAttribute(GENERATION_SEED, seed);
			person.getAttributes().putAttribute(GENERATION_RUN, generationRun);
		}

		writePopulation(population);
		log.info("Generated {} counterfactual plans from {} mobile adult donors using seed {}.", targets.size(), donors.size(), seed);
		return 0;
	}

	private void writePopulation(Population population) throws Exception {
		Path parent = outputPopulation.toAbsolutePath().getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		PopulationUtils.writePopulation(population, outputPopulation.toString());
	}

	private Target toTarget(Person person, Network network) {
		if (isFreight(person) || age(person) < minimumTargetAge || person.getSelectedPlan() == null) {
			return null;
		}
		Plan plan = person.getSelectedPlan();
		List<Activity> activities = mainActivities(plan);
		if (!TripStructureUtils.getTrips(plan).isEmpty() || activities.isEmpty() || activities.stream().anyMatch(activity -> !isHome(activity))) {
			return null;
		}
		Activity home = activities.getFirst();
		if (resolveCoord(home, network) == null) {
			log.warn("Skipping target {} without a usable home coordinate.", person.getId());
			return null;
		}
		return new Target(person, home, resolveCoord(home, network));
	}

	private Donor toDonor(Person person, Network network) {
		if (isFreight(person) || age(person) < minimumTargetAge || person.getSelectedPlan() == null) {
			return null;
		}
		Plan plan = person.getSelectedPlan();
		List<Activity> activities = mainActivities(plan);
		if (TripStructureUtils.getTrips(plan).isEmpty() || activities.size() < 2 || !isHome(activities.getFirst())
			|| activities.stream().anyMatch(activity -> resolveCoord(activity, network) == null)) {
			return null;
		}
		return new Donor(person, activities);
	}

	private Map<String, List<Location>> buildDestinationInventory(List<Donor> donors, Network network, Geometry studyArea) {
		Map<String, List<Location>> result = new HashMap<>();
		for (Donor donor : donors) {
			for (Activity activity : donor.activities()) {
				if (isHome(activity)) {
					continue;
				}
				Coord coord = resolveCoord(activity, network);
				Link link = resolveLink(activity, network);
				if (link != null && studyArea.contains(MGC.coord2Point(coord))) {
					result.computeIfAbsent(activity.getType(), ignored -> new ArrayList<>()).add(new Location(coord, link.getId()));
				}
			}
		}
		return result;
	}

	private DonorMatch selectDonor(Person target, List<Donor> donors, SplittableRandom random,
		Set<MatchingCohort> warnedSmallCohorts) {
		String targetAgeBand = ageBand(age(target));
		String targetSex = attribute(target, "sex");
		String targetIncome = attribute(target, "MiD:hheink_gr2");
		for (int level = 0; level < 4; level++) {
			int matchingLevel = level;
			List<Donor> candidates = donors.stream()
				.filter(donor -> matches(donor.person(), targetAgeBand, targetSex, targetIncome, matchingLevel))
				.toList();
			if (!candidates.isEmpty()) {
				MatchingCohort cohort = matchingCohort(matchingLevel, targetAgeBand, targetSex, targetIncome);
				if (candidates.size() < MINIMUM_COHORT_SIZE && warnedSmallCohorts.add(cohort)) {
					log.warn("Matching cohort {} has only {} eligible donor agents; recommended minimum is {}.",
						cohort, candidates.size(), MINIMUM_COHORT_SIZE);
				}
				return new DonorMatch(candidates.get(random.nextInt(candidates.size())), level);
			}
		}
		throw new IllegalStateException("No donor found for target " + target.getId());
	}

	static boolean matches(Person donor, String targetAgeBand, String targetSex, String targetIncome, int level) {
		if (level == 3) {
			return true;
		}
		if (!ageBand(age(donor)).equals(targetAgeBand)) {
			return false;
		}
		if (level == 2) {
			return true;
		}
		if (!attribute(donor, "sex").equals(targetSex)) {
			return false;
		}
		return level != 0 || attribute(donor, "MiD:hheink_gr2").equals(targetIncome);
	}

	private Plan createSyntheticPlan(PopulationFactory factory, Target target, Donor donor,
		Map<String, List<Location>> locationsByActivityType, Network network, SplittableRandom random) {
		Plan result = factory.createPlan();
		Coord donorHome = resolveCoord(donor.activities().getFirst(), network);
		for (int index = 0; index < donor.activities().size(); index++) {
			Activity donorActivity = donor.activities().get(index);
			Activity activity;
			if (isHome(donorActivity)) {
				activity = factory.createActivityFromCoord(donorActivity.getType(), target.homeCoord());
				activity.setLinkId(target.home().getLinkId());
			} else {
				Location location = chooseLocation(locationsByActivityType.get(donorActivity.getType()), donorHome, resolveCoord(donorActivity, network), random);
				activity = factory.createActivityFromCoord(donorActivity.getType(), location.coord());
				activity.setLinkId(location.linkId());
			}
			copyTimes(donorActivity, activity);
			result.addActivity(activity);
			if (index < donor.activities().size() - 1) {
				Leg leg = factory.createLeg(TransportMode.walk);
				result.addLeg(leg);
			}
		}
		result.getAttributes().putAttribute(SYNTHETIC_PLAN, true);
		result.getAttributes().putAttribute(DONOR_PERSON_ID, donor.person().getId().toString());
		result.getAttributes().putAttribute(GENERATION_SEED, seed);
		result.getAttributes().putAttribute(GENERATION_RUN, generationRun);
		return result;
	}

	private static Location chooseLocation(List<Location> locations, Coord donorHome, Coord donorActivity, SplittableRandom random) {
		double desiredDistance = squaredDistance(donorHome, donorActivity);
		double bestDistanceDifference = locations.stream()
			.mapToDouble(location -> Math.abs(squaredDistance(donorHome, location.coord()) - desiredDistance))
			.min().orElseThrow();
		List<Location> equallyGood = locations.stream()
			.filter(location -> Math.abs(Math.abs(squaredDistance(donorHome, location.coord()) - desiredDistance) - bestDistanceDifference) < 1e-6)
			.toList();
		return equallyGood.get(random.nextInt(equallyGood.size()));
	}

	private static void copyTimes(Activity source, Activity target) {
		if (source.getStartTime().isDefined()) {
			target.setStartTime(source.getStartTime().seconds());
		}
		if (source.getEndTime().isDefined()) {
			target.setEndTime(source.getEndTime().seconds());
		}
		if (source.getMaximumDuration().isDefined()) {
			target.setMaximumDuration(source.getMaximumDuration().seconds());
		}
	}

	private static List<Activity> mainActivities(Plan plan) {
		return plan.getPlanElements().stream()
			.filter(Activity.class::isInstance)
			.map(Activity.class::cast)
			.filter(activity -> !TripStructureUtils.isStageActivityType(activity.getType()))
			.toList();
	}

	private static boolean isHome(Activity activity) {
		return activity.getType().startsWith("home");
	}

	private static boolean isFreight(Person person) {
		return "freight".equals(person.getAttributes().getAttribute("subpopulation"));
	}

	private static int age(Person person) {
		Integer age = PersonUtils.getAge(person);
		return age == null ? -1 : age;
	}

	static String ageBand(int age) {
		if (age <= 29) {
			return "18-29";
		}
		if (age <= 49) {
			return "30-49";
		}
		if (age <= 69) {
			return "50-69";
		}
		return "70+";
	}

	private static String attribute(Person person, String name) {
		Object value = person.getAttributes().getAttribute(name);
		return value == null ? "unknown" : value.toString();
	}

	private static Coord resolveCoord(Activity activity, Network network) {
		if (activity.getCoord() != null) {
			return activity.getCoord();
		}
		Link link = resolveLink(activity, network);
		return link == null ? null : link.getCoord();
	}

	private static Link resolveLink(Activity activity, Network network) {
		if (network == null || activity.getLinkId() == null) {
			return null;
		}
		return network.getLinks().get(activity.getLinkId());
	}

	private static double squaredDistance(Coord first, Coord second) {
		double dx = first.getX() - second.getX();
		double dy = first.getY() - second.getY();
		return dx * dx + dy * dy;
	}

	private static boolean isUrl(String value) {
		return value.startsWith("http://") || value.startsWith("https://");
	}

	private static MatchingCohort matchingCohort(int level, String ageBand, String sex, String incomeGroup) {
		return switch (level) {
			case 0 -> new MatchingCohort(level, ageBand, sex, incomeGroup);
			case 1 -> new MatchingCohort(level, ageBand, sex, "all");
			case 2 -> new MatchingCohort(level, ageBand, "all", "all");
			case 3 -> new MatchingCohort(level, "all", "all", "all");
			default -> throw new IllegalArgumentException("Unknown matching level: " + level);
		};
	}

	private record Target(Person person, Activity home, Coord homeCoord) {
	}

	private record Donor(Person person, List<Activity> activities) {
	}

	private record DonorMatch(Donor donor, int level) {
	}

	private record MatchingCohort(int level, String ageBand, String sex, String incomeGroup) {
	}

	private record Location(Coord coord, Id<Link> linkId) {
	}
}
