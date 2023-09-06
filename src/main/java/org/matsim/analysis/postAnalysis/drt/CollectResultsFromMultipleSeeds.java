package org.matsim.analysis.postAnalysis.drt;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.matsim.application.MATSimAppCommand;
import picocli.CommandLine;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CommandLine.Command(
		name = "collect-results",
		description = "Collect KPI from multiple seeds"
)
public class CollectResultsFromMultipleSeeds implements MATSimAppCommand {
	@CommandLine.Option(names = "--directory", description = "path to root output directory", required = true)
	private Path directory;

	@CommandLine.Option(names = "--run-id", description = "MATSim runId", required = true)
	private Path runId;

	@CommandLine.Option(names = "--seeds", description = "path to root output directory, separated by comma", defaultValue = "1111,1234,2222,4711,5678")
	private String seedsInput;

	@CommandLine.Option(names = "--common-part", description = "common part of the folders", defaultValue = "output-ASC-0.15-dist-0.00006-3_av-seed")
	private String commonParts;

	@CommandLine.Option(names = "--case-study", description = "case studies, separated by comma", defaultValue = "CORE,CORE_WITH_SHOP,HOHENPFAHL,BAUERNSIEDLUNG")
	private String caseStudiesInput;

	@Override
	public Integer call() throws Exception {
		String[] seeds = seedsInput.split(",");
		String[] caseStudies = caseStudiesInput.split(",");

		CSVPrinter tsvWriter = new CSVPrinter(new FileWriter(directory.toString() + "/results-summary.tsv"), CSVFormat.TDF);
		List<String> titleRow = Arrays.asList(
				"case_study",
						"num_kexi_trips", "kexi_mean_waiting_time", "kexi_euclidean_distance", "kexi_in_vehicle_time",
						"num_av_trips", "av_mean_waiting_time", "av_euclidean_distance", "av_in_vehicle_time");
		tsvWriter.printRecord(titleRow);

		CSVPrinter avVehicleKpiWriter = new CSVPrinter(new FileWriter(directory.toString() + "/av-vehicle_KPI.tsv"), CSVFormat.TDF);
		CSVPrinter kexiVehicleKPIWriter = new CSVPrinter(new FileWriter(directory.toString() + "/kexi-vehicle_KPI.tsv"), CSVFormat.TDF);
		List<String> vehicleKPITitleRow = Arrays.asList(
				"case_study", "num_vehicle", "total_distance", "total_passenger_distance", "total_empty_distance",
						"average_vehicle_distance", "d_p/d_t", "empty_ratio");
		avVehicleKpiWriter.printRecord(vehicleKPITitleRow);
		kexiVehicleKPIWriter.printRecord(vehicleKPITitleRow);

		processCaseStudies(seeds, caseStudies, tsvWriter, avVehicleKpiWriter, kexiVehicleKPIWriter);

		tsvWriter.close();
		kexiVehicleKPIWriter.close();
		avVehicleKpiWriter.close();

		return 0;
	}

	private void processCaseStudies(String[] seeds, String[] caseStudies, CSVPrinter tsvWriter, CSVPrinter avVehicleKpiWriter, CSVPrinter kexiVehicleKPIWriter) throws IOException {
		for (String caseStudy : caseStudies) {
			double totalNumKexiTrips = 0;
			double kexiSumWaitingTime = 0;
			double kexiSumEuclideanDistance = 0;
			double kexiSumInVehicleTime = 0;
			double kexiSumTotalDistance = 0;
			double kexiSumTotalPassengerDistance = 0;
			double kexiSumTotalEmptyDistance = 0;
			double kexiSumAverageVehicleDistance = 0;
			int kexiFleetSize = 0;

			double totalAvTrips = 0;
			double avSumWaitingTime = 0;
			double avSumEuclideanDistance = 0;
			double avSumInVehicleTime = 0;
			double avSumTotalDistance = 0;
			double avSumTotalPassengerDistance = 0;
			double avSumTotalEmptyDistance = 0;
			double avSumAverageVehicleDistance = 0;
			int avFleetSize = 0;

			for (String seed : seeds) {
				String folder = directory.toString() + "/" + commonParts + seed + "-" + caseStudy;
				// Reading av data
				try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(folder + "/"+ runId + ".drt_customer_stats_av.csv")),
						CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
					CSVRecord lastRow = null;
					for (CSVRecord row : parser.getRecords()) {
						lastRow = row;
					}
					assert lastRow != null;
					double avRides = Double.parseDouble(lastRow.get(2));
					double avWaitingTime = Double.parseDouble(lastRow.get(3));
					double avInVehicleTime = Double.parseDouble(lastRow.get(10));
					totalAvTrips += avRides;
					avSumInVehicleTime += avInVehicleTime * avRides;
					avSumWaitingTime += avWaitingTime * avRides;
				}

				avSumEuclideanDistance = getSumEuclideanDistance(folder, "/analysis-drt-service-quality/av_KPI.tsv", avSumEuclideanDistance);

				try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(folder + "/"+ runId + ".drt_vehicle_stats_av.csv")),
						CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
					CSVRecord lastRow = null;
					for (CSVRecord row : parser.getRecords()) {
						lastRow = row;
					}
					assert lastRow != null;

