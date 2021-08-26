package org.matsim.run.prepare;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;

public class PrepareDrtStops {
    private static final String mode = "av"; // drt, av or other modes...
    private static final String shapefilePath = "/Users/luchengqi/Documents/MATSimScenarios/Kelheim/shape-file/AvOperatingArea-all.shp";


    public static void main(String[] args) throws IOException {
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile("/Users/luchengqi/Documents/MATSimScenarios/Kelheim/kelheim-v1.0-network-with-pt.xml.gz");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();

        DrtStopsWriter drtStopsWriter = new DrtStopsWriter(mode, shapefilePath);
        drtStopsWriter.write("/Users/luchengqi/Documents/MATSimScenarios/Kelheim/kelheim-v1.0-" + mode + "-stops.xml", network);
    }
}
