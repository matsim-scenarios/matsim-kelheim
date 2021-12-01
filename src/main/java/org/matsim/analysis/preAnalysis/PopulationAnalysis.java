package org.matsim.analysis.preAnalysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import picocli.CommandLine;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@CommandLine.Command(
        name = "analyze-population",
        description = "Extract the home location of the persons in the population file"
)
public class PopulationAnalysis implements MATSimAppCommand {
    @CommandLine.Option(names = "--population", description = "Path to input population", required = true)
    private String populationPath;

    @CommandLine.Option(names = "--output-folder", description = "Path to analysis output folder", required = true)
    private String outputFolder;

    @CommandLine.Mixin
    private ShpOptions shp = new ShpOptions();

    public static String HOME_LOCATION = "home_location";

    public enum HomeLocationCategory {inside, outside, unknown}

    private final List<Person> personsLivesInAnalyzedArea = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        new PopulationAnalysis().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        Population population = PopulationUtils.readPopulation(populationPath);
        System.out.println("There are in total " + population.getPersons().size() + " persons in the population file");

        Geometry analyzedArea = null;
        if (shp.getShapeFile() != null) {
            analyzedArea = shp.getGeometry();
        }

        analyzeHomeLocation(population, analyzedArea);
        summarizePersonAttribute(population, analyzedArea);

        return 0;
    }

    private void analyzeHomeLocation(Population population, Geometry analyzedArea) throws IOException {
        CSVPrinter csvWriter = new CSVPrinter(new FileWriter(outputFolder + "/persons-home-locations.csv"), CSVFormat.TDF);
        csvWriter.printRecord("person", "home_x", "home_y", "home_location");

        System.out.println("Start person home location analysis...");
        int counter = 0;
        int numPersonsLiveInKelheim = 0;
        for (Person person : population.getPersons().values()) {
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity) {
                    String actType = ((Activity) planElement).getType();
                    if (actType.startsWith("home")) {
                        Coord homeCoord = ((Activity) planElement).getCoord();
                        counter++;
                        if (analyzedArea == null) {
                            csvWriter.printRecord(person.getId().toString(),
                                    Double.toString(homeCoord.getX()), Double.toString(homeCoord.getY()), HomeLocationCategory.unknown);
                        } else if (analyzedArea.contains(MGC.coord2Point(homeCoord))) {
                            csvWriter.printRecord(person.getId().toString(),
                                    Double.toString(homeCoord.getX()), Double.toString(homeCoord.getY()), HomeLocationCategory.inside);
                            personsLivesInAnalyzedArea.add(person);
                            numPersonsLiveInKelheim++;
                        } else {
                            csvWriter.printRecord(person.getId().toString(),
                                    Double.toString(homeCoord.getX()), Double.toString(homeCoord.getY()), HomeLocationCategory.outside);
                        }
                        break;
                    }
                }
            }
        }
        csvWriter.close();

        System.out.println("There are " + counter + " persons with home activity");
        if (analyzedArea != null) {
            System.out.println("There are " + numPersonsLiveInKelheim +
                    " persons living in the analyzed area (with home location inside the provided shape file");
            // Write the list of persons live in the area
            CSVPrinter csvWriter2 = new CSVPrinter(new FileWriter(outputFolder + "/relevant-persons.csv"), CSVFormat.TDF);
            csvWriter2.printRecord("person-id");
            for (Person person : personsLivesInAnalyzedArea) {
                csvWriter2.printRecord(person.getId().toString());
            }
            csvWriter2.close();
        }
    }

    private void summarizePersonAttribute(Population population, Geometry kelheim) throws IOException {
        List<Person> personsToAnalyze = new ArrayList<>();
        if (kelheim != null) {
            personsToAnalyze.addAll(personsLivesInAnalyzedArea);
        } else {
            personsToAnalyze.addAll(population.getPersons().values());
        }

        CSVPrinter csvWriter = new CSVPrinter(new FileWriter(outputFolder + "/persons-attributes.csv"), CSVFormat.TDF);
        csvWriter.printRecord("person", "age", "sex", "household_size", "household_income_group", "estimated_personal_allowance", "number_of_trips_per_day");

        for (Person person : personsToAnalyze) {
            Double income = PersonUtils.getIncome(person); // This value may be null;
            Integer age = PersonUtils.getAge(person); // THis value may be null;
            String sex = PersonUtils.getSex(person);
            String incomeGroup = (String) person.getAttributes().getAttribute("MiD:hheink_gr2");
            String householdSize = (String) person.getAttributes().getAttribute("MiD:hhgr_gr");

            if (income == null) {
                income = -1.0;
            }

            if (age == null) {
                age = -1;
            }

            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());
            int numOfTripsPerDay = trips.size();

            csvWriter.printRecord(person.getId().toString(), age.toString(), sex,
                    householdSize, incomeGroup, income.toString(), Integer.toString(numOfTripsPerDay));
        }
    }

    public static boolean checkIfPersonLivesInArea(Person person, Geometry analyzedArea) {
        for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
            if (planElement instanceof Activity) {
                String actType = ((Activity) planElement).getType();
                if (actType.startsWith("home")) {
                    Coord homeCoord = ((Activity) planElement).getCoord();
                    if (analyzedArea == null) {
                        throw new RuntimeException("The analyzed area is null! ");
                    } else return analyzedArea.contains(MGC.coord2Point(homeCoord));
                }
            }
        }
        return false; // Person with no home activity --> false
    }
}
