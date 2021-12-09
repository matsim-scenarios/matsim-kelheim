package org.matsim.run;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.google.common.collect.Sets;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.matsim.analysis.KelheimMainModeIdentifier;
import org.matsim.analysis.ModeChoiceCoverageControlerListener;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimApplication;
import org.matsim.application.analysis.CheckPopulation;
import org.matsim.application.analysis.traffic.LinkStats;
import org.matsim.application.analysis.travelTimeValidation.TravelTimeAnalysis;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.prepare.CreateLandUseShp;
import org.matsim.application.prepare.freight.ExtractRelevantFreightTrips;
import org.matsim.application.prepare.network.CreateNetworkFromSumo;
import org.matsim.application.prepare.population.*;
import org.matsim.application.prepare.pt.CreateTransitScheduleFromGtfs;
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
import org.matsim.run.prepare.PrepareNetwork;
import org.matsim.run.prepare.PreparePopulation;
import org.matsim.run.utils.KelheimCaseStudyTool;
import org.matsim.run.utils.StrategyWeightFadeout;
import org.matsim.vehicles.VehicleType;
import picocli.CommandLine;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CommandLine.Command(header = ":: Open Kelheim Scenario ::", version = RunKelheimScenario.VERSION)
@MATSimApplication.Prepare({
        CreateNetworkFromSumo.class, CreateTransitScheduleFromGtfs.class, TrajectoryToPlans.class, GenerateShortDistanceTrips.class,
        MergePopulations.class, ExtractRelevantFreightTrips.class, DownSamplePopulation.class, PrepareNetwork.class,
        CreateLandUseShp.class, ResolveGridCoordinates.class, PreparePopulation.class, CleanPopulation.class
})
@MATSimApplication.Analysis({
        TravelTimeAnalysis.class, LinkStats.class, CheckPopulation.class
})
public class RunKelheimScenario extends MATSimApplication {

    static final String VERSION = "1.0";

    @CommandLine.Mixin
    private final SampleOptions sample = new SampleOptions(25, 10, 1);

    @CommandLine.Option(names = "--with-drt", defaultValue = "false", description = "enable DRT service")
    private boolean drt;

    @CommandLine.Option(names = "--income-dependent", defaultValue = "true", description = "enable income dependent monetary utility", negatable = true)
    private boolean incomeDependent;

    @CommandLine.Option(names = "--av-fare", defaultValue = "2.0", description = "AV fare (euro per trip)")
    private double avFare;

    @CommandLine.Option(names = "--case-study", defaultValue = "BASE", description = "Case study for the av scenario")
    private KelheimCaseStudyTool.AV_SERVICE_AREAS avServiceArea;

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

            for (String act : List.of("home", "restaurant", "other", "visit", "errands",
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

        config.qsim().setFlowCapFactor(sample.getSize() / 100.0);
        config.qsim().setStorageCapFactor(sample.getSize() / 100.0);

        config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.info);
        config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);

        if (drt) {
            MultiModeDrtConfigGroup multiModeDrtConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
            ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);
            DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.planCalcScore(), config.plansCalcRoute());
        }

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

                addControlerListenerBinding().to(StrategyWeightFadeout.class).in(Singleton.class);
                Multibinder<StrategyWeightFadeout.Schedule> schedules = Multibinder.newSetBinder(binder(), StrategyWeightFadeout.Schedule.class);

                schedules.addBinding().toInstance(new StrategyWeightFadeout.Schedule(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode, "person", 0.75, 0.85));

                if (incomeDependent) {
                    bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class).asEagerSingleton();
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
                if (drtCfg.getMode().equals("av")){
                    KelheimCaseStudyTool.setConfigFile(config, drtCfg, avServiceArea);
                }
            }

        }
    }
}
