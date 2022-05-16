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

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.utils.geometry.GeometryUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PrepareNetworkForKelfleet {

	public static void main(String[] args) {

		String inputNetwork = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/kelheim-v2.0/input/kelheim-v2.0-network-with-pt.xml.gz";
		String outputNetwork = "D:/KelFleet/sektor9/kelheim-v2.0-network-av-sektor9.xml.gz";
		String inputArea = "D:/KelFleet/sektor9/kelheim-kelfleet-sektor9.shp";

		Network network = NetworkUtils.readNetwork(inputNetwork);

		//first delete drt and av mode from all links
		for (Link link : network.getLinks().values()) {
			Set<String> allowedModes = link.getAllowedModes();
			Set<String> newModes = new HashSet<>();
			for (String allowedMode : allowedModes) {
				if( ! (allowedMode.equals("drt") || allowedMode.equals("av")) ){
					newModes.add(allowedMode);
				}
			}
			link.setAllowedModes(newModes);
		}

		//now add av to links in area
		List<PreparedGeometry> geoms = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(inputArea));
		for (Link link : network.getLinks().values()) {
			Object type = link.getAttributes().getAttribute("type");
			if(link.getAllowedModes().contains(TransportMode.car) && (type == null || !((String) type).contains("motorway") )){
				if(ShpGeometryUtils.isCoordInPreparedGeometries(link.getFromNode().getCoord(), geoms) ||
						ShpGeometryUtils.isCoordInPreparedGeometries(link.getToNode().getCoord(), geoms)){
					Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
					allowedModes.add("av");
					link.setAllowedModes(allowedModes);
				}
			}
		}
		new MultimodalNetworkCleaner(network).run(Set.of("av"));
		NetworkUtils.writeNetwork(network, outputNetwork);

	}


}
