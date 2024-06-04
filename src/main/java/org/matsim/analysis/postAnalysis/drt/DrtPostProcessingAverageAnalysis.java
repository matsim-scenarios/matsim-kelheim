package org.matsim.analysis.postAnalysis.drt;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.*;

import static org.matsim.application.ApplicationUtils.globFile;

@CommandLine.Command(name = "average-drt", description = "Calculates average drt stats based on several sim runs with different random seeds.")
@CommandSpec(
	requires = {"runs", "mode"},
	produces = {"rides_per_veh_avg_demand_stats.csv", "avg_wait_time_avg_demand_stats.csv", "requests_avg_demand_stats.csv", "avg_total_travel_time_avg_demand_stats.csv",
		"rides_avg_demand_stats.csv", "avg_direct_distance_[km]_avg_demand_stats.csv", "rejections_avg_demand_stats.csv", "95th_percentile_wait_time_avg_demand_stats.csv",
		"avg_in-vehicle_time_avg_demand_stats.csv", "avg_ride_distance_[km]_avg_demand_stats.csv", "rejection_rate_avg_demand_stats.csv",
		"avg_fare_[MoneyUnit]_avg_demand_stats.csv", "total_service_hours_avg_supply_stats.csv", "pooling_ratio_avg_supply_stats.csv", "detour_ratio_avg_supply_stats.csv",
		"total_vehicle_mileage_[km]_avg_supply_stats.csv", "empty_ratio_avg_supply_stats.csv", "number_of_stops_avg_supply_stats.csv", "total_pax_distance_[km]_avg_supply_stats.csv", "vehicles_avg_supply_stats.csv"}
)
public class DrtPostProcessingAverageAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(DrtPostProcessingAverageAnalysis.class);
	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(DrtPostProcessingAverageAnalysis.class);
	@CommandLine.Option(names = "--no-runs", defaultValue = "5", description = "Number of simulation runs to be averaged.")
	private Integer noRuns;

	private final Map<String, List<Double>> demandStats = new HashMap<>();
	private final Map<String, List<Double>> supplyStats = new HashMap<>();
	private final Map<String, Double[]> demandAvgs = new HashMap<>();
	private final Map<String, Double[]> supplyAvgs = new HashMap<>();
	Map<String, List<String>> params = new HashMap<>();

	private final CsvOptions csv = new CsvOptions();

	String supplyInfo = "info[titleCase]";
	String value = "value";

	public static void main(String[] args) {
		new DrtPostProcessingAverageAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		String runs = input.getPath("runs");

		List<String> foldersSeeded = Arrays.stream(runs.split(",")).toList();

//		add stats from every run to map
		for (String folder : foldersSeeded) {

			String demandKpiCsv = globFile(Path.of(folder + "/analysis/" + input.getPath("mode")), "*demand_kpi.csv*").toString();
			String supplyKpiCsv = globFile(Path.of(folder + "/analysis/" + input.getPath("mode")), "*supply_kpi.csv*").toString();

			Table demand = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(demandKpiCsv))
				.sample(false)
				.separator(csv.detectDelimiter(demandKpiCsv)).build());

			Table supply = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(supplyKpiCsv))
				.sample(false)
				.separator(csv.detectDelimiter(supplyKpiCsv)).build());

//			get all demand stats
			for (int i = 0; i < demand.rowCount(); i++) {
				Row row = demand.row(i);

				if (!demandStats.containsKey(row.getString("Info"))) {
					demandStats.put(row.getString("Info"), new ArrayList<>());
				}

//				some values are in format hh:mm:ss or empty
				if (row.getString(value).isEmpty()) {
					demandStats.get(row.getString("Info")).add(0.);
				} else if (row.getString(value).contains(":")) {
					demandStats.get(row.getString("Info")).add((double) LocalTime.parse(row.getString(value)).toSecondOfDay());
				} else {
					demandStats.get(row.getString("Info")).add(Double.parseDouble(row.getString(value)));
				}
			}

