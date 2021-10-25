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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Fade-out the strategy weight during the simulation.
 * This class as well as the {@link Schedule}s has be bound with guice in the controller.
 */
public final class StrategyWeightFadeout implements IterationStartsListener {

	private final Logger log = LogManager.getLogger(StrategyWeightFadeout.class);

	@Inject
	private Map<StrategyConfigGroup.StrategySettings, PlanStrategy> planStrategies;

	@Inject
	private Config config;

	@Inject
	private StrategyManager strategyManager;

	@Inject
	private Set<Schedule> schedules;

	@Override
	public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {

		for (Schedule s : schedules) {

			StrategyConfigGroup.StrategySettings settings = null;

			for (StrategyConfigGroup.StrategySettings strategySettings : planStrategies.keySet()) {
				if (strategySettings.getStrategyName().equals(s.name) && strategySettings.getSubpopulation().equals(s.subpopulation)) {
					settings = strategySettings;
					break;
				}
			}

			if (settings == null) {
				log.info("Strategy settings for {} not found", s.name);
				return;
			}

			String strategyName = settings.getStrategyName();

			if (Double.isNaN(s.initialWeight)) {
				s.initialWeight = settings.getWeight();
				s.startIteration = (int) (config.controler().getLastIteration() * s.startAt);
				double disable = config.strategy().getFractionOfIterationsToDisableInnovation();

				// use disable after if it is set
				if (!Double.isNaN(s.endAt))
					s.endIteration = (int) (config.controler().getLastIteration() * s.endAt);
				else if (settings.getDisableAfter() > 0 && settings.getDisableAfter() < Integer.MAX_VALUE && settings.getDisableAfter() <= disable)
					s.endIteration = settings.getDisableAfter();
				else if (Double.isFinite(disable) && disable < Integer.MAX_VALUE)
					s.endIteration = (int) (config.controler().getLastIteration() * disable);
				else
					s.endIteration = settings.getDisableAfter();

				log.info("{} fadeout from iteration {} to {} with start weight {}", strategyName, s.startIteration, s.endIteration, s.initialWeight);
			}

			// Find the implementation to update the strategy weight
			List<GenericPlanStrategy<Plan, Person>> strategies = strategyManager.getStrategies(s.subpopulation);
			Optional<GenericPlanStrategy<Plan, Person>> strategy = strategies.stream().filter(st -> st.toString().contains(strategyName)).findFirst();

			if (strategy.isEmpty()) {
				log.warn("Could not find loaded strategy for {}", strategy);
				return;
			}

			if (iterationStartsEvent.getIteration() > s.startIteration && iterationStartsEvent.getIteration() <= s.endIteration) {
				double step = s.initialWeight / (s.endIteration - s.startIteration);
				double weight = s.initialWeight + step * (s.startIteration - iterationStartsEvent.getIteration());

				log.info("Setting {} weight at iteration {} to {}", strategyName, iterationStartsEvent.getIteration(), weight);

				strategyManager.changeWeightOfStrategy(strategy.get(), s.subpopulation, weight);
			}
		}
	}

	/**
	 * Defines the fade-out schedule for certain strategies.
	 */
	public static class Schedule {

		/**
		 * Start weight for fade-out.
		 */
		private double initialWeight = Double.NaN;

		/**
		 * Start and end iteration for fade-out.
		 */
		private int startIteration;
		private int endIteration;

		private final String name;
		private final String subpopulation;
		private final double startAt;
		private final double endAt;

		/**
		 * Constructor where the end is taken from the config and not given explicitly.
		 */
		public Schedule(String name, String subpopulation, double startAt) {
			this.name = name;
			this.subpopulation = subpopulation;
			this.startAt = startAt;
			this.endAt = Double.NaN;
		}

		public Schedule(String name, String subpopulation, double startAt, double endAt) {
			this.name = name;
			this.subpopulation = subpopulation;
			this.startAt = startAt;
			this.endAt = endAt;
		}
	}
}