					avSumTotalDistance += Double.parseDouble(lastRow.get(3));
					avSumTotalPassengerDistance += Double.parseDouble(lastRow.get(6));
					avSumTotalEmptyDistance += Double.parseDouble(lastRow.get(4));
					avSumAverageVehicleDistance +=Double.parseDouble(lastRow.get(7));
					// It should always be the same for each seed, so we just use the number from the last seed
					avFleetSize = Integer.parseInt(lastRow.get(2));
				}


				// Reading kexi data
				try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(folder + "/"+ runId + ".drt_customer_stats_drt.csv")),
						CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
					CSVRecord lastRow = null;
					for (CSVRecord row : parser.getRecords()) {
						lastRow = row;
					}
					assert lastRow != null;
					double kexiRides = Double.parseDouble(lastRow.get(2));
					double kexiWaitingTime = Double.parseDouble(lastRow.get(3));
					double kexiInVehicleTime = Double.parseDouble(lastRow.get(10));
					totalNumKexiTrips += kexiRides;
					kexiSumWaitingTime += kexiWaitingTime * kexiRides;
					kexiSumInVehicleTime += kexiInVehicleTime * kexiRides;
				}

				kexiSumEuclideanDistance = getSumEuclideanDistance(folder, "/analysis-drt-service-quality/drt_KPI.tsv", kexiSumEuclideanDistance);

				try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(folder + "/"+ runId + ".drt_vehicle_stats_drt.csv")),
						CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
					CSVRecord lastRow = null;
					for (CSVRecord row : parser.getRecords()) {
						lastRow = row;
					}
					assert lastRow != null;

					kexiSumTotalDistance += Double.parseDouble(lastRow.get(3));
					kexiSumTotalPassengerDistance += Double.parseDouble(lastRow.get(6));
					kexiSumTotalEmptyDistance += Double.parseDouble(lastRow.get(4));
					kexiSumAverageVehicleDistance += Double.parseDouble(lastRow.get(7));
					// It should always be the same for each seed, so we just use the number from the last seed
					kexiFleetSize = Integer.parseInt(lastRow.get(2));
				}
			}

			double averageKexiRides = totalNumKexiTrips / seeds.length;
			if (averageKexiRides == 0) {
				// avoid division by zero
				averageKexiRides = -1;
			}
			double averageKexiWaitingTime = kexiSumWaitingTime / totalNumKexiTrips;
			double averageKexiEuclideanDistance = kexiSumEuclideanDistance / totalNumKexiTrips;
			double averageKexiInVehicleTime = kexiSumInVehicleTime / totalNumKexiTrips;

			double averageAvRides = totalAvTrips / seeds.length;
			if (averageAvRides == 0) {
				averageAvRides = -1;
			}
			double averageAvWaitingTime = avSumWaitingTime / totalAvTrips;
			double averageAvEuclideanDistance = avSumEuclideanDistance / totalAvTrips;
			double averageAvInVehicleTime = avSumInVehicleTime / totalAvTrips;

			printOutput(tsvWriter, caseStudy, averageKexiRides, averageKexiWaitingTime, averageKexiEuclideanDistance, averageKexiInVehicleTime, averageAvRides, averageAvWaitingTime, averageAvEuclideanDistance, averageAvInVehicleTime);

			// kexi vehicle kpi
			double kexiMeanTotalDistance = kexiSumTotalDistance / seeds.length;
			double kexiMeanTotalPassengerDistance = kexiSumTotalPassengerDistance / seeds.length;
			double kexiMeanEmptyDistance = kexiSumTotalEmptyDistance / seeds.length;
			double kexiMeanAverageDistance = kexiSumAverageVehicleDistance / seeds.length;
			double kexiMeanDpOverDt = kexiSumTotalPassengerDistance / kexiSumTotalDistance;
			double kexiMeanEmptyRatio = kexiSumTotalEmptyDistance / kexiSumTotalDistance;

			printOperatorStats(kexiVehicleKPIWriter, caseStudy, kexiFleetSize, kexiMeanTotalDistance, kexiMeanTotalPassengerDistance, kexiMeanEmptyDistance, kexiMeanAverageDistance, kexiMeanDpOverDt, kexiMeanEmptyRatio);

			// av vehicle kpi
			double avMeanTotalDistance = avSumTotalDistance / seeds.length;
			double avMeanTotalPassengerDistance = avSumTotalPassengerDistance / seeds.length;
			double avMeanEmptyDistance = avSumTotalEmptyDistance / seeds.length;
			double avMeanAverageDistance = avSumAverageVehicleDistance / seeds.length;
			double avMeanDpOverDt = avSumTotalPassengerDistance / avSumTotalDistance;
			double avMeanEmptyRatio = avSumTotalEmptyDistance / avSumTotalDistance;

			printOperatorStats(avVehicleKpiWriter, caseStudy, avFleetSize, avMeanTotalDistance, avMeanTotalPassengerDistance, avMeanEmptyDistance, avMeanAverageDistance, avMeanDpOverDt, avMeanEmptyRatio);
		}
	}

	private static void printOutput(CSVPrinter tsvWriter, String caseStudy, double averageKexiRides, double averageKexiWaitingTime, double averageKexiEuclideanDistance, double averageKexiInVehicleTime, double averageAvRides, double averageAvWaitingTime, double averageAvEuclideanDistance, double averageAvInVehicleTime) throws IOException {
		List<String> outputRow = new ArrayList<>();
		outputRow.add(caseStudy);
		outputRow.add(Double.toString(averageKexiRides));
		outputRow.add(Double.toString(averageKexiWaitingTime));
		outputRow.add(Double.toString(averageKexiEuclideanDistance));
		outputRow.add(Double.toString(averageKexiInVehicleTime));

		outputRow.add(Double.toString(averageAvRides));
		outputRow.add(Double.toString(averageAvWaitingTime));
		outputRow.add(Double.toString(averageAvEuclideanDistance));
		outputRow.add(Double.toString(averageAvInVehicleTime));

		tsvWriter.printRecord(outputRow);
	}

	private static void printOperatorStats(CSVPrinter vehicleKPIWriter, String caseStudy, int fleetSize, double meanTotalDistance, double meanTotalPassengerDistance, double meanEmptyDistance, double meanAverageDistance, double meanDpOverDt, double meanEmptyRatio) throws IOException {
		List<String> kexiVehicleKpiRow = Arrays.asList(
			caseStudy, Integer.toString(fleetSize), Double.toString(meanTotalDistance),
				Double.toString(meanTotalPassengerDistance), Double.toString(meanEmptyDistance),
				Double.toString(meanAverageDistance), Double.toString(meanDpOverDt),
				Double.toString(meanEmptyRatio)
		);
		vehicleKPIWriter.printRecord(kexiVehicleKpiRow);
	}

	private static double getSumEuclideanDistance(String folder, String x, double avSumEuclideanDistance) throws IOException {
		try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(folder + x)),
			CSVFormat.TDF.withFirstRecordAsHeader())) {
			CSVRecord lastRow = null;
			for (CSVRecord row : parser.getRecords()) {
				lastRow = row;
			}
			assert lastRow != null;
			double avRides = Double.parseDouble(lastRow.get(0));
			double avEuclideanDistance = Double.parseDouble(lastRow.get(6));
			avSumEuclideanDistance += avEuclideanDistance * avRides;
		}
		return avSumEuclideanDistance;
	}

	public static void main(String[] args) {
		new CollectResultsFromMultipleSeeds().execute(args);
	}
}
