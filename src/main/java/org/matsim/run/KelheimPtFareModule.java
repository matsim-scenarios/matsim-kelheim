package org.matsim.run;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import playground.vsp.pt.fare.DistanceBasedPtFareHandler;
import playground.vsp.pt.fare.DistanceBasedPtFareParams;
import playground.vsp.pt.fare.PtFareConfigGroup;
import playground.vsp.pt.fare.PtFareUpperBoundHandler;

/**
 * Module to install classes for pt fare.
 */
public class KelheimPtFareModule extends AbstractModule {
	@Override
	public void install() {
		// Set the money related thing in the config (planCalcScore) file to 0.
		getConfig().planCalcScore().getModes().get(TransportMode.pt).setDailyMonetaryConstant(0);
		getConfig().planCalcScore().getModes().get(TransportMode.pt).setMarginalUtilityOfDistance(0);

		// Initialize config group (and also write in the output config)
		PtFareConfigGroup ptFareConfigGroup = ConfigUtils.addOrGetModule(this.getConfig(), PtFareConfigGroup.class);
		DistanceBasedPtFareParams distanceBasedPtFareParams = ConfigUtils.addOrGetModule(this.getConfig(), DistanceBasedPtFareParams.class);


		// Add bindings
		addEventHandlerBinding().toInstance(new DistanceBasedPtFareHandler(distanceBasedPtFareParams));
		if (ptFareConfigGroup.getApplyUpperBound()) {
			PtFareUpperBoundHandler ptFareUpperBoundHandler = new PtFareUpperBoundHandler(ptFareConfigGroup.getUpperBoundFactor());
			addEventHandlerBinding().toInstance(ptFareUpperBoundHandler);
			addControlerListenerBinding().toInstance(ptFareUpperBoundHandler);
		}
	}
}
