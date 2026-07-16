package org.matsim.run;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import jakarta.annotation.Nullable;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.locationtech.jts.geom.Geometry;
import org.matsim.analysis.KelheimMainModeIdentifier;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.analysis.postAnalysis.accessibility.run.RunOfflineAccessibilityKelheim;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimApplication;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.application.prepare.CreateLandUseShp;
import org.matsim.application.prepare.longDistanceFreightGER.tripExtraction.ExtractRelevantFreightTrips;
import org.matsim.application.prepare.network.CreateNetworkFromSumo;
import org.matsim.application.prepare.population.*;
import org.matsim.application.prepare.pt.CreateTransitScheduleFromGtfs;
import org.matsim.contrib.common.conventions.vsp.SnzActivities;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.companions.DrtCompanionParams;
import org.matsim.contrib.drt.extension.companions.MultiModeDrtCompanionModule;
import org.matsim.contrib.drt.optimizer.insertion.parallel.DrtParallelInserterParams;
import org.matsim.contrib.drt.optimizer.insertion.parallel.ParallelRequestInserterModule;
import org.matsim.contrib.drt.optimizer.rebalancing.NoRebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.prebooking.PrebookingParams;
import org.matsim.contrib.drt.prebooking.logic.ProbabilityBasedPrebookingLogicParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpModeLimitedMaxSpeedTravelTimeModule;
import org.matsim.contrib.vsp.pt.fare.PtFareModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.drtFare.KelheimDrtFareModule;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesConfigGroup;
import org.matsim.run.prepare.PrepareNetwork;
import org.matsim.run.prepare.PreparePopulation;
import org.matsim.run.prepare.GenerateCounterfactualImmobilePlans;
import org.matsim.rebalancing.WaitingPointsBasedRebalancingModule;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import picocli.CommandLine;
import org.matsim.contrib.vsp.pt.fare.DistanceBasedPtFareParams;
import org.matsim.contrib.vsp.pt.fare.PtFareConfigGroup;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

import java.nio.file.Path;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

@CommandLine.Command(header = ":: Open Kelheim Scenario ::", version = RunKelheimScenario.VERSION, mixinStandardHelpOptions = true)
@MATSimApplication.Prepare({
	CreateNetworkFromSumo.class, CreateTransitScheduleFromGtfs.class, TrajectoryToPlans.class, GenerateShortDistanceTrips.class,
	MergePopulations.class, ExtractRelevantFreightTrips.class, DownSamplePopulation.class, PrepareNetwork.class, ExtractHomeCoordinates.class,
	CreateLandUseShp.class, ResolveGridCoordinates.class, PreparePopulation.class, CleanPopulation.class, FixSubtourModes.class, SplitActivityTypesDuration.class,
	GenerateCounterfactualImmobilePlans.class
})
@MATSimApplication.Analysis({
	RunOfflineAccessibilityKelheim.class
})


//--config
//input/v3.1/kelheim-v3.1-25pct.kexi.config.xml
//--1pct
//--with-drt
//--with-drt-expandedServiceArea
//--iterations=1
//	--drt-fleet-size=10
//	--write-drt-service-quality-probe
//--config:controller.overwriteFiles=deleteDirectoryIfExists
//--config:swissRailRaptor.useIntermodalAccessEgress=false
//run
public class RunKelheimScenario extends MATSimApplication {

	public static final String VERSION = "3.1";
	private static final double WEIGHT_1_PASSENGER = 22235.;
	private static final double WEIGHT_2_PASSENGER = 2850.;
	private static final double WEIGHT_3_PASSENGER = 752.;
	private static final double WEIGHT_4_PASSENGER = 233.;
	private static final double WEIGHT_5_PASSENGER = 28.;
	private static final double WEIGHT_6_PASSENGER = 18.;
	private static final double WEIGHT_7_PASSENGER = 1.;
	private static final double WEIGHT_8_PASSENGER = 0.;
	private static final double DRT_SERVICE_BEGIN_TIME = 21600.;
	private static final double DRT_SERVICE_END_TIME = 82800.;
	private static final String DRT_VEHICLE_TYPE = "conventional_vehicle";
	private static final List<Integer> DRT_SERVICE_QUALITY_PROBE_TIMES = IntStream.iterate(6 * 3600,
		time -> time <= 21 * 3600, time -> time + 60 * 60)
		.boxed()
		.toList();

