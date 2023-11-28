package org.matsim.run;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import org.matsim.analysis.KelheimMainModeIdentifier;
import org.matsim.analysis.ModeChoiceCoverageControlerListener;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.analysis.postAnalysis.drt.DrtServiceQualityAnalysis;
import org.matsim.analysis.postAnalysis.drt.DrtVehiclesRoadUsageAnalysis;
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
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.companions.DrtCompanionParams;
import org.matsim.contrib.drt.extension.companions.MultiModeDrtCompanionModule;
import org.matsim.contrib.drt.extension.estimator.MultiModalDrtLegEstimator;
import org.matsim.contrib.drt.extension.estimator.run.DrtEstimatorConfigGroup;
import org.matsim.contrib.drt.extension.estimator.run.DrtEstimatorModule;
import org.matsim.contrib.drt.extension.estimator.run.MultiModeDrtEstimatorConfigGroup;
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
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.drtFare.KelheimDrtFareModule;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesConfigGroup;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.InformedModeChoiceModule;
import org.matsim.modechoice.ModeOptions;
import org.matsim.modechoice.estimators.DefaultActivityEstimator;
import org.matsim.modechoice.estimators.DefaultLegScoreEstimator;
import org.matsim.modechoice.estimators.FixedCostsEstimator;
import org.matsim.modechoice.pruning.DistanceBasedPruner;
import org.matsim.run.prepare.PrepareNetwork;
import org.matsim.run.prepare.PreparePopulation;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.vehicles.VehicleType;
import picocli.CommandLine;
import playground.vsp.pt.fare.DistanceBasedPtFareParams;
import playground.vsp.pt.fare.PtFareConfigGroup;
import playground.vsp.pt.fare.PtTripWithDistanceBasedFareEstimator;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

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

		// Relative to config
		sw.defaultParams().shp = "../shp/dilutionArea.shp";
		sw.defaultParams().mapCenter = "11.89,48.91";
		sw.defaultParams().mapZoomLevel = 11d;
		sw.defaultParams().sampleSize = sample.getSample();

		// Config needs to be loaded at least once
		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);
		imc.setTopK(5);

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
				install(new SimWrapperModule());

				bind(AnalysisMainModeIdentifier.class).to(KelheimMainModeIdentifier.class);
				addControlerListenerBinding().to(ModeChoiceCoverageControlerListener.class);

				// Configure mode-choice strategy
				InformedModeChoiceModule.Builder imc = new InformedModeChoiceModule.Builder()
					.withFixedCosts(FixedCostsEstimator.DailyConstant.class, TransportMode.car)
					.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.AlwaysAvailable.class, TransportMode.bike, TransportMode.ride, TransportMode.walk)
					.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.ConsiderIfCarAvailable.class, TransportMode.car)
					.withTripEstimator(PtTripWithDistanceBasedFareEstimator.class, ModeOptions.AlwaysAvailable.class, TransportMode.pt)
					.withActivityEstimator(DefaultActivityEstimator.class)
					.withPruner("ad999", new DistanceBasedPruner(3.03073657, 0.22950583))
					.withPruner("ad99", new DistanceBasedPruner(2.10630819, 0.0917091))
					.withPruner("ad95", new DistanceBasedPruner(1.72092386, 0.03189323));

				if (drt) {
					imc.withLegEstimator(MultiModalDrtLegEstimator.class, ModeOptions.AlwaysAvailable.class, TransportMode.drt, "av");
				}

				install(imc.build());

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
			double maxSpeed = controler.getScenario()
				.getVehicles()
				.getVehicleTypes()
				.get(Id.create("autonomous_vehicle", VehicleType.class))
				.getMaximumVelocity();
			controler.addOverridingModule(
				new DvrpModeLimitedMaxSpeedTravelTimeModule("av", config.qsim().getTimeStepSize(),
					maxSpeed));

			for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
				controler.addOverridingModule(new KelheimDrtFareModule(drtCfg, network, avFare));
			}

			controler.addOverridingModule(new DrtEstimatorModule());

			MultiModeDrtEstimatorConfigGroup estimatorConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtEstimatorConfigGroup.class);
			estimatorConfig.addParameterSet(new DrtEstimatorConfigGroup("drt"));
			estimatorConfig.addParameterSet(new DrtEstimatorConfigGroup("av"));

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
	}
}
