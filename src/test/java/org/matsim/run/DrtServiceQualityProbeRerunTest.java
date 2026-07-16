package org.matsim.run;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.optimizer.constraints.DrtRouteConstraints;
import org.matsim.contrib.drt.optimizer.insertion.selective.SelectiveInsertionSearchParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.load.IntegerLoadType;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.testcases.MatsimTestUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DrtServiceQualityProbeRerunTest {
	private static final String DRT_MODE = TransportMode.drt;
	private static final IntegerLoadType LOAD_TYPE = new IntegerLoadType("passengers");
	private static final int FIRST_RUN_ITERATIONS = 1;
	private static final double PROBE_NUMERIC_TOLERANCE = 0.01;

	@RegisterExtension
	final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void rerunningFinalPlansForZeroIterationsProducesIdenticalServiceQualityProbe() throws IOException {
		Path fixtureDirectory = Path.of(utils.getOutputDirectory()).resolve("fixture");
		Fixture fixture = createFixture(fixtureDirectory);
		Path iterativeOutput = Path.of(utils.getOutputDirectory()).resolve("iterative");

		run(fixture, iterativeOutput, FIRST_RUN_ITERATIONS, null);

		Path effectiveIterativeOutput = effectiveOutput(iterativeOutput, FIRST_RUN_ITERATIONS);
		Path finalPlans = effectiveIterativeOutput.resolve("ITERS/it." + FIRST_RUN_ITERATIONS
			+ "/null-iter_" + FIRST_RUN_ITERATIONS + "." + FIRST_RUN_ITERATIONS + ".plans.xml.zst");
		Path iterativeProbe = effectiveIterativeOutput.resolve("null-iter_" + FIRST_RUN_ITERATIONS + ".drt_service_quality_probes.csv.gz");
		assertThat(finalPlans).isRegularFile();
		assertThat(iterativeProbe).isRegularFile();

		Path rerunOutput = Path.of(utils.getOutputDirectory()).resolve("rerun");
		run(fixture, rerunOutput, 0, finalPlans);

		Path rerunProbe = effectiveOutput(rerunOutput, 0).resolve("null-iter_0.drt_service_quality_probes.csv.gz");
		assertThat(rerunProbe).isRegularFile();
		assertProbeCsvEqual(readLines(iterativeProbe), readLines(rerunProbe));
	}

	private static void assertProbeCsvEqual(List<String> expected, List<String> actual) {
		assertThat(actual).hasSameSizeAs(expected);
		assertThat(actual.get(0)).isEqualTo(expected.get(0));
		for (int row = 1; row < expected.size(); row++) {
			String[] expectedColumns = expected.get(row).split(";", -1);
			String[] actualColumns = actual.get(row).split(";", -1);
			assertThat(actualColumns).as("probe row %d", row).hasSameSizeAs(expectedColumns);
			for (int column = 0; column < expectedColumns.length; column++) {
				if (column == 1 || column == 2) {
					assertThat(actualColumns[column]).as("probe row %d column %d", row, column)
						.isEqualTo(expectedColumns[column]);
				} else {
					double difference = Math.abs(Double.parseDouble(actualColumns[column])
						- Double.parseDouble(expectedColumns[column]));
					assertThat(difference).as("probe row %d column %d", row, column)
						.isLessThanOrEqualTo(PROBE_NUMERIC_TOLERANCE);
				}
			}
		}
	}

	private static Path effectiveOutput(Path outputDirectory, int iterations) {
		return outputDirectory.resolveSibling(outputDirectory.getFileName() + "-iter_" + iterations);
	}

	private static void run(Fixture fixture, Path outputDirectory, int iterations, Path plans) {
		Config config = createConfig(fixture, outputDirectory, plans);
		Path configFile = outputDirectory.getParent().resolve(outputDirectory.getFileName() + "-config.xml");
		ConfigUtils.writeConfig(config, configFile.toString());
		String[] args = plans == null
			? new String[]{"run", "--config", configFile.toString(), "--with-drt", "--iterations=" + iterations, "--random-seed", "4711",
				"--write-drt-service-quality-probe", "--drt-service-quality-probe-stop-pair-input-files",
				fixture.stopPairs().toString()}
			: new String[]{"run", "--config", configFile.toString(), "--with-drt", "--iterations=" + iterations, "--random-seed", "4711",
				"--write-drt-service-quality-probe", "--drt-service-quality-probe-stop-pair-input-files",
				fixture.stopPairs().toString()};
		MATSimApplication.execute(LocalRunKelheimScenario.class, args);
	}

	private static Config createConfig(Fixture fixture, Path outputDirectory, Path plans) {
		Config config = ConfigUtils.createConfig();
		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);
		config.qsim().setStartTime(0);
		config.qsim().setEndTime(24 * 3600);
		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		config.qsim().setSimEndtimeInterpretation(QSimConfigGroup.EndtimeInterpretation.onlyUseEndtime);
		config.network().setInputFile(fixture.network().toString());
		Path populationInput = plans == null ? fixture.population() : plans;
		config.plans().setInputFile(plans == null ? populationInput.toString() : populationInput.toUri().toString());
		config.controller().setOutputDirectory(outputDirectory.toString());
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setWritePlansInterval(10);
		config.replanning().clearStrategySettings();
		StrategySettings keepLastSelected = new StrategySettings();
		keepLastSelected.setStrategyName(DefaultSelector.KeepLastSelected);
		keepLastSelected.setWeight(1);
		config.replanning().addStrategySettings(keepLastSelected);
		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(12 * 3600);
		config.scoring().addActivityParams(home);
		ActivityParams work = new ActivityParams("work");
		work.setTypicalDuration(8 * 3600);
		config.scoring().addActivityParams(work);
		config.scoring().addModeParams(new ModeParams(DRT_MODE));
		SimWrapperConfigGroup simWrapper = new SimWrapperConfigGroup();
		simWrapper.setDefaultDashboards(SimWrapperConfigGroup.DefaultDashboardsMode.disabled);
		simWrapper.setPackages(Set.of("org.matsim.test.noop"));
		config.addModule(simWrapper);

		DvrpConfigGroup dvrpConfig = new DvrpConfigGroup();
		dvrpConfig.setNetworkModes(Set.of(DRT_MODE));
		config.addModule(dvrpConfig);
		MultiModeDrtConfigGroup multiModeDrtConfig = new MultiModeDrtConfigGroup(DrtWithExtensionsConfigGroup::new);
		DrtWithExtensionsConfigGroup drtConfig = (DrtWithExtensionsConfigGroup) multiModeDrtConfig.createParameterSet("drt");
		drtConfig.setMode(DRT_MODE);
		drtConfig.setOperationalScheme(DrtConfigGroup.OperationalScheme.stopbased);
		drtConfig.setTransitStopFile(fixture.stops().toString());
		drtConfig.setVehiclesFile(fixture.fleet().toString());
		drtConfig.setStopDuration(30);
		drtConfig.setUseModeFilteredSubnetwork(true);
		drtConfig.addDrtInsertionSearchParams(new SelectiveInsertionSearchParams());
		var constraints = drtConfig.addOrGetDrtOptimizationConstraintsParams()
			.addOrGetDefaultDrtOptimizationConstraintsSet();
		constraints.setMaxWaitTime(1_800);
		constraints.setMaxTravelTimeAlpha(2);
		constraints.setMaxTravelTimeBeta(1_800);
		constraints.setRejectRequestIfMaxWaitOrTravelTimeViolated(false);
		multiModeDrtConfig.addParameterSet(drtConfig);
		config.addModule(multiModeDrtConfig);

		return config;
	}

	private static Fixture createFixture(Path directory) throws IOException {
		directory = directory.toAbsolutePath();
		Files.createDirectories(directory);
		Path networkFile = directory.resolve("network.xml.gz");
		Path populationFile = directory.resolve("population.xml.gz");
		Path stopsFile = directory.resolve("drt-stops.xml.gz");
		Path fleetFile = directory.resolve("drt-fleet.xml");
		Path stopPairsFile = directory.resolve("stop-pairs.csv");

		Network network = createNetwork();
		NetworkUtils.writeNetwork(network, networkFile.toString());
		writePopulation(populationFile);
		writeStops(stopsFile);
		writeFleet(fleetFile);
		try (BufferedWriter writer = Files.newBufferedWriter(stopPairsFile)) {
			writer.write("accessStopId;accessLinkId;egressStopId;egressLinkId\n");
			writer.write("stop-a;a-b;stop-b;b-a\n");
		}

		return new Fixture(networkFile, populationFile, stopsFile, fleetFile, stopPairsFile);
	}

	private static Network createNetwork() {
		Network network = NetworkUtils.createNetwork();
		Node nodeA = network.getFactory().createNode(Id.createNodeId("a"), new Coord(0, 0));
		Node nodeB = network.getFactory().createNode(Id.createNodeId("b"), new Coord(1_000, 0));
		network.addNode(nodeA);
		network.addNode(nodeB);
		addLink(network, "a-b", nodeA, nodeB);
		addLink(network, "b-a", nodeB, nodeA);
		return network;
	}

	private static void addLink(Network network, String id, Node from, Node to) {
		Link link = network.getFactory().createLink(Id.createLinkId(id), from, to);
		link.setLength(1_000);
		link.setFreespeed(10);
		link.setCapacity(10_000);
		link.setAllowedModes(Set.of(TransportMode.car, DRT_MODE));
		network.addLink(link);
	}

	private static void writePopulation(Path populationFile) {
		Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		population.getFactory().getRouteFactories().setRouteFactory(DrtRoute.class, new org.matsim.contrib.drt.routing.DrtRouteFactory());
		addDrtPerson(population, "person-a", "a-b", "b-a", 7 * 3600);
		addDrtPerson(population, "person-b", "b-a", "a-b", 8 * 3600);
		PopulationUtils.writePopulation(population, populationFile.toString());
	}

	private static void addDrtPerson(Population population, String personId, String fromLink, String toLink, double departureTime) {
		Person person = population.getFactory().createPerson(Id.createPersonId(personId));
		PersonUtils.setIncome(person, 2_364);
		Plan plan = population.getFactory().createPlan();
		Activity origin = population.getFactory().createActivityFromLinkId("home", Id.createLinkId(fromLink));
		origin.setEndTime(departureTime);
		plan.addActivity(origin);
		Leg leg = population.getFactory().createLeg(DRT_MODE);
		DrtRoute route = new DrtRoute(Id.createLinkId(fromLink), Id.createLinkId(toLink));
		route.setDirectRideTime(100);
		route.setDistance(1_000);
		route.setTravelTime(300);
		route.setLoad(LOAD_TYPE.fromInt(1), LOAD_TYPE);
		route.setConstraints(new DrtRouteConstraints(1_800, 1_800, 1_800, 1_800, 0, true));
		leg.setRoute(route);
		plan.addLeg(leg);
		plan.addActivity(population.getFactory().createActivityFromLinkId("work", Id.createLinkId(toLink)));
		person.addPlan(plan);
		population.addPerson(person);
	}

	private static void writeStops(Path stopsFile) {
		TransitSchedule schedule = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getTransitSchedule();
		TransitScheduleFactory factory = schedule.getFactory();
		TransitStopFacility stopA = factory.createTransitStopFacility(Id.create("stop-a", TransitStopFacility.class), new Coord(500, 0), false);
		stopA.setLinkId(Id.createLinkId("a-b"));
		schedule.addStopFacility(stopA);
		TransitStopFacility stopB = factory.createTransitStopFacility(Id.create("stop-b", TransitStopFacility.class), new Coord(500, 0), false);
		stopB.setLinkId(Id.createLinkId("b-a"));
		schedule.addStopFacility(stopB);
		new TransitScheduleWriter(schedule).writeFile(stopsFile.toString());
	}

	private static void writeFleet(Path fleetFile) {
		ImmutableDvrpVehicleSpecification vehicle = ImmutableDvrpVehicleSpecification.newBuilder()
			.id(Id.create("drt-vehicle", DvrpVehicle.class))
			.startLinkId(Id.createLinkId("a-b"))
			.serviceBeginTime(6 * 3600)
			.serviceEndTime(23 * 3600)
			.capacity(4)
			.build();
		new FleetWriter(List.of(vehicle).stream(), LOAD_TYPE).write(fleetFile.toString());
	}

	private static List<String> readLines(Path file) throws IOException {
		try (BufferedReader reader = IOUtils.getBufferedReader(file.toString())) {
			return reader.lines().toList();
		}
	}

	private record Fixture(Path network, Path population, Path stops, Path fleet, Path stopPairs) {
	}

	public static class LocalRunKelheimScenario extends RunKelheimScenario {
		@Override
		protected Config prepareConfig(Config config) {
			Config preparedConfig = super.prepareConfig(config);
			if (preparedConfig.replanning().getStrategySettings().isEmpty()) {
				StrategySettings keepLastSelected = new StrategySettings();
				keepLastSelected.setStrategyName(DefaultSelector.KeepLastSelected);
				keepLastSelected.setWeight(1);
				preparedConfig.replanning().addStrategySettings(keepLastSelected);
			}
			preparedConfig.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
			return preparedConfig;
		}
	}
}
