package org.matsim.run;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.testcases.MatsimTestUtils;

public class RunKelheimIntegrationTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void runToyExamplePopulationTest() {
		Config config = ConfigUtils.loadConfig("scenarios/input/test-v1.0-1pct.config.xml");
		config.plans().setInputFile("../../test/input/v1.3-testing.plans.xml");
		config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/"
				+ "de/duesseldorf/duesseldorf-v1.0/input/duesseldorf-v1.0-network-with-freight.xml.gz");
		config.controler().setLastIteration(1);
		config.strategy().setFractionOfIterationsToDisableInnovation(1);
		config.controler()
				.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());

		MATSimApplication.execute(RunKelheimScenario.class, config,
				"run", "--1pct");
	}

}
