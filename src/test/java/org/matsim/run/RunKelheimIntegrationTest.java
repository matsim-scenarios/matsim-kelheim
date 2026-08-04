package org.matsim.run;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.simwrapper.SimWrapperConfigGroup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * integration test.
 */
@Tag("remote")
public class RunKelheimIntegrationTest {
	private static final String WORKER_ARGUMENT = "--kelheim-service-quality-probe-worker";
	private static final double FIRST_PROBE_TIME = 28800.;
	private static final String TEST_ITERATION_SUFFIX = ".0.events.xml.zst";

	/** Entry point used by the parent test to run exactly one MATSim controller per JVM. */
	public static void main(String[] args) throws Exception {
		if (args.length != 4 || !WORKER_ARGUMENT.equals(args[0])) {
			throw new IllegalArgumentException("Expected: " + WORKER_ARGUMENT + " <output> <probe> <sentinel>");
		}

		Path output = Path.of(args[1]);
		boolean enableProbe = Boolean.parseBoolean(args[2]);
		Path sentinel = Path.of(args[3]);
		runActualKelheimTestInput(output, enableProbe, sentinel);
	}

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
	public final void serviceQualityProbeHasNoSimulationSideEffects() throws IOException, InterruptedException {
		Path root = Path.of("test/output/kelheim-service-quality-probe");
		Path disabledRoot = root.resolve("disabled-a");
		Path repeatedDisabledRoot = root.resolve("disabled-b");
		Path enabledRoot = root.resolve("enabled");

		runInSeparateJvm(disabledRoot, false);
		runInSeparateJvm(repeatedDisabledRoot, false);
		runInSeparateJvm(enabledRoot, true);

		Path disabledEvents = findSingle(disabledRoot, ".0.events.xml.zst");
		Path repeatedDisabledEvents = findSingle(repeatedDisabledRoot, ".0.events.xml.zst");
		Path enabledEvents = findSingle(enabledRoot, ".0.events.xml.zst");
		assertEventFilesEqual("disabled run A vs disabled run B", disabledEvents, repeatedDisabledEvents);
		assertEventFilesEqual("disabled run A vs enabled run", disabledEvents, enabledEvents);
		assertDecompressedFilesEqual("final plans", disabledRoot, repeatedDisabledRoot, ".0.plans.xml.zst");
		assertDecompressedFilesEqual("final experienced plans", disabledRoot, repeatedDisabledRoot, ".0.experienced_plans.xml.zst");
		assertDecompressedFilesEqual("final plans with probe", disabledRoot, enabledRoot, ".0.plans.xml.zst");
		assertDecompressedFilesEqual("final experienced plans with probe", disabledRoot, enabledRoot, ".0.experienced_plans.xml.zst");
		assertThat(readSentinel(disabledRoot)).isEqualTo(readSentinel(repeatedDisabledRoot));
		assertThat(readSentinel(disabledRoot)).isEqualTo(readSentinel(enabledRoot));

		assertThat(findAll(disabledRoot, ".drt_service_quality_probes.csv.gz")).isEmpty();
		assertThat(findAll(repeatedDisabledRoot, ".drt_service_quality_probes.csv.gz")).isEmpty();
		assertThat(findAll(enabledRoot, ".drt_service_quality_probes.csv.gz")).hasSize(1);
	}

	private static void runInSeparateJvm(Path outputRoot, boolean enableProbe) throws IOException, InterruptedException {
		Path absoluteOutput = outputRoot.toAbsolutePath();
		Path sentinel = absoluteOutput.resolve("rng-sentinel.txt");
		Path java = Path.of(System.getProperty("java.home"), "bin", "java");
		Process process = new ProcessBuilder(
			java.toString(),
			"-cp", System.getProperty("java.class.path"),
			RunKelheimIntegrationTest.class.getName(),
			WORKER_ARGUMENT,
			absoluteOutput.toString(),
			Boolean.toString(enableProbe),
			sentinel.toString())
			.inheritIO()
			.start();
		int exitCode = process.waitFor();
		assertThat(exitCode).as("Kelheim worker exit code for %s", outputRoot).isZero();
		assertThat(waitForComplete(findSingle(outputRoot, TEST_ITERATION_SUFFIX)))
			.as("completed event stream for %s", outputRoot).isTrue();
	}

