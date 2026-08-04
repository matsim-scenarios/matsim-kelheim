package org.matsim.analysis.postAnalysis.accessibility.prepare;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesWriter;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.File;

import static org.matsim.core.scenario.ScenarioUtils.createScenario;

public class PreparePoisInput {

	public static void main(String[] args) {
		File supermarketsCsv = new File("../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/_data/1_processed/osm_supermarkets_buffer5km/supermarkets_25832.csv");
		File poisXml = new File("../public-svn/matsim/scenarios/countries/de/kelheim/drtAccessibility/_data/1_processed/osm_supermarkets_buffer5km/pois.xml");


		// create scenario
		Config config = ConfigUtils.createConfig();
		Scenario scenario = createScenario(config);

		ActivityFacilitiesFactory aff = scenario.getActivityFacilities().getFactory();

		// 1) ADD TRAIN STATION
		ActivityFacility trainStationFacility = aff.createActivityFacility(Id.create("train_station", ActivityFacility.class), new Coord(714967.281485386,5420712.93057985));
		trainStationFacility.addActivityOption(aff.createActivityOption("train_station"));
		scenario.getActivityFacilities().addActivityFacility(trainStationFacility);

		// 2) ADD AMAZON LOGISTIC CENTER (in Rohr)
		ActivityFacility amazonFacility = aff.createActivityFacility(Id.create("logistic", ActivityFacility.class), new Coord(715535.90,5410431.39));
		amazonFacility.addActivityOption(aff.createActivityOption("logistic"));
		scenario.getActivityFacilities().addActivityFacility(amazonFacility);

		// 3) ADD SUPERMARKETS

		CsvReadOptions options = CsvReadOptions.builder(supermarketsCsv)
			.separator(',')          // e.g. for European CSVs
			.header(true)             // default: true
			.missingValueIndicator("") // optional
			.build();

		Table table = Table.read().csv(options);


		ActivityOption ao = aff.createActivityOption("supermarket");
		for (int i = 0; i < table.rowCount(); i++) {

			Id<ActivityFacility> id = Id.create("supermarket-" + i, ActivityFacility.class);

			double x = table.doubleColumn("x").get(i);
			double y = table.doubleColumn("y").get(i);
			ActivityFacility fac = aff.createActivityFacility(id, new Coord(x, y));
			fac.addActivityOption(ao);
			scenario.getActivityFacilities().addActivityFacility(fac);
		}

		new FacilitiesWriter(scenario.getActivityFacilities()).write(poisXml.toString());

	}

	private static Coordinate transformCoordinate(CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS, Coordinate sourceCoordinate) throws TransformException, FactoryException {

		// Create transform
		boolean lenient = true;
		MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, lenient);

		// Create coordinate
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
		Point sourcePoint = geometryFactory.createPoint(sourceCoordinate);

		// Transform
		Point targetPoint = (Point) org.geotools.geometry.jts.JTS.transform(sourcePoint, transform);

		return targetPoint.getCoordinate();
	}
}
