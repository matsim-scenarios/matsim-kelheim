package org.matsim.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.rebalancing.WaitingPointsBasedRebalanceModule;

public class RunDrtTest {
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0], new MultiModeDrtConfigGroup(), new DvrpConfigGroup());
		// It will fail at this line:
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = DrtControlerCreator.createControler(config, false);
		MultiModeDrtConfigGroup multiModeDrtConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
			if (!args[1].equals("")) {
				controler.addOverridingModule(new WaitingPointsBasedRebalanceModule(drtCfg, args[1]));
			}
		}
		controler.run();
	}

}
