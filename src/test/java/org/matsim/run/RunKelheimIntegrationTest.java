package org.matsim.run;

import org.junit.jupiter.api.Test;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.simwrapper.SimWrapperConfigGroup;

/**
 * integration test.
 */
public class RunKelheimIntegrationTest {

	@Test
	public final void runExamplePopulationTest() {
		Config config = ConfigUtils.loadConfig("input/test.config.xml");
		config.plans().setInputFile(
			String.format("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/kelheim-v%s/input/kelheim-v%s-test.plans.xml",
				RunKelheimScenario.VERSION, RunKelheimScenario.VERSION));

		config.controller().setLastIteration(1);
		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class).defaultDashboards = SimWrapperConfigGroup.Mode.disabled;

		MATSimApplication.execute(RunKelheimScenario.class, config,
			"run", "--1pct");
	}

	@Test
	public final void runDrtExamplePopulationTest() {
		Config config = ConfigUtils.loadConfig("input/test.with-drt.config.xml");
		config.plans().setInputFile(
			String.format("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/kelheim-v%s/input/kelheim-v%s-test.with-drt.plans.xml",
				RunKelheimScenario.VERSION, RunKelheimScenario.VERSION));

		config.controller().setLastIteration(1);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class).defaultDashboards = SimWrapperConfigGroup.Mode.disabled;

		MATSimApplication.execute(RunKelheimScenario.class, config,
			"run", "--1pct", "--with-drt");
	}

}
