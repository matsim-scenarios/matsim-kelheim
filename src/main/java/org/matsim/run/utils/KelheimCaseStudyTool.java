package org.matsim.run.utils;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.config.Config;

public class KelheimCaseStudyTool {
    public enum AV_SERVICE_AREAS {NULL, CORE, HOHENPFAHL, BAUERNSIEDLUNG}
    // NULL: do not change anything; CORE: Donaupark + Altstadt; HOHENPFAHL: CORE + Hohenpfahl area; BAUERNSIEDLUNG: CORE + Bauernsiedlung area

    public static void setConfigFile(Config config, DrtConfigGroup drtConfig, AV_SERVICE_AREAS avServiceAreas) {
        // Set drt related things (vehicle file, stops file)
        if (avServiceAreas == AV_SERVICE_AREAS.CORE) {
            drtConfig.setTransitStopFile("av-stops-DP-AS.xml");
        }

        if (avServiceAreas == AV_SERVICE_AREAS.HOHENPFAHL) {
            drtConfig.setTransitStopFile("av-stops-Hohenpfahl-DP-AS.xml");
        }

        if (avServiceAreas == AV_SERVICE_AREAS.BAUERNSIEDLUNG) {
            drtConfig.setTransitStopFile("av-stops-Bauernsiedlung-DP-AS.xml");
        }

        // Update output directory
        if (avServiceAreas != AV_SERVICE_AREAS.NULL){
            String outputPath = config.controler().getOutputDirectory() + "-" + avServiceAreas.toString();
            config.controler().setOutputDirectory(outputPath);
        }
    }
}
