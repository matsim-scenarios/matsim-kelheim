package org.matsim.run;

import org.matsim.application.MATSimAppCommand;
import org.matsim.application.automatedCalibration.AutomaticScenarioCalibrator;
import picocli.CommandLine;

import java.io.IOException;

@CommandLine.Command(
        name = "auto-tuning",
        description = "Auto tune the Kelheim scenario"
)
public class RunKelheimAutoTuning implements MATSimAppCommand {
    @CommandLine.Option(names = "--config", description = "config file", required = true)
    private String configFile;

    @CommandLine.Option(names = "--reference", description = "Path to reference data", required = true)
    private String referenceDataFile;

    @CommandLine.Option(names = "--target", description = "target error", defaultValue = "0.005")
    private double targetError;

    @CommandLine.Option(names = "--time", description = "target error", defaultValue = "604800")
    private long maxRunningTime;

    @CommandLine.Option(names = "--patience", description = "patience for the tuning", defaultValue = "5")
    private int patience;

    @CommandLine.Option(names = "--persons", description = "Path to the list of persons to consider")
    private String relevantPersonsFile;

    private static class KelheimAutoTuning extends AutomaticScenarioCalibrator {
        public KelheimAutoTuning(String configFile, String referenceDataFile, double targetError,
                                 long maxRunningTime, int patience, String relevantPersonsFile) throws IOException {
            super(configFile, referenceDataFile, targetError, maxRunningTime, patience, relevantPersonsFile);
        }

        @Override
        public void runSimulation() {


        }
    }

    @Override
    public Integer call() throws Exception {
        new KelheimAutoTuning(configFile, referenceDataFile, targetError, maxRunningTime,
                patience, relevantPersonsFile).calibrate();
        return 0;
    }

    public static void main(String[] args) throws IOException {
        new RunKelheimAutoTuning().execute(args);
    }


}