	private static void runActualKelheimTestInput(Path outputRoot, boolean enableProbe, Path sentinel) throws IOException {
		RandomSentinel randomSentinel = new RandomSentinel();
		SentinelKelheimScenario.randomSentinel = randomSentinel;
		List<String> arguments = new java.util.ArrayList<>(List.of(
			"run",
			"--config", "input/v3.1/kelheim-v3.1-25pct.kexi.config.xml",
			"--1pct",
			"--with-drt",
			"--with-drt-expandedServiceArea",
			"--iterations=0",
			"--random-seed", "4711",
			"--drt-fleet-size=100",
			"--drt-expanded-service-area-stops", "expanded-service-area/drt_stops_landkreis.xml",
			"--drt-study-area-shp", "input/shp/lk-kelheim/lk-kelheim.shp",
			"--drt-service-quality-probe-stop-pair-input-files",
			"input/v3.1/expanded-service-area/drt_stops_queried_train_station.csv",
			"--drt-fleet-start-link-weights",
			"input/v3.1/expanded-service-area/populationLinkWeights.csv",
			"--config:global.numberOfThreads=1",
			"--config:qsim.numberOfThreads=1",
			"--config:controller.overwriteFiles=deleteDirectoryIfExists",
			"--config:swissRailRaptor.useIntermodalAccessEgress=false",
			"--output", outputRoot.toAbsolutePath().toString()
		));
		if (enableProbe) {
			arguments.add("--write-drt-service-quality-probe");
		}
		MATSimApplication.execute(SentinelKelheimScenario.class, null, arguments.toArray(String[]::new));
		try {
			Files.writeString(sentinel, randomSentinel.toString());
		} finally {
			SentinelKelheimScenario.randomSentinel = null;
		}
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

	private static void assertDecompressedFilesEqual(String comparison, Path firstRoot, Path secondRoot, String suffix) throws IOException {
		assertEventFilesEqual(comparison, findSingle(firstRoot, suffix), findSingle(secondRoot, suffix));
	}

	private static String readSentinel(Path root) throws IOException {
		return Files.readString(root.resolve("rng-sentinel.txt"));
	}

	private static boolean waitForComplete(Path compressedFile) throws IOException, InterruptedException {
		long deadline = System.nanoTime() + 15 * 60 * 1_000_000_000L;
		while (System.nanoTime() < deadline) {
			try (var reader = IOUtils.getBufferedReader(compressedFile.toString())) {
				while (reader.readLine() != null) {
					// Read to EOF so the zstd stream validates its final frame.
				}
				return true;
			} catch (IOException e) {
				TimeUnit.SECONDS.sleep(1);
			}
		}
		return false;
	}

	public static final class SentinelKelheimScenario extends RunKelheimScenario {
		private static RandomSentinel randomSentinel;

		public SentinelKelheimScenario() {
			super();
		}

		public SentinelKelheimScenario(Config config) {
			super(config);
		}

		@Override
		protected void prepareControler(Controler controler) {
			super.prepareControler(controler);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addMobsimListenerBinding().toInstance(randomSentinel);
				}
			});
		}
	}

	private static final class RandomSentinel implements MobsimBeforeSimStepListener, MobsimAfterSimStepListener {
		private Double before;
		private Double after;

		@Override
		public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent event) {
			if (before == null && event.getSimulationTime() >= FIRST_PROBE_TIME) {
				before = MatsimRandom.getLocalInstance().nextDouble();
			}
		}

		@Override
		public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent event) {
			if (before != null && after == null && event.getSimulationTime() >= FIRST_PROBE_TIME) {
				after = MatsimRandom.getLocalInstance().nextDouble();
			}
		}

		@Override
		public String toString() {
			return String.format(Locale.ROOT, "before=%.17g%nafter=%.17g%n", before, after);
		}
	}

}
