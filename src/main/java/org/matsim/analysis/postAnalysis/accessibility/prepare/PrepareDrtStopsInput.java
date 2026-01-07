package org.matsim.analysis.postAnalysis.accessibility.prepare;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import static org.matsim.core.scenario.ScenarioUtils.createScenario;

public class PrepareDrtStopsInput {

	public static void main(String[] args) throws IOException, FactoryException, TransformException {
		//define files
		String networkFile = "../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/0000-kelheim-scratch/kexi-seed1-ASC-2.45.output_network.xml.gz";
		File stopsCsv = new File("../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/_data/1_processed/drt_stops/stops_25832.csv");
		File stopXmlOutput = new File("../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/_data/1_processed/drt_stops/drt_stops.xml");


		// create scenario
		Config config = ConfigUtils.createConfig();
		Scenario scenario = createScenario(config);

		// read network
		TransitScheduleFactory tsf = scenario.getTransitSchedule().getFactory();

		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(networkFile);

		Network subNetwork = NetworkUtils.createNetwork(config.network());
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(subNetwork, Set.of(TransportMode.car));


//		GeometryFactory gf = new GeometryFactory();
		// Read stops csv
		CsvReadOptions options = CsvReadOptions.builder(stopsCsv)
			.separator(',')          // e.g. for European CSVs
			.header(true)             // default: true
			.missingValueIndicator("") // optional
			.build();

		Table table = Table.read().csv(options);


		for (int i = 0; i < table.rowCount(); i++) {
			double x = table.doubleColumn("x").get(i);
			double y = table.doubleColumn("y").get(i);
			Coord coord = new Coord(x, y);
			TransitStopFacility transitStopFacility = tsf.createTransitStopFacility(Id.create(i, TransitStopFacility.class), coord, false);
			Link nearestLink = NetworkUtils.getNearestLink(subNetwork, coord);
			transitStopFacility.setLinkId(nearestLink.getId());
			scenario.getTransitSchedule().addStopFacility(transitStopFacility);
		}

		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(stopXmlOutput.toString());


	}
}
