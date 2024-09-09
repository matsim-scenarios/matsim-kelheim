package org.matsim.analysis.postAnalysis;

import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.file.FileReader;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.util.Utf8;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.avro.XYTData;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.matsim.application.ApplicationUtils.globFile;

@CommandLine.Command(name = "average-noise", description = "Calculates average noise stats based on several sim runs with different random seeds.")
@CommandSpec(
	requires = {"runs"},
	produces = {"mean_emission_per_day.csv", "mean_immission_per_day.avro", "mean_immission_per_hour.avro", "mean_noise_stats.csv",
		"mean_damages_receiverPoint_per_day.avro", "mean_damages_receiverPoint_per_hour.avro"}
)
public class NoiseAverageAnalysis implements MATSimAppCommand {
	private final Logger log = LogManager.getLogger(NoiseAverageAnalysis.class);
	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(NoiseAverageAnalysis.class);
	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(NoiseAverageAnalysis.class);
	@CommandLine.Option(names = "--no-runs", defaultValue = "5", description = "Number of simulation runs to be averaged.")
	private Integer noRuns;

	private static final String ANALYSIS_DIR = "/analysis/noise";
	private static final String LINK_ID = "Link Id";
	private static final String VALUE = "value";
	private List<GenericRecord> imissionsPerDay = new ArrayList<>();
	private List<GenericRecord> imissionsPerHour = new ArrayList<>();
	private Map<String, List<Double>> emissionsPerDay = new HashMap<>();
	private Map<String, Double> meanEmissionsPerDay = new HashMap<>();
	private Map<String, List<Double>> totalStats = new HashMap<>();
	private Map<String, Double> meanTotalStatsPerDay = new HashMap<>();
	private List<GenericRecord> damagesPerDay = new ArrayList<>();
	private List<GenericRecord> damagesPerHour = new ArrayList<>();


	public static void main(String[] args) {
		new NoiseAverageAnalysis().execute(args);
	}

	public NoiseAverageAnalysis() {
//		constructor needed for test only.. otherwise the read and write methods of this class would have to be copied, which is ugly. -sme0624

	}

	@Override
	public Integer call() throws Exception {
		String runs = input.getPath("runs");

		List<String> foldersSeeded = Arrays.stream(runs.split(",")).toList();

		//		add stats from every run to map
		for (String folder : foldersSeeded) {
			final Path analysisDir = Path.of(folder + ANALYSIS_DIR);
			String emissionsCsv = globFile(analysisDir, "*emission_per_day.csv*").toString();
			String imissionsPerDayAvro = globFile(analysisDir, "*immission_per_day.avro*").toString();
			String imissionsPerHourAvro = globFile(analysisDir, "*immission_per_hour.avro*").toString();
			String totalStatsCsv = globFile(analysisDir, "*noise_stats.csv*").toString();
			String damagesPerDayAvro = globFile(analysisDir, "*damages_receiverPoint_per_day.avro*").toString();
			String damagesPerHourAvro = globFile(analysisDir, "*damages_receiverPoint_per_hour.avro*").toString();

//			read
			Table emissions = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(emissionsCsv))
				.columnTypesPartial(Map.of(LINK_ID, ColumnType.STRING, VALUE, ColumnType.DOUBLE))
				.sample(false)
				.separator(CsvOptions.detectDelimiter(emissionsCsv)).build());

			Table totalStatsTable = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(totalStatsCsv))
				.columnTypes(new ColumnType[]{ColumnType.STRING, ColumnType.DOUBLE})
				.header(false)
				.sample(false)
				.separator(CsvOptions.detectDelimiter(totalStatsCsv)).build());

//			read avro files
			readAvroFile(imissionsPerDayAvro, imissionsPerDay);
			readAvroFile(imissionsPerHourAvro, imissionsPerHour);
			readAvroFile(damagesPerDayAvro, damagesPerDay);
			readAvroFile(damagesPerHourAvro, damagesPerHour);

//			get all emission stats
			for (int i = 0; i < emissions.rowCount(); i++) {
				Row row = emissions.row(i);

				if (!emissionsPerDay.containsKey(row.getString(LINK_ID))) {
					emissionsPerDay.put(row.getString(LINK_ID), new ArrayList<>());
				}
				emissionsPerDay.get(row.getString(LINK_ID)).add(row.getDouble(VALUE));
			}

//			get all total stats
			for (int i = 0; i < totalStatsTable.rowCount(); i++) {
				Row row = totalStatsTable.row(i);

				if (!totalStats.containsKey(row.getString(0))) {
					totalStats.put(row.getString(0), new ArrayList<>());
				}
				totalStats.get(row.getString(0)).add(row.getDouble(1));
			}
		}

//		calc emission means and write to mean map
		calcCsvMeans(emissionsPerDay, meanEmissionsPerDay);
//		calc mean total stats and write to mean map
		calcCsvMeans(totalStats, meanTotalStatsPerDay);

//		calc avro means
		XYTData imissionsPerDayMean = calcAvroMeans(imissionsPerDay, "imissions");
		XYTData imissionsPerHourMean = calcAvroMeans(imissionsPerHour, "imissions");
		XYTData damagesPerDayMean = calcAvroMeans(damagesPerDay, "damages_receiverPoint");
		XYTData damagesPerHourMean = calcAvroMeans(damagesPerHour, "damages_receiverPoint");

//		write emission mean stats
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("mean_emission_per_day.csv")), CSVFormat.DEFAULT)) {
			printer.printRecord(LINK_ID, VALUE);

			for (Map.Entry<String, Double> e : meanEmissionsPerDay.entrySet()) {
				printer.printRecord(e.getKey(), e.getValue());
			}
		}

