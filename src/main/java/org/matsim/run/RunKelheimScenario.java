package org.matsim.run;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.matsim.analysis.KelheimMainModeIdentifier;
import org.matsim.analysis.ModeChoiceCoverageControlerListener;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.analysis.postAnalysis.drt.DrtServiceQualityAnalysis;
import org.matsim.analysis.postAnalysis.drt.DrtVehiclesRoadUsageAnalysis;
import org.matsim.api.core.v01.Coord;
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
import org.matsim.application.analysis.CheckPopulation;
import org.matsim.application.analysis.traffic.LinkStats;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.prepare.CreateLandUseShp;
import org.matsim.application.prepare.freight.tripExtraction.ExtractRelevantFreightTrips;
import org.matsim.application.prepare.network.CreateNetworkFromSumo;
import org.matsim.application.prepare.population.*;
import org.matsim.application.prepare.pt.CreateTransitScheduleFromGtfs;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.companions.DrtCompanionParams;
import org.matsim.contrib.drt.extension.companions.MultiModeDrtCompanionModule;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpModeLimitedMaxSpeedTravelTimeModule;
import org.matsim.contrib.vsp.scenario.SnzActivities;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.drtFare.KelheimDrtFareModule;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesConfigGroup;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.run.prepare.PrepareNetwork;
import org.matsim.run.prepare.PreparePopulation;
//import org.matsim.simwrapper.SimWrapperConfigGroup;
//import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.vehicles.VehicleType;
import picocli.CommandLine;
import playground.vsp.pt.fare.DistanceBasedPtFareParams;
import playground.vsp.pt.fare.PtFareConfigGroup;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

//run --1pct --with-drt --accessibility --config input/v3.1/kelheim-v3.1-25pct.kexi.config.xml

@CommandLine.Command(header = ":: Open Kelheim Scenario ::", version = RunKelheimScenario.VERSION, mixinStandardHelpOptions = true)
@MATSimApplication.Prepare({
	CreateNetworkFromSumo.class, CreateTransitScheduleFromGtfs.class, TrajectoryToPlans.class, GenerateShortDistanceTrips.class,
	MergePopulations.class, ExtractRelevantFreightTrips.class, DownSamplePopulation.class, PrepareNetwork.class, ExtractHomeCoordinates.class,
	CreateLandUseShp.class, ResolveGridCoordinates.class, PreparePopulation.class, CleanPopulation.class, FixSubtourModes.class, SplitActivityTypesDuration.class
})
@MATSimApplication.Analysis({
	LinkStats.class, CheckPopulation.class, DrtServiceQualityAnalysis.class, DrtVehiclesRoadUsageAnalysis.class
})
public class RunKelheimScenario extends MATSimApplication {

	public static final String VERSION = "3.1";
	private static final double WEIGHT_1_PASSENGER = 16517.;
	private static final double WEIGHT_2_PASSENGER = 2084.;
	private static final double WEIGHT_3_PASSENGER = 532.;
	private static final double WEIGHT_4_PASSENGER = 163.;
	private static final double WEIGHT_5_PASSENGER = 20.;
	private static final double WEIGHT_6_PASSENGER = 5.;
	private static final double WEIGHT_7_PASSENGER = 0.;
	private static final double WEIGHT_8_PASSENGER = 0.;
	@CommandLine.Mixin
	private final SampleOptions sample = new SampleOptions(25, 10, 1);

	@CommandLine.Option(names = "--with-drt", defaultValue = "false", description = "enable DRT service")
	private boolean drt;

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

	@CommandLine.Option(names = "--accessibility", defaultValue = "false", description = "enable accessibility calculations")
	private boolean acc;

	public RunKelheimScenario(@Nullable Config config) {
		super(config);
	}

	public RunKelheimScenario() {
		super(String.format("input/v%s/kelheim-v%s-config.xml", VERSION, VERSION));
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

	@Nullable
	@Override
	protected Config prepareConfig(Config config) {

		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(0);

		// stuff needed for accessibility
		SnzActivities.addScoringParams(config);

		config.controller().setOutputDirectory(sample.adjustName(config.controller().getOutputDirectory()));
		config.plans().setInputFile(sample.adjustName(config.plans().getInputFile()));
		config.controller().setRunId(sample.adjustName(config.controller().getRunId()));

		config.qsim().setFlowCapFactor(sample.getSize() / 100.0);
		config.qsim().setStorageCapFactor(sample.getSize() / 100.0);

		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort);
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);

		config.global().setRandomSeed(randomSeed);

//		SimWrapperConfigGroup sw = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
//
//		// Relative to config
//		sw.defaultParams().shp = "../shp/dilutionArea.shp";
//		sw.defaultParams().mapCenter = "11.89,48.91";
//		sw.defaultParams().mapZoomLevel = 11d;
//		sw.defaultParams().sampleSize = sample.getSample();

		if (intermodal) {
			ConfigUtils.addOrGetModule(config, PtIntermodalRoutingModesConfigGroup.class);
		}

