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

@CommandLine.Command(
		name = "population",
		description = "Set the car availability attribute in the population"
)
public class PreparePopulation implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(PreparePopulation.class);

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to input population")
	private Path input;

	@CommandLine.Option(names= "--output", description = "Path to output population", required = true)
	private Path output;

	@Override
	public Integer call() throws Exception {

		if (!Files.exists(input)) {
			log.error("Input population does not exist: {}",  input);
			return 2;
		}

		Population population = PopulationUtils.readPopulation(input.toString());

		for (Person person : population.getPersons().values()) {

			Object age = person.getAttributes().getAttribute("microm:modeled:age");

			String avail = "always";
			if ((int) age < 18)
				avail = "never";

			PersonUtils.setCarAvail(person, avail);
		}


		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}
}
