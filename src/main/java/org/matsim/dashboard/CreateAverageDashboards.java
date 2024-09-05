package org.matsim.dashboard;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.application.MATSimAppCommand;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.SimWrapper;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * class to create average dashboards and run the necessary analysis for that.
 */
final class CreateAverageDashboards implements MATSimAppCommand {
	@CommandLine.Option(names = "--input-path", required = true, description = "Path to directory with run directories.")
	private String inputPath;
	@CommandLine.Option(names = "--no-runs", defaultValue = "5", description = "Number of simulation runs to be averaged.")
	private Integer noRuns;
	@CommandLine.Option(names = "--base-run", description = "Path to directory base run.", defaultValue = "/net/ils/matsim-kelheim/v3.0-release/output-base/25pct")
	private String pathToBaseRun;

	public static void main(String[] args) {
		new CreateAverageDashboards().execute(args);
	}

	CreateAverageDashboards() {

	}



	@Override
	public Integer call() throws Exception {
		// Collect all folder names
		File[] foldersList = new File(inputPath).listFiles();
		List<String> foldersSeeded = new ArrayList<>();

		String analysisDir = "";

		for (File folder : Objects.requireNonNull(foldersList)) {
			if (!folder.isDirectory() || !folder.getAbsolutePath().contains("seed")) continue;

			String absPath = folder.getAbsolutePath();

			foldersSeeded.add(absPath);

			if (analysisDir.isEmpty()) {
				analysisDir = absPath + "/analysis";
			}
		}

//		get drt modes for different dashboards from analysis folder of one run
		List<String> modes = new ArrayList<>();

		Arrays.stream(new File(analysisDir).listFiles())
			.filter(d -> d.getAbsolutePath().contains(TransportMode.drt))
			.forEach(f -> modes.add(f.getName()));

		SimWrapper sw = SimWrapper.create();

		for (String m : modes) {
			Dashboard.Customizable d = Dashboard.customize(new AverageDrtDashboard(foldersSeeded, m, noRuns))
				.context(m);

			sw.addDashboard(d);
		}

		sw.addDashboard(Dashboard.customize(new AverageKelheimEmissionsDashboard(foldersSeeded, noRuns, pathToBaseRun)).context("emissions"));
		sw.addDashboard(Dashboard.customize(new AverageKelheimNoiseDashboard(foldersSeeded, noRuns)).context("noise"));
		sw.generate(Path.of(inputPath), true);
		sw.run(Path.of(inputPath));

		return 0;
	}

	/**
	 * A helper method to copy an already existing Geojson / avro network rather than creating it all over again.
	 */
	String copyVizNetwork(List<String> dirs, String fileType) {

		for (String dir : dirs) {
			File networkFile = new File(dir + "/analysis/network/network" + fileType);
			Path target = Path.of(Path.of(dir).getParent() + "/analysis/network");

			if (Files.notExists(target) && networkFile.exists() && networkFile.isFile()) {
				try {
					Files.createDirectories(target);
					Files.copy(networkFile.toPath(), Path.of(target + "/network" + fileType));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		}
		return "analysis/network/network" + fileType;
	}
}