		if (drt) {

			config.addModule(new MultiModeDrtConfigGroup(DrtWithExtensionsConfigGroup::new));

			MultiModeDrtConfigGroup multiModeDrtConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);

			for (DrtConfigGroup drtConfigGroup : multiModeDrtConfig.getModalElements()) {
				//only the KEXI (conventionally driven drt) should get companions
				if (drtConfigGroup.getMode().equals(TransportMode.drt)) {
					DrtWithExtensionsConfigGroup drtWithExtensionsConfigGroup = (DrtWithExtensionsConfigGroup) drtConfigGroup;
					addDrtCompanionParameters(drtWithExtensionsConfigGroup);
				}
			}

			ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);
			DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.scoring(), config.routing());
		}

		if (acc) {

			// yyyyyyy TODO: added to avoid following error "java.lang.RuntimeException: DynAgents require simulation to start from the very beginning. Set 'QSim.simStarttimeInterpretation' to onlyUseStarttime" -JR May'24
			config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);


			// yyyyyyy TODO: had to turn off intermodal pt access/egress because "= this.routingModules.get(mode) = null" in DefaultRaptorStopFinder. No drt router present...
			SwissRailRaptorConfigGroup swissRailRaptorConfigGroup = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
			swissRailRaptorConfigGroup.setUseIntermodalAccessEgress(false);

			MultiModeDrtConfigGroup multiModeDrtConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
			for (DrtConfigGroup drtConfigGroup : multiModeDrtConfig.getModalElements()) {
				//TODO: temp, allow accessibility computations to occur more than 1.5km away from drt stops.
				drtConfigGroup.maxWalkDistance = 100000.;

				drtConfigGroup.transitStopFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/kelheim-v3.0/input/kelheim-v3.0-drt-stops.xml";

			}

			// TODO: what is a good constant for DRT. The existing one of 2.45 makes drt trips really attractive; you no longer see a difference with stops that are far away and ones that are close.
//			ScoringConfigGroup.ModeParams drtParams = config.scoring().getOrCreateModeParams(TransportMode.drt);
//			drtParams.setConstant(0.0);


			AccessibilityConfigGroup accConfig = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
			//yyyyyyyyy this was neccessary for the RandomizingTimeDistanceTravelDisutility. TODO: is this compatible with the kelheim runs?
			config.routing().setRoutingRandomness(0);

// settings for kelheim city
			double mapCenterX = 712144.17;
			double mapCenterY = 5422153.87;

			double tileSize = 200;
			double num_rows = 23;

// settings for east kelheim:
//			double mapCenterX = 721455;
//			double mapCenterY = 5410601;
//
//			double tileSize = 200;
//			double num_rows = 50;

// settings for landkreis
//			double mapCenterX = 711014;
//			double mapCenterY = 5409253;
//
//			double tileSize = 500;
//			double num_rows = 50;

			accConfig.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromBoundingBox);
			accConfig.setBoundingBoxLeft(mapCenterX - num_rows*tileSize - tileSize/2);
			accConfig.setBoundingBoxRight(mapCenterX + num_rows*tileSize + tileSize/2);
			accConfig.setBoundingBoxBottom(mapCenterY - num_rows*tileSize - tileSize/2);
			accConfig.setBoundingBoxTop(mapCenterY + num_rows*tileSize + tileSize/2);
			accConfig.setTileSize_m((int) tileSize);
			accConfig.setTimeOfDay(19 * 60 * 60.);
			accConfig.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, false); // works
			accConfig.setComputingAccessibilityForMode(Modes4Accessibility.car, true); // works
//			accConfig.setComputingAccessibilityForMode(Modes4Accessibility.bike, false); // ??
			accConfig.setComputingAccessibilityForMode(Modes4Accessibility.pt, true); // works
			accConfig.setComputingAccessibilityForMode(Modes4Accessibility.estimatedDrt, true); // works
