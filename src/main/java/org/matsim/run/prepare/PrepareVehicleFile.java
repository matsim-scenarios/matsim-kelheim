package org.matsim.run.prepare;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PrepareVehicleFile implements MATSimAppCommand {
	@CommandLine.Option(names = "--output", description = "path to output vehicles file", required = true)
	private String outputFiles;

	@CommandLine.Option(names = "--network", description = "path to network file file", required = true)
	private String networkPath;

	@CommandLine.Option(names = "--fleet-size", description = "number of vehicles to generate", defaultValue = "1000")
	private int fleetSize;

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	public static void main(String[] args) {
		new PrepareVehicleFile().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		Network network = NetworkUtils.readNetwork(networkPath);
		List<Link> startLocations = new ArrayList<>();
		if (shp.isDefined()) {
			Geometry serviceArea = shp.getGeometry();
			startLocations.addAll(network.getLinks().values().stream().
				filter(l -> l.getAllowedModes().contains(TransportMode.drt)).
				filter(l -> serviceArea.contains(MGC.coord2Point(l.getToNode().getCoord()))).toList());
		} else {
			startLocations.addAll(network.getLinks().values().stream().filter(l -> l.getAllowedModes().contains(TransportMode.drt)).toList());
		}
		int numOfLinks = startLocations.size();
		Random random = new Random();

		// Prepare vehicle files
		VehicleType carVehicleType = VehicleUtils.createVehicleType(Id.create("car", VehicleType.class));

		VehicleType rideVehicleType = VehicleUtils.createVehicleType(Id.create("ride", VehicleType.class));

		VehicleType freightVehicleType = VehicleUtils.createVehicleType(Id.create("freight", VehicleType.class));
		freightVehicleType.setLength(15);
		freightVehicleType.setPcuEquivalents(3.5);

		VehicleType drtVehicleType = VehicleUtils.createVehicleType(Id.create("conventional_vehicle", VehicleType.class));
		drtVehicleType.setDescription("Conventional DRT");
		drtVehicleType.getCapacity().setSeats(8);

		VehicleType avVehicleType = VehicleUtils.createVehicleType(Id.create("autonomous_vehicle", VehicleType.class));
		avVehicleType.setDescription("Autonomous DRT");
		avVehicleType.getCapacity().setSeats(6);
		avVehicleType.setMaximumVelocity(5);

		Vehicles vehicles = new VehiclesImpl();
		vehicles.addVehicleType(carVehicleType);
		vehicles.addVehicleType(rideVehicleType);
		vehicles.addVehicleType(freightVehicleType);
		vehicles.addVehicleType(drtVehicleType);
		vehicles.addVehicleType(avVehicleType);

		for (int i = 1; i <= fleetSize; i++) {
			Link startLink = startLocations.get(random.nextInt(numOfLinks));

			Vehicle drtVehicle = VehicleUtils.createVehicle(Id.createVehicleId("KEXI-" + i), drtVehicleType);
			drtVehicle.getAttributes().putAttribute("dvrpMode", TransportMode.drt);
			drtVehicle.getAttributes().putAttribute("startLink", startLink.getId().toString());
			drtVehicle.getAttributes().putAttribute("serviceBeginTime", 21600.0);
			drtVehicle.getAttributes().putAttribute("serviceEndTime", 82800.0);

			vehicles.addVehicle(drtVehicle);
		}

		// Future extension: add autonomous vehicles.

		VehicleUtils.writeVehicles(vehicles, outputFiles);
		return 0;
	}
}
