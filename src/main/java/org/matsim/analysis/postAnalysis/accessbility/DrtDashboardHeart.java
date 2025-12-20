//package org.matsim.analysis.postAnalysis.accessbility;
//
//import org.matsim.application.analysis.accessibility.AccessibilityAnalysis;
//import org.matsim.application.analysis.accessibility.PrepareDrtStops;
//import org.matsim.application.analysis.accessibility.PrepareHouseholds;
//import org.matsim.application.analysis.accessibility.PreparePois;
//import org.matsim.contrib.accessibility.Modes4Accessibility;
//import org.matsim.simwrapper.Dashboard;
//import org.matsim.simwrapper.Data;
//import org.matsim.simwrapper.Header;
//import org.matsim.simwrapper.Layout;
//import org.matsim.simwrapper.viz.*;
//
//import java.util.List;
//
///**
// * Shows emission in the scenario.
// */
//public class DrtDashboardHeart implements Dashboard {
//
//
//	public double[] globalCenter;
//
//	public double height = 15d;
//
//
//	/**
//	 * Best provide the crs from {@link org.matsim.core.config.groups.GlobalConfigGroup}
//	 *
//	 */
//	public DrtDashboardHeart() {
//
////		this.coordinateSystem = coordinateSystem;
//
//	;}
//
//	@Override
//	public void configure(Header header, Layout layout) {
//
//
//		header.title = "DRT";
//		header.description = "Shows DRT Stops";
//		header.fullScreen = true;
//
//
//
//		layout.row("drtStops").el(MapPlot.class, ((viz, data) -> {
//			viz.title = "Demand Responsive Transit";
//			viz.description = "Shows DRT stops";
//			viz.setShape(data.compute(PrepareDrtStops.class, "stops.shp"));
//			viz.height = height;
//			viz.center = globalCenter;
//		}));
//
//	}
//
//}
