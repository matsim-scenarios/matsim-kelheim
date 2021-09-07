package org.matsim.run.prepare;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class PrepareRealDrtDemand {
    private final static String DRT_STOPS = "/Users/luchengqi/Documents/MATSimScenarios/Kelheim/drt-stops-locations.csv";
    private final static String REAL_DEMANDS = "/Users/luchengqi/Documents/RStudio-workspace/Kelheim/20201126-demand.csv";
    private final static String OUTPUT_PATH = "/Users/luchengqi/Documents/MATSimScenarios/Kelheim/drt-only-scenario/20201126-drt-only.plans.xml";

    public static void main(String[] args) throws IOException {
        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem("EPSG:25832");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();
        PopulationFactory populationFactory = population.getFactory();

        Map<String, Coord> stationCoordMap = loadStationCoordinates(DRT_STOPS);

        try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(REAL_DEMANDS)),
                CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader())) {
            int counter = 0;
            for (CSVRecord record : parser) {
                String from = record.get(3);
                String to = record.get(4);
                Coord fromCoord = stationCoordMap.get(from);
                Coord toCoord = stationCoordMap.get(to);
                double departureTime = Double.parseDouble(record.get(2));

                Person person = populationFactory.createPerson(Id.createPersonId("drt_" + counter));
                Plan plan = populationFactory.createPlan();
                Activity activity0 = populationFactory.createActivityFromCoord("home", fromCoord);
                activity0.setEndTime(departureTime);
                Leg leg = populationFactory.createLeg("drt");
                Activity activity1 = populationFactory.createActivityFromCoord("work", toCoord);
                plan.addActivity(activity0);
                plan.addLeg(leg);
                plan.addActivity(activity1);
                person.addPlan(plan);
                population.addPerson(person);
                counter += 1;
            }
            System.out.println("There are in total " + counter + " DRT requests on that day");
        }

        // Write DRT plans
        PopulationWriter populationWriter = new PopulationWriter(population);
        populationWriter.write(OUTPUT_PATH);

    }

    private static Map<String, Coord> loadStationCoordinates(String drtStops) throws IOException {
        Map<String, Coord> stationCoordMap = new HashMap<>();
        try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(drtStops)),
                CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                String stationName = record.get(0);
                double x = Double.parseDouble(record.get(2));
                double y = Double.parseDouble(record.get(3));
                stationCoordMap.put(stationName, new Coord(x, y));
            }
            System.out.println("There should be 147 DRT stops. The actual number of stops is " + stationCoordMap.size() + ".");
        }
        return stationCoordMap;
    }
}
