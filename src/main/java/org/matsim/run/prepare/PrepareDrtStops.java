package org.matsim.run.prepare;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import picocli.CommandLine;

import java.io.IOException;

@CommandLine.Command(
        name = "prepare-drt-stops",
        description = "Write drt stops"
)
public class PrepareDrtStops implements MATSimAppCommand {
    @CommandLine.Mixin
    private ShpOptions shp = new ShpOptions();

    @CommandLine.Option(names = "--network", description = "network file", required = true)
    private String network;

    @CommandLine.Option(names = "--mode", description = "mode of the drt", required = true)
    private String mode;
    // mode = "drt", "av" or other specific drt operator mode

    @CommandLine.Option(names = "--output-folder", description = "path to output folder", required = true)
    private String outputFolder;

    public static void main(String[] args) throws IOException {
        new PrepareDrtStops().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(network);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();

        DrtStopsWriter drtStopsWriter = new DrtStopsWriter(network, mode, shp, outputFolder);
        drtStopsWriter.write();
        return 0;
    }
}
