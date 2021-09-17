package org.matsim.analysis.preAnalysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import picocli.CommandLine;

import java.io.FileWriter;
import java.io.IOException;


@CommandLine.Command(
        name = "analyze-population",
        description = "Extract the home location of the persons in the population file"
)
public class PopulationAnalysis implements MATSimAppCommand {
    @CommandLine.Option(names = "--population", description = "Path to input population", required = true)
    private String populationPath;

    @CommandLine.Option(names = "--output", description = "Path to analysis output", required = true)
    private String outputPath;

    @CommandLine.Mixin
    private ShpOptions shp = new ShpOptions();

    public static void main(String[] args) throws IOException {
        new PopulationAnalysis().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem("EPSG:25832");
        config.plans().setInputFile(populationPath);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();

        Geometry kelheim = null;
        if (shp.getShapeFile() != null) {
            kelheim = shp.getGeometry();
        }

        CSVPrinter csvWriter = new CSVPrinter(new FileWriter(outputPath), CSVFormat.DEFAULT);
        csvWriter.printRecord("person", "home_x", "home_y");

        System.out.println("Start processing population file...");
        int counter = 0;
        int homelessPersons = 0;
        for (Person person : population.getPersons().values()) {
            boolean homeless = true;
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity) {
                    String actType = ((Activity) planElement).getType();
                    if (actType.startsWith("home")) {
                        Coord homeCoord = ((Activity) planElement).getCoord();
                        homeless = false;
                        if (kelheim == null || kelheim.contains(MGC.coord2Point(homeCoord))) {
                            csvWriter.printRecord(person.getId().toString(),
                                    Double.toString(homeCoord.getX()), Double.toString(homeCoord.getY()));
                            counter += 1;
                        }
                        break;
                    }
                }
            }

            if (homeless) {
                homelessPersons += 1;
            }
        }
        csvWriter.close();
        if (kelheim==null){
            System.out.println("There are " + counter + " persons with home activity");
        } else
            System.out.println("There are " + counter + " persons living in Kelheim (with home activity in Landkreis Kelheim");

        System.out.println("There are " + homelessPersons + " persons without any home activity");

        return 0;
    }
}
