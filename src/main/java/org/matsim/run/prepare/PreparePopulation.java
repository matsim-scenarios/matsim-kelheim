package org.matsim.run.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
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

    @CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to input population")
    private Path input;

    @CommandLine.Option(names = "--output", description = "Path to output population", required = true)
    private Path output;

    @Override
    public Integer call() throws Exception {

        if (!Files.exists(input)) {
            log.error("Input population does not exist: {}", input);
            return 2;
        }

        Population population = PopulationUtils.readPopulation(input.toString());

        for (Person person : population.getPersons().values()) {
            // Set car availability to "never" for agents below 18 years old
            Object age = person.getAttributes().getAttribute("microm:modeled:age");
            String avail = "always";
            if (age != null && (int) age < 18)
                avail = "never";
            PersonUtils.setCarAvail(person, avail);

            // Assign income to person
            if (person.getId().toString().startsWith("freight")){
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
            Random rnd = new Random(1234);
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
            person.getAttributes().putAttribute("income", income);
        }
        PopulationUtils.writePopulation(population, output.toString());

        return 0;
    }
}
