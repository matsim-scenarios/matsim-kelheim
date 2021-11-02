package org.matsim.run.prepare;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
    @CommandLine.Option(names = "--drt-stops", description = "path to drt stop xml file", required = true)
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
            for (CSVRecord record : parser) {
                double fromX = Double.parseDouble(record.get(4));
                double fromY = Double.parseDouble(record.get(5));
                double toX = Double.parseDouble(record.get(7));
                double toY = Double.parseDouble(record.get(8));
                Coord fromCoord = new Coord(fromX, fromY);
                Coord transformedFromCoord = crs.getTransformation().transform(fromCoord);
                Coord toCoord = new Coord(toX, toY);
                Coord transformedToCoord = crs.getTransformation().transform(toCoord);
                double departureTime = Double.parseDouble(record.get(2));

                Person person = populationFactory.createPerson(Id.createPersonId("drt_" + counter));
                Plan plan = populationFactory.createPlan();
                Activity activity0 = populationFactory.createActivityFromCoord("home", transformedFromCoord);
                activity0.setEndTime(departureTime);
                Leg leg = populationFactory.createLeg("drt");
                Activity activity1 = populationFactory.createActivityFromCoord("work", transformedToCoord);
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
        populationWriter.write(output);

        return 0;
    }

    private Map<String, Coord> loadStationCoordinates() throws IOException {
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
