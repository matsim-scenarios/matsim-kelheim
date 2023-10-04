package org.matsim.run.prepare;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@CommandLine.Command(
	name = "generate-real-drt-demand",
	description = "Prepare drt only population based on real data"
)
public class PrepareRealDrtDemand implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(PrepareRealDrtDemand.class);

	@CommandLine.Option(names = "--drt-stops", description = "path to drt stop xml file", defaultValue = "")
	private String drtStops;

	@CommandLine.Option(names = "--demands", description = "path to real drt demand csv file", required = true)
	private String demands;

	@CommandLine.Option(names = "--output", description = "output path of drt only plans", required = true)
	private String output;

	@CommandLine.Mixin
	private CrsOptions crs = new CrsOptions();

	public static void main(String[] args) throws IOException {
		new PrepareRealDrtDemand().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:25832");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population population = scenario.getPopulation();
		PopulationFactory populationFactory = population.getFactory();

//        Map<String, Coord> stationCoordMap = loadStationCoordinates();

		try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(demands)),
			CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader())) {
			int counter = 0;
			for (CSVRecord row : parser) {
				double fromX = Double.parseDouble(row.get("from_x"));
				double fromY = Double.parseDouble(row.get("from_y"));
				double toX = Double.parseDouble(row.get("to_x"));
				double toY = Double.parseDouble(row.get("to_y"));
				Coord fromCoord = new Coord(fromX, fromY);
				Coord transformedFromCoord = crs.getTransformation().transform(fromCoord);
				Coord toCoord = new Coord(toX, toY);
				Coord transformedToCoord = crs.getTransformation().transform(toCoord);
				double departureTime = Double.parseDouble(row.get("time_in_seconds"));
				int numberOfPassengers = Integer.parseInt(row.get("number_of_passengers"));

				for (int i = 0; i < numberOfPassengers; i++) {
					Person person = populationFactory.createPerson(Id.createPersonId("drt_person_" + counter));
					Plan plan = populationFactory.createPlan();
					Activity activity0 = populationFactory.createActivityFromCoord("dummy", transformedFromCoord);
					activity0.setEndTime(departureTime);
					Leg leg = populationFactory.createLeg(TransportMode.drt);
					Activity activity1 = populationFactory.createActivityFromCoord("dummy", transformedToCoord);
					plan.addActivity(activity0);
					plan.addLeg(leg);
					plan.addActivity(activity1);
					person.addPlan(plan);
					population.addPerson(person);
					counter += 1;
				}
			}
			log.info("There are in total {} DRT requests on that day", counter);
		}

		// Write DRT plans
		PopulationWriter populationWriter = new PopulationWriter(population);
		populationWriter.write(output);

		return 0;
	}

	private Map<String, Coord> loadStationCoordinates() throws IOException {
		Map<String, Coord> stationCoordMap = new HashMap<>();
		try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(drtStops)),
			CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader())) {
			for (CSVRecord row : parser) {
				String stationName = row.get(0);
				double x = Double.parseDouble(row.get(2));
				double y = Double.parseDouble(row.get(3));
				stationCoordMap.put(stationName, new Coord(x, y));
			}

			log.info("There should be 147 DRT stops. The actual number of stops is {}.", stationCoordMap.size());
		}
		return stationCoordMap;
	}
}
