package org.matsim.dashboard;

import org.matsim.analysis.postAnalysis.NoiseAverageAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Data;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.ColorScheme;
import org.matsim.simwrapper.viz.GridMap;
import org.matsim.simwrapper.viz.MapPlot;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows emission in the scenario.
 */
public class AverageKelheimNoiseDashboard implements Dashboard {

	private double minDb = 40;
	private double maxDb = 80;
	private final List<String> dirs;
	private final Integer noRuns;
	private static final String NOISE = "noise";
	private static final String DARK_BLUE = "#1175b3";
	private static final String LIGHT_BLUE = "#95c7df";
	private static final String ORANGE = "#f4a986";
	private static final String RED = "#cc0c27";

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

		layout.row("aggregate noise")
			.el(GridMap.class, (viz, data) -> {
				viz.title = "Noise Immissions (Grid)";
				viz.description = "Aggregate Noise Immissions per day";
				viz.height = 12.0;
				viz.cellSize = 250;
				viz.opacity = 0.2;
				viz.maxHeight = 20;
				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
				viz.setColorRamp(new double[]{40, 50, 60}, new String[]{DARK_BLUE, LIGHT_BLUE, ORANGE, RED});
				viz.file = postProcess(data, "mean_immission_per_day.avro");
			})
			.el(MapPlot.class, (viz, data) -> {
				viz.title = "Noise Emissions (Link)";
				viz.description = "Aggregate Noise Emissions per day";
				viz.height = 12.0;
				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
				viz.minValue = minDb;
				viz.maxValue = maxDb;
				viz.setShape(new CreateAverageDashboards().copyGeoJsonNetwork(dirs));
				viz.addDataset(NOISE, postProcess(data, "mean_emission_per_day.csv"));
				viz.display.lineColor.dataset = NOISE;
				viz.display.lineColor.columnName = "value";
				viz.display.lineColor.join = "Link Id";
				viz.display.lineColor.fixedColors = new String[]{DARK_BLUE, LIGHT_BLUE, ORANGE, RED};
				viz.display.lineColor.setColorRamp(ColorScheme.RdYlBu, 4, true, "45, 55, 65");
				viz.display.lineWidth.dataset = NOISE;
				viz.display.lineWidth.columnName = "value";
				viz.display.lineWidth.scaleFactor = 8d;
				viz.display.lineWidth.join = "Link Id";
			});
		layout.row("hourly noise")
			.el(GridMap.class, (viz, data) -> {
				viz.title = "Hourly Noise Immissions (Grid)";
				viz.description = "Noise Immissions per hour";
				viz.height = 12.0;
				viz.cellSize = 250;
				viz.opacity = 0.2;
				viz.maxHeight = 20;
				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
				viz.setColorRamp(new double[]{40, 50, 60}, new String[]{DARK_BLUE, LIGHT_BLUE, ORANGE, RED});
				viz.file = postProcess(data, "mean_immission_per_hour.avro");
			});
	}
}
