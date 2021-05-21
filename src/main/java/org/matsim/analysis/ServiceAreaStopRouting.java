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
import org.matsim.contrib.freight.Freight;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
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
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public class ServiceAreaStopRouting {



	private static final String INPUT_STOPS_FILE = "D:/svn/shared-svn/projects/KelRide/data/KEXI/KEXI_Haltestellen_Liste_Kelheim_utm32n.csv";
	private static final String INPUT_NETWORK = "D:/svn/shared-svn/projects/matsim-kelheim/input/kelheim-v1.0-network.xml.gz";
	/**
	 * shape file with multiple polygons each of which represents a possible service area
	 */
	private static final String INPUT_SERVICE_AREAS_SHAPE = "D:/svn/shared-svn/projects/KelRide/data/ServiceAreas/2021_05_possibleServiceAreasForAutomatedVehicles.shp";

	public static void main(String[] args) {
//		convertStopCoordinates(); already converted if you read in file ending on 'utm32'

		Collection<Stop> stops = readStops(INPUT_STOPS_FILE);
		Network network = NetworkUtils.readNetwork(INPUT_NETWORK);

		//read in service area map
		PreparedGeometryFactory factory = new PreparedGeometryFactory();
		Map<String, PreparedGeometry> serviceAreas = StreamEx.of(ShapeFileReader.getAllFeatures(IOUtils.getFileUrl(INPUT_SERVICE_AREAS_SHAPE)))
				.mapToEntry(sf -> (String) sf.getAttribute("NAME"), sf -> factory.create((Geometry) sf.getDefaultGeometry()))
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

		//map service area geometry to a collection of stops inside of it
		Map<PreparedGeometry, Collection<Stop>> area2Stops = StreamEx.of(serviceAreas.values())
				.mapToEntry(a -> a, a -> getAllStopsInArea(a,stops))
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));



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

//		Controler controler = new Controler(scenario);
//		Freight.configure(controler);
//		controler.run();

		carriers.getCarriers().values().forEach(carrier -> printTotalCoveredDistance(carrier, network));
	}

	private static void printTotalCoveredDistance(Carrier carrier, Network network) {

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

		System.out.println("A tour to all stops in " + carrier.getId() + " covers " + coveredDistance + " m of road");
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
			depotLink = Id.createLinkId("376292334#1");
		} else if(areaName.contains("Altstadt")) {
			depotLink = Id.createLinkId("-131546911");
		} else {
			depotLink = Id.createLinkId("26526533#1");
		}
		Link l = network.getLinks().get(depotLink);
		CarrierVehicle.Builder vBuilder = CarrierVehicle.Builder.newInstance(Id.create((areaName + "_shuttle"), Vehicle.class), depotLink);
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


	private static Collection<Stop> getAllStopsInArea(PreparedGeometry a, Collection<Stop> stops) {
		return stops.stream()
				.filter(stop -> {
					Point point = MGC.coord2Point(stop.coord);
					return a.contains(point);
				})
				.collect(Collectors.toList());
	}

	private static Collection<Stop> readStops(String inputStopsFile) {
		BufferedReader reader = IOUtils.getBufferedReader(inputStopsFile);
		List<Stop> stops = new ArrayList<>();
		try {
			String[] header = reader.readLine().split(";");
			String line = reader.readLine();

			while (line != null) {
				String[] lineArr = line.split(";");
				Coord c = new Coord(Double.parseDouble(lineArr[2]), Double.parseDouble(lineArr[3]));
				//transform coord

				stops.add(new Stop(Integer.parseInt(lineArr[0]), lineArr[1], c));
				line = reader.readLine();
			}
		} catch (IOException e){
			e.printStackTrace();
		}
		return stops;
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
		int id;
		String lage;
		Coord coord;

		Stop(int id, String lage, Coord coord) {
			this.id = id;
			this.lage = lage;
			this.coord = coord;
		}
	}

}
