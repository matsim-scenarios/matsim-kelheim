package org.matsim.dashboard;


import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.*;

/**
 * Custom example dashboard for matsim class..
 */
public class MatsimClassCustomDashboard implements Dashboard {
	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Custom Dashboard";
		header.description = "An example of how to copy an existing dashboard and customize it.";

		layout.row("Spatial trip origin distribution")
			.el(Hexagons.class, (viz, data) -> {
				viz.title = "Spatial trip distribution";
				viz.description = "Origins of trips.";
				viz.projection = "EPSG:25832";
				viz.file = "./kelheim-v3.1-1pct-iter_1.output_trips.csv.gz";
				viz.addAggregation("Origins", "origins", "start_x", "start_y");
				viz.addAggregation("Destinations", "destinations", "end_x", "end_y");

				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
				viz.height = 7d;
			});
	}
}
