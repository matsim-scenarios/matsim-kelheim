package org.matsim.run.prepare;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;

public class PrepareDrtStops {
    public static void main(String[] args) throws IOException {
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile("/Users/luchengqi/Documents/MATSimScenarios/Kelheim/kelheim-v1.0-network.xml.gz");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();

        DrtStopsWriter drtStopsWriter = new DrtStopsWriter();
        drtStopsWriter.write("/Users/luchengqi/Documents/MATSimScenarios/Kelheim/drtStops.xml", network);
    }
}
