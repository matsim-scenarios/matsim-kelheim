package org.matsim.run;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimApplication;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.*;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RunKelheimIntegrationTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	private static final String URL = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/leipzig/leipzig-v1.2/input/";
	private static final String exampleShp = "input/v1.3/drtServiceArea/Leipzig_stadt.shp";

	@Test
	public final void runExamplePopulationTest() {

		String output = utils.getOutputDirectory();



		Config config = ConfigUtils.loadConfig("input/test.config.xml");

		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);
		config.controller().setLastIteration(1);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(output);

		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class).defaultDashboards = SimWrapperConfigGroup.Mode.disabled;


		MATSimApplication.execute(RunKelheimScenario.class, config, "run", "--1pct", "--slow-speed-area", exampleShp,
				"--slow-speed-relative-change", "0.5","--drt-area", exampleShp, "--post-processing", "disabled"
		);

		//EventsUtils.createEventsFingerprint("kelheim-test-junit/kelheim.output_events.xml.gz","RunKelheimIntegrationTest_events.fp.zst");

		assertThat(EventsUtils.createAndCompareEventsFingerprint(
				new File(output, "kelheim.output_events.xml.gz"),
				(utils.getInputDirectory()+"RunKelheimIntegrationTest_events.fp.zst")
		)).isEqualTo(ComparisonResult.FILES_ARE_EQUAL);


	}

	@Test
	public final void runDrtExamplePopulationTest() {
		Config config = ConfigUtils.loadConfig("input/test.with-drt.config.xml");
		config.controller().setLastIteration(1);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class).defaultDashboards = SimWrapperConfigGroup.Mode.disabled;

		MATSimApplication.execute(RunKelheimScenario.class, config,
			"run", "--1pct", "--with-drt");
	}

}
