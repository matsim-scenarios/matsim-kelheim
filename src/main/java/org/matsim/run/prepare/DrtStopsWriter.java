package org.matsim.run.prepare;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DrtStopsWriter extends MatsimXmlWriter {
    private final String mode;
    private Geometry serviceArea = null;
    private final String outputFolder;
    private final Network network;

    DrtStopsWriter(Network network, String mode, ShpOptions shp, String outputFolder) {
        this.network = network;
        this.mode = mode;
        this.outputFolder = outputFolder;
        if (shp.getShapeFile() != null) {
            serviceArea = shp.getGeometry();
        }
    }

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
        System.out.println("Start processing the network. This may take some time...");
        URL data = new URL("https://svn.vsp.tu-berlin.de/" +
                "repos/public-svn/matsim/scenarios/countries/de/kelheim/original-data/" +
                "KEXI_Haltestellen_Liste_Kelheim_utm32n.csv");

        BufferedReader csvReader = new BufferedReader(new InputStreamReader(data.openStream()));
        csvReader.readLine();
        while (true) {
            String stopEntry = csvReader.readLine();
            if (stopEntry == null) {
                break;
            }
            String[] stopData = stopEntry.split(";");
            // write stop
            Coord coord = new Coord(Double.parseDouble(stopData[2]), Double.parseDouble(stopData[3]));

            if (serviceArea == null || MGC.coord2Point(coord).within(serviceArea)) {
                List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>(5);
                attributes.add(createTuple("id", stopData[0]));
                attributes.add(createTuple("x", stopData[2]));
                attributes.add(createTuple("y", stopData[3]));
                Link link = getStopLink(coord, network);
                attributes.add(createTuple("linkRefId", link.getId().toString()));
                this.writeStartTag("stopFacility", attributes, true);

                csvWriter.append(stopData[0]);
                csvWriter.append(",");
                csvWriter.append(link.getId().toString());
                csvWriter.append(",");
                csvWriter.append(Double.toString(link.getToNode().getCoord().getX()));
                csvWriter.append(",");
                csvWriter.append(Double.toString(link.getToNode().getCoord().getY()));
                csvWriter.append("\n");
            }
        }
        csvWriter.close();
    }

    private Link getStopLink(Coord coord, Network network) {
        double shortestDistance = Double.MAX_VALUE;
        Link nearestLink = null;
        for (Link link : network.getLinks().values()) {
            if (!link.getAllowedModes().contains("car")) {
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
