package org.matsim.run;

import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.analysis.postAnalysis.NoiseAverageAnalysis;
import org.matsim.application.avro.XYTData;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.dashboard.AverageKelheimNoiseDashboard;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.testcases.MatsimTestUtils;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MeanNoiseDashboardTest {

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private final NoiseAverageAnalysis analysis = new NoiseAverageAnalysis();

	/**
	 * Test for building means over noise emission and immission values of several runs. The display of the values has to be done manually by looking at the resulting mean noise dashbaord.
	 */
	@Test
	void runMeanNoiseDashboardTest() throws IOException {

		String networkPath = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/kelheim-v3.0/input/kelheim-v3.0-network.xml.gz";
		String crs = "EPSG:25832";
		String path = utils.getInputDirectory();

		List<String> foldersSeeded = new ArrayList<>();

		new CreateAvroNetwork().execute(List.of("--network", networkPath, "--with-properties", "--shp", "./input/shp/dilutionArea.shp", "--output-network", path + "1seed/analysis/network/network.avro",
			"--input-crs", "EPSG:25832").toArray(new String[0]));

//		write dummy data
		for (int i = 1; i <= 3; i++) {
			List<Float> xCoords = new ArrayList<>();
			List<Float> yCoords = new ArrayList<>();
			List<Integer> timeStamps = new ArrayList<>();
			Map<CharSequence, List<Float>> immissionData = new HashMap<>();
			Map<CharSequence, List<Float>> damageData = new HashMap<>();

			xCoords.add(710419.08F);
			xCoords.add(710424.82F);
			yCoords.add(5421673.49F);
			yCoords.add(5422288.95F);

			timeStamps.add(28800);

			immissionData.put("imissions", List.of((float) i));
			damageData.put("damages_receiverPoint", List.of((float) i));

			String seedDir = path + i + "seed/";
			foldersSeeded.add(seedDir);

//			write avro dummy files
			Files.createDirectories(Path.of(seedDir + "analysis/noise/"));
			analysis.writeAvro(new XYTData(crs, xCoords, yCoords, List.of(0), immissionData), new File(seedDir + "analysis/noise/immission_per_day.avro"));
			analysis.writeAvro(new XYTData(crs, xCoords, yCoords, timeStamps, immissionData), new File(seedDir + "analysis/noise/immission_per_hour.avro"));
			analysis.writeAvro(new XYTData(crs, xCoords, yCoords, List.of(0), damageData), new File(seedDir + "analysis/noise/damages_receiverPoint_per_day.avro"));
			analysis.writeAvro(new XYTData(crs, xCoords, yCoords, timeStamps, damageData), new File(seedDir + "analysis/noise/damages_receiverPoint_per_hour.avro"));

//			write emissions csv dummy file
			try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(Path.of(seedDir + "analysis/noise/emission_per_day.csv")), CSVFormat.DEFAULT)) {
				printer.printRecord("Link Id", "value");
				printer.printRecord("-27443742#0", i);
			}

//			write total stats csv dummy file
			try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(Path.of(seedDir + "analysis/noise/noise_stats.csv")), CSVFormat.DEFAULT)) {
				printer.printRecord("Annual cost rate per pop. unit [€]:", i);
				printer.printRecord("Total damages at receiver points", i);
				printer.printRecord("Total immission at receiver points", i);
			}
		}

		SimWrapper sw = SimWrapper.create();
		sw.getConfigGroup().defaultParams().mapCenter = "11.89,48.91";
		sw.addDashboard(Dashboard.customize(new AverageKelheimNoiseDashboard(foldersSeeded, 3)).context("noise"));
		try {
			sw.generate(Path.of(path), true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		sw.run(Path.of(path));

//		assert that:
//		a) mean immission is 2.0 on daily and hourly data
//		b) hourly immission data has timestamp 28800
//		c) mean emission is 2.0
//		d) all mean total stats are 2.0
//		e) mean damage is 2.0 on daily and hourly data
//		f) hourly damage data has timestamp
		List<GenericRecord> dailyImmission = new ArrayList<>();
		List<GenericRecord> hourlyImmission = new ArrayList<>();
		List<GenericRecord> dailyDamage = new ArrayList<>();
		List<GenericRecord> hourlyDamage = new ArrayList<>();
		analysis.readAvroFile(path + "analysis/postAnalysis-noise/mean_immission_per_day.avro", dailyImmission);
		analysis.readAvroFile(path + "analysis/postAnalysis-noise/mean_immission_per_hour.avro", hourlyImmission);
		analysis.readAvroFile(path + "analysis/postAnalysis-noise/mean_damages_receiverPoint_per_day.avro", dailyDamage);
		analysis.readAvroFile(path + "analysis/postAnalysis-noise/mean_damages_receiverPoint_per_hour.avro", hourlyDamage);

		assertValue(dailyImmission);
		assertValue(hourlyImmission);
		assertTimeStamp(hourlyImmission);

		Table emissions = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(path + "analysis/postAnalysis-noise/mean_emission_per_day.csv"))
			.columnTypesPartial(Map.of("Link Id", ColumnType.STRING, "value", ColumnType.DOUBLE))
			.sample(false)
			.separator(CsvOptions.detectDelimiter(path + "analysis/postAnalysis-noise/mean_emission_per_day.csv")).build());

		String linkId = emissions.row(0).getString("Link Id");
		double emission = emissions.row(0).getDouble("value");

		Assertions.assertEquals("-27443742#0", linkId);
		Assertions.assertEquals(2.0, emission);

		Table totalStats = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(path + "analysis/postAnalysis-noise/mean_noise_stats.csv"))
			.columnTypes(new ColumnType[]{ColumnType.STRING, ColumnType.DOUBLE})
			.header(false)
			.sample(false)
			.separator(CsvOptions.detectDelimiter(path + "analysis/postAnalysis-noise/mean_noise_stats.csv")).build());

		String row1ParamName = (String) totalStats.get(0, 0);
		String row2ParamName = (String) totalStats.get(1, 0);
		String row3ParamName = (String) totalStats.get(2, 0);
		double row1Value = (Double) totalStats.get(0, 1);
		double row2Value = (Double) totalStats.get(1, 1);
		double row3Value = (Double) totalStats.get(2, 1);

		Assertions.assertEquals("Annual cost rate per pop. unit [€]:", row1ParamName);
		Assertions.assertEquals("Total damages at receiver points", row2ParamName);
		Assertions.assertEquals("Total immission at receiver points", row3ParamName);
		Assertions.assertEquals(2.0, row1Value);
		Assertions.assertEquals(2.0, row2Value);
		Assertions.assertEquals(2.0, row3Value);

		assertValue(dailyDamage);
		assertValue(hourlyDamage);
		assertTimeStamp(hourlyDamage);
	}

	private static void assertTimeStamp(List<GenericRecord> records) {
		if (records.getFirst().get(3) instanceof GenericData.Array<?>) {
			int timeStamp = ((GenericData.Array<Integer>) records.getFirst().get(3)).getFirst();
			Assertions.assertEquals(28800, timeStamp);
		}
	}

	private static void assertValue(List<GenericRecord> records) {
		if (records.getFirst().get(4) instanceof HashMap<?, ?>) {
			Map.Entry<?, ?> entry = ((HashMap<?, ?>) records.getFirst().get(4)).entrySet().stream().toList().getFirst();
			if (entry.getKey() instanceof Utf8 && entry.getValue() instanceof GenericData.Array<?>) {
				float value = ((GenericData.Array<Float>) entry.getValue()).getFirst();
				Assertions.assertEquals(2.0, value);
			}
		}
	}
}
