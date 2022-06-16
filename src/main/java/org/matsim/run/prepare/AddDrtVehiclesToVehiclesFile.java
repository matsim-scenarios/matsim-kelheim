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

package org.matsim.run.prepare;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.*;

public class AddDrtVehiclesToVehiclesFile {

	public static void main(String[] args) {

		int nrOfVehiclesToAdd = 100;
		String inputFile = "D:/KelFleet/kelheim-v2.0-vehicleTypes.xml";
		String outputFile = "D:/svn/shared-svn/projects/matsim-kelheim/data/sektor3/kelheim-v2.0-sektor3u5-" + nrOfVehiclesToAdd + "-vehicles.xml";

		String dvrpMode = "av";
		Id<VehicleType> vehicleTypeId = Id.create("autonomous_vehicle", VehicleType.class); //vehicle type must exist in input file already
		String startLinkId = "485579462#0";
		double serviceStart = 6 * 3600;
		double serviceEnd = 22 * 3600;

		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		MatsimVehicleReader reader = new MatsimVehicleReader(vehicles);

		reader.readFile(inputFile);

		VehicleType type = vehicles.getVehicleTypes().get(vehicleTypeId);

		for (int i = 1; i <= nrOfVehiclesToAdd; i++) {
			Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId(dvrpMode + "-" + i), type);
			vehicle.getAttributes().putAttribute("dvrpMode", dvrpMode);
			vehicle.getAttributes().putAttribute("startLink", startLinkId);
			vehicle.getAttributes().putAttribute("serviceBeginTime", serviceStart);
			vehicle.getAttributes().putAttribute("serviceEndTime", serviceEnd);
			vehicles.addVehicle(vehicle);
		}

		new MatsimVehicleWriter(vehicles).writeFile(outputFile);
	}
}
