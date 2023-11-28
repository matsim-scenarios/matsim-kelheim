package org.matsim.run;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.testcases.MatsimTestUtils;

public class RunKelheimIntegrationTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void runExamplePopulationTest() {
		Config config = ConfigUtils.loadConfig("input/test.config.xml");
		config.controller().setLastIteration(2);
		config.global().setNumberOfThreads(2);
		config.qsim().setNumberOfThreads(2);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class).defaultDashboards = SimWrapperConfigGroup.Mode.disabled;

		MATSimApplication.execute(RunKelheimScenario.class, config,
			"run", "--1pct");
	}

	@Test
	public final void runDrtExamplePopulationTest() {
		Config config = ConfigUtils.loadConfig("input/test.with-drt.config.xml");
		config.controller().setLastIteration(2);
		config.global().setNumberOfThreads(2);
		config.qsim().setNumberOfThreads(2);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class).defaultDashboards = SimWrapperConfigGroup.Mode.disabled;

		MATSimApplication.execute(RunKelheimScenario.class, config,
			"run", "--1pct", "--with-drt");
	}

}
