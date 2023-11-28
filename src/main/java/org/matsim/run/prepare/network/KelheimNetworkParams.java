package org.matsim.run.prepare.network;

import org.matsim.application.prepare.network.opt.FeatureRegressor;
import org.matsim.application.prepare.network.opt.NetworkModel;

public class KelheimNetworkParams implements NetworkModel {
	@Override
	public FeatureRegressor capacity(String junctionType) {
		return switch (junctionType) {
			case "traffic_light" -> KelheimNetworkParams_capacity_traffic_light.INSTANCE;
			case "right_before_left" -> KelheimNetworkParams_capacity_right_before_left.INSTANCE;
			case "priority" -> KelheimNetworkParams_capacity_priority.INSTANCE;
			default -> throw new IllegalArgumentException("Unknown type: " + junctionType);
		};
	}

	@Override
	public FeatureRegressor speedFactor(String junctionType) {
		return switch (junctionType) {
			case "traffic_light" -> KelheimNetworkParams_speedRelative_traffic_light.INSTANCE;
			case "right_before_left" -> KelheimNetworkParams_speedRelative_right_before_left.INSTANCE;
			case "priority" -> KelheimNetworkParams_speedRelative_priority.INSTANCE;
			default -> throw new IllegalArgumentException("Unknown type: " + junctionType);
		};
	}
}
