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
		header.description = "Overview for the demand-responsive mode '" + mode + "'" +
			"/n" + "This dashboard shows average values for " + noRuns +
			" simulation runs. For the results of the specific runs please choose the according directory next to this dashboard.yaml.";

//		DEMAND
		layout.row("one")
			.el(Table.class, (viz, data) -> {
				viz.title = "Rides per vehicle";
				viz.description = "Final demand statistics and KPI.";
				viz.dataset = postProcess(data, "rides_per_veh_avg_demand_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			})
			.el(Table.class, (viz, data) -> {
				viz.title = "Avg wait time";
				viz.description = "Final demand statistics and KPI.";
				viz.dataset = postProcess(data, "avg_wait_time_avg_demand_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			})
			.el(Table.class, (viz, data) -> {
				viz.title = "Requests";
				viz.description = "Final demand statistics and KPI.";
				viz.dataset = postProcess(data, "requests_avg_demand_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			});

		layout.row("two")
			.el(Table.class, (viz, data) -> {
				viz.title = "Avg total travel time";
				viz.description = "Final demand statistics and KPI.";
				viz.dataset = postProcess(data, "avg_total_travel_time_avg_demand_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			})
			.el(Table.class, (viz, data) -> {
				viz.title = "Rides";
				viz.description = "Final demand statistics and KPI.";
				viz.dataset = postProcess(data, "rides_avg_demand_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			})
			.el(Table.class, (viz, data) -> {
				viz.title = "Avg direct distance [km]";
				viz.description = "Final demand statistics and KPI.";
				viz.dataset = postProcess(data, "avg_direct_distance_[km]_avg_demand_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			});

		layout.row("three")
			.el(Table.class, (viz, data) -> {
				viz.title = "Rejections";
				viz.description = "Final demand statistics and KPI.";
				viz.dataset = postProcess(data, "rejections_avg_demand_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			})
			.el(Table.class, (viz, data) -> {
				viz.title = "95th percentile wait time";
				viz.description = "Final demand statistics and KPI.";
				viz.dataset = postProcess(data, "95th_percentile_wait_time_avg_demand_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			})
			.el(Table.class, (viz, data) -> {
				viz.title = "Avg in-vehicle time";
				viz.description = "Final demand statistics and KPI.";
				viz.dataset = postProcess(data, "avg_in-vehicle_time_avg_demand_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			});

		layout.row("four")
			.el(Table.class, (viz, data) -> {
				viz.title = "Avg ride distance [km]";
				viz.description = "Final demand statistics and KPI.";
				viz.dataset = postProcess(data, "avg_ride_distance_[km]_avg_demand_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			})
			.el(Table.class, (viz, data) -> {
				viz.title = "Rejection rate";
				viz.description = "Final demand statistics and KPI.";
				viz.dataset = postProcess(data, "rejection_rate_avg_demand_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			})
			.el(Table.class, (viz, data) -> {
				viz.title = "Avg fare [MoneyUnit]";
				viz.description = "Final demand statistics and KPI.";
				viz.dataset = postProcess(data, "avg_fare_[MoneyUnit]_avg_demand_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			});

//		SUPPLY
		supplyTabs(layout);
	}

	private void supplyTabs(Layout layout) {
		layout.row("six")
			.el(Table.class, (viz, data) -> {
				viz.title = "Total service hours";
				viz.description = "Final configuration and service KPI.";
				viz.dataset = postProcess(data, "total_service_hours_avg_supply_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			})
			.el(Table.class, (viz, data) -> {
				viz.title = "Pooling ratio";
				viz.description = "Final configuration and service KPI.";
				viz.dataset = postProcess(data, "pooling_ratio_avg_supply_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			})
			.el(Table.class, (viz, data) -> {
				viz.title = "Detour ratio";
				viz.description = "Final configuration and service KPI.";
				viz.dataset = postProcess(data, "detour_ratio_avg_supply_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			});

		layout.row("seven")
			.el(Table.class, (viz, data) -> {
				viz.title = "Total vehicle mileage [km]";
				viz.description = "Final configuration and service KPI.";
				viz.dataset = postProcess(data, "total_vehicle_mileage_[km]_avg_supply_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			})
			.el(Table.class, (viz, data) -> {
				viz.title = "Empty ratio";
				viz.description = "Final configuration and service KPI.";
				viz.dataset = postProcess(data, "empty_ratio_avg_supply_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			})
			.el(Table.class, (viz, data) -> {
				viz.title = "Number of stops";
				viz.description = "Final configuration and service KPI.";
				viz.dataset = postProcess(data, "number_of_stops_avg_supply_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			});

		layout.row("eight")
			.el(Table.class, (viz, data) -> {
				viz.title = "Total pax distance [km]";
				viz.description = "Final configuration and service KPI.";
				viz.dataset = postProcess(data, "total_pax_distance_[km]_avg_supply_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			})
			.el(Table.class, (viz, data) -> {
				viz.title = "Vehicles";
				viz.description = "Final configuration and service KPI.";
				viz.dataset = postProcess(data, "vehicles_avg_supply_stats.csv");
				viz.showAllRows = true;
				viz.width = 1.;
			});
	}
}
