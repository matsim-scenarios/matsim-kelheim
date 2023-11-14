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

package org.matsim.analysis.postAnalysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

class TransformCoordinatesTripCSV {

	private TransformCoordinatesTripCSV(){}

	public static void main(String[] args) {
		String path = "C:/Users/Tilmann/tubCloud/VSP_WiMi/Projekte/KelRide/2023-10-results-exchange-VIA/AV-speed-mps-5/SAR2023-AV5/seed-5-SAR2023";

		Iterator<File> files = FileUtils.iterateFiles(new File(path), new WildcardFileFilter(Arrays.asList("*trips*", "*legs*")), null);
		files.forEachRemaining(file -> process(file));

	}

	/**
	 *
	 * transforms coordinates in output legs and output trips csv files from UTM32N to WGS84
	 *
	 * @param input the output file to process
	 */
	private static void process(File input) {
		String output = input.getAbsolutePath().substring(0, input.getAbsolutePath().lastIndexOf(".csv")) + "_WGS84.csv";

		CoordinateTransformation transformer = TransformationFactory.getCoordinateTransformation("EPSG:25832", TransformationFactory.WGS84);

		try {
			CSVParser reader = new CSVParser(IOUtils.getBufferedReader(input.getAbsolutePath()),
				CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader());
			String[] header = reader.getHeaderNames().toArray(new String[0]);

			CSVPrinter writer = new CSVPrinter(IOUtils.getBufferedWriter(output),
				CSVFormat.DEFAULT.withDelimiter(';').withHeader(header));

			Map<String, Integer> headerMap = reader.getHeaderMap();

			Iterator<CSVRecord> iterator = reader.iterator();
			while (iterator.hasNext()){
				CSVRecord dataSet = iterator.next();

				Integer fromX_idx = headerMap.get("fromX") == null ? headerMap.get("start_x") : headerMap.get("fromX");
				Integer fromY_idx = headerMap.get("fromY") == null ? headerMap.get("start_y") : headerMap.get("fromY");
				Integer toX_idx = headerMap.get("toX") == null ? headerMap.get("end_x") : headerMap.get("toX");
				Integer toY_idx = headerMap.get("toY") == null ? headerMap.get("end_y") : headerMap.get("toY");

				Coord from = new Coord(Double.parseDouble(dataSet.get(fromX_idx)), Double.parseDouble(dataSet.get(fromY_idx)));
				Coord to = new Coord(Double.parseDouble(dataSet.get(toX_idx)), Double.parseDouble(dataSet.get(toY_idx)));

				Coord fromTransformed = transformer.transform(from);
				Coord toTransformed =transformer.transform(to);

				String[] modified = dataSet.values().clone();
				modified[fromX_idx] = String.valueOf(fromTransformed.getX());
				modified[fromY_idx] = String.valueOf(fromTransformed.getY());
				modified[toX_idx] = String.valueOf(toTransformed.getX());
				modified[toY_idx] = String.valueOf(toTransformed.getY());

				writer.printRecord(modified);
			}

			reader.close();
			writer.close();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


}
