package org.matsim.run;

import org.junit.jupiter.api.Test;
import org.matsim.contrib.drt.prebooking.logic.ProbabilityBasedPrebookingLogicParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunKelheimScenarioPrebookingTest {

	@Test
	void addsProbabilityBasedPrebookingWithConfiguredValues() {
		DrtConfigGroup drtConfigGroup = new DrtConfigGroup();

		RunKelheimScenario.addProbabilityBasedPrebooking(drtConfigGroup, 1.0, 1800);

		ProbabilityBasedPrebookingLogicParams logicParams = drtConfigGroup.getPrebookingParams()
			.orElseThrow()
			.getProbabilityBasedLogicParams()
			.orElseThrow();
		assertEquals(1.0, logicParams.getProbability());
		assertEquals(1800, logicParams.getSubmissionSlack());
	}

	@Test
	void replacesExistingPrebookingParameters() {
		DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
		RunKelheimScenario.addProbabilityBasedPrebooking(drtConfigGroup, 0.5, 900);

		RunKelheimScenario.addProbabilityBasedPrebooking(drtConfigGroup, 1.0, 1800);

		assertTrue(drtConfigGroup.getPrebookingParams().isPresent());
		assertEquals(1.0, drtConfigGroup.getPrebookingParams().orElseThrow()
			.getProbabilityBasedLogicParams().orElseThrow().getProbability());
	}
}
