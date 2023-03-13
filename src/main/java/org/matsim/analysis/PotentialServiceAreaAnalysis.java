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

package org.matsim.analysis;

import one.util.streamex.StreamEx;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public class PotentialServiceAreaAnalysis {



	private static final String INPUT_STOPS_FILE = "C:/Users/Simon/Documents/shared-svn/projects/KelRide/data/KEXI/Haltestellen/KEXI_Haltestellen_Liste_Kelheim_utm32n.csv";
	private static final String INPUT_DEMAND_FILE = "C:/Users/Simon/Documents/shared-svn/projects/KelRide/data/KEXI/IOKI_Rides_202006_202105.csv";
	private static final String INPUT_NETWORK = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/kelheim-v2.0/input/kelheim-v2.0-network-with-pt.xml.gz";
	/**
	 * shape file with multiple polygons each of which represents a possible service area
	 */
	private static final String INPUT_SERVICE_AREAS_SHAPE = "C:/Users/Simon/Desktop/wd/2022-10-18/2022-10_possibleAreasForAV.shp";



	public static void main(String[] args) {
//		convertStopCoordinates(); already converted if you read in file ending on 'utm32'


		Map<Id<Stop>, Stop> stops = readStops();
		Map<Tuple<Id<Stop>, Id<Stop>>, Integer> relations = readDemandAndGetRelations(stops);

		Network fullNetwork = NetworkUtils.readNetwork(INPUT_NETWORK);

		//filter out pt links
		TransportModeNetworkFilter networkFilter = new TransportModeNetworkFilter(fullNetwork);
		Network network = NetworkUtils.createNetwork();

		HashSet<String> modes = new HashSet<String>();
		modes.add(TransportMode.car);
		networkFilter.filter(network, modes);


		//read in service area map
		PreparedGeometryFactory factory = new PreparedGeometryFactory();
		Map<String, PreparedGeometry> serviceAreas = StreamEx.of(ShapeFileReader.getAllFeatures(IOUtils.getFileUrl(INPUT_SERVICE_AREAS_SHAPE)))
				.mapToEntry(sf -> (String) sf.getAttribute("name"), sf -> factory.create((Geometry) sf.getDefaultGeometry()))
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

		//map service area geometry to a collection of stops inside of it
		Map<PreparedGeometry, Collection<Stop>> area2Stops = StreamEx.of(serviceAreas.values())
				.mapToEntry(a -> a, a -> getAllStopsInArea(a,stops))
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

		//calculate round tours within each geometry (serving each stop once)
		Carriers carriers = getCarriersWithPlannedRoundTours(network, serviceAreas, area2Stops);

		// run the sim such that you can visualize the round tour
//		Controler controler = new Controler(scenario);
//		Freight.configure(controler);
//		controler.run();

		//calculate round tours
		carriers.getCarriers().values().forEach(carrier -> getTotalCoveredDistanceOfCarrierTours(carrier, network));

		//produce and dump output
		writeStats(network, serviceAreas, area2Stops, carriers, relations);
	}

	private static void writeStats(Network network, Map<String, PreparedGeometry> serviceAreas, Map<PreparedGeometry, Collection<Stop>> area2Stops, Carriers carriers, Map<Tuple<Id<Stop>, Id<Stop>>, Integer> relations) {
		String outputFileName = INPUT_SERVICE_AREAS_SHAPE.substring(0, INPUT_SERVICE_AREAS_SHAPE.lastIndexOf(".")) + "_stats.csv";
		LeastCostPathCalculator router = new FastAStarLandmarksFactory(4).createPathCalculator(network, new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				return link.getLength();
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return link.getLength();
			}
		}, new FreeSpeedTravelTime());

		try {
			System.out.println("will try to write to " + outputFileName);
			BufferedWriter writer = IOUtils.getBufferedWriter(outputFileName);
			writer.write("areaName;area[sqm];nrStops[1];totalRoadMeter[m];roundTourDistance[m];longestDistBetw2Stops[m];totalOriginatingTrips[1];totalEndingTrips[1];totalTripsWithin[1]");
			for (Map.Entry<String, PreparedGeometry> entry : serviceAreas.entrySet()) {
				String name = entry.getKey();
				PreparedGeometry geom = entry.getValue();
				Collection<Stop> areaStops = area2Stops.get(geom);
				double area = geom.getGeometry().getArea();
				double totalCarNetworkMeter = getTotalNetworkCarKMInsideGeom(geom, network);
				double roundTourMeter = getTotalCoveredDistanceOfCarrierTours(carriers.getCarriers().get(Id.create(name, Carrier.class)), network);
				double longestRouteBetween2Stops = getLongestRouteDistanceBetweenStops(areaStops, network, router);
				int totalOriginatingTrips = areaStops.stream()
						.mapToInt(stop -> stop.originatingTrips)
						.sum();
				int totalEndingTrips = areaStops.stream()
						.mapToInt(stop -> stop.endingTrips)
						.sum();
				int totalTripsWithin = getTotalTripsWithin(areaStops, relations);
				System.out.println(name + "\t" + totalCarNetworkMeter + "\t" + roundTourMeter + "\t" + longestRouteBetween2Stops + "\t" + totalOriginatingTrips + "\t" + totalEndingTrips);
				writer.newLine();
				writer.write(name + ";" + area + ";" + areaStops.size() + ";"
						+ totalCarNetworkMeter + ";" + roundTourMeter + ";" + longestRouteBetween2Stops
						+ ";" + totalOriginatingTrips + ";" + totalEndingTrips + ";" + totalTripsWithin);
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static int getTotalTripsWithin(Collection<Stop> areaStops, Map<Tuple<Id<Stop>, Id<Stop>>, Integer> relations) {
		int cnt = 0;
		for (Stop stop : areaStops) {
			ArrayList<Stop> otherStops = new ArrayList<>(areaStops);
			otherStops.remove(stop);
			for(Stop otherStop : otherStops){
				Integer add = relations.get(new Tuple<>(stop.id, otherStop.id));
				cnt = add == null ? cnt : cnt + add;
			}
		}
		return cnt;
	}

	/**
	 * route from every stop to every other stop. return the longest route distance between two stops
	 *
	 * @param stops
	 * @param network
	 * @param router
	 * @return
	 */
	private static double getLongestRouteDistanceBetweenStops(Collection<Stop> stops, Network network, LeastCostPathCalculator router){
		double longestDistance = Double.NEGATIVE_INFINITY;
		for (Stop stop : stops) {
			ArrayList<Stop> otherStops = new ArrayList<>(stops);
			otherStops.remove(stop);
			Node stopNode = NetworkUtils.getNearestNode(network, stop.coord);
			for (Stop otherStop : otherStops){
				//link travel disutility was set to link length
				double distance = router.calcLeastCostPath(stopNode, NetworkUtils.getNearestNode(network, otherStop.coord), 0, null, null).travelCost;
				longestDistance = distance > longestDistance ? distance : longestDistance;
			}
		}
		return longestDistance;
	}

	/**
	 * Creates a {@code Carrier} object per entry in {@code serviceAreas} that has to serve each stop in the corresponding geometry once.
	 * Then runs jsprit to solve the VRP (thus, we create a round tour) and returns the Carriers container object.
	 *
	 * @param network
	 * @param serviceAreas
	 * @param area2Stops
	 * @return
	 */
	private static Carriers getCarriersWithPlannedRoundTours(Network network, Map<String, PreparedGeometry> serviceAreas, Map<PreparedGeometry, Collection<Stop>> area2Stops) {
		Config config = ConfigUtils.createConfig();
		config.controler().setLastIteration(0);
		config.network().setInputFile(INPUT_NETWORK);
		config.controler().setOutputDirectory("D:/KelRide-test/testServiceAreaTours/");
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//container
		Carriers carriers = FreightUtils.addOrGetCarriers(scenario);
		VehicleType type = createVehicleType();
		FreightUtils.getCarrierVehicleTypes(scenario).getVehicleTypes().put(type.getId(), type);

		//iterate over service areas and create carrier
		serviceAreas.forEach((areaName, geom) -> carriers.addCarrier(buildCarrier(areaName, area2Stops.get(geom), network, type)));

		try {
			FreightUtils.runJsprit(scenario);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return carriers;
	}

	private static double getTotalCoveredDistanceOfCarrierTours(Carrier carrier, Network network) {

		Set<Id<Link>> coveredLinks = new HashSet<>();
		double coveredDistance = 0.;

		for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
			coveredLinks.add(scheduledTour.getTour().getStartLinkId());
			coveredDistance += network.getLinks().get(scheduledTour.getTour().getStartLinkId()).getLength();

			List<Tour.TourElement> legs = scheduledTour.getTour().getTourElements().stream()
					.filter(tourElement -> tourElement instanceof Tour.Leg)
					.collect(Collectors.toList());

			for (Tour.TourElement leg : legs) {
				NetworkRoute route = ((NetworkRoute) ((Tour.Leg) leg).getRoute());
				for (Id<Link> linkId : route.getLinkIds()) {
					if(!coveredLinks.contains(linkId)){
						coveredLinks.add(linkId);
						Link link = network.getLinks().get(linkId);
						//fuer die Vermessung zaehlt die Fahrrichtung...
//						Link opposite = NetworkUtils.findLinkInOppositeDirection(link);
//						if(opposite != null) coveredLinks.add(opposite.getId());
						coveredDistance += link.getLength();
					}
				}
			}
		}

//		System.out.println("A tour to all stops in " + carrier.getId() + " covers " + coveredDistance + " m of road");
		return coveredDistance;
	}

	private static double getTotalNetworkCarKMInsideGeom(PreparedGeometry geom, Network network){
		return network.getLinks().values().stream()
				.filter(link -> link.getAllowedModes().contains(TransportMode.car))
				.filter(l -> isLinkInsideGeom(l, geom))
				.mapToDouble(l -> l.getLength())
				.sum();
	}


	private static boolean isLinkInsideGeom(Link l, PreparedGeometry geom){
		return geom.contains(MGC.coord2Point(l.getFromNode().getCoord())) && geom.contains(MGC.coord2Point(l.getToNode().getCoord()));
	}

	private static Carrier buildCarrier(String areaName, Collection<Stop> stops, Network network, VehicleType vehicleType) {
		//carrier
		Carrier carrier = CarrierUtils.createCarrier(Id.create(areaName, Carrier.class));
		CarrierUtils.setCarrierMode(carrier, TransportMode.car);
		CarrierUtils.setJspritIterations(carrier, 10000);
		carrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.FINITE); //we will specify one vehicle and only want one tour
		carrier.getCarrierCapabilities().getVehicleTypes().add(vehicleType);

		//CarrierVehicle
		Id<Link> depotLink;
		if(areaName.contains("Donaupark")){
			depotLink = Id.createLinkId("485579462#0");
		} else if(areaName.contains("Altstadt")) {
			depotLink = Id.createLinkId("-96590898");
		} else {
			depotLink = Id.createLinkId("485579462#0");
		}
		Link l = network.getLinks().get(depotLink);
		CarrierVehicle.Builder vBuilder = CarrierVehicle.Builder.newInstance(Id.create((areaName + "_shuttle"), Vehicle.class), depotLink,vehicleType);
		vBuilder.setEarliestStart(0 * 60 * 60);
		vBuilder.setLatestEnd(24 * 60 * 60);
		vBuilder.setType(vehicleType);
		vBuilder.setTypeId(vehicleType.getId());
		CarrierVehicle vehicle = vBuilder.build();
		carrier.getCarrierCapabilities().getCarrierVehicles().put(vehicle.getId(), vehicle);

		stops.forEach(stop -> {
			CarrierService service = CarrierService.Builder.newInstance(Id.create(stop.id, CarrierService.class), NetworkUtils.getNearestLinkExactly(network, stop.coord).getId())
					.setName(stop.lage)
					.build();
			carrier.getServices().put(service.getId(), service);
		});

		return carrier;
	}

	private static VehicleType createVehicleType() {
		//VehicleType
		VehicleType type = VehicleUtils.createVehicleType(Id.create("shuttle", VehicleType.class));
		type.getCapacity().setOther(10000); //very large capacity such that all stops are served
		//we want a distance-optimal tour, so only specify distance costs
		type.getCostInformation().setFixedCost(0.);
		type.getCostInformation().setCostsPerMeter(1000.);
		type.getCostInformation().setCostsPerSecond(0.00);
		return type;
	}


	private static Collection<Stop> getAllStopsInArea(PreparedGeometry a, Map<Id<Stop>,Stop> stops) {
		return stops.values().stream()
				.filter(stop -> {
					Point point = MGC.coord2Point(stop.coord);
					return a.contains(point);
				})
				.collect(Collectors.toList());
	}

	private static Map<Id<Stop>, Stop> readStops() {
		BufferedReader stopsReader = IOUtils.getBufferedReader(INPUT_STOPS_FILE);
		Map<Id<Stop>,Stop> stops = new HashMap<>();
		try {
			//read stops first
			String[] header = stopsReader.readLine().split(";");
			String line = stopsReader.readLine();

			while (line != null) {
				String[] lineArr = line.split(";");
				Coord c = new Coord(Double.parseDouble(lineArr[2]), Double.parseDouble(lineArr[3]));
				//transform coord

				Id<Stop> id = Id.create(Integer.parseInt(lineArr[0]), Stop.class);
				stops.put(id, new Stop(id, lineArr[1], c));
				line = stopsReader.readLine();
			}

		} catch (IOException e){
			e.printStackTrace();
		}
		return stops;
	}

	private static Map<Tuple<Id<Stop>,Id<Stop>>,Integer> readDemandAndGetRelations(Map<Id<Stop>,Stop> stops){
		BufferedReader demandReader = IOUtils.getBufferedReader(INPUT_DEMAND_FILE);
		Map<Tuple<Id<Stop>,Id<Stop>>,Integer> relations = new HashMap<>();
		try {
			String[] header = demandReader.readLine().split(";");
			String line = demandReader.readLine();

			while (line != null) {
				String[] lineArr = line.split(";");
				Id<Stop> from = Id.create(Integer.parseInt(lineArr[14]), Stop.class);
				Id<Stop> to = Id.create(Integer.parseInt(lineArr[19]), Stop.class);
				stops.get(from).originatingTrips += 1;
				stops.get(to).endingTrips += 1;
				relations.compute(new Tuple<>(from, to), (k,v) -> v == null ? 1 : v+1);

				line = demandReader.readLine();
			}

		} catch (IOException e){
			e.printStackTrace();
		}
		return relations;
	}

	private static void convertStopCoordinates() {
		BufferedReader reader = IOUtils.getBufferedReader(INPUT_STOPS_FILE);

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "EPSG:25832");

		BufferedWriter writer = IOUtils.getBufferedWriter(INPUT_STOPS_FILE.substring(0, INPUT_STOPS_FILE.lastIndexOf(".")) + "_utm32n.csv");
		try {
			String[] header = reader.readLine().split(";");
			String line = reader.readLine();

			writer.write("Haltestellen-Nr.;Lage;x;y");

			while(line != null){
				String[] lineArr = line.split(";");
				Coord c = new Coord(Double.parseDouble(lineArr[4]),Double.parseDouble(lineArr[3]));
				//transform coord
				c = ct.transform(c);

				writer.newLine();
				writer.write(lineArr[0] + ";" + lineArr[1] + ";" + c.getX() + ";" + c.getY());
				line = reader.readLine();
			}

			reader.close();
			writer.close();


		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static class Stop{
		Id<Stop> id;
		String lage;
		Coord coord;

		int originatingTrips = 0;
		int endingTrips = 0;

		Stop(Id<Stop> id, String lage, Coord coord) {
			this.id = id;
			this.lage = lage;
			this.coord = coord;
		}
	}

}