//			get all supply stats
			for (int i = 0; i < supply.rowCount(); i++) {
				Row row = supply.row(i);

				if (!supplyStats.containsKey(row.getString(supplyInfo))) {
					supplyStats.put(row.getString(supplyInfo), new ArrayList<>());
				}

				if (row.getColumnType(value) == ColumnType.INTEGER) {
					supplyStats.get(row.getString(supplyInfo)).add((double) row.getInt(value));
				} else {
					supplyStats.get(row.getString(supplyInfo)).add(row.getDouble(value));
				}
			}
		}

		fillAvgMap(demandStats, demandAvgs);
		fillAvgMap(supplyStats, supplyAvgs);

		params.put("avg_demand_stats.csv", List.of("rides_per_veh", "avg_wait_time", "requests", "avg_total_travel_time", "rides", "avg_direct_distance_[km]",
			"rejections", "95th_percentile_wait_time", "avg_in-vehicle_time", "avg_ride_distance_[km]", "rejection_rate", "avg_fare_[MoneyUnit]"));
		params.put("avg_supply_stats.csv", List.of("total_service_hours", "pooling_ratio", "detour_ratio", "total_vehicle_mileage_[km]", "empty_ratio", "number_of_stops",
			"total_pax_distance_[km]", "vehicles"));

		for (Map.Entry<String, List<String>> e : params.entrySet()) {
			for (String param : params.get(e.getKey())) {
				if (e.getKey().contains("demand")) {
					writeFile(e.getKey(), demandAvgs, param);
				} else {
					writeFile(e.getKey(), supplyAvgs, param);
				}
			}
		}

		return 0;
	}

	private void writeFile(String fileName, Map<String, Double[]> values, String param) throws IOException {
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath(param + "_" + fileName)), CSVFormat.DEFAULT)) {

			printer.printRecord("info", value);

			for (Map.Entry<String, Double[]> e : values.entrySet()) {
				String transformed = e.getKey().toLowerCase().replace(".", "").replace(" ", "_");
				if (transformed.contains(param)) {
					printer.printRecord("mean-" + e.getKey(), e.getValue()[0]);
					printer.printRecord("median-" + e.getKey(), e.getValue()[1]);
					printer.printRecord("sd-" + e.getKey(), e.getValue()[2]);
					printer.printRecord("min-" + e.getKey(), e.getValue()[3]);
					printer.printRecord("max-" + e.getKey(), e.getValue()[4]);
				}
			}
		}
	}

	private void fillAvgMap(Map<String, List<Double>> source, Map<String, Double[]> destination) {
		for (Map.Entry<String, List<Double>> e: source.entrySet()) {

			String key = e.getKey();
			Double[] values = new Double[5];

			double sum = 0.;

			for (double d : source.get(key)) {
				sum += d;
			}
			double mean = sum / source.get(key).size();

			values[0] = mean;
			values[1] = calcMedian(source.get(key));
			values[2] = calcStandardDeviation(source.get(key), mean);
			values[3] = Collections.min(source.get(key));
			values[4] = Collections.max(source.get(key));

			destination.put(key, values);
		}
	}

	private Double calcStandardDeviation(List<Double> values, double mean) {

		double sumSquaredDiff = 0;
		for (double num : values) {
			sumSquaredDiff += Math.pow(num - mean, 2);
		}

		return Math.sqrt(sumSquaredDiff / values.size());
	}

	private Double calcMedian(List<Double> values) {
		Collections.sort(values);

		int length = values.size();
		// Check if the length of the array is odd or even
		if (length % 2 != 0) {
			// If odd, return the middle element
			return values.get(length / 2);
		} else {
			// If even, return the average of the two middle elements
			int midIndex1 = length / 2 - 1;
			int midIndex2 = length / 2;
			return (values.get(midIndex1) + values.get(midIndex2)) / 2.0;
		}
	}
}

