package org.matsim.run.prepare;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CommandLine.Command(
        name = "network",
        description = "Add network allowed mode for DRT and AV"
)
public class PrepareNetwork implements MATSimAppCommand {
    @CommandLine.Option(names = "--network", description = "Path to network file", required = true)
    private String networkFile;

    @CommandLine.Mixin
    private ShpOptions shp = new ShpOptions();

    @CommandLine.Option(names = "--output", description = "Output path of the prepared network", required = true)
    private String outputPath;

    public static void main(String[] args) {
        new PrepareNetwork().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        Geometry drtOperationArea = null;
        Geometry avOperationArea = null;
        List<SimpleFeature> features = shp.readFeatures();
        for (SimpleFeature feature : features) {
            if (feature.getAttribute("mode").equals("drt")) {
                if (drtOperationArea == null) {
                    drtOperationArea = (Geometry) feature.getDefaultGeometry();
                } else {
                    drtOperationArea.union((Geometry) feature.getDefaultGeometry());
                }
            }

            if (feature.getAttribute("mode").equals("av")) {
                if (avOperationArea == null) {
                    avOperationArea = (Geometry) feature.getDefaultGeometry();
                } else {
                    avOperationArea.union((Geometry) feature.getDefaultGeometry());
                }
            }
        }

        Network network = NetworkUtils.readNetwork(networkFile);
        for (Link link : network.getLinks().values()) {
            if (!link.getAllowedModes().contains("car")){
                continue;
            }
            boolean isDrtAllowed = MGC.coord2Point(link.getFromNode().getCoord()).within(drtOperationArea) ||
                    MGC.coord2Point(link.getToNode().getCoord()).within(drtOperationArea);
            boolean isAvAllowed = MGC.coord2Point(link.getFromNode().getCoord()).within(avOperationArea) ||
                    MGC.coord2Point(link.getToNode().getCoord()).within(avOperationArea);

            if (isDrtAllowed) {
                Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
                allowedModes.add("drt");
                link.setAllowedModes(allowedModes);
            }

            if (isAvAllowed) {
                Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
                allowedModes.add("av");
                link.setAllowedModes(allowedModes);
            }
        }

        NetworkUtils.writeNetwork(network, outputPath);
        return 0;
    }
}
