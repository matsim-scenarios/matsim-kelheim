package org.matsim.run;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.KelheimMainModeIdentifier;
import org.matsim.analysis.ModeChoiceCoverageControlerListener;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.options.SampleOptions;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.companions.DrtCompanionParams;
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
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.drtFare.KelheimDrtFareModule;
import org.matsim.run.utils.KelheimCaseStudyTool;
import org.matsim.run.utils.StrategyWeightFadeout;
import org.matsim.vehicles.VehicleType;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks;

public class RunTest {

    private final static double WEIGHT_1_PASSENGER = 26387.;
    private final static double WEIGHT_2_PASSENGER = 3843.;
    private final static double WEIGHT_3_PASSENGER = 879.;
    private final static double WEIGHT_4_PASSENGER = 409.;
    private final static double WEIGHT_5_PASSENGER = 68.;
    private final static double WEIGHT_6_PASSENGER = 18.;
    private final static double WEIGHT_7_PASSENGER = 4.;
    private final static double WEIGHT_8_PASSENGER = 1.;

    private static final Logger log = LogManager.getLogger(RunTest.class );

    private static final SampleOptions sample = new SampleOptions(25, 10, 1);


    public static void main(String[] args) {
        if ( args.length==0 ) {
            args = new String[] {"scenarios/input/test.with-drt.config.xml"}  ;
        }

        Config config = prepareConfig( args, new MultiModeDrtConfigGroup(DrtWithExtensionsConfigGroup::new)) ;
        Scenario scenario = prepareScenario( config ) ;
        Controler controler = prepareControler( scenario ) ;
        controler.run();
    }

    public static Controler prepareControler( Scenario scenario ) {

        final Controler controler = new Controler( scenario );
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

                addControlerListenerBinding().to(StrategyWeightFadeout.class).in(Singleton.class);
                Multibinder<StrategyWeightFadeout.Schedule> schedules = Multibinder.newSetBinder(binder(), StrategyWeightFadeout.Schedule.class);

                schedules.addBinding().toInstance(new StrategyWeightFadeout.Schedule(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode, "person", 0.75, 0.85));




            }
        });

        if (true) {
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
                System.out.println(drtCfg.getMode());
                System.out.println(drtCfg.maxWaitTime);
                System.out.println(drtCfg.stopDuration);
                double avFare = 2.0;
                controler.addOverridingModule(new KelheimDrtFareModule(drtCfg, network, avFare));
                if (drtCfg.getMode().equals("av")) {
                    KelheimCaseStudyTool.AV_SERVICE_AREAS avServiceArea = KelheimCaseStudyTool.AV_SERVICE_AREAS.NULL;
                    KelheimCaseStudyTool.setConfigFile(config, drtCfg, avServiceArea);
                }

            }


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
        return controler;
    }

    public static Scenario prepareScenario( Config config ) {
        Scenario scenario = ScenarioUtils.createScenario(config);
        for (Link link : scenario.getNetwork().getLinks().values()) {
            Set<String> modes = link.getAllowedModes();

            // allow freight traffic together with cars
            if (modes.contains("car")) {
                HashSet<String> newModes = Sets.newHashSet(modes);
                newModes.add("freight");

                link.setAllowedModes(newModes);
            }
        }

        if (true) {
            scenario.getPopulation()
                    .getFactory()
                    .getRouteFactories()
                    .setRouteFactory(DrtRoute.class, new DrtRouteFactory());
        }

        Gbl.assertNotNull( config );

        // note that the path for this is different when run from GUI (path of original config) vs.
        // when run from command line/IDE (java root).  :-(    See comment in method.  kai, jul'18
        // yy Does this comment still apply?  kai, jul'19

        /*
         * We need to set the DrtRouteFactory before loading the scenario. Otherwise DrtRoutes in input plans are loaded
         * as GenericRouteImpls and will later cause exceptions in DrtRequestCreator. So we do this here, although this
         * class is also used for runs without drt.
         */

        ScenarioUtils.loadScenario(scenario);
        return scenario;
    }

    public static Config prepareConfig( String [] args,
                                        ConfigGroup... customModules ) {
        OutputDirectoryLogging.catchLogEntries();

        String[] typedArgs = Arrays.copyOfRange( args, 1, args.length );
        ConfigGroup[] customModulesToAdd = new ConfigGroup[0];
        //customModulesToAdd = new ConfigGroup[]{new DrtWithExtensionsConfigGroup()};

        ConfigGroup[] customModulesAll = new ConfigGroup[customModules.length + customModulesToAdd.length];

        int counter = 0;
        for (ConfigGroup customModule : customModules) {
            customModulesAll[counter] = customModule;
            counter++;
        }

        for (ConfigGroup customModule : customModulesToAdd) {
            customModulesAll[counter] = customModule;
            counter++;
        }

        final Config config = ConfigUtils.loadConfig( args[ 0 ], customModulesAll );

        MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config,MultiModeDrtConfigGroup.class);
            var test = multiModeDrtConfigGroup.getModalElements().iterator().next();
            DrtWithExtensionsConfigGroup drtWithExtensionsConfigGroup  = (DrtWithExtensionsConfigGroup) multiModeDrtConfigGroup.getModalElements().iterator().next();
            DrtCompanionParams drtCompanionParams  = new DrtCompanionParams();
            drtCompanionParams.setDrtCompanionSamplingWeights(List.of(WEIGHT_1_PASSENGER,
                    WEIGHT_2_PASSENGER,
                    WEIGHT_3_PASSENGER,
                    WEIGHT_4_PASSENGER,
                    WEIGHT_5_PASSENGER,
                    WEIGHT_6_PASSENGER,
                    WEIGHT_7_PASSENGER,
                    WEIGHT_8_PASSENGER
                    ));
            drtWithExtensionsConfigGroup.addParameterSet(drtCompanionParams);
            ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);
           DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfigGroup , config.planCalcScore(), config.plansCalcRoute());
        //drtWithExtensionsConfigGroup.maxWaitTime = multiModeDrtConfigGroup.getParameterSets("drt")



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

        config.global().setRandomSeed(4711);

        ConfigUtils.applyCommandline( config, typedArgs ) ;

        ConfigUtils.writeConfig(config,"test.xml");

        return config ;
    }
}
