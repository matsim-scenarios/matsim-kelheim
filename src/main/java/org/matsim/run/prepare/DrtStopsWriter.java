package org.matsim.run.prepare;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedReader;
import java.io.FileReader;
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
        // Read original data csv
        BufferedReader csvReader = new BufferedReader(new FileReader("/Users/luchengqi/Downloads/KEXI_Haltestellen_Liste_Kelheim_utm32n.csv"));
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
            attributes.add(createTuple("linkRefId", NetworkUtils.getNearestLink(network,coord).getId().toString()));
            this.writeStartTag("stopFacility", attributes, true);
        }
    }
}
