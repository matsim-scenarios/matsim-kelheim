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

package org.matsim.run.utils;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;

import java.util.Map;

/**
 * Fade-out the mode choice parameter during the end of simulations.
 */
public final class TuneModeChoice implements IterationStartsListener {

	private final Logger log = LogManager.getLogger(TuneModeChoice.class);

	@Inject
	private Map<StrategyConfigGroup.StrategySettings, PlanStrategy> planStrategies;

	@Inject
	private Config config;

	/**
	 * Start weight for fade-out.
	 */
	private double initialWeight = Double.NaN;

	/**
	 * Start and end iteration for fade-out.
	 */
	private int startAt;
	private int endAt;

	@Override
	public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {
		StrategyConfigGroup.StrategySettings modeChoice = null;

		for (StrategyConfigGroup.StrategySettings strategySettings : planStrategies.keySet()) {
			if ( (strategySettings.getStrategyName().equals(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice)
					|| strategySettings.getStrategyName().equals(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode) ) //assuming that only one of the mode choice strategies is configured
			&&	strategySettings.getSubpopulation().equals("person") ) {
				modeChoice = strategySettings;
				break;
			}
		}

		if (Double.isNaN(initialWeight)) {
			initialWeight = modeChoice.getWeight();
			// TODO: start fade-out at 0.8 hard-coded
			startAt = (int) (config.controler().getLastIteration() * 0.8);
			endAt = (int) (config.controler().getLastIteration() * config.strategy().getFractionOfIterationsToDisableInnovation());
			log.info("Mode-choice fadeout starting at iteration {} with value {}", startAt, initialWeight);
		}


		if (iterationStartsEvent.getIteration() > startAt && iterationStartsEvent.getIteration() <= endAt) {
			double step = initialWeight / (endAt - startAt);
			double weight = initialWeight + step * (startAt - iterationStartsEvent.getIteration());

			log.info("Setting mode-choice weight at iteration {} to {}", iterationStartsEvent.getIteration(), weight);
			modeChoice.setWeight(weight);
		}
	}
}
