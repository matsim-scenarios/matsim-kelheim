package org.matsim.run.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.analysis.preAnalysis.PopulationAnalysis;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

@CommandLine.Command(
        name = "population",
        description = "Set the car availability attribute in the population"
)
public class PreparePopulation implements MATSimAppCommand {

    public static void main(String[] args) {
        new PreparePopulation().execute(args);
    }

    private static final Logger log = LogManager.getLogger(PreparePopulation.class);
    private final Random rnd = new Random(1234);

    @CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to input population")
    private Path input;

    @CommandLine.Option(names = "--output", description = "Path to output population", required = true)
    private Path output;

    @CommandLine.Mixin
    private ShpOptions shp = new ShpOptions();

    @Override
    public Integer call() throws Exception {

        if (!Files.exists(input)) {
            log.error("Input population does not exist: {}", input);
            return 2;
        }

        Population population = PopulationUtils.readPopulation(input.toString());

        Geometry studyArea = null;
        if (shp.getShapeFile() != null) {
            studyArea = shp.getGeometry();
        }


        for (Person person : population.getPersons().values()) {
            // Remove the trailing ".0" in the activity name
            for (Plan plan : person.getPlans()) {
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Activity) {
                        String originalType = ((Activity) planElement).getType();
                        String newType = originalType;
                        if (originalType.endsWith(".0")) {
                            newType = originalType.replace(".0", "");
                        }
                        ((Activity) planElement).setType(newType);
                    }
                }
            }

            // Set car availability to "never" for agents below 18 years old
            // Standardize the attribute "age"
            String avail = "always";
            Object age = person.getAttributes().getAttribute("microm:modeled:age");
            if (age != null) {
                PersonUtils.setAge(person, (int) age);
                person.getAttributes().removeAttribute("microm:modeled:age");
                if ((int) age < 18) {
                    avail = "never";
                }
            }
            PersonUtils.setCarAvail(person, avail);

            // Standardize the attribute "sex"
            Object sex = person.getAttributes().getAttribute("microm:modeled:sex");
            if (sex != null) {
                PersonUtils.setSex(person, (String) sex);
                person.getAttributes().removeAttribute("microm:modeled:sex");
            }

            // Determine if an agent lives inside the study area
            if (studyArea != null) {
                if (PopulationAnalysis.checkIfPersonLivesInArea(person, studyArea)) {
                    person.getAttributes().putAttribute(PopulationAnalysis.HOME_LOCATION, PopulationAnalysis.HomeLocationCategory.inside);
                } else {
                    person.getAttributes().putAttribute(PopulationAnalysis.HOME_LOCATION, PopulationAnalysis.HomeLocationCategory.outside);
                }
            }

            // Assign income to person (skip the freight agents)
            if (person.getId().toString().startsWith("freight")) {
                continue;
            }

            String incomeGroupString = (String) person.getAttributes().getAttribute("MiD:hheink_gr2");
            String householdSizeString = (String) person.getAttributes().getAttribute("MiD:hhgr_gr");
            int incomeGroup = 0;
            double householdSize = 1;
            if (incomeGroupString != null && householdSizeString != null) {
                incomeGroup = Integer.parseInt(incomeGroupString);
                householdSize = Double.parseDouble(householdSizeString);
            }

            double income = 0;
            switch (incomeGroup) {
                case 1:
                    income = 500 / householdSize;
                    break;
                case 2:
                    income = (rnd.nextInt(400) + 500) / householdSize;
                    break;
                case 3:
                    income = (rnd.nextInt(600) + 900) / householdSize;
                    break;
                case 4:
                    income = (rnd.nextInt(500) + 1500) / householdSize;
                    break;
                case 5:
                    income = (rnd.nextInt(1000) + 2000) / householdSize;
                    break;
                case 6:
                    income = (rnd.nextInt(1000) + 3000) / householdSize;
                    break;
                case 7:
                    income = (rnd.nextInt(1000) + 4000) / householdSize;
                    break;
                case 8:
                    income = (rnd.nextInt(1000) + 5000) / householdSize;
                    break;
                case 9:
                    income = (rnd.nextInt(1000) + 6000) / householdSize;
                    break;
                case 10:
                    income = (Math.abs(rnd.nextGaussian()) * 1000 + 7000) / householdSize;
                    break;
                default:
                    income = 2364; // Average monthly household income per Capita (2021). See comments below for details
                    break;
                // Average Gross household income: 4734 Euro
                // Average household size: 83.1M persons /41.5M households = 2.0 persons / household
                // Average household income per capita: 4734/2.0 = 2364 Euro
                // Source (Access date: 21 Sep. 2021):
                // https://www.destatis.de/EN/Themes/Society-Environment/Income-Consumption-Living-Conditions/Income-Receipts-Expenditure/_node.html
                // https://www.destatis.de/EN/Themes/Society-Environment/Population/Households-Families/_node.html
                // https://www.destatis.de/EN/Themes/Society-Environment/Population/Current-Population/_node.html;jsessionid=E0D7A060D654B31C3045AAB1E884CA75.live711
            }
            PersonUtils.setIncome(person, income);
        }
        PopulationUtils.writePopulation(population, output.toString());

        return 0;
    }
}
