package org.matsim.run.utils;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.config.Config;

public class KelheimCaseStudyTool {
    public enum AV_SERVICE_AREAS {BASE, HOHENPFAHL, BAUERNSIEDLUNG} // BASE: do not change anything

    public static void setConfigFile(Config config, DrtConfigGroup drtConfig, AV_SERVICE_AREAS avServiceAreas) {
        // Set drt related things (vehicle file, stops file)
        if (avServiceAreas == AV_SERVICE_AREAS.HOHENPFAHL) {
            drtConfig.setVehiclesFile("av-vehicles-Hohenpfahl-DP-AS.xml");
            drtConfig.setTransitStopFile("av-stops-Hohenpfahl-DP-AS.xml");
        }

        if (avServiceAreas == AV_SERVICE_AREAS.BAUERNSIEDLUNG) {
            drtConfig.setVehiclesFile("av-vehicles-Bauernsiedlung-DP-AS.xml");
            drtConfig.setTransitStopFile("av-stops-Bauernsiedlung-DP-AS.xml");
        }

        // Update output directory
        if (avServiceAreas != AV_SERVICE_AREAS.BASE){
            String outputPath = config.controler().getOutputDirectory() + "-" + avServiceAreas.toString();
            config.controler().setOutputDirectory(outputPath);
        }
    }
}
