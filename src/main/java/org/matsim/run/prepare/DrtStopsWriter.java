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
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;
import org.opengis.feature.simple.SimpleFeature;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Write DRT stops xml.
 */
public final class DrtStopsWriter extends MatsimXmlWriter {

	private static final Logger log = LogManager.getLogger(DrtStopsWriter.class);

	private final String mode;
	private Geometry serviceArea = null;
	private final String outputFolder;
	private final Network network;

	DrtStopsWriter(Network network, String mode, ShpOptions shp, String outputFolder) {
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
	public void write() throws UncheckedIOException, IOException {
		this.openFile(outputFolder + "/" + mode + "-stops.xml");
		this.writeXmlHead();
		this.writeDoctype("transitSchedule", "http://www.matsim.org/files/dtd/transitSchedule_v1.dtd");
		this.writeStartTag("transitSchedule", null);
		this.writeStartTag("transitStops", null);
		this.writeTransitStops(network);
		this.writeEndTag("transitStops");
		this.writeEndTag("transitSchedule");
		this.close();
	}

	private void writeTransitStops(Network network) throws IOException {
		// Write csv file for adjusted stop location
		FileWriter csvWriter = new FileWriter(outputFolder + "/"
				+ mode + "-stops-locations.csv");
		csvWriter.append("Stop ID");
		csvWriter.append(",");
		csvWriter.append("Link ID");
		csvWriter.append(",");
		csvWriter.append("X");
		csvWriter.append(",");
		csvWriter.append("Y");
		csvWriter.append("\n");

		// Read original data csv
		log.info("Start processing the network. This may take some time...");
		URL data = new URL("https://svn.vsp.tu-berlin.de/" +
				"repos/public-svn/matsim/scenarios/countries/de/kelheim/original-data/" +
				"KEXI_Haltestellen_Liste_Kelheim_utm32n.csv");

        try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(data.getPath())),
                CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
            for (CSVRecord row : parser) {
                Coord coord = new Coord(Double.parseDouble(row.get("x")), Double.parseDouble(row.get("y")));
                if (serviceArea == null || MGC.coord2Point(coord).within(serviceArea)) {
                    List<Tuple<String, String>> attributes = new ArrayList<>(5);
                    attributes.add(createTuple("id", row.get("Haltestell")));
                    attributes.add(createTuple("x", row.get("x")));
                    attributes.add(createTuple("y", row.get("y")));
                    Link link = null;
                    // If link is already determined by hand in the raw data, then use that link directly.
                    if (row.get("link_id")!=null){
                        link = network.getLinks().get(Id.createLinkId(row.get("link_id")));
                    } else {
                        link = getStopLink(coord, network);
                    }
                    attributes.add(createTuple("linkRefId", link.getId().toString()));
                    this.writeStartTag("stopFacility", attributes, true);

                    csvWriter.append(row.get("Haltestell"));
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
