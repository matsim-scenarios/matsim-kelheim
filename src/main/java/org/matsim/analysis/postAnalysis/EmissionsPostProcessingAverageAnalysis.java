package org.matsim.analysis.postAnalysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.Coord;
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
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.matsim.application.ApplicationUtils.globFile;

@CommandLine.Command(name = "average-emissions", description = "Calculates average emission stats based on several sim runs with different random seeds.")
@CommandSpec(
	requires = {"runs"},
	produces = {"mean_emissions_total.csv", "mean_emissions_per_link_per_m.csv", "mean_emissions_grid_per_day.xyt.csv", "mean_emissions_grid_per_hour.csv"}
)
public class EmissionsPostProcessingAverageAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(EmissionsPostProcessingAverageAnalysis.class);
	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(EmissionsPostProcessingAverageAnalysis.class);
	@CommandLine.Option(names = "--no-runs", defaultValue = "5", description = "Number of simulation runs to be averaged.")
	private Integer noRuns;

	private final Map<String, List<Double>> totalStats = new HashMap<>();
	private final Map<String, List<Double[]>> perLinkMStats = new HashMap<>();
	private final Map<Map.Entry<Double, Coord>, List<Double>> gridPerDayStats = new HashMap<>();
	private final Map<Map.Entry<Double, Coord>, List<Double>> gridPerHourStats = new HashMap<>();
	private final Map<String, Double> meanTotal = new HashMap<>();
	private final Map<String, Double[]> meanPerLinkM = new HashMap<>();
	private final Map<Map.Entry<Double, Coord>, Double> meanGridPerDay = new HashMap<>();
	private final Map<Map.Entry<Double, Coord>, Double> meanGridPerHour = new HashMap<>();

	private final CsvOptions csv = new CsvOptions();
	private static final String VALUE = "value";
	private static final String LINK_ID = "linkId";
	private static final String POLLUTANT = "Pollutant";
	private static final String ANALYSIS_DIR = "/analysis/emissions";

	public static void main(String[] args) {
		new EmissionsPostProcessingAverageAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		String runs = input.getPath("runs");

//		function to determine column types
		Function<String, ColumnType> columnTypeFunction = columnName -> {
			if (columnName.equals(LINK_ID) || columnName.equals(POLLUTANT)) {
				return ColumnType.STRING;
			} else {
				return ColumnType.DOUBLE;
			}
		};

		List<String> foldersSeeded = Arrays.stream(runs.split(",")).toList();

//		add stats from every run to map
		for (String folder : foldersSeeded) {
			final Path analysisDir = Path.of(folder + ANALYSIS_DIR);
			String totalCsv = globFile(analysisDir, "*emissions_total.csv*").toString();
			String emissionsPerLinkMCsv = globFile(analysisDir, "*emissions_per_link_per_m.csv*").toString();
			String emissionsGridPerDayCsv = globFile(analysisDir, "*emissions_grid_per_day.xyt.csv*").toString();
			String emissionsGridPerHourCsv = globFile(analysisDir, "*emissions_grid_per_hour.csv*").toString();

			Table total = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(totalCsv))
				.columnTypes(columnTypeFunction)
				.sample(false)
				.separator(csv.detectDelimiter(totalCsv)).build());

			Table emissionsPerLinkM = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(emissionsPerLinkMCsv))
				.columnTypes(columnTypeFunction)
				.sample(false)
				.separator(csv.detectDelimiter(emissionsPerLinkMCsv)).build());

			Table emissionsGridPerDay = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(emissionsGridPerDayCsv))
				.columnTypes(columnTypeFunction)
				.sample(false)
				.separator(csv.detectDelimiter(emissionsGridPerDayCsv)).header(true).build());

			Table emissionsGridPerHour = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(emissionsGridPerHourCsv))
				.columnTypes(columnTypeFunction)
				.sample(false)
				.separator(csv.detectDelimiter(emissionsGridPerHourCsv)).build());

//			get all total stats
			for (int i = 0; i < total.rowCount(); i++) {
				Row row = total.row(i);

				if (!totalStats.containsKey(row.getString(POLLUTANT))) {
					totalStats.put(row.getString(POLLUTANT), new ArrayList<>());
				}
				totalStats.get(row.getString(POLLUTANT)).add(row.getDouble("kg"));
			}

//			get all per link per m stats
			for (int i = 0; i < emissionsPerLinkM.rowCount(); i++) {
				Row row = emissionsPerLinkM.row(i);
				Double[] values = new Double[emissionsPerLinkM.columnCount() - 1];

//				iterate through columns. this file contains 23 params per link, as of may24
				for (int j = 1; j < emissionsPerLinkM.columnCount(); j++) {
					if (!perLinkMStats.containsKey(row.getString(LINK_ID))) {
						perLinkMStats.put(row.getString(LINK_ID), new ArrayList<>());
					}
					values[j - 1] = row.getDouble(j);
				}
				perLinkMStats.get(row.getString(LINK_ID)).add(values);
			}

//			get all grid per day stats
			getGridData(emissionsGridPerDay, gridPerDayStats);
//			get all grid per day stats
			getGridData(emissionsGridPerHour, gridPerHourStats);
		}

//		calc means for every map
//		total means
		for (Map.Entry<String, List<Double>> e : totalStats.entrySet()) {
			AtomicReference<Double> sum = new AtomicReference<>(0.);
			e.getValue().forEach(d -> sum.set(sum.get() + d));

			meanTotal.put(e.getKey(), sum.get() / e.getValue().size());
		}

//		per linkM means
		for (Map.Entry<String, List<Double[]>> e : perLinkMStats.entrySet()) {

			Double[] sums = new Double[e.getValue().get(0).length];

			for (Double[] d : e.getValue()) {
				for (int i = 0; i <= d.length - 1; i++) {
//					initial array values are null
					if (sums[i] == null) {
						sums[i] = 0.;
					}
					sums[i] += d[i];
				}
			}

			Double[] means = new Double[sums.length];
			for (int i = 0; i <= sums.length - 1; i++) {
				means[i] = sums[i] / e.getValue().size();
			}
			meanPerLinkM.put(e.getKey(), means);
		}

//		grid per day means
		calcGridMeans(gridPerDayStats, meanGridPerDay);
//		grid per hour means
		calcGridMeans(gridPerHourStats, meanGridPerHour);

		NumberFormat nf = NumberFormat.getInstance(Locale.US);
		nf.setMaximumFractionDigits(4);
		nf.setGroupingUsed(false);

//		write total mean stats
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("mean_emissions_total.csv")), CSVFormat.DEFAULT)) {
			printer.printRecord(POLLUTANT, "kg");

			for (Map.Entry<String, Double> e : meanTotal.entrySet()) {
				printer.printRecord("" + e.getKey() + " (mean)", nf.format(e.getValue()));
			}
		}

//		write per linkM mean stats
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("mean_emissions_per_link_per_m.csv")), CSVFormat.DEFAULT)) {
			printer.printRecord(LINK_ID, "CO [g/m]", "CO2_TOTAL [g/m]", "FC [g/m]", "HC [g/m]", "NMHC [g/m]", "NOx [g/m]", "NO2 [g/m]", "PM [g/m]", "SO2 [g/m]",
				"FC_MJ [g/m]", "CO2_rep [g/m]", "CO2e [g/m]", "PM2_5 [g/m]", "PM2_5_non_exhaust [g/m]", "PM_non_exhaust [g/m]", "BC_exhaust [g/m]", "BC_non_exhaust [g/m]",
				"Benzene [g/m]", "PN [g/m]", "Pb [g/m]", "CH4 [g/m]", "N2O [g/m]", "NH3 [g/m]"
			);

			for (Map.Entry<String, Double[]> e : meanPerLinkM.entrySet()) {
				printer.printRecord(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2], e.getValue()[3], e.getValue()[4], e.getValue()[5],
					e.getValue()[6], e.getValue()[7], e.getValue()[8], e.getValue()[9], e.getValue()[10], e.getValue()[11], e.getValue()[12], e.getValue()[13],
					e.getValue()[14], e.getValue()[15], e.getValue()[16], e.getValue()[17], e.getValue()[18], e.getValue()[19], e.getValue()[20], e.getValue()[21],
					e.getValue()[22]);
			}
		}

//		write grid mean stats
		writeGridFile("mean_emissions_grid_per_day.xyt.csv", meanGridPerDay);
		writeGridFile("mean_emissions_grid_per_hour.csv", meanGridPerHour);

		return 0;
	}

	private void calcGridMeans(Map<Map.Entry<Double, Coord>, List<Double>> originMap, Map<Map.Entry<Double, Coord>, Double> targetMap) {
		for (Map.Entry<Map.Entry<Double, Coord>, List<Double>> e : originMap.entrySet()) {
			AtomicReference<Double> sum = new AtomicReference<>(0.);
			e.getValue().forEach(d -> sum.set(sum.get() + d));

			targetMap.put(e.getKey(), sum.get() / e.getValue().size());
		}
	}

	private void getGridData(Table gridTable, Map<Map.Entry<Double, Coord>, List<Double>> dataMap) {
		for (int i = 0; i < gridTable.rowCount(); i++) {
			Row row = gridTable.row(i);
			Map.Entry<Double, Coord> entry = new AbstractMap.SimpleEntry<>(row.getDouble("time"), new Coord(row.getDouble("x"), row.getDouble("y")));

			dataMap.computeIfAbsent(entry, key -> new ArrayList<>());
			dataMap.get(entry).add(row.getDouble(VALUE));
		}
	}

	private void writeGridFile(String fileName, Map<Map.Entry<Double, Coord>, Double> values) throws IOException {
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath(fileName)), CSVFormat.DEFAULT)) {

			//set the projection in the YAML instead, as this is put out with a quote atm...
			//printer.printRecord("# EPSG:25832");
			printer.printRecord("time", "x", "y", VALUE);

			for (Map.Entry<Map.Entry<Double, Coord>, Double> e : values.entrySet()) {
				printer.printRecord(e.getKey().getKey(), e.getKey().getValue().getX(), e.getKey().getValue().getY(), e.getValue());
			}
		}
	}
}

