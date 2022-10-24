package org.matsim.run.prepare;

import org.apache.log4j.Logger;
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

    private static final Logger log = Logger.getLogger(PrepareNetwork.class);

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
                    drtOperationArea = drtOperationArea.union((Geometry) feature.getDefaultGeometry());
                }
            }

            System.out.println(feature.getAttributes());

            if (feature.getAttribute("mode").equals("av")) {
                if (avOperationArea == null) {
                    avOperationArea = (Geometry) feature.getDefaultGeometry();
                } else {
                    avOperationArea = avOperationArea.union((Geometry) feature.getDefaultGeometry());
                }
            }
        }

        boolean isDrtAllowed;
        boolean isAvAllowed;
        int linkCount[] = new int[2];

        Network network = NetworkUtils.readNetwork(networkFile);
        for (Link link : network.getLinks().values()) {
            if (!link.getAllowedModes().contains("car")){
                continue;
            }

            if(drtOperationArea != null) {
                isDrtAllowed = MGC.coord2Point(link.getFromNode().getCoord()).within(drtOperationArea) ||
                        MGC.coord2Point(link.getToNode().getCoord()).within(drtOperationArea);
            } else {
                isDrtAllowed = false;
            }

            if(avOperationArea != null) {
                isAvAllowed = MGC.coord2Point(link.getFromNode().getCoord()).within(avOperationArea) ||
                        MGC.coord2Point(link.getToNode().getCoord()).within(avOperationArea);
            } else {
                isAvAllowed = false;
            }

            if (isDrtAllowed) {
                Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
                allowedModes.add("drt");
                link.setAllowedModes(allowedModes);
                linkCount[0] = linkCount[0] + 1;
            }

            if (isAvAllowed) {
                Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
                allowedModes.add("av");
                link.setAllowedModes(allowedModes);
                linkCount[1] = linkCount[1] + 1;
            }
        }

        NetworkUtils.writeNetwork(network, outputPath);
        log.info("For " + linkCount[0] + " links drt has been added as an allowed mode.");
        log.info("For " + linkCount[1] + " links av has been added as an allowed mode.");
        return 0;
    }
}
