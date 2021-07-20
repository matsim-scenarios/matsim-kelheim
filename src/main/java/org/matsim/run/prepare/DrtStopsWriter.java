package org.matsim.run.prepare;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DrtStopsWriter extends MatsimXmlWriter {

    public void write(final String filename, Network network) throws UncheckedIOException, IOException {
        this.openFile(filename);
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
        FileWriter csvWriter = new FileWriter("/Users/luchengqi/Documents/MATSimScenarios/Kelheim/drt-stops-locations.csv");
        csvWriter.append("Stop ID");
        csvWriter.append(",");
        csvWriter.append("Link ID");
        csvWriter.append(",");
        csvWriter.append("X");
        csvWriter.append(",");
        csvWriter.append("Y");
        csvWriter.append("\n");

        // Read original data csv
        BufferedReader csvReader = new BufferedReader(new FileReader("/Users/luchengqi/Documents/MATSimScenarios/Kelheim/KEXI_Haltestellen_Liste_Kelheim_utm32n.csv"));
        csvReader.readLine();
        while (true) {
            String stopEntry = csvReader.readLine();
            if (stopEntry == null) {
                break;
            }
            String[] stopData = stopEntry.split(";");
            // write stop
            Coord coord = new Coord(Double.parseDouble(stopData[2]), Double.parseDouble(stopData[3]));
            List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>(5);
            attributes.add(createTuple("id", stopData[0]));
            attributes.add(createTuple("x", stopData[2]));
            attributes.add(createTuple("y", stopData[3]));
            Link link = NetworkUtils.getNearestLink(network,coord);
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
        csvWriter.close();
    }
}