//		write total mean stats
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("mean_noise_stats.csv")), CSVFormat.DEFAULT)) {
			for (Map.Entry<String, Double> e : meanTotalStatsPerDay.entrySet()) {
				printer.printRecord(e.getKey(), e.getValue());
			}
		}

//		write avro mean files
		writeAvro(imissionsPerDayMean, new File(output.getPath("mean_immission_per_day.avro").toString()));
		writeAvro(imissionsPerHourMean, new File(output.getPath("mean_immission_per_hour.avro").toString()));
		writeAvro(damagesPerDayMean, new File(output.getPath("mean_damages_receiverPoint_per_day.avro").toString()));
		writeAvro(damagesPerHourMean, new File(output.getPath("mean_damages_receiverPoint_per_hour.avro").toString()));

		return 0;
	}

	private void calcCsvMeans(Map<String, List<Double>> dataMap, Map<String, Double> meanMap) {
		for (Map.Entry<String, List<Double>> e : dataMap.entrySet()) {
			AtomicReference<Double> sum = new AtomicReference<>(0.);
			e.getValue().forEach(d -> sum.set(sum.get() + d));

			meanMap.put(e.getKey(), sum.get() / e.getValue().size());
		}
	}

	/**
	 * write an .avro file containing immission / damage data.
	 */
	public void writeAvro(XYTData xytData, File outputFile) {
		DatumWriter<XYTData> datumWriter = new SpecificDatumWriter<>(XYTData.class);
		try (DataFileWriter<XYTData> dataFileWriter = new DataFileWriter<>(datumWriter)) {
			dataFileWriter.setCodec(CodecFactory.deflateCodec(9));
			dataFileWriter.create(xytData.getSchema(), IOUtils.getOutputStream(IOUtils.getFileUrl(outputFile.toString()), false));
			dataFileWriter.append(xytData);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private XYTData calcAvroMeans(List<GenericRecord> recordList, String dataFieldName) {
		String crs = null;
		List<Float> xCoords = new ArrayList<>();
		List<Float> yCoords = new ArrayList<>();
		List<Integer> timeStamps = new ArrayList<>();
		Map<CharSequence, List<Float>> data = new HashMap<>();

		for (GenericRecord genericRecord: recordList) {

//			for every record: 0 crs, 1 xCoords, 2 yCoords, 3 timeStamps, 4 actual immission data
			String object0 = genericRecord.get(0).toString();
			Object object1 = genericRecord.get(1);
			Object object2 = genericRecord.get(2);
			Object object3 = genericRecord.get(3);
			Object object4 = genericRecord.get(4);

			if (crs == null) {
				crs = object0;
			}

			getCoordData(object1, xCoords);
			getCoordData(object2, yCoords);


//			TODO: for the example data even for the hourly data there was only one time stamp. This might be different with real data. this needs to be checked
			if (object3 instanceof GenericData.Array<?>) {
				List<Integer> ints = new ArrayList<>((GenericData.Array<Integer>) object3);

				if (!timeStamps.equals(ints)) {
					if (timeStamps.isEmpty()) {
						timeStamps.addAll(ints);
					} else {
						log.error("List of time stamps from the different run seeds are not identical, this should not happen. Abort.");
						throw new IllegalArgumentException();
					}
				}
			}

			//	there should be only one key in the map
			if (object4 instanceof HashMap<?, ?>) {
				List<Float> values = new ArrayList<>();

				for (Map.Entry<?, ?> entry : ((HashMap<?, ?>) object4).entrySet()) {
					if (entry.getKey() instanceof Utf8 && entry.getKey().toString().equals(dataFieldName) && entry.getValue() instanceof GenericData.Array<?>) {
						values.addAll((GenericData.Array<Float>) entry.getValue());

						String entryString = ((Utf8) entry.getKey()).toString();

						if (data.get(entryString) == null) {
//							if map = list (which is its only value) is empty: set values as list (it is the first iteration of this for loop)
							data.put(entryString, values);
						} else {
//							if there already is an entry in the map, take the values from list and update them
							for (Float f : data.get(entryString)) {
								data.get(entryString).set(data.get(entryString).indexOf(f), f + values.get(data.get(entryString).indexOf(f)));
							}
						}
					}
				}
			}
		}

//		calc mean values for each datapoint out of sums and number of records (1 record = 1 run seed)
		data.entrySet()
			.stream()
			.filter(entry -> entry.getKey().equals(dataFieldName))
			.forEach(entry -> entry.getValue()
				.forEach(value -> entry.getValue().set(entry.getValue().indexOf(value), value / recordList.size())));

		return new XYTData(crs, xCoords, yCoords, timeStamps, data);
	}

	private void getCoordData(Object object, List<Float> target) {
		if (object instanceof GenericData.Array<?>) {
			List<Float> floats = new ArrayList<>((GenericData.Array<Float>) object);

			if (!target.equals(floats)) {
				if (target.isEmpty()) {
					target.addAll(floats);
				} else {
					log.error("List of coords from the different run seeds are not identical, this should not happen. Abort.");
					throw new IllegalArgumentException();
				}
			}
		}
	}

	/**
	 * read an .avro file containing immissions / damages.
	 */
	public void readAvroFile(String input, List<GenericRecord> target) {
		try {
			// Read the schema from the Avro file
			FileReader<GenericRecord> fileReader = DataFileReader.openReader(new File(input), new GenericDatumReader<>());

			// Print the schema
			log.info("Reading .avro file from {} with schema {}.", input, fileReader.getSchema());

			// Read records and save to list
			while (fileReader.hasNext()) {
				target.add(fileReader.next());
			}

			fileReader.close();
		} catch (IOException e) {
			log.error(e);
		}
	}
}
