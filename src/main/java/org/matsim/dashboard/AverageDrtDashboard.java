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

		layout.row("Results")
			.el(Table.class, (viz, data) -> {
				viz.title = "Service results";
				viz.description = "Final configuration and service KPI.";
				viz.dataset = postProcess(data, "avg_supply_stats.csv");
//				viz.style = "topsheet";
				viz.showAllRows = true;
				viz.width = 1.;
			})
			.el(Table.class, (viz, data) -> {
				viz.title = "Demand results";
				viz.description = "Final demand statistics and KPI.";
				viz.dataset = postProcess(data, "avg_demand_stats.csv");
//				viz.style = "topsheet";
				viz.showAllRows = true;
				viz.width = 1.;
			});
//			.el(MapPlot.class, (viz, data) -> {
//				switch (drtConfigGroup.operationalScheme) {
//					case stopbased -> {
//						viz.title = "Map of stops";
//						viz.setShape(postProcess(data, "stops.shp"), "id");
//
//						viz.addDataset("trips", postProcess(data, "trips_per_stop.csv"));
//
//						viz.display.radius.dataset = "trips";
//						viz.display.radius.columnName = "departures";
//						viz.display.radius.join = "stop_id";
//						viz.display.radius.scaleFactor = 10d;
//
//					}
//					case door2door -> {
//						//TODO add drtNetwork !?
//					}
//					case serviceAreaBased -> {
//						viz.title = "Service area";
//						viz.setShape(postProcess(data, "serviceArea.shp"), "something");
//					}
//				}
//				viz.center = data.context().getCenter();
//				viz.zoom = data.context().mapZoomLevel;
//				viz.width = 2.;
//			});
//
//		//TODO: discuss: should we make XYTime plots out of those ??
//		layout.row("Spatial demand distribution")
//			.el(Hexagons.class, (viz, data) -> {
//				viz.title = "Spatial demand distribution";
//				viz.description = "Origins and destinations.";
//				viz.projection = this.crs;
//				viz.file = data.output("*output_drt_legs_" + drtConfigGroup.mode + ".csv");
//				viz.addAggregation("OD", "origins", "fromX", "fromY", "destinations", "toX", "toY");
//
//				viz.center = data.context().getCenter();
//				viz.zoom = data.context().mapZoomLevel;
//				viz.height = 7d;
//			})
//			.el(Hexagons.class, (viz, data) -> {
//				viz.title = "Spatial rejection distribution";
//				viz.description = "Requested (and rejected) origins and destinations.";
//				viz.projection = this.crs;
//				viz.file = data.output("ITERS/it." + lastIteration + "/*rejections_" + drtConfigGroup.mode + ".csv");
//				viz.addAggregation("rejections", "origins", "fromX", "fromY", "destinations", "toX", "toY");
//
//				viz.center = data.context().getCenter();
//				viz.zoom = data.context().mapZoomLevel;
//			})
//		;
//
////		This plot is not absolutely necesarry given the hex plots
////		if (drtConfigGroup.operationalScheme == DrtConfigGroup.OperationalScheme.stopbased)
////			layout.row("od").el(AggregateOD.class, (viz, data) -> {
////
////				viz.shpFile = postProcess(data, "stops.shp");
////				viz.dbfFile = postProcess(data, "stops.shp").replace(".shp", ".dbf");
////				viz.csvFile = postProcess(data, "od.csv");
////
////			});
//
//		//Final Demand stats
//		layout.row("Final Demand And Wait Time Statistics")
//			.el(Plotly.class, (viz, data) -> {
//				viz.title = "Final Demand and Wait Stats over day time";
//				viz.description = "Number of rides (customers) is displayed in bars, wait statistics in lines";
//
//				Plotly.DataSet waitStats = viz.addDataset(data.output("*_waitStats_" + drtConfigGroup.mode + ".csv"));
//				Plotly.DataSet rejections = viz.addDataset(data.output("*drt_rejections_perTimeBin_" + drtConfigGroup.mode + ".csv"));
//
//				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
//					.xAxis(Axis.builder().title("Time Bin").build())
//					.yAxis(Axis.builder().title("Wait Time [s]").build())
//					.yAxis2(Axis.builder().title("Nr of Rides/Rejections")
//						.side(Axis.Side.right)
//						.overlaying(ScatterTrace.YAxis.Y)
//						.build())
//					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
//					.build();
//
//				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
//						.mode(ScatterTrace.Mode.LINE)
//						.name("Average")
//						.build(),
//					waitStats.mapping()
//						.x("timebin")
//						.y("average_wait")
//				);
//
//				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
//						.mode(ScatterTrace.Mode.LINE)
//						.name("P5")
//						.build(),
//					waitStats.mapping()
//						.x("timebin")
//						.y("p_5")
//				);
//
//				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
//						.mode(ScatterTrace.Mode.LINE)
//						.name("P95")
//						.build(),
//					waitStats.mapping()
//						.x("timebin")
//						.y("p_95")
//				);
//
//				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT)
//						.opacity(0.3)
//						.yAxis(ScatterTrace.YAxis.Y2.toString())
//						.name("Rides")
//						.build(),
//					waitStats.mapping()
//						.x("timebin")
//						.y("legs")
//				);
//
//				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT)
//						.opacity(0.3)
//						.yAxis(ScatterTrace.YAxis.Y2.toString())
//						.name("Rejections")
//						.build(),
//					rejections.mapping()
//						.x("timebin")
//						.y("rejections")
//				);
//
//			})
//			.el(Area.class, (viz, data) -> {
//				//actually, without title the area plot won't work
//				viz.title = "Vehicle occupancy";
//				viz.description = "Number of passengers on board at a time";
//				viz.dataset = data.output("ITERS/it." + lastIteration + "/*occupancy_time_profiles_" + drtConfigGroup.mode + ".txt");
//				viz.x = "time";
//				viz.xAxisName = "Time";
//				viz.yAxisName = "Vehicles [1]";
//			});
//
//
//		//from here on, we show the 'evolution' of statistics over iterations
//		//based on CL's feedback this is not helpful for low iteration numbers
//		//this implementation actually does not account for situations where firstIteration > 0.
//		if (lastIteration >= 3) {
//
//			//Demand stats over iterations
//			layout.row("Demand and Wait Time Statistics per iteration")
//				.el(Plotly.class, (viz, data) -> {
//					viz.title = "Rides and rejections per iteration";
//					viz.description = "Number of rides (customers) and rejections over the course of the simulation";
//
//					Plotly.DataSet dataset = viz.addDataset(data.output("*_customer_stats_" + drtConfigGroup.mode + ".csv"));
//
//					viz.layout = tech.tablesaw.plotly.components.Layout.builder()
//						.xAxis(Axis.builder().title("Iteration").build())
//						.yAxis(Axis.builder().title("Wait Time [s]").build())
//						.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
//						.build();
//
//					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT)
//							.name("Rejections")
//							.build(),
//						dataset.mapping()
//							.x("iteration")
//							.y("rejections")
////						.color(Plotly.ColorScheme.RdBu)
//					);
//
//					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT)
//							.name("Rides")
//							.build(),
//						dataset.mapping()
//							.x("iteration")
//							.y("rides")
////						.color(Plotly.ColorScheme.RdBu)
//					);
//
//				})
//				.el(Line.class, (viz, data) -> {
//					viz.title = "Waiting time statistics per iteration";
//					viz.description = "";
//					viz.dataset = data.output("*customer_stats_" + drtConfigGroup.mode + ".csv");
//					viz.x = "iteration";
//					viz.columns = List.of("wait_average", "wait_median", "wait_p95");
//					viz.legendName = List.of("Average", "Median", "95th Percentile");
//					viz.xAxisName = "Iteration";
//					viz.yAxisName = "Waiting Time [s]";
//				})
//				;
//
//			layout.row("Demand And Travel Time Statistics per iteration")
//				.el(Plotly.class, (viz, data) -> {
//					viz.title = "Travel time components per iteration";
//					viz.description = "Comparing mean wait vs. mean in-vehicle travel time per customer";
//
//					Plotly.DataSet dataset = viz.addDataset(data.output("*_customer_stats_" + drtConfigGroup.mode + ".csv"));
//
//					viz.layout = tech.tablesaw.plotly.components.Layout.builder()
//						.xAxis(Axis.builder().title("Iteration").build())
//						.yAxis(Axis.builder().title("Time [s]")
//							.build())
//						.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
//						.build();
//
//					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT)
////							.opacity(0.5)
//							.name("In Vehicle")
//							.build(),
//						dataset.mapping()
//							.x("iteration")
//							.y("inVehicleTravelTime_mean")
////						.color(Plotly.ColorScheme.RdBu)
//					);
//
//					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT)
////							.opacity(0.5)
//							.name("Wait")
//							.build(),
//						dataset.mapping()
//							.x("iteration")
//							.y("wait_average")
////						.color(Plotly.ColorScheme.RdBu)
//					);
//				})
//				.el(Line.class, (viz, data) -> {
//					viz.title = "Customer travel distance per iteration";
//					viz.dataset = data.output("*customer_stats_" + drtConfigGroup.mode + ".csv");
//					viz.description = "Customer traveled distance versus customer direct distance";
//					viz.x = "iteration";
//					viz.columns = List.of("directDistance_m_mean", "distance_m_mean");
//					viz.legendName = List.of("Mean direct distance", "Mean driven distance");
//					viz.xAxisName = "Iteration";
//					viz.yAxisName = "distance [m]";
//				});
//
//
//			//Evolution of fleet stats
//			layout.row("Fleet Stats per Iteration")
//				.el(Plotly.class, (viz, data) -> {
//					viz.title = "Fleet stats per iteration";
//					viz.description = "Number of " + drtConfigGroup.mode + " vehicles (customers) is displayed as bars, distance stats as lines";
//
//					Plotly.DataSet dataset = viz.addDataset(data.output("*_vehicle_stats_" + drtConfigGroup.mode + ".csv"));
//
//					viz.layout = tech.tablesaw.plotly.components.Layout.builder()
//						.xAxis(Axis.builder().title("Iteration").build())
//						.yAxis(Axis.builder().title("Total Distance [m]")
////					.overlaying(ScatterTrace.YAxis.Y2)
//							.build())
//						.yAxis2(Axis.builder().title("Nr Of Vehicles")
//							.side(Axis.Side.right)
//							.overlaying(ScatterTrace.YAxis.Y)
//							.build())
//						.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
//						.build();
//
//					viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
//							.mode(ScatterTrace.Mode.LINE)
//							.name("Pax Dist.")
//							.build(),
//						dataset.mapping()
//							.x("iteration")
//							.y("totalPassengerDistanceTraveled")
//					);
//
//					viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
//							.mode(ScatterTrace.Mode.LINE)
//							.name("Vehicle mileage")
//							.build(),
//						dataset.mapping()
//							.x("iteration")
//							.y("totalDistance")
//					);
//
//					viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
//							.mode(ScatterTrace.Mode.LINE)
//							.name("Empty mileage")
//							.build(),
//						dataset.mapping()
//							.x("iteration")
//							.y("totalEmptyDistance")
//					);
//
//					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT)
//							.opacity(0.2)
//							.yAxis(ScatterTrace.YAxis.Y2.toString())
//							.name("Vehicles")
//							.build(),
//						dataset.mapping()
//							.x("iteration")
//							.y("vehicles")
////						.color(Plotly.ColorScheme.RdBu)
//					);
//				})
//				.el(Line.class, (viz, data) -> {
//					viz.title = "Relative Statistics per iteration";
//					viz.dataset = data.output("*vehicle_stats_" + drtConfigGroup.mode + ".csv");
//					viz.description = "Pooling ratio (Pax distance / Vehicle mileage), Detour ratio, and Empty Ratio";
//					viz.x = "iteration";
//					viz.columns = List.of("d_p/d_t", "l_det", "emptyRatio");
//					viz.legendName = List.of("Pooling ratio", "Detour ratio", "Empty Ratio");
//					viz.xAxisName = "Iteration";
//					viz.yAxisName = "Value";
//				});
//		}


	}
}
