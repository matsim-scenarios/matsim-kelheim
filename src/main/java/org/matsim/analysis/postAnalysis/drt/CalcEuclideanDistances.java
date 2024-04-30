/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.analysis.postAnalysis.drt;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.common.util.DistanceUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Write Euclidean trip distances into trips.csv.
 */
final class CalcEuclideanDistances {

	private static final Logger log = LogManager.getLogger(CalcEuclideanDistances.class);

	private CalcEuclideanDistances(){}

	public static void main(String[] args) {

		Path input = Path.of("C:/Users/Tilmann/tubCloud/VSP_WiMi/Projekte/KelRide/2023-10-results-exchange-VIA/AV-speed-mps-5/SAR2023-AV5/seed-5-SAR2023/AV5-MPS5-SAR2023-seed5.output_drt_legs_av.csv");
//		Path input = Path.of("C:/Users/Tilmann/tubCloud/VSP_WiMi/Projekte/KelRide/2023-10-results-exchange-VIA/AV-speed-mps-5/SAR2023-AV5/seed-5-SAR2023/AV5-MPS5-SAR2023-seed5.av_output_trips.csv");

		String output = input.toString().substring(0, input.toString().lastIndexOf('.')) + "_withDistance.csv";
		CSVPrinter writer;
		try (CSVParser parser = new CSVParser(Files.newBufferedReader(input),
			 CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {

			writer = new CSVPrinter(new FileWriter(output), CSVFormat.DEFAULT.withDelimiter(';'));


			for (CSVRecord row : parser.getRecords()) {

				Coord fromCoord = new Coord(Double.parseDouble(row.get(4)), Double.parseDouble(row.get(5)));
				Coord toCoord = new Coord(Double.parseDouble(row.get(7)), Double.parseDouble(row.get(8)));



//				Coord fromCoord = new Coord(Double.parseDouble(row.get(parser.getHeaderMap().get("start_x"))), Double.parseDouble(row.get(parser.getHeaderMap().get("start_y"))));
//				Coord toCoord = new Coord(Double.parseDouble(row.get(parser.getHeaderMap().get("end_x"))), Double.parseDouble(row.get(parser.getHeaderMap().get("end_y"))));


				double euclideanDistance = DistanceUtils.calculateDistance(fromCoord, toCoord);

				List<String> outputRow = new ArrayList<>();
				writer.printRecord(Arrays.stream(row.values()).toList(), euclideanDistance);
			}
			parser.close();
			writer.close();

		} catch (IOException e){
			log.fatal(e.getStackTrace());
		}

	}

}
