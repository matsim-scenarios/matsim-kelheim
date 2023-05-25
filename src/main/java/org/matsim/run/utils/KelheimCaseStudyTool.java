package org.matsim.run.utils;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.config.Config;

/**
 * Helper class to setup config file for case studies.
 */
public final class KelheimCaseStudyTool {

	private KelheimCaseStudyTool() {
	}

	/**
	 * Different possible services areas for the AV.
	 */
	public enum AvServiceArea {NULL, CORE, CORE_WITH_SHOP, HOHENPFAHL, BAUERNSIEDLUNG}
	// NULL: do not change anything; CORE: Donaupark + Altstadt; HOHENPFAHL: CORE + Hohenpfahl area; BAUERNSIEDLUNG: CORE + Bauernsiedlung area

	public static void setConfigFile(Config config, DrtConfigGroup drtConfig, AvServiceArea avServiceAreas) {
		// Set drt related things (vehicle file, stops file)
		if (avServiceAreas == AvServiceArea.CORE) {
			drtConfig.transitStopFile = "av-stops-DP-AS.xml";
		}

		if (avServiceAreas == AvServiceArea.CORE_WITH_SHOP) {
			drtConfig.transitStopFile = "av-stops-DP-AS-shops.xml";
		}

		if (avServiceAreas == AvServiceArea.HOHENPFAHL) {
			drtConfig.transitStopFile = "av-stops-Hohenpfahl-DP-AS.xml";
		}

		if (avServiceAreas == AvServiceArea.BAUERNSIEDLUNG) {
			drtConfig.transitStopFile = "av-stops-Bauernsiedlung-DP-AS.xml";
		}

		// Update output directory
		if (avServiceAreas != AvServiceArea.NULL) {
			String outputPath = config.controler().getOutputDirectory() + "-" + avServiceAreas.toString();
			config.controler().setOutputDirectory(outputPath);
		}
	}
}
