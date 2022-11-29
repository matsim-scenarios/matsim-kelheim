package org.matsim.run;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
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
import org.matsim.application.analysis.travelTimeValidation.TravelTimeAnalysis;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.prepare.CreateLandUseShp;
import org.matsim.application.prepare.freight.tripExtraction.ExtractRelevantFreightTrips;
import org.matsim.application.prepare.network.CreateNetworkFromSumo;
import org.matsim.application.prepare.population.*;
import org.matsim.application.prepare.pt.CreateTransitScheduleFromGtfs;
import org.matsim.contrib.drt.estimator.MultiModalDrtLegEstimator;
import org.matsim.contrib.drt.estimator.run.DrtEstimatorConfigGroup;
import org.matsim.contrib.drt.estimator.run.DrtEstimatorModule;
import org.matsim.contrib.drt.estimator.run.MultiModeDrtEstimatorConfigGroup;
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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.drtFare.KelheimDrtFareModule;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesConfigGroup;
import org.matsim.modechoice.ModeOptions;
import org.matsim.modechoice.commands.GenerateChoiceSet;
import org.matsim.modechoice.commands.StrategyOptions;
import org.matsim.modechoice.estimators.DefaultActivityEstimator;
import org.matsim.modechoice.estimators.DefaultLegScoreEstimator;
import org.matsim.modechoice.estimators.FixedCostsEstimator;
import org.matsim.modechoice.pruning.DistanceBasedPruner;
import org.matsim.modechoice.pruning.ModeDistanceBasedPruner;
import org.matsim.run.prepare.PrepareNetwork;
import org.matsim.run.prepare.PreparePopulation;
import org.matsim.run.utils.KelheimCaseStudyTool;
import org.matsim.run.utils.StrategyWeightFadeout;
import org.matsim.vehicles.VehicleType;
import picocli.CommandLine;
import playground.vsp.pt.fare.DistanceBasedPtFareParams;
import playground.vsp.pt.fare.PtFareConfigGroup;
import playground.vsp.pt.fare.PtTripFareEstimator;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

import javax.annotation.Nullable;
import java.util.*;

@CommandLine.Command(header = ":: Open Kelheim Scenario ::", version = RunKelheimScenario.VERSION, mixinStandardHelpOptions = true)
@MATSimApplication.Prepare({
		CreateNetworkFromSumo.class, CreateTransitScheduleFromGtfs.class, TrajectoryToPlans.class, GenerateShortDistanceTrips.class,
		MergePopulations.class, ExtractRelevantFreightTrips.class, DownSamplePopulation.class, PrepareNetwork.class, ExtractHomeCoordinates.class,
		CreateLandUseShp.class, ResolveGridCoordinates.class, PreparePopulation.class, CleanPopulation.class, GenerateChoiceSet.class
})
@MATSimApplication.Analysis({
		TravelTimeAnalysis.class, LinkStats.class, CheckPopulation.class, DrtServiceQualityAnalysis.class, DrtVehiclesRoadUsageAnalysis.class
})
public class RunKelheimScenario extends MATSimApplication {

	static final String VERSION = "3.x";

	@CommandLine.Mixin
	private final SampleOptions sample = new SampleOptions(25, 10, 1);

	@CommandLine.Option(names = "--with-drt", defaultValue = "false", description = "enable DRT service")
	private boolean drt;

	@CommandLine.Option(names = "--income-dependent", defaultValue = "true", description = "enable income dependent monetary utility", negatable = true)
	private boolean incomeDependent;

	@CommandLine.Option(names = "--av-fare", defaultValue = "2.0", description = "AV fare (euro per trip)")
	private double avFare;

	@CommandLine.Option(names = "--case-study", defaultValue = "NULL", description = "Case study for the av scenario")
	private KelheimCaseStudyTool.AV_SERVICE_AREAS avServiceArea;

	@CommandLine.Option(names = "--bike-rnd", defaultValue = "false", description = "enable randomness in ASC of bike")
	private boolean bikeRnd;