	@CommandLine.Mixin
	private final SampleOptions sample = new SampleOptions(25, 10, 1);

	@CommandLine.Option(names = "--with-drt", defaultValue = "false", description = "enable DRT service")
	private boolean drt;

	@CommandLine.Option(names = "--with-drt-expandedServiceArea", defaultValue = "false", description = "enable DRT service")
	private boolean drtExpandedServiceArea;

	@CommandLine.Option(names = "--drt-fleet-size", defaultValue = "-1", description = "Replace the DRT fleet with this many conventional vehicles. If unset, keep the vehicles from the input file.")
	private int drtFleetSize;

	@CommandLine.Option(names = "--drt-fleet-start-link-weights", defaultValue = "", description = "Optional semicolon-delimited CSV/CSV.GZ with linkId and weight columns for population-weighted fleet placement.")
	private String drtFleetStartLinkWeights;

	@CommandLine.Option(names = "--write-drt-service-quality-probe", defaultValue = "false", description = "Write DRT service quality probes in the last iteration.")
	private boolean writeDrtServiceQualityProbe;

	@CommandLine.Option(names = "--drt-service-quality-probe-stop-pair-input-files", defaultValue = "", description = "Comma-separated accessibility stop-pair CSV/CSV.GZ files. When set, probe only their unique directed stop pairs.")
	private String drtServiceQualityProbeStopPairInputFiles;

	@CommandLine.Option(names = "--prebooking", defaultValue = "false", description = "Enable probability-based prebooking for the conventional DRT mode.")
	private boolean prebooking;

	@CommandLine.Option(names = "--prebooking-probability", defaultValue = "1.0", description = "Probability that a conventional DRT trip is prebooked (0.0 to 1.0).")
	private double prebookingProbability;

	@CommandLine.Option(names = "--prebooking-submission-slack", defaultValue = "1800", description = "Seconds before planned departure at which a prebooking request is submitted.")
	private double prebookingSubmissionSlack;
	// a couple of CommandLine.Options below actually are not strictly necessary but rather allow for circumvention of settings directly via config and/or config options.... (ts 07/23)

	/**
	 * the KEXI service has a zone-dependent fare system which is why we are using a custom fare implementation. Via this option, one can set a flat (constant) price for the AV service.
	 */
	@CommandLine.Option(names = "--av-fare", defaultValue = "0.0", description = "AV fare (euro per trip)")
	private double avFare;

	@CommandLine.Option(names = "--bike-rnd", defaultValue = "false", description = "enable randomness in ASC of bike")
	private boolean bikeRnd;

	@CommandLine.Option(names = "--random-seed", defaultValue = "4711", description = "setting random seed for the simulation")
	private long randomSeed;

	@CommandLine.Option(names = "--intermodal", defaultValue = "false", description = "enable intermodality for DRT service")
	private boolean intermodal;

	@CommandLine.Option(names = "--plans", defaultValue = "", description = "Use different input plans")
	private String planOrigin;

	@CommandLine.Option(names = "--base-fare", defaultValue = "2.0", description = "Base fare of KEXI trip")
	private double baseFare;

	@CommandLine.Option(names = "--surcharge", defaultValue = "1.0", description = "Surcharge of KEXI trip from / to train station")
	private double surcharge;

	@CommandLine.Option(names = "--drt-fare-zone-shp", defaultValue = "", description = "Optional DRT fare-zone shapefile. If unset, every DRT trip is charged the base fare.")
	private String drtFareZoneShp;

	@CommandLine.Option(names = "--rebalancing", description = "enable waiting point based rebalancing strategy or not", defaultValue = "false")
	private boolean rebalancing;

	@CommandLine.Option(names = "--waiting-points", description = "waiting points for rebalancing strategy. If unspecified, the starting" +
		"points of the fleet will be set as waiting points", defaultValue = "")
	private String waitingPointsPath;
	@CommandLine.Option(names = "--drt-expanded-service-area-stops", defaultValue = "expanded-service-area/drt_stops_landkreis.xml", description = "Transit stop file used with --with-drt-expandedServiceArea. Relative paths are resolved against the configuration file.")
	private String expandedDrtStopsFile;

	@CommandLine.Option(names = "--drt-study-area-shp", defaultValue = "input/shp/lk-kelheim/lk-kelheim.shp", description = "Study-area shapefile used to define the expanded DRT service area and eligible fleet start links. Relative paths are resolved against the working directory.")
	private String drtServiceAreaShp;


	public RunKelheimScenario(@Nullable Config config) {
		super(config);
	}

	public RunKelheimScenario() {
		super();
		configPath = String.format("input/v%s/kelheim-v%s-config.xml", VERSION, VERSION);
	}

	public static void main(String[] args) {
		MATSimApplication.run(RunKelheimScenario.class, args);
	}

	public static void addDrtCompanionParameters(DrtWithExtensionsConfigGroup drtWithExtensionsConfigGroup) {
		DrtCompanionParams drtCompanionParams = new DrtCompanionParams();
		drtCompanionParams.setDrtCompanionSamplingWeights(List.of(
			WEIGHT_1_PASSENGER,
			WEIGHT_2_PASSENGER,
			WEIGHT_3_PASSENGER,
			WEIGHT_4_PASSENGER,
			WEIGHT_5_PASSENGER,
			WEIGHT_6_PASSENGER,
			WEIGHT_7_PASSENGER,
			WEIGHT_8_PASSENGER
		));
		drtWithExtensionsConfigGroup.addParameterSet(drtCompanionParams);
	}

	static void addProbabilityBasedPrebooking(DrtConfigGroup drtConfigGroup, double probability, double submissionSlack) {
		if (probability < 0 || probability > 1) {
			throw new IllegalArgumentException("--prebooking-probability must be between 0.0 and 1.0.");
		}
		if (submissionSlack < 0) {
			throw new IllegalArgumentException("--prebooking-submission-slack must not be negative.");
		}

		drtConfigGroup.getPrebookingParams().ifPresent(drtConfigGroup::removeParameterSet);
		PrebookingParams prebookingParams = new PrebookingParams();
		ProbabilityBasedPrebookingLogicParams logicParams = new ProbabilityBasedPrebookingLogicParams();
		logicParams.setProbability(probability);
		logicParams.setSubmissionSlack(submissionSlack);
		prebookingParams.addParameterSet(logicParams);
		drtConfigGroup.addParameterSet(prebookingParams);
	}

