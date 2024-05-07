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
 * @author michalm
 */
public final class WaitingPointBasedRebalancingStrategyParams extends ReflectiveConfigGroup
		implements RebalancingParams.RebalancingStrategyParams {
	public static final String SET_NAME = "waitingPointBasedRebalancingStrategy";

	@Parameter("relocationCalculatorType")
	@Comment("Specific the zone free relocation calculator. Default is fast heuristic zone free relocation calculator.")
	@NotNull
	public String waitingPointPath = "/path";

	public WaitingPointBasedRebalancingStrategyParams() {
		super(SET_NAME);
	}
}
