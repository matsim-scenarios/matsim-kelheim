package org.matsim.run;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.google.common.collect.Sets;
import org.matsim.analysis.KelheimMainModeIdentifier;
import org.matsim.analysis.ModeChoiceCoverageControlerListener;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.vsp.pt.fare.DistanceBasedPtFareParams;
import org.matsim.contrib.vsp.pt.fare.PtFareConfigGroup;
import org.matsim.contrib.vsp.pt.fare.PtFareModule;
import org.matsim.contrib.vsp.scenario.SnzActivities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.annealing.ReplanningAnnealerConfigGroup;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

import java.util.Set;

public class Run1pctKelheimScenario {
	private static final double SAMPLE = 0.01;

	public static void main(String[] args) {
		// ======= Load & adapt config =======
		String configPath = "input/v3.1/kelheim-v3.1-config.xml";
		Config config = ConfigUtils.loadConfig(configPath);

		SnzActivities.addScoringParams(config);

		config.controller().setOutputDirectory("./output/output-kelheim-v3.1-1pct");
		config.plans().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/kelheim-v3.0/input/kelheim-v3.0-1pct-plans.xml.gz");
		config.controller().setRunId("kelheim-v3.1-1pct");

		config.qsim().setFlowCapFactor(SAMPLE);
		config.qsim().setStorageCapFactor(SAMPLE);

		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort);
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);

		config.global().setRandomSeed(4711);

		SimWrapperConfigGroup sw = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);

		// Relative to config
		sw.defaultParams().shp = "../shp/dilutionArea.shp";
		sw.defaultParams().mapCenter = "11.89,48.91";
		sw.defaultParams().mapZoomLevel = 11d;
		sw.sampleSize = SAMPLE;

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

		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		// ======= Load & adapt scenario =======
		Scenario scenario = ScenarioUtils.loadScenario(config);

		for (Link link : scenario.getNetwork().getLinks().values()) {
			Set<String> modes = link.getAllowedModes();

			// allow freight traffic together with cars
			if (modes.contains("car")) {
				Set<String> newModes = Sets.newHashSet(modes);
				newModes.add("freight");

				link.setAllowedModes(newModes);
			}
		}

		// ======= Load & adapt controller ======
		Controller controller = ControllerUtils.createController(scenario);

		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new PtFareModule());
				install(new SwissRailRaptorModule());
				install(new PersonMoneyEventsAnalysisModule());
				install(new SimWrapperModule());

				bind(AnalysisMainModeIdentifier.class).to(KelheimMainModeIdentifier.class);
				addControlerListenerBinding().to(ModeChoiceCoverageControlerListener.class);

				//use income-dependent marginal utility of money
				bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class).asEagerSingleton();
			}
		});
		controller.run();
	}
}
