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
import org.matsim.application.prepare.network.CreateGeoJsonNetwork;
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

	/**
	 * Test for building means over noise emission and immission values of several runs. The display of the values has to be done manually by looking at the resulting mean noise dashbaord.
	 */
	@Test
	void runMeanNoiseDashboardTest() throws IOException {

		String networkPath = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/kelheim-v3.0/input/kelheim-v3.0-network.xml.gz";
		String crs = "EPSG:25832";
		String path = utils.getInputDirectory();
		NoiseAverageAnalysis analysis = new NoiseAverageAnalysis();

		List<String> foldersSeeded = new ArrayList<>();

		new CreateGeoJsonNetwork().execute(List.of("--network", networkPath, "--with-properties", "--shp", "./input/shp/dilutionArea.shp", "--output-network", path + "1seed/analysis/network/network.geojson",
			"--input-crs", "EPSG:25832").toArray(new String[0]));

//		write dummy data
		for (int i = 1; i <= 3; i++) {
			List<Float> xCoords = new ArrayList<>();
			List<Float> yCoords = new ArrayList<>();
			List<Integer> timeStamps = new ArrayList<>();
			Map<CharSequence, List<Float>> data = new HashMap<>();

			xCoords.add(710419.08F);
			xCoords.add(710424.82F);
			yCoords.add(5421673.49F);
			yCoords.add(5422288.95F);

			timeStamps.add(28800);

			data.put("imissions", List.of((float) i));

			String seedDir = path + i + "seed/";
			foldersSeeded.add(seedDir);

//			write avro dummy files
			Files.createDirectories(Path.of(seedDir + "analysis/noise/"));
			analysis.writeAvro(new XYTData(crs, xCoords, yCoords, List.of(0), data), new File(seedDir + "analysis/noise/immission_per_day.avro"));
			analysis.writeAvro(new XYTData(crs, xCoords, yCoords, timeStamps, data), new File(seedDir + "analysis/noise/immission_per_hour.avro"));

//			write emissions csv dummy file
			try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(Path.of(seedDir + "analysis/noise/emission_per_day.csv")), CSVFormat.DEFAULT)) {
				printer.printRecord("Link Id", "value");
				printer.printRecord("-27443742#0", i);
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

//		assert that: a) mean immission is 2.0 on daily and hourly data b) hourly data has timestamp 28800 c) mean emission is 2.0
		List<GenericRecord> daily = new ArrayList<>();
		List<GenericRecord> hourly = new ArrayList<>();
		analysis.readAvroFile(path + "analysis/postAnalysis-noise/mean_immission_per_day.avro", daily);
		analysis.readAvroFile(path + "analysis/postAnalysis-noise/mean_immission_per_hour.avro", hourly);

		if (daily.getFirst().get(4) instanceof HashMap<?, ?>) {
			Map.Entry<?, ?> entry = ((HashMap<?, ?>) daily.getFirst().get(4)).entrySet().stream().toList().getFirst();
			if (entry.getKey() instanceof Utf8 && entry.getValue() instanceof GenericData.Array<?>) {
				float dailyImmission = ((GenericData.Array<Float>) entry.getValue()).getFirst();
				Assertions.assertEquals(2.0, dailyImmission);
			}
		}

		if (hourly.getFirst().get(4) instanceof HashMap<?, ?>) {
			Map.Entry<?, ?> entry = ((HashMap<?, ?>) hourly.getFirst().get(4)).entrySet().stream().toList().getFirst();
			if (entry.getKey() instanceof Utf8 && entry.getValue() instanceof GenericData.Array<?>) {
				float hourlyImmission = ((GenericData.Array<Float>) entry.getValue()).getFirst();
				Assertions.assertEquals(2.0, hourlyImmission);
			}
		}

		if (hourly.getFirst().get(3) instanceof GenericData.Array<?>) {
			int timeStamp = ((GenericData.Array<Integer>) hourly.getFirst().get(3)).getFirst();
			Assertions.assertEquals(28800, timeStamp);
		}

		Table emissions = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(path + "analysis/postAnalysis-noise/mean_emission_per_day.csv"))
			.columnTypesPartial(Map.of("Link Id", ColumnType.STRING, "value", ColumnType.DOUBLE))
			.sample(false)
			.separator(CsvOptions.detectDelimiter(path + "analysis/postAnalysis-noise/mean_emission_per_day.csv")).build());

		String linkId = emissions.row(0).getString("Link Id");
		double emission = emissions.row(0).getDouble("value");

		Assertions.assertEquals("-27443742#0", linkId);
		Assertions.assertEquals(2.0, emission);
	}
}
