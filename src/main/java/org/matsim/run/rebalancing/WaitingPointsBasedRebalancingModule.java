/*
 * *********************************************************************** *
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
 * *********************************************************************** *
 */

package org.matsim.run.rebalancing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;

import java.io.IOException;

/**
 * @author Chengqi Lu
 */
public class WaitingPointsBasedRebalancingModule extends AbstractDvrpModeModule {
	private static final Logger log = LogManager.getLogger(WaitingPointsBasedRebalancingStrategy.class);
	private final DrtConfigGroup drtCfg;
	private final String waitingPointsPath;

	public WaitingPointsBasedRebalancingModule(DrtConfigGroup drtCfg, String waitingPointsPath) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
		this.waitingPointsPath = waitingPointsPath;
	}

	@Override
	public void install() {
		log.info("Waiting-points-based rebalancing strategy is now being installed!");
		RebalancingParams generalParams = drtCfg.getRebalancingParams().orElseThrow();

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(WaitingPointsBasedRebalancingStrategy.class).toProvider(modalProvider(
					getter -> {
						try {
							return new WaitingPointsBasedRebalancingStrategy(getter.getModal(Network.class),
								waitingPointsPath, generalParams, getter.getModal(Fleet.class));
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					})).asEagerSingleton();

				// binding event handler
				bindModal(RebalancingStrategy.class).to(modalKey(WaitingPointsBasedRebalancingStrategy.class));
			}
		});
	}
}
