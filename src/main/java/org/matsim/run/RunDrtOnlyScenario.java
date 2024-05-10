package org.matsim.run;

import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.drt.analysis.afterSimAnalysis.DrtVehicleStoppingTaskWriter;
import org.matsim.contrib.drt.optimizer.rebalancing.NoRebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.run.rebalancing.WaitingPointsBasedRebalancingModule;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;

/**
 * This temporary run script may be helpful for comparing results with the real world data from VIA. Therefore, it is kept for now.
 */
public class RunDrtOnlyScenario implements MATSimAppCommand {
	@CommandLine.Option(names = "--config", description = "config path", required = true)
	private String configPath;

	@CommandLine.Option(names = "--waiting-points", description = "waiting points for rebalancing strategy", defaultValue = "")
	private String waitingPointsPath;

	public static void main(String[] args) throws IOException {
		new RunDrtOnlyScenario().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		Config config = ConfigUtils.loadConfig(configPath, new MultiModeDrtConfigGroup(), new DvrpConfigGroup());
		String outputDirectory = config.controller().getOutputDirectory();
		Controler controler = DrtControlerCreator.createControler(config, false);
		MultiModeDrtConfigGroup multiModeDrtConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
			if (!waitingPointsPath.equals("")) {
				controler.addOverridingModule(new WaitingPointsBasedRebalancingModule(drtCfg, waitingPointsPath));
			} else {
				controler.addOverridingModule(new AbstractDvrpModeModule(drtCfg.mode) {
					@Override
					public void install() {
						bindModal(RebalancingStrategy.class).to(NoRebalancingStrategy.class).asEagerSingleton();
					}
				});
			}
		}
		controler.run();
		new DrtVehicleStoppingTaskWriter(Path.of(outputDirectory)).run();
		return 0;
	}
}
