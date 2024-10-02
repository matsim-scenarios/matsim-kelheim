package org.matsim.dashboard;

import org.matsim.analysis.postAnalysis.NoiseAverageAnalysis;
import org.matsim.run.RunKelheimScenario;
import org.matsim.simwrapper.*;
import org.matsim.simwrapper.viz.ColorScheme;
import org.matsim.simwrapper.viz.GridMap;
import org.matsim.simwrapper.viz.MapPlot;
import org.matsim.simwrapper.viz.Tile;

import java.util.ArrayList;
import java.util.List;

/**
 * Averages and displays the noise outcome of single runs.
 */
public class AverageKelheimNoiseDashboard implements Dashboard {

	private double minDb = 40;
	private double maxDb = 80;
	private final List<String> dirs;
	private final Integer noRuns;
	private static final String NOISE = "noise";


	public AverageKelheimNoiseDashboard(List<String> dirs, Integer noRuns) {
		this.dirs = dirs;
		this.noRuns = noRuns;
	}

	private String postProcess(Data data, String outputFile) {
//		args for analysis have to be: list of paths to run dirs + drt modes / folder names
		List<String> args = new ArrayList<>(List.of("--input-runs", String.join(",", dirs), "--no-runs", noRuns.toString()));

		return data.compute(NoiseAverageAnalysis.class, outputFile, args.toArray(new String[0]));
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Average Noise";
		header.description = "Shows the average noise footprint and spatial distribution for several simulation runs.";

		layout.row("stats")
			.el(Tile.class, (viz, data) -> {
				viz.dataset = postProcess(data, "mean_noise_stats.csv");
				viz.height = 0.1;
			});

		layout.row("emissions")
			.el(MapPlot.class, (viz, data) -> {
				viz.title = "Noise Emissions (Link)";
				viz.description = "Maximum Noise Level per day [dB]";
				viz.height = 12.0;
				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
				viz.minValue = minDb;
				viz.maxValue = maxDb;
				viz.setShape(new CreateAverageDashboards().copyVizNetwork(dirs, ".avro"), "id");
				viz.addDataset(NOISE, postProcess(data, "mean_emission_per_day.csv"));
				viz.display.lineColor.dataset = NOISE;
				viz.display.lineColor.columnName = "value";
				viz.display.lineColor.join = "Link Id";
				viz.display.lineColor.setColorRamp(ColorScheme.Oranges, 8, false, "35, 45, 55, 65, 75, 85, 95");
				viz.display.lineWidth.dataset = NOISE;
				viz.display.lineWidth.columnName = "value";
				viz.display.lineWidth.scaleFactor = 8d;
				viz.display.lineWidth.join = "Link Id";
			});
		layout.row("immissions")
			.el(GridMap.class, (viz, data) -> {
				viz.title = "Noise Immissions (Grid)";
				viz.description = "Total Noise Immissions per day";
				DashboardUtils.setGridMapStandards(viz, data, "EPSG:25832");
				viz.file = postProcess(data, "mean_immission_per_day.avro");
			})
			.el(GridMap.class, (viz, data) -> {
				viz.title = "Hourly Noise Immissions (Grid)";
				viz.description = "Noise Immissions per hour";
				DashboardUtils.setGridMapStandards(viz, data, "EPSG:25832");
				viz.file = postProcess(data, "mean_immission_per_hour.avro");
			});
		layout.row("damages")
			.el(GridMap.class, (viz, data) -> {
				viz.title = "Daily Noise Damages (Grid)";
				viz.description = "Total Noise Damages per day [€]";
				DashboardUtils.setGridMapStandards(viz, data, "EPSG:25832");
				viz.file = postProcess(data, "mean_damages_receiverPoint_per_day.avro");
			})
			.el(GridMap.class, (viz, data) -> {
				viz.title = "Hourly Noise Damages (Grid)";
				viz.description = "Noise Damages per hour [€]";
				DashboardUtils.setGridMapStandards(viz, data, "EPSG:25832");
				viz.file = postProcess(data, "mean_damages_receiverPoint_per_hour.avro");
			});
	}

}
