package org.matsim.run.prepare;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GenerateCounterfactualImmobilePlansTest {
	@RegisterExtension
	final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void assignsConfiguredAdultAgeBandsAtTheirBoundaries() {
		assertThat(GenerateCounterfactualImmobilePlans.ageBand(18)).isEqualTo("18-29");
		assertThat(GenerateCounterfactualImmobilePlans.ageBand(29)).isEqualTo("18-29");
		assertThat(GenerateCounterfactualImmobilePlans.ageBand(30)).isEqualTo("30-49");
		assertThat(GenerateCounterfactualImmobilePlans.ageBand(49)).isEqualTo("30-49");
		assertThat(GenerateCounterfactualImmobilePlans.ageBand(50)).isEqualTo("50-69");
		assertThat(GenerateCounterfactualImmobilePlans.ageBand(69)).isEqualTo("50-69");
		assertThat(GenerateCounterfactualImmobilePlans.ageBand(70)).isEqualTo("70+");
	}

	@Test
	void progressivelyRelaxesIncomeThenSexThenAgeBand() {
		Person donor = PopulationUtils.getFactory().createPerson(Id.createPersonId("donor"));
		PersonUtils.setAge(donor, 35);
		donor.getAttributes().putAttribute("sex", "f");
		donor.getAttributes().putAttribute("MiD:hheink_gr2", "4");

		assertThat(GenerateCounterfactualImmobilePlans.matches(donor, "30-49", "f", "4", 0)).isTrue();
		assertThat(GenerateCounterfactualImmobilePlans.matches(donor, "30-49", "f", "5", 0)).isFalse();
		assertThat(GenerateCounterfactualImmobilePlans.matches(donor, "30-49", "f", "5", 1)).isTrue();
		assertThat(GenerateCounterfactualImmobilePlans.matches(donor, "30-49", "m", "5", 1)).isFalse();
		assertThat(GenerateCounterfactualImmobilePlans.matches(donor, "30-49", "m", "5", 2)).isTrue();
		assertThat(GenerateCounterfactualImmobilePlans.matches(donor, "50-69", "m", "5", 2)).isFalse();
		assertThat(GenerateCounterfactualImmobilePlans.matches(donor, "50-69", "m", "5", 3)).isTrue();
	}

	@Test
	void replacesHomeOnlyAdultPlanWithSyntheticWalkPlan() {
		Path output = Path.of(utils.getOutputDirectory());
		Path populationFile = output.resolve("input-population.xml.gz");
		Path networkFile = output.resolve("network.xml.gz");
		Path outputPopulation = output.resolve("counterfactual-population.xml.gz");
		Network network = createNetwork();
		NetworkUtils.writeNetwork(network, networkFile.toString());

		Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		Person target = population.getFactory().createPerson(Id.createPersonId("target"));
		PersonUtils.setAge(target, 35);
		target.getAttributes().putAttribute("sex", "f");
		target.getAttributes().putAttribute("MiD:hheink_gr2", "4");
		target.addPlan(homeOnlyPlan(population, "home", new Coord(710000, 5420000)));
		population.addPerson(target);

		Person donor = population.getFactory().createPerson(Id.createPersonId("donor"));
		PersonUtils.setAge(donor, 35);
		donor.getAttributes().putAttribute("sex", "f");
		donor.getAttributes().putAttribute("MiD:hheink_gr2", "4");
		donor.addPlan(mobilePlan(population));
		population.addPerson(donor);
		PopulationUtils.writePopulation(population, populationFile.toString());

		new GenerateCounterfactualImmobilePlans().execute(new String[]{
			"--input-population", populationFile.toString(),
			"--output-population", outputPopulation.toString(),
			"--network", networkFile.toString(),
			"--study-area-shp", "input/shp/lk-kelheim/lk-kelheim.shp",
			"--seed", "123"
		});

		Person generatedTarget = PopulationUtils.readPopulation(outputPopulation.toString()).getPersons().get(Id.createPersonId("target"));
		assertThat(generatedTarget.getPlans()).hasSize(1);
		assertThat(PersonUtils.getCarAvail(generatedTarget)).isEqualTo("never");
		assertThat(generatedTarget.getAttributes().getAttribute(GenerateCounterfactualImmobilePlans.DONOR_PERSON_ID)).isEqualTo("donor");
		assertThat(generatedTarget.getSelectedPlan().getPlanElements()).filteredOn(Leg.class::isInstance)
			.allSatisfy(element -> assertThat(((Leg) element).getMode()).isEqualTo(TransportMode.walk));
		assertThat(generatedTarget.getSelectedPlan().getPlanElements()).filteredOn(Activity.class::isInstance)
			.extracting(element -> ((Activity) element).getType())
			.containsExactly("home", "work", "home");
	}

	private static Network createNetwork() {
		Network network = NetworkUtils.createNetwork();
		Node first = network.getFactory().createNode(Id.createNodeId("first"), new Coord(709900, 5419900));
		Node second = network.getFactory().createNode(Id.createNodeId("second"), new Coord(710000, 5420000));
		Node third = network.getFactory().createNode(Id.createNodeId("third"), new Coord(710500, 5420200));
		network.addNode(first);
		network.addNode(second);
		network.addNode(third);
		addLink(network, "home", first, second);
		addLink(network, "work", second, third);
		return network;
	}

	private static void addLink(Network network, String id, Node from, Node to) {
		Link link = network.getFactory().createLink(Id.createLinkId(id), from, to);
		link.setLength(100);
		link.setFreespeed(10);
		link.setCapacity(1000);
		link.setAllowedModes(Set.of(TransportMode.car, TransportMode.walk));
		network.addLink(link);
	}

	private static Plan homeOnlyPlan(Population population, String link, Coord home) {
		Plan plan = population.getFactory().createPlan();
		Activity activity = population.getFactory().createActivityFromCoord("home", home);
		activity.setLinkId(Id.createLinkId(link));
		plan.addActivity(activity);
		return plan;
	}

	private static Plan mobilePlan(Population population) {
		Plan plan = population.getFactory().createPlan();
		Activity home = population.getFactory().createActivityFromCoord("home", new Coord(710000, 5420000));
		home.setLinkId(Id.createLinkId("home"));
		home.setEndTime(8 * 3600);
		plan.addActivity(home);
		plan.addLeg(population.getFactory().createLeg(TransportMode.car));
		Activity work = population.getFactory().createActivityFromCoord("work", new Coord(710500, 5420200));
		work.setLinkId(Id.createLinkId("work"));
		work.setEndTime(17 * 3600);
		plan.addActivity(work);
		plan.addLeg(population.getFactory().createLeg(TransportMode.car));
		Activity returnHome = population.getFactory().createActivityFromCoord("home", new Coord(710000, 5420000));
		returnHome.setLinkId(Id.createLinkId("home"));
		plan.addActivity(returnHome);
		return plan;
	}
}
