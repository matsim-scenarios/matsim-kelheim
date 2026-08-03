package org.matsim.run;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.drt.optimizer.insertion.parallel.DrtServiceQualityProbeParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.simwrapper.SimWrapperConfigGroup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * integration test.
 */
@Tag("remote")
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

		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class).setDefaultDashboards(SimWrapperConfigGroup.DefaultDashboardsMode.disabled);

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

		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class).setDefaultDashboards(SimWrapperConfigGroup.DefaultDashboardsMode.disabled);
		MATSimApplication.execute(RunKelheimScenario.class, config,
			"run", "--1pct", "--with-drt");
	}

	@Test
	public final void serviceQualityProbeHasNoSimulationSideEffects() throws IOException {
		Path root = Path.of("test/output/kelheim-service-quality-probe");
		Path disabledRoot = root.resolve("disabled-a");
		Path repeatedDisabledRoot = root.resolve("disabled-b");
		Path enabledRoot = root.resolve("enabled");

		runActualKelheimTestInput(disabledRoot, false);
		runActualKelheimTestInput(repeatedDisabledRoot, false);
		runActualKelheimTestInput(enabledRoot, true);

		Path disabledEvents = findSingle(disabledRoot, "output_events.xml.zst");
		Path repeatedDisabledEvents = findSingle(repeatedDisabledRoot, "output_events.xml.zst");
		Path enabledEvents = findSingle(enabledRoot, "output_events.xml.zst");
		assertEventFilesEqual("disabled run A vs disabled run B", disabledEvents, repeatedDisabledEvents);
		assertEventFilesEqual("disabled run A vs enabled run", disabledEvents, enabledEvents);

		assertThat(findAll(disabledRoot, ".drt_service_quality_probes.csv.gz")).isEmpty();
		assertThat(findAll(repeatedDisabledRoot, ".drt_service_quality_probes.csv.gz")).isEmpty();
		assertThat(findAll(enabledRoot, ".drt_service_quality_probes.csv.gz")).hasSize(1);
	}

	private static void runActualKelheimTestInput(Path outputRoot, boolean enableProbe) {
		Config config = ConfigUtils.loadConfig("input/test.with-drt.config.xml");
		config.plans().setInputFile(String.format(
			"https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/kelheim-v%s/input/kelheim-v%s-test.with-drt.plans.xml",
			RunKelheimScenario.VERSION, RunKelheimScenario.VERSION));
		config.controller().setOutputDirectory(outputRoot.toString());
		config.controller().setRunId(enableProbe ? "probe-enabled" : "probe-disabled");
		config.controller().setLastIteration(1);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.global().setNumberOfThreads(1);
		config.global().setRandomSeed(4711);
		config.qsim().setNumberOfThreads(1);
		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class)
			.setDefaultDashboards(SimWrapperConfigGroup.DefaultDashboardsMode.disabled);
		ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class)
			.getModalElements().forEach(mode -> mode.setNumberOfThreads(1));

		if (enableProbe) {
			MultiModeDrtConfigGroup drt = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
			DrtConfigGroup drtMode = drt.getModalElements().stream()
				.filter(mode -> mode.getMode().equals("drt"))
				.findFirst().orElseThrow();
			DrtServiceQualityProbeParams probeParams = new DrtServiceQualityProbeParams();
			probeParams.setWriteServiceQualityProbes(true);
			probeParams.setServiceQualityProbeTimes("28800,32400");
			probeParams.setServiceQualityProbeOutputFile("drt_service_quality_probes.csv.gz");
			drtMode.addParameterSet(probeParams);
		}

		MATSimApplication.execute(RunKelheimScenario.class, config, "run", "--with-drt", "--1pct");
	}

	private static Path findSingle(Path root, String suffix) throws IOException {
		List<Path> matches = findAll(root, suffix);
		assertThat(matches).as("files ending in %s", suffix).hasSize(1);
		return matches.get(0);
	}

	private static List<Path> findAll(Path root, String suffix) throws IOException {
		try (var paths = Files.walk(root)) {
			return paths.filter(Files::isRegularFile)
				.filter(path -> path.getFileName().toString().endsWith(suffix))
				.toList();
		}
	}

	private static void assertEventFilesEqual(String comparison, Path disabledEvents, Path enabledEvents) throws IOException {
		String firstDifference = null;
		try (var disabledReader = IOUtils.getBufferedReader(disabledEvents.toString());
			 var enabledReader = IOUtils.getBufferedReader(enabledEvents.toString())) {
			for (int lineNumber = 1; ; lineNumber++) {
				String disabledLine = disabledReader.readLine();
				String enabledLine = enabledReader.readLine();
				if (disabledLine == null && enabledLine == null) {
					break;
				}
				String disabledValue = disabledLine == null ? "<missing>" : disabledLine;
				String enabledValue = enabledLine == null ? "<missing>" : enabledLine;
				if (!java.util.Objects.equals(disabledLine, enabledLine)) {
					firstDifference = "line " + lineNumber + "\n"
						+ "  probe disabled: " + disabledValue + "\n"
						+ "  probe enabled : " + enabledValue;
					break;
				}
			}
		}
		assertThat(firstDifference).as("first decompressed event-file difference: %s", comparison).isNull();
	}

}
