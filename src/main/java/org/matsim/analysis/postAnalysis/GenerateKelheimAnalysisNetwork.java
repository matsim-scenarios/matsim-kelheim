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

package org.matsim.analysis.postAnalysis;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.zone.ZonalSystems;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.prepare.PrepareNetwork;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import picocli.CommandLine;

import java.util.HashSet;
import java.util.Set;

public class GenerateKelheimAnalysisNetwork implements MATSimAppCommand {

	@CommandLine.Option(names = "--network", description = "Path to network file", required = true)
	private String networkFile;

	@CommandLine.Option(names = "--shape-file", description = "Path to shape file for filtering")
	private String shapeFile;

	@CommandLine.Option(names = "--output", description = "Output path of the prepared network", required = true)
	private String outputPath;

	@CommandLine.Option(names = "--with-pt", description = "Include PT links in output network", defaultValue = "false")
	private String includePT;

	public static void main(String[] args) {
		new GenerateKelheimAnalysisNetwork().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Network network = NetworkUtils.readNetwork(networkFile);

		Set<Node> nodesWithinArea = new HashSet<>(
				ZonalSystems.selectNodesWithinArea(network.getNodes().values(), ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(shapeFile))));

		NetworkFilterManager networkFilterManager = new NetworkFilterManager(network, new NetworkConfigGroup());
		networkFilterManager.addLinkFilter(
				l -> nodesWithinArea.contains(l.getFromNode()) || nodesWithinArea.contains(l.getToNode()));

		if(!Boolean.parseBoolean(includePT)){
			networkFilterManager.addLinkFilter(
					l -> !l.getAllowedModes().contains(TransportMode.pt));
		}

		Network filteredNetwork = networkFilterManager.applyFilters();

		NetworkUtils.writeNetwork(filteredNetwork, outputPath);

		return 0;
	}
}
