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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fade-out the mode choice parameter during the end of simulations.
 */
public final class TuneModeChoice implements IterationStartsListener {

	private final Logger log = LogManager.getLogger(TuneModeChoice.class);

	private static final String SUBPOPULATION = "person";

	private static final double START = 0.8;

	@Inject
	private Map<StrategyConfigGroup.StrategySettings, PlanStrategy> planStrategies;

	@Inject
	private Config config;

	@Inject
	private StrategyManager strategyManager;

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
		StrategyConfigGroup.StrategySettings settings = null;
		StrategyConfigGroup.StrategySettings reRoute = null;

		for (StrategyConfigGroup.StrategySettings strategySettings : planStrategies.keySet()) {
			if ( (strategySettings.getStrategyName().equals(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice)
					|| strategySettings.getStrategyName().equals(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode) ) //assuming that only one of the mode choice strategies is configured
			&&	strategySettings.getSubpopulation().equals(SUBPOPULATION) ) {
				settings = strategySettings;
				break;
			}

			if (strategySettings.getStrategyName().equals(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute) && strategySettings.getSubpopulation().equals(SUBPOPULATION))
				reRoute = strategySettings;

		}

		if (settings == null && reRoute != null) {
			log.info("No mode-choice strategy found, using re-routing instead");
			settings = reRoute;
		} else if (settings == null) {
			// neither strategy found
			return;
		}

		String strategyName = settings.getStrategyName();

		if (Double.isNaN(initialWeight)) {
			initialWeight = settings.getWeight();
			startAt = (int) (config.controler().getLastIteration() * START);
			double disable = config.strategy().getFractionOfIterationsToDisableInnovation();

			if (Double.isFinite(disable) && disable < Integer.MAX_VALUE)
				endAt = (int) (config.controler().getLastIteration() * disable);
			else
				endAt = settings.getDisableAfter();

			log.info("{} fadeout from iteration {} to {} with start weight {}", strategyName, startAt, endAt, initialWeight);
		}

		// Find the implementation to update the strategy weight
		List<GenericPlanStrategy<Plan, Person>> strategies = strategyManager.getStrategies(SUBPOPULATION);
		Optional<GenericPlanStrategy<Plan, Person>> strategy = strategies.stream().filter(s -> s.toString().contains(strategyName)).findFirst();

		if (strategy.isEmpty()) {
			log.warn("Could not find loaded strategy for {}", strategy);
			return;
		}

		if (iterationStartsEvent.getIteration() > startAt && iterationStartsEvent.getIteration() <= endAt) {
			double step = initialWeight / (endAt - startAt);
			double weight = initialWeight + step * (startAt - iterationStartsEvent.getIteration());

			log.info("Setting {} weight at iteration {} to {}", strategyName, iterationStartsEvent.getIteration(), weight);

			strategyManager.changeWeightOfStrategy(strategy.get(), SUBPOPULATION, weight);
		}
	}
}
