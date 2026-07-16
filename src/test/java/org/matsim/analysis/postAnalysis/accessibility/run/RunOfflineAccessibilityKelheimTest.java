package org.matsim.analysis.postAnalysis.accessibility.run;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.matsim.contrib.accessibility.Modes4Accessibility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RunOfflineAccessibilityKelheimTest {

	@Test
	void parsesCommaSeparatedOptions() {
		assertThat(RunOfflineAccessibilityKelheim.parsePois("supermarket, logistic")).containsExactly("supermarket", "logistic");
		assertThat(RunOfflineAccessibilityKelheim.parseModes("car,pt"))
			.containsExactly(Modes4Accessibility.car, Modes4Accessibility.pt);
		assertThat(RunOfflineAccessibilityKelheim.parseTimes("21600, 25200")).containsExactly(21600d, 25200d);
	}

	@Test
	void rejectsInvalidModeAndTimeValues() {
		assertThatThrownBy(() -> RunOfflineAccessibilityKelheim.parseModes("not-a-mode"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> RunOfflineAccessibilityKelheim.parseTimes("morning"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void requiresExactlyOneProbe(@TempDir Path directory) throws IOException {
		assertThatThrownBy(() -> RunOfflineAccessibilityKelheim.findServiceQualityProbe(directory))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("found 0");

		Path probe = Files.createFile(directory.resolve("run.drt_service_quality_probes.csv.gz"));
		assertThat(RunOfflineAccessibilityKelheim.findServiceQualityProbe(directory)).isEqualTo(probe);

		Files.createFile(directory.resolve("another.drt_service_quality_probes.csv"));
		assertThatThrownBy(() -> RunOfflineAccessibilityKelheim.findServiceQualityProbe(directory))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("found 2");
	}
}