	@CommandLine.Option(names = "--random-seed", defaultValue = "4711", description = "setting random seed for the simulation")
	private long randomSeed;

	@CommandLine.Option(names = "--intermodal", defaultValue = "false", description = "enable DRT service")
	private boolean intermodal;

	@CommandLine.Option(names = "--plans", defaultValue = "", description = "Use different input plans")
	private String planOrigin;

	@CommandLine.Mixin
	StrategyOptions strategy = new StrategyOptions(StrategyOptions.ModeChoice.subTourModeChoice, "person");

	public RunKelheimScenario(@Nullable Config config) {
		super(config);
	}

	public RunKelheimScenario() {
		super(String.format("scenarios/input/kelheim-v%s-25pct.config.xml", VERSION));
	}

	public static void main(String[] args) {
		MATSimApplication.run(RunKelheimScenario.class, args);
	}

	@Nullable
	@Override
	protected Config prepareConfig(Config config) {

		for (long ii = 600; ii <= 97200; ii += 600) {

			for (String act : List.of("home", "restaurant", "other", "visit", "errands", "accomp_other", "accomp_children",
					"educ_higher", "educ_secondary", "educ_primary", "educ_tertiary", "educ_kiga", "educ_other")) {
				config.planCalcScore()
						.addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams(act + "_" + ii).setTypicalDuration(ii));
			}

			config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("work_" + ii).setTypicalDuration(ii)
					.setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));
			config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("business_" + ii).setTypicalDuration(ii)
					.setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));
			config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("leisure_" + ii).setTypicalDuration(ii)
					.setOpeningTime(9. * 3600.).setClosingTime(27. * 3600.));

			config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("shop_daily_" + ii).setTypicalDuration(ii)
					.setOpeningTime(8. * 3600.).setClosingTime(20. * 3600.));
			config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("shop_other_" + ii).setTypicalDuration(ii)
					.setOpeningTime(8. * 3600.).setClosingTime(20. * 3600.));
		}

		config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("car interaction").setTypicalDuration(60));
		config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("other").setTypicalDuration(600 * 3));

		config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("freight_start").setTypicalDuration(60 * 15));
		config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("freight_end").setTypicalDuration(60 * 15));

		config.controler().setOutputDirectory(sample.adjustName(config.controler().getOutputDirectory()));
		config.plans().setInputFile(sample.adjustName(config.plans().getInputFile()));
		config.controler().setRunId(sample.adjustName(config.controler().getRunId()));

		config.qsim().setFlowCapFactor(sample.getSize() / 100.0);
		config.qsim().setStorageCapFactor(sample.getSize() / 100.0);

		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.info);
		config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);

		config.global().setRandomSeed(randomSeed);

		if (intermodal) {
			ConfigUtils.addOrGetModule(config, PtIntermodalRoutingModesConfigGroup.class);
		}

		if (drt) {
			MultiModeDrtConfigGroup multiModeDrtConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
			ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);
			DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.planCalcScore(), config.plansCalcRoute());
		}

		PtFareConfigGroup ptFareConfigGroup = ConfigUtils.addOrGetModule(config, PtFareConfigGroup.class);
		DistanceBasedPtFareParams distanceBasedPtFareParams = ConfigUtils.addOrGetModule(config, DistanceBasedPtFareParams.class);

		// Set parameters
		ptFareConfigGroup.setApplyUpperBound(true);
		ptFareConfigGroup.setUpperBoundFactor(1.5);

		distanceBasedPtFareParams.setMinFare(2.0);  // Minimum fare (e.g. short trip or 1 zone ticket)
		distanceBasedPtFareParams.setLongDistanceTripThreshold(50000); // Division between long trip and short trip (unit: m)
		distanceBasedPtFareParams.setNormalTripSlope(0.00017); // y = ax + b --> a value, for short trips
		distanceBasedPtFareParams.setNormalTripIntercept(1.6); // y = ax + b --> b value, for short trips
		distanceBasedPtFareParams.setLongDistanceTripSlope(0.00025); // y = ax + b --> a value, for long trips
		distanceBasedPtFareParams.setLongDistanceTripIntercept(30); // y = ax + b --> b value, for long trips

		strategy.applyConfig(config, this::addRunOption);

		if (iterations != -1)
			addRunOption(config, "iter", iterations);

		if (!planOrigin.isBlank()) {
			config.plans().setInputFile(
					config.plans().getInputFile().replace(".plans", ".plans-" + planOrigin)
			);

			addRunOption(config, planOrigin);
		}

		config.planCalcScore().setExplainScores(true);

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {

		for (Link link : scenario.getNetwork().getLinks().values()) {
			Set<String> modes = link.getAllowedModes();

			// allow freight traffic together with cars
			if (modes.contains("car")) {
				HashSet<String> newModes = Sets.newHashSet(modes);
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
			Random bicycleRnd = new Random(8765);
			for (Person person : scenario.getPopulation().getPersons().values()) {
				double width = 2; //TODO this value is to be determined
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

				bind(AnalysisMainModeIdentifier.class).to(KelheimMainModeIdentifier.class);
				addControlerListenerBinding().to(ModeChoiceCoverageControlerListener.class);

				// Configure mode-choice strategy
				addControlerListenerBinding().to(StrategyWeightFadeout.class).in(Singleton.class);
				Multibinder<StrategyWeightFadeout.Schedule> schedules = Multibinder.newSetBinder(binder(), StrategyWeightFadeout.Schedule.class);
				schedules.addBinding().toInstance(new StrategyWeightFadeout.Schedule(strategy.getModeChoice().getName(), "person", 0.75, 0.85));
				schedules.addBinding().toInstance(new StrategyWeightFadeout.Schedule(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute, "person", 0.78));


				install(strategy.applyModule(binder(), config, builder ->
						builder.withFixedCosts(FixedCostsEstimator.DailyConstant.class, TransportMode.car)
								.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.AlwaysAvailable.class, TransportMode.bike, TransportMode.ride, TransportMode.walk)
								.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.ConsiderIfCarAvailable.class, TransportMode.car)
								.withLegEstimator(MultiModalDrtLegEstimator.class, ModeOptions.AlwaysAvailable.class, "drt", "av")
								.withTripEstimator(PtTripFareEstimator.class, ModeOptions.AlwaysAvailable.class, TransportMode.pt)
								.withActivityEstimator(DefaultActivityEstimator.class)
								.withPruner("d99", new DistanceBasedPruner(3.28179737, 0.16710464))
								.withPruner("d95", new DistanceBasedPruner(3.09737874, 0.03390164))
								.withPruner("m99", new ModeDistanceBasedPruner(2.54076057, Map.of(
										"bike", 0.32642463,
										"walk", 0.13978577,
										"car", 0.0448102,
										"ride", 0.07041452,
										"pt", 0.13576849
								)))
								// These are with activity estimation enabled
								.withPruner("ad999", new DistanceBasedPruner(3.03073657, 0.22950583))
								.withPruner("ad99", new DistanceBasedPruner(2.10630819, 0.0917091))
								.withPruner("ad95", new DistanceBasedPruner( 1.72092386, 0.03189323))
								.withPruner("am99", new ModeDistanceBasedPruner(2.68083795, Map.of(
										"bike", 0.22681661,
										"walk", 0d,
										"car", 0.052746,
										"ride", 0.11132056,
										"pt", 0.07964946
								)))
						)
				);


				if (incomeDependent) {
					bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class).asEagerSingleton();
				}

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
                if (drtCfg.getMode().equals("av")) {
                    KelheimCaseStudyTool.setConfigFile(config, drtCfg, avServiceArea);
                }
            }

			controler.addOverridingModule(new DrtEstimatorModule());
			MultiModeDrtEstimatorConfigGroup estimatorConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtEstimatorConfigGroup.class);

			// Use estimators with default values
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