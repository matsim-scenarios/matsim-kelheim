package org.matsim.analysis.postAnalysis.drt;

import org.matsim.contrib.drt.analysis.afterSimAnalysis.DrtVehicleStoppingTaskWriter;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Plot DRT stopping task (idle and pickup/drop-off stops) on the map, with start time and end time.
 * Please specify the output directory of the DRT in the input argument.
 */
public class PlotDrtStoppingTasks {
	public static void main(String[] args) throws IOException {
		new DrtVehicleStoppingTaskWriter(Path.of(args[0])).run();
	}
}
