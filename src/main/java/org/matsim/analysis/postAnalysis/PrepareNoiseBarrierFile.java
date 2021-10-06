package org.matsim.analysis.postAnalysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.GeometryCollector;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import picocli.CommandLine;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@CommandLine.Command(
        name = "TODO",
        description = "TODO"
)
public class PrepareNoiseBarrierFile implements MATSimAppCommand {
    @CommandLine.Option(names = "--noise-barrier", description = "Noise barrier file", required = true)
    private String noiseBarrierFile;

    @CommandLine.Option(names = "--network", description = "Input network file", required = true)
    private String networkFile;

    @CommandLine.Option(names = "--output", description = "Output path for the geoJson file", required = true)
    private Path outputPath;


    @Override
    public Integer call() throws Exception {
        Network network = NetworkUtils.readNetwork(networkFile);
        GeometryFactory geometryFactory = new GeometryFactory();
        GeometryCollector collector = new GeometryCollector();

        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
        SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
        featureTypeBuilder.setName("soundBarriers");
        featureTypeBuilder.add("geometry", Geometry.class);
        final SimpleFeatureType featureType = featureTypeBuilder.buildFeatureType();

        int counter = 0;
        try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(noiseBarrierFile)),
                CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                if (record.get(2).contains("Lärm") || record.get(2).contains("tunnel")) {
                    if (record.get(9).isBlank() || record.get(10).isBlank()) {
                        continue;
                    }
                    double x = Double.parseDouble(record.get(9).replace(",", "."));
                    double y = Double.parseDouble(record.get(10).replace(",", "."));
                    if (x == 0 || y == 0) {
                        continue;
                    }

                    Coord noiseBarrierCoord = new Coord(x, y);
                    Link noiseBarrierLink = NetworkUtils.getNearestLink(network, noiseBarrierCoord);

                    Coord coord1 = noiseBarrierLink.getFromNode().getCoord();
                    Coord coord2 = noiseBarrierLink.getToNode().getCoord();

                    Coordinate[] coordinates = new Coordinate[]{MGC.coord2Coordinate(coord1), MGC.coord2Coordinate(coord2)};
                    Geometry line = geometryFactory.createLineString(coordinates);
                    Geometry polygon = line.buffer(5);
                    collector.add(polygon);

                    SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
                    featureBuilder.add(polygon);
                    SimpleFeature feature = featureBuilder.buildFeature("noise_barrier_" + counter);
                    featureCollection.add(feature);
                    counter++;
                }
            }
        }

        // Write json file. For some reason, the reader cannot read gzip file properly. So only use .json or .geojson as the ending of the output path!
        FeatureJSON featureJSON = new FeatureJSON();
        if (!Files.exists(outputPath.getParent()))
            Files.createDirectories(outputPath.getParent());

        try (OutputStream outputStream = IOUtils.getOutputStream(outputPath.toFile().toURI().toURL(), false)) {
            featureJSON.writeFeatureCollection(featureCollection, outputStream);
        }
        return 0;
    }

    public static void main(String[] args) {
        new PrepareNoiseBarrierFile().execute(args);
    }

}
