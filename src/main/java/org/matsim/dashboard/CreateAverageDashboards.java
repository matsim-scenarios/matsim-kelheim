package org.matsim.dashboard;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.application.MATSimAppCommand;
import org.matsim.simwrapper.SimWrapper;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * class to create average dashboards and run the necessary analysis for that.
 */
public class CreateAverageDashboards implements MATSimAppCommand {
	@CommandLine.Option(names = "--input-path", required = true, description = "Path to directory with run directories.")
	private String inputPath;
	@CommandLine.Option(names = "--no-runs", defaultValue = "5", description = "Number of simulation runs to be averaged.")
	private Integer noRuns;

	public static void main(String[] args) {
		new CreateAverageDashboards().execute(args);
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
			.forEach(f -> modes.add(f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf("\\") + 1)));

		for (String m : modes) {
//			TODO: how to write output files to drt / drt-av folder rather than overwrite existing ones
//			TODO: same for dashbord, how to write a second dashboard for av instead of overwriting the existing one
			SimWrapper sw = SimWrapper.create();
			sw.addDashboard(new AverageDrtDashboard(foldersSeeded, m, noRuns));
			sw.generate(Path.of(inputPath));
			sw.run(Path.of(inputPath));
		}



		return 0;
	}
}
