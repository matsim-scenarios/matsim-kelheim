package org.matsim.run.prepare;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.run.RunKelheimScenario;
import org.opengis.feature.simple.SimpleFeature;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Write DRT stops xml.
 */
public final class DrtStopsWriter_JB extends MatsimXmlWriter {

	private static final Logger log = LogManager.getLogger(DrtStopsWriter_JB.class);

	private final String mode;
	private Geometry serviceArea = null;
	private final String outputFolder;
	private final Network network;

	DrtStopsWriter_JB(Network network, String mode, ShpOptions shp, String outputFolder) {
		this.network = network;
		this.mode = mode;
		this.outputFolder = outputFolder;
		//If you just say serviceArea = shp.getGeometry() instead of looping through features
		//somehow the first feature only is taken -sm0222
		if (shp.isDefined()){
			List<SimpleFeature> features = shp.readFeatures();
			for (SimpleFeature feature : features) {
				if (shp.getShapeFile() != null) {
					if (serviceArea == null) {
						serviceArea = (Geometry) feature.getDefaultGeometry();
					} else {
						serviceArea = serviceArea.union((Geometry) feature.getDefaultGeometry());
					}
				}
			}
		}

	}

	/**
	 * Write content to specified folder.
	 */
	public void write() throws IOException {
		this.openFile(outputFolder + "/" + mode + "-stops.xml");
		this.writeXmlHead();
		this.writeDoctype("transitSchedule", "http://www.matsim.org/files/dtd/transitSchedule_v1.dtd");
		this.writeStartTag("transitSchedule", null);
		this.writeStartTag("transitStops", null);
		this.writeTransitStopsAndVizFiles(network);
		this.writeEndTag("transitStops");
		this.writeEndTag("transitSchedule");
		this.close();
	}

	/**
	 * additionally to writing the stops xml file, also writes a csv file that contains the same information as well as a network file that contains only
	 * the links assigned to stops (for visualisation).
	 * @param network to retrieve link id's from
	 * @throws IOException if some file can't be opened or written
	 */
	private void writeTransitStopsAndVizFiles(Network network) throws IOException {
		// Write csv file for adjusted stop location
		FileWriter csvWriter = new FileWriter(outputFolder + "/"
				+ mode + "-stops-locations.csv");
		csvWriter.append("Stop ID");
		csvWriter.append(",");
		csvWriter.append("Link ID");
		csvWriter.append("Link ID");
		csvWriter.append(",");
		csvWriter.append("X");
		csvWriter.append(",");
		csvWriter.append("Y");
		csvWriter.append("\n");

		// Read original data csv
		log.info("Start processing the network. This may take some time...");
		String data = "C:/Users/J/Downloads/Haltestellen_Landkexi_tidy2.csv";
		Set<Id<Link>> allLinks = new HashSet<>();

		try (CSVParser parser = new CSVParser(IOUtils.getBufferedReader(data),
				CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
			for (CSVRecord row : parser) {
				Coord coord = new Coord(Double.parseDouble(row.get("x")), Double.parseDouble(row.get("y")));
				if (serviceArea == null || MGC.coord2Point(coord).within(serviceArea)) {
					List<Tuple<String, String>> attributes = new ArrayList<>(5);
					attributes.add(createTuple("id", row.get("Name")));
					attributes.add(createTuple("x", row.get("x")));
					attributes.add(createTuple("y", row.get("y")));
					Link link = null;
					// If link is already determined by hand in the raw data, then use that link directly.

					System.out.println(row);
					if (row.get("linkId_v" + RunKelheimScenario.VERSION)!=null && !row.get("linkId_v" + RunKelheimScenario.VERSION).isEmpty()){
						link = network.getLinks().get(Id.createLinkId(row.get("linkId_v" + RunKelheimScenario.VERSION)));
					} else {
						link = getStopLink(coord, network);
					}
					allLinks.add(link.getId());
					attributes.add(createTuple("linkRefId", link.getId().toString()));

					//write into stops xml file
					this.writeStartTag("stopFacility", attributes, true);

					//write into csv file for viz
					csvWriter.append(row.get("Name"));
					csvWriter.append(",");
					csvWriter.append(link.getId().toString());
					csvWriter.append(",");
					csvWriter.append(Double.toString(link.getToNode().getCoord().getX()));
					csvWriter.append(",");
					csvWriter.append(Double.toString(link.getToNode().getCoord().getY()));
					csvWriter.append("\n");
				}
			}
		}

		csvWriter.close();

		//write filtered network file (for viz)
		writeFilteredNetwork(network, allLinks);
	}

	private void writeFilteredNetwork(Network network, Set<Id<Link>> allLinks) {
		//remove all links but the ones in the set
		network.getLinks().keySet()
			.forEach(linkId -> {
				if (!allLinks.contains(linkId)) {
					network.removeLink(linkId);
				}
			});
		//remove 'empty' nodes
		network.getNodes().values().stream()
			.filter(node -> node.getInLinks().size() == 0 && node.getOutLinks().size() == 0)
			.forEach(node -> network.removeNode(node.getId()));

		NetworkUtils.writeNetwork(network, outputFolder + "/" + mode + "-stops-links.xml.gz");
	}

	private Link getStopLink(Coord coord, Network network) {
		double shortestDistance = Double.MAX_VALUE;
		Link nearestLink = null;
		for (Link link : network.getLinks().values()) {
			if (!link.getAllowedModes().contains(mode)) {
				continue;
			}
			double dist = CoordUtils.distancePointLinesegment(link.getFromNode().getCoord(), link.getToNode().getCoord(), coord);
			if (dist < shortestDistance) {
				shortestDistance = dist;
				nearestLink = link;
			}
		}


		double distanceToFromNode = CoordUtils.calcEuclideanDistance(nearestLink.getFromNode().getCoord(), coord);
		double distanceToToNode = CoordUtils.calcEuclideanDistance(nearestLink.getToNode().getCoord(), coord);

		// If to node is closer to the stop coordinate, we will use this link as the stop location
		if (distanceToToNode < distanceToFromNode) {
			return nearestLink;
		}

		// Otherwise, we will use the opposite link as the stop location
		Set<Link> linksConnectToToNode = new HashSet<>(nearestLink.getToNode().getOutLinks().values());
		linksConnectToToNode.retainAll(nearestLink.getFromNode().getInLinks().values());
		if (!linksConnectToToNode.isEmpty()) {
			return linksConnectToToNode.iterator().next();
		}

		// However, if this link does not have an opposite direction counterpart, we will use it anyway.
		return nearestLink;
	}
}
