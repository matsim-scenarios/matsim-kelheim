package org.matsim.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;

public class RemoveHighway {
	public static void main(String[] args) {

		var network = NetworkUtils.readNetwork("C:/Users/charl/Documents/TUB/SS24/MATSim/seminar/matsim-kelheim/kelheim-v3.0-1pct.output_network.xml.gz");
		for (Link link : network.getLinks().values()) {
			if(link.getId().equals(Id.createLinkId("322183347")) || link.getId().equals(Id.createLinkId("322186089"))){
			link.setFreespeed(10.);
			}
		}
		NetworkUtils.writeNetwork(network,"C:/Users/charl/Documents/TUB/SS24/MATSim/seminar/matsim-kelheim/output.xml.gz" );
	}
}
