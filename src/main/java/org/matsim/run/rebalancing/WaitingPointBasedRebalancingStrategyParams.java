/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.run.rebalancing;

import jakarta.validation.constraints.NotNull;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author Chengqi Lu
 * // TODO this is not needed for Kelheim scenario. I will move it to the matsim-lib after eeverything works well here.
 */
public final class WaitingPointBasedRebalancingStrategyParams extends ReflectiveConfigGroup
		implements RebalancingParams.RebalancingStrategyParams {
	public static final String SET_NAME = "waitingPointBasedRebalancingStrategy";

	@Parameter("waiting points of the vehicle")
	@Comment("The path to the waiting point file (csv/tsv) can be specified here. title row of the file: link_id	capacity" +
		"If unspecified (i.e., empty string by default), starting points of the fleet will be used as the waiting points")
	@NotNull
	public String waitingPointPath = "";

	public WaitingPointBasedRebalancingStrategyParams() {
		super(SET_NAME);
	}
}