	@Nullable
	@Override
	protected Config prepareConfig(Config config) {

		SnzActivities.addScoringParams(config);

		config.controller().setOutputDirectory(sample.adjustName(config.controller().getOutputDirectory()));
		config.plans().setInputFile(sample.adjustName(config.plans().getInputFile()));
		config.controller().setRunId(sample.adjustName(config.controller().getRunId()));

		config.qsim().setFlowCapFactor(sample.getSize() / 100.0);
		config.qsim().setStorageCapFactor(sample.getSize() / 100.0);

		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort);
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);

		config.global().setRandomSeed(randomSeed);

		SimWrapperConfigGroup sw = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);

		sw.defaultParams().setShp(Path.of("input/shp/dilutionArea.shp").toAbsolutePath().toString());
		sw.defaultParams().setMapCenter("11.89,48.91");
		sw.defaultParams().setMapZoomLevel(11d);
		sw.setSampleSize(sample.getSample());

		if (intermodal) {
			ConfigUtils.addOrGetModule(config, PtIntermodalRoutingModesConfigGroup.class);
		}

		if (drt) {
			config.addModule(new MultiModeDrtConfigGroup(DrtWithExtensionsConfigGroup::new));

			MultiModeDrtConfigGroup multiModeDrtConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
			if (multiModeDrtConfig.getModalElements().isEmpty()) {
				throw new IllegalStateException("--with-drt requires a config with at least one DRT parameter set. "
					+ "Use e.g. --config input/v" + VERSION + "/kelheim-v" + VERSION + "-25pct.kexi.config.xml");
			}

			for (DrtConfigGroup drtConfigGroup : multiModeDrtConfig.getModalElements()) {
				//only the KEXI (conventionally driven drt) should get companions
				if (drtConfigGroup.getMode().equals(TransportMode.drt)) {
					DrtWithExtensionsConfigGroup drtWithExtensionsConfigGroup = (DrtWithExtensionsConfigGroup) drtConfigGroup;
					addDrtCompanionParameters(drtWithExtensionsConfigGroup);
					if (prebooking) {
						addProbabilityBasedPrebooking(drtConfigGroup, prebookingProbability, prebookingSubmissionSlack);
					}

					if (drtExpandedServiceArea) {
						drtConfigGroup.setTransitStopFile(expandedDrtStopsFile);
					}

					if (writeDrtServiceQualityProbe) {
						DrtParallelInserterParams parallelInserterParams = drtConfigGroup.getDrtParallelInserterParams()
							.orElseGet(() -> {
								DrtParallelInserterParams params = new DrtParallelInserterParams();
								drtConfigGroup.addParameterSet(params);
								return params;
							});
						parallelInserterParams.setWriteServiceQualityProbes(true);
						String probeTimesString = DRT_SERVICE_QUALITY_PROBE_TIMES.stream()
							.map(String::valueOf)
							.collect(java.util.stream.Collectors.joining(","));

						parallelInserterParams.setServiceQualityProbeTimes(probeTimesString);
						if (drtServiceQualityProbeStopPairInputFiles.isBlank()) {
							parallelInserterParams.setServiceQualityProbeSpatialResolution(
								DrtParallelInserterParams.ServiceQualityProbeSpatialResolution.ZONE_TO_ZONE
							);
							parallelInserterParams.setServiceQualityProbeZoneCellSize(1000.);
						} else {
							parallelInserterParams.setServiceQualityProbeSpatialResolution(
								DrtParallelInserterParams.ServiceQualityProbeSpatialResolution.STOP_TO_STOP
							);
							parallelInserterParams.setServiceQualityProbeStopPairInputFiles(
								drtServiceQualityProbeStopPairInputFiles
							);
						}
					}
				}
			}

			ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);
			config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
			DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.scoring(), config.routing());
		}

		// Config is always needed
		/* Informed-Mode-Choice
		MultiModeDrtEstimatorConfigGroup estimatorConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtEstimatorConfigGroup.class);
		// Use estimators with default values
		estimatorConfig.addParameterSet(new DrtEstimatorConfigGroup("drt"));
		 */

		PtFareConfigGroup ptFareConfigGroup = ConfigUtils.addOrGetModule(config, PtFareConfigGroup.class);
		DistanceBasedPtFareParams distanceBasedPtFareParams = ConfigUtils.addOrGetModule(config, DistanceBasedPtFareParams.class);

		// Set parameters
		ptFareConfigGroup.setApplyUpperBound(true);
		ptFareConfigGroup.setUpperBoundFactor(1.5);

		// Minimum fare (e.g. short trip or 1 zone ticket)
		distanceBasedPtFareParams.setMinFare(2.0);

		distanceBasedPtFareParams.setTransactionPartner("pt-operator");
		DistanceBasedPtFareParams.DistanceClassLinearFareFunctionParams shortDistance = distanceBasedPtFareParams.getOrCreateDistanceClassFareParams(50000);
		shortDistance.setFareIntercept(1.6);
		shortDistance.setFareSlope(0.00017);

		DistanceBasedPtFareParams.DistanceClassLinearFareFunctionParams longDistance = distanceBasedPtFareParams.getOrCreateDistanceClassFareParams(Double.POSITIVE_INFINITY);
		longDistance.setFareIntercept(30);
		longDistance.setFareSlope(0.00025);
		distanceBasedPtFareParams.setOrder(1);

		ptFareConfigGroup.addParameterSet(distanceBasedPtFareParams);

		//enable plan inheritance analysis
		config.planInheritance().setEnabled(true);

		if (iterations != -1)
			addRunOption(config, "iter", iterations);

		if (!planOrigin.isBlank()) {
			String plans = planOrigin.strip();
			if (isPlansFile(plans)) {
				config.plans().setInputFile(plans);
			} else {
				config.plans().setInputFile(
					config.plans().getInputFile().replace(".plans", ".plans-" + plans)
				);
			}

			addRunOption(config, "plans", getRunOptionLabel(plans));
		}

		return config;
	}

	@Override
	protected Scenario createScenario(Config config) {
		Scenario scenario = ScenarioUtils.createScenario(config);
		if (drt) {
			registerDrtRouteFactory(scenario);
		}

		ScenarioUtils.loadScenario(scenario);
		return scenario;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {

		for (Link link : scenario.getNetwork().getLinks().values()) {
			Set<String> modes = link.getAllowedModes();

			// allow freight traffic together with cars
			if (modes.contains("car")) {
				Set<String> newModes = Sets.newHashSet(modes);
				newModes.add("freight");

				link.setAllowedModes(newModes);
			}
		}

		if (drt) {
			registerDrtRouteFactory(scenario);

			Geometry drtServiceArea = null;
			if (drtExpandedServiceArea) {

				// add 11km buffer around LK  to include those drt stops that are located outside of LK
				drtServiceArea = getBufferedDrtServiceArea();

				for (Link link : scenario.getNetwork().getLinks().values()) {
					if (drtServiceArea.contains(MGC.coord2Point(link.getCoord()))) {

						Set<String> updatedModes = new HashSet<>(link.getAllowedModes());
						updatedModes.add(TransportMode.drt);
						link.setAllowedModes(updatedModes);

					}
				}

				NetworkUtils.cleanNetwork(scenario.getNetwork(), Set.of(TransportMode.drt));
			}

			if (drtFleetSize > 0) {
				if (drtServiceArea == null) {
					drtServiceArea = getBufferedDrtServiceArea();
				}
				replaceDrtFleet(scenario, drtServiceArea);
			}
		}

		if (bikeRnd) {
			SplittableRandom bicycleRnd = new SplittableRandom(8765);
			for (Person person : scenario.getPopulation().getPersons().values()) {
				//TODO this value is to be determined
				double width = 2;
				double number = width * (bicycleRnd.nextGaussian());
				person.getAttributes().putAttribute("bicycleLove", number);
			}
		}

	}

	@Override
	protected void prepareControler(Controler controler) {
		Config config = controler.getConfig();
		Network network = controler.getScenario().getNetwork();

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new PtFareModule());
				install(new SwissRailRaptorModule());
				install(new PersonMoneyEventsAnalysisModule());
				install(new SimWrapperModule());

				bind(AnalysisMainModeIdentifier.class).to(KelheimMainModeIdentifier.class);
//				addControlerListenerBinding().to(ModeChoiceCoverageControlerListener.class);

				/*
				if (strategy.getModeChoice() == StrategyOptions.ModeChoice.randomSubtourMode) {
					// Configure mode-choice strategy
					install(strategy.applyModule(binder(), config, builder ->
								builder.withFixedCosts(FixedCostsEstimator.DailyConstant.class, TransportMode.car)
									.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.AlwaysAvailable.class, TransportMode.bike, TransportMode.ride, TransportMode.walk)
									.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.ConsiderIfCarAvailable.class, TransportMode.car)
//											.withLegEstimator(MultiModalDrtLegEstimator.class, ModeOptions.AlwaysAvailable.class, "drt", "av")
									.withTripEstimator(PtTripWithDistanceBasedFareEstimator.class, ModeOptions.AlwaysAvailable.class, TransportMode.pt)
									.withActivityEstimator(DefaultActivityEstimator.class)
									// These are with activity estimation enabled
									.withPruner("ad999", new DistanceBasedPruner(3.03073657, 0.22950583))
									.withPruner("ad99", new DistanceBasedPruner(2.10630819, 0.0917091))
									.withPruner("ad95", new DistanceBasedPruner(1.72092386, 0.03189323))
						)
					);
				}
				*/

				//use income-dependent marginal utility of money
				bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class).asEagerSingleton();

				if (bikeRnd) {
					addEventHandlerBinding().toInstance(new PersonDepartureEventHandler() {
						@Inject
						EventsManager events;
						@Inject
						Population population;

						@Override
						public void handleEvent(PersonDepartureEvent event) {
							if (event.getLegMode().equals(TransportMode.bike)) {
								double bicycleLove = (double) population.getPersons().get(event.getPersonId()).getAttributes().getAttribute("bicycleLove");
								events.processEvent(new PersonScoreEvent(event.getTime(), event.getPersonId(), bicycleLove, "bicycleLove"));
							}
						}
					});
				}
			}
		});

		if (drt) {
			MultiModeDrtConfigGroup multiModeDrtConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
			controler.addOverridingModule(new DvrpModule());
			controler.addOverridingModule(new MultiModeDrtModule());
			controler.addOverridingModule(new MultiModeDrtCompanionModule());
			controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfig));

			boolean hasAvMode = multiModeDrtConfig.getModalElements().stream()
				.anyMatch(drtCfg -> drtCfg.getMode().equals("av"));
			VehicleType autonomousVehicleType = controler.getScenario()
				.getVehicles()
				.getVehicleTypes()
				.get(Id.create("autonomous_vehicle", VehicleType.class));
			if (hasAvMode && autonomousVehicleType != null) {
				controler.addOverridingModule(
					new DvrpModeLimitedMaxSpeedTravelTimeModule("av", config.qsim().getTimeStepSize(),
						autonomousVehicleType.getMaximumVelocity()));
			}

			for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
				controler.addOverridingModule(new KelheimDrtFareModule(drtCfg, network, avFare, baseFare, surcharge, drtFareZoneShp));
				if (writeDrtServiceQualityProbe && drtCfg.getMode().equals(TransportMode.drt)) {
					controler.addOverridingQSimModule(new ParallelRequestInserterModule(drtCfg));
				}
				if (rebalancing && drtCfg.getMode().equals("av")) {
					controler.addOverridingModule(new WaitingPointsBasedRebalancingModule(drtCfg, waitingPointsPath));
				} else {
					// No rebalancing strategy
					controler.addOverridingModule(new AbstractDvrpModeModule(drtCfg.getMode()) {
						@Override
						public void install() {
							bindModal(RebalancingStrategy.class).to(NoRebalancingStrategy.class).asEagerSingleton();
						}
					});
				}
			}

			//controler.addOverridingModule(new DrtEstimatorModule());

			// TODO: when to include AV?
			//estimatorConfig.addParameterSet(new DrtEstimatorConfigGroup("av"));

		}
	}

	private Geometry getBufferedDrtServiceArea() {
		return new ShpOptions(drtServiceAreaShp, null, null)
			.getGeometry()
			.buffer(11000);
	}

	private static boolean isPlansFile(String plans) {
		String lowerCasePlans = plans.toLowerCase();
		return Path.of(plans).isAbsolute()
			|| lowerCasePlans.endsWith(".xml")
			|| lowerCasePlans.endsWith(".xml.gz")
			|| lowerCasePlans.endsWith(".xml.zst");
	}

	private static String getRunOptionLabel(String plans) {
		String fileName = isPlansFile(plans) ? Path.of(plans).getFileName().toString() : plans;
		String label = fileName
			.replaceAll("\\.xml(\\.gz|\\.zst)?$", "")
			.replaceAll("[^A-Za-z0-9_-]+", "-")
			.replaceAll("^-+|-+$", "");

		if (label.isBlank()) {
			return "custom";
		}

		return label;
	}

	private static void registerDrtRouteFactory(Scenario scenario) {
		scenario.getPopulation()
			.getFactory()
			.getRouteFactories()
			.setRouteFactory(DrtRoute.class, new DrtRouteFactory());
	}

	private void replaceDrtFleet(Scenario scenario, Geometry serviceArea) {
		Vehicles vehicles = scenario.getVehicles();
		VehicleType vehicleType = vehicles.getVehicleTypes().get(Id.create(DRT_VEHICLE_TYPE, VehicleType.class));
		if (vehicleType == null) {
			throw new IllegalStateException("Cannot generate DRT fleet: vehicle type '" + DRT_VEHICLE_TYPE + "' is missing.");
		}

		List<Id<Vehicle>> oldDrtVehicles = vehicles.getVehicles().values().stream()
			.filter(vehicle -> TransportMode.drt.equals(vehicle.getAttributes().getAttribute("dvrpMode")))
			.map(Vehicle::getId)
			.toList();
		oldDrtVehicles.forEach(vehicles::removeVehicle);

		List<Link> eligibleStartLinks = scenario.getNetwork().getLinks().values().stream()
			.map(link -> (Link)link)
			.filter(link -> link.getAllowedModes().contains(TransportMode.drt))
			.filter(link -> serviceArea.contains(MGC.coord2Point(link.getCoord())))
			.sorted(Comparator.comparing(link -> link.getId().toString()))
			.toList();

		if (eligibleStartLinks.isEmpty()) {
			throw new IllegalStateException("Cannot generate DRT fleet: no DRT links found in the configured service area.");
		}

		List<WeightedStartLink> startLinks = drtFleetStartLinkWeights.isBlank()
			? eligibleStartLinks.stream().map(link -> new WeightedStartLink(link, 1.)).toList()
			: readWeightedStartLinks(drtFleetStartLinkWeights, eligibleStartLinks);
		double totalWeight = startLinks.stream().mapToDouble(WeightedStartLink::weight).sum();
		// This generator is deliberately independent of MatsimRandom, so fleet placement does not shift any global RNG stream.
		SplittableRandom random = new SplittableRandom(randomSeed);
		for (int i = 0; i < drtFleetSize; i++) {
			Link startLink = drawStartLink(startLinks, totalWeight, random);
			Vehicle vehicle = vehicles.getFactory()
				.createVehicle(Id.createVehicleId("KEXI-" + (i + 1)), vehicleType);
			vehicle.getAttributes().putAttribute("dvrpMode", TransportMode.drt);
			vehicle.getAttributes().putAttribute("startLink", startLink.getId().toString());
			vehicle.getAttributes().putAttribute("serviceBeginTime", DRT_SERVICE_BEGIN_TIME);
			vehicle.getAttributes().putAttribute("serviceEndTime", DRT_SERVICE_END_TIME);
			vehicles.addVehicle(vehicle);
		}
	}

	private static List<WeightedStartLink> readWeightedStartLinks(String csvFile, List<Link> eligibleStartLinks) {
		Map<Id<Link>, Link> eligibleById = new LinkedHashMap<>();
		eligibleStartLinks.forEach(link -> eligibleById.put(link.getId(), link));
		CSVFormat format = CSVFormat.DEFAULT.builder().setDelimiter(';').setHeader().setSkipHeaderRecord(true).build();
		try (CSVParser parser = new CSVParser(IOUtils.getBufferedReader(csvFile), format)) {
			Map<Id<Link>, WeightedStartLink> weightedLinks = new LinkedHashMap<>();
			for (CSVRecord record : parser) {
				Id<Link> linkId = Id.createLinkId(record.get("linkId"));
				Link link = eligibleById.get(linkId);
				if (link == null) {
					throw new IllegalArgumentException("DRT fleet start-link weight references an ineligible or missing link: " + linkId);
				}
				double weight = Double.parseDouble(record.get("weight"));
				if (!Double.isFinite(weight) || weight < 0) {
					throw new IllegalArgumentException("Invalid DRT fleet start-link weight for " + linkId + ": " + weight);
				}
				if (weightedLinks.put(linkId, new WeightedStartLink(link, weight)) != null) {
					throw new IllegalArgumentException("Duplicate DRT fleet start-link weight for " + linkId);
				}
			}
			List<WeightedStartLink> result = weightedLinks.values().stream()
				.filter(weightedLink -> weightedLink.weight() > 0)
				.sorted(Comparator.comparing(weightedLink -> weightedLink.link().getId().toString()))
				.toList();
			if (result.isEmpty()) {
				throw new IllegalArgumentException("No positive DRT fleet start-link weights found in " + csvFile);
			}
			return result;
		} catch (IOException e) {
			throw new RuntimeException("Could not read DRT fleet start-link weights from " + csvFile, e);
		}
	}

	private static Link drawStartLink(List<WeightedStartLink> startLinks, double totalWeight, SplittableRandom random) {
		double draw = random.nextDouble(totalWeight);
		double cumulativeWeight = 0;
		for (WeightedStartLink startLink : startLinks) {
			cumulativeWeight += startLink.weight();
			if (draw < cumulativeWeight) {
				return startLink.link();
			}
		}
		return startLinks.getLast().link();
	}

	private record WeightedStartLink(Link link, double weight) {
	}
}
