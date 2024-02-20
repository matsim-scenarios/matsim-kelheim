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

package org.matsim.drt;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;

import java.util.Map;
import java.util.stream.Collectors;

public class StartLinkAsDepot implements DepotFinder {

	private Map<DvrpVehicle, Link> vehicleToStartLink;

	public StartLinkAsDepot(Fleet fleet) {
		this.vehicleToStartLink = fleet.getVehicles()
			.values()
			.stream()
			.collect(Collectors.toUnmodifiableMap(v -> v, DvrpVehicle::getStartLink));
	}
	@Override
	public Link findDepot(DvrpVehicle vehicle) {
		return vehicleToStartLink.get(vehicle);
	}

}