//			accConfig.setAccessibilityMeasureType(AccessibilityConfigGroup.AccessibilityMeasureType.gravity);
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
		// Division between long trip and short trip (unit: m)
		distanceBasedPtFareParams.setLongDistanceTripThreshold(50000);
		// y = ax + b --> a value, for short trips
		distanceBasedPtFareParams.setNormalTripSlope(0.00017);
		// y = ax + b --> b value, for short trips
		distanceBasedPtFareParams.setNormalTripIntercept(1.6);
		// y = ax + b --> a value, for long trips
		distanceBasedPtFareParams.setLongDistanceTripSlope(0.00025);
		// y = ax + b --> b value, for long trips
		distanceBasedPtFareParams.setLongDistanceTripIntercept(30);

		if (iterations != -1)
			addRunOption(config, "iter", iterations);

		if (!planOrigin.isBlank()) {
			config.plans().setInputFile(
				config.plans().getInputFile().replace(".plans", ".plans-" + planOrigin)
			);

			addRunOption(config, planOrigin);
		}

		return config;
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
			scenario.getPopulation()
				.getFactory()
				.getRouteFactories()
				.setRouteFactory(DrtRoute.class, new DrtRouteFactory());
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

		if (acc) {
			// add opportunity facility
			ActivityFacilitiesFactory af = scenario.getActivityFacilities().getFactory();


			// Use this method if reading facilities from a csv.
			Path filePath = Path.of("supermarkets_LK.csv");
			try (CSVParser parser = new CSVParser(new BufferedReader(new InputStreamReader(Files.newInputStream(filePath))),
				CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader())) {

				for (CSVRecord record : parser) {

					String id = record.get("id");
					double x = Double.parseDouble(record.get("x"));
					double y = Double.parseDouble(record.get("y"));
					String type = record.get("type");
					ActivityFacility fac = af.createActivityFacility(Id.create(id, ActivityFacility.class), new Coord(x, y));
					ActivityOption ao = af.createActivityOption(type);
					fac.addActivityOption(ao);
					scenario.getActivityFacilities().addActivityFacility(fac);


				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}


//			{// set facility manually
//				double trainStationX = 715041.71;
//				double trainStationY = 5420617.28;
//				ActivityFacility fac1 = af.createActivityFacility(Id.create("xxx", ActivityFacility.class), new Coord(trainStationX, trainStationY));
//				ActivityOption ao = af.createActivityOption("train station");
//				fac1.addActivityOption(ao);
//				scenario.getActivityFacilities().addActivityFacility(fac1);
//			}

		}

	}

	@Override
	protected void prepareControler(Controler controler) {
		Config config = controler.getConfig();
		Network network = controler.getScenario().getNetwork();

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new KelheimPtFareModule());
				install(new SwissRailRaptorModule());
				install(new PersonMoneyEventsAnalysisModule());
				//install(new SimWrapperModule());

				bind(AnalysisMainModeIdentifier.class).to(KelheimMainModeIdentifier.class);
				addControlerListenerBinding().to(ModeChoiceCoverageControlerListener.class);

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

			// Add speed limit to av vehicle
			// yyyyyyyy gives me error, no "autonomous_vehicle VehicleType found. Replaced with value for car, as a quick hack. -JR May'24
			double maxSpeed = controler.getScenario()
				.getVehicles()
				.getVehicleTypes()
//				.get(Id.create("autonomous_vehicle", VehicleType.class))
				.get(Id.create("car", VehicleType.class))
				.getMaximumVelocity();
			controler.addOverridingModule(
				new DvrpModeLimitedMaxSpeedTravelTimeModule("av", config.qsim().getTimeStepSize(),
					maxSpeed));

			for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
				controler.addOverridingModule(new KelheimDrtFareModule(drtCfg, network, avFare, baseFare, surcharge));
			}

			//controler.addOverridingModule(new DrtEstimatorModule());

			// TODO: when to include AV?
			//estimatorConfig.addParameterSet(new DrtEstimatorConfigGroup("av"));


//            if (intermodal){
//                controler.addOverridingModule(new IntermodalTripFareCompensatorsModule());
//                controler.addOverridingModule(new PtIntermodalRoutingModesModule());
//                controler.addOverridingModule(new AbstractModule() {
//                    @Override
//                    public void install() {
//                        bind(RaptorIntermodalAccessEgress.class).to(EnhancedRaptorIntermodalAccessEgress.class);
//                    }
//                });
//            }
		}

		if (acc) {
//			final AccessibilityModule moduleTrain = new AccessibilityModule();
//			moduleTrain.setConsideredActivityType("train_station");
//			controler.addOverridingModule(moduleTrain);

//			final AccessibilityModule moduleHealth = new AccessibilityModule();
//			moduleHealth.setConsideredActivityType("health");
//			controler.addOverridingModule(moduleHealth);
//
//			final AccessibilityModule moduleDoctors = new AccessibilityModule();
//			moduleDoctors.setConsideredActivityType("doctor");
//			controler.addOverridingModule(moduleDoctors);

//			final AccessibilityModule moduleSport = new AccessibilityModule();
//			moduleSport.setConsideredActivityType("sport");
//			controler.addOverridingModule(moduleSport);

//			final AccessibilityModule moduleGroceries = new AccessibilityModule();
//			moduleGroceries.setConsideredActivityType("groceries");
//			controler.addOverridingModule(moduleGroceries);

			final AccessibilityModule moduleSupermarkets = new AccessibilityModule();
			moduleSupermarkets.setConsideredActivityType("supermarket");
			controler.addOverridingModule(moduleSupermarkets);
//
//			final AccessibilityModule moduleAltstadt = new AccessibilityModule();
//			moduleAltstadt.setConsideredActivityType("altstadt");
//			controler.addOverridingModule(moduleAltstadt);

//			final AccessibilityModule moduleBuildings = new AccessibilityModule();
//			moduleBuildings.setConsideredActivityType("building");
//			controler.addOverridingModule(moduleBuildings);

//			final AccessibilityModule moduleSenioren = new AccessibilityModule();
//			moduleSenioren.setConsideredActivityType("senioren");
//			controler.addOverridingModule(moduleSenioren);
		}
	}
}
