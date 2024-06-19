package org.matsim.dashboard;


import org.matsim.analysis.postAnalysis.drt.DrtPostProcessingAverageAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Data;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Average DRT dashboard for several runs with the same config but a different random seed. Dashboard for one specific DRT service.
 */
public class AverageDrtDashboard implements Dashboard {
	private final List<String> dirs;
	private final String mode;
	private final Integer noRuns;

	public AverageDrtDashboard(List<String> dirs, String mode, Integer noRuns) {
		this.dirs = dirs;
		this.mode = mode;
		this.noRuns = noRuns;
	}

	private String postProcess(Data data, String outputFile) {
//		args for analysis have to be: list of paths to run dirs + drt modes / folder names
		List<String> args = new ArrayList<>(List.of("--input-runs", String.join(",", dirs), "--input-mode", mode,
			"--no-runs", noRuns.toString()));

		return data.compute(DrtPostProcessingAverageAnalysis.class, outputFile, args.toArray(new String[0]));
	}

	@Override
	public void configure(Header header, Layout layout) {
		header.title = mode;
		header.description = "Overview for the demand-responsive mode '" + mode + "'. This dashboard shows average values for " + noRuns +
			" simulation runs. For the results of the specific runs please choose the according directory next to this dashboard.yaml.";

//		DEMAND
		layout.row("demand")
			.el(Table.class, (viz, data) -> {
				viz.title = "Average demand results";
				viz.dataset = postProcess(data, "avg_demand_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			});

//		SUPPLY
		layout.row("supply")
			.el(Table.class, (viz, data) -> {
				viz.title = "Average service results";
				viz.dataset = postProcess(data, "avg_supply_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			});
	}
}
