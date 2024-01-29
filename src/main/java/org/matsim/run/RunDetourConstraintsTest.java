package org.matsim.run;

import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.drt.passenger.DrtOfferAcceptor;
import org.matsim.contrib.drt.passenger.MaxDetourOfferAcceptor;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import picocli.CommandLine;

public class RunDetourConstraintsTest implements MATSimAppCommand {
	@CommandLine.Option(names = "--config", description = "path to config file", required = true)
	private String configFile;
	@CommandLine.Option(names = "--plans", description = "path to input plans", required = true)
	private String inputPlans;
	@CommandLine.Option(names = "--output", description = "path of output folder", required = true)
	private String output;

	public static void main(String[] args) {
		new RunDetourConstraintsTest().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		Config config = ConfigUtils.loadConfig(configFile, new MultiModeDrtConfigGroup(), new DvrpConfigGroup());
		config.plans().setInputFile(inputPlans);
		config.controller().setOutputDirectory(output);
		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.info);
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		Controler controler = DrtControlerCreator.createControler(config, false);

		MultiModeDrtConfigGroup multiModeDrtConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
			controler.addOverridingQSimModule(new AbstractDvrpModeQSimModule(drtCfg.mode) {
				@Override
				protected void configureQSim() {
					bindModal(DrtOfferAcceptor.class).toProvider(modalProvider(getter -> new MaxDetourOfferAcceptor(drtCfg.maxAllowedPickupDelay)));
				}
			});
		}

		controler.run();

		return 0;
	}
}
