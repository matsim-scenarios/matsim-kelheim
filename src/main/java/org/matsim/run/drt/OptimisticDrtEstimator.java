package org.matsim.run.drt;

import org.matsim.contrib.drt.extension.estimator.DrtInitialEstimator;
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * Estimates using a constant detour factor and waiting time.
 */
public class OptimisticDrtEstimator implements DrtInitialEstimator {

	private final DrtConfigGroup drtConfig;

	/**
	 * Proportion of the max total travel time.
	 */
	private final double proportion;

	/**
	 * Constant waiting time estimate in seconds.
	 */
	private final double waitingTime;

	public OptimisticDrtEstimator(DrtConfigGroup drtConfig, double proportion, double waitingTime) {
		this.drtConfig = drtConfig;
		this.proportion = proportion;
		this.waitingTime = waitingTime;
	}

	@Override
	public Estimate estimate(DrtRoute route, OptionalTime departureTime) {

		double distance = route.getDistance();

		double maxTravelTime = Math.min(route.getDirectRideTime() + this.drtConfig.maxAbsoluteDetour,
			route.getDirectRideTime() * this.drtConfig.maxTravelTimeAlpha + this.drtConfig.maxTravelTimeBeta);

		double fare = 0;
		if (drtConfig.getDrtFareParams().isPresent()) {
			DrtFareParams fareParams = drtConfig.getDrtFareParams().get();
			fare = fareParams.distanceFare_m * distance
				+ fareParams.timeFare_h * route.getDirectRideTime() / 3600.0
				+ fareParams.baseFare;

			fare = Math.max(fare, fareParams.minFarePerTrip);


		}

		double travelTime = Math.max(route.getDirectRideTime(), maxTravelTime * proportion);

		return new Estimate(distance, travelTime, waitingTime, fare, 0);
	}
}
