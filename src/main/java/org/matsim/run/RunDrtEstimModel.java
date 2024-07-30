package org.matsim.run;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class RunDrtEstimModel {


	public static void main(String[] args) throws IOException {

		Path filePath = Path.of("/Users/jakob/git/matsim-kelheim/input/v3.0-release/output-KEXI/seed-1-kexi/kexi-seed1.output_drt_legs_drt.csv");

		DoubleList inVehicleTravelTime = new DoubleArrayList();
		DoubleList directTravelDistance_m = new DoubleArrayList();



		double n = 0.;
		double waitTimeSum = 0.;

		try (CSVParser parser = new CSVParser(new BufferedReader(new InputStreamReader(Files.newInputStream(filePath))),
			CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {

			for (CSVRecord record : parser) {
				n++;
				waitTimeSum += Double.parseDouble(record.get("waitTime"));
				inVehicleTravelTime.add(Double.parseDouble(record.get("inVehicleTravelTime")));
				directTravelDistance_m.add(Double.parseDouble(record.get("travelDistance_m")));
			}
		}
		double waitTime_s = waitTimeSum / n;
		System.out.println("Wait time (seconds):" + waitTime_s);
		System.out.println("Wait time (minutes):" + waitTime_s/60);

		SimpleRegression regression = new SimpleRegression(true);

		for (int i = 0; i < n; i++) {
			regression.addData(directTravelDistance_m.getDouble(i), inVehicleTravelTime.getDouble(i));
		}

		// Get the intercept and slope
		double intercept = regression.getIntercept();
		double slope = regression.getSlope();


		System.out.println("Intercept: " + intercept);
		System.out.println("Slope: " + slope);
		System.out.println("R2: " + regression.getRSquare());



	}
}
