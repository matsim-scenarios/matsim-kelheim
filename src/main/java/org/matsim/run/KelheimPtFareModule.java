package org.matsim.run;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import playground.vsp.pt.fare.DistanceBasedPtFareHandler;
import playground.vsp.pt.fare.DistanceBasedPtFareParams;
import playground.vsp.pt.fare.PtFareConfigGroup;
import playground.vsp.pt.fare.PtFareUpperBoundHandler;

public class KelheimPtFareModule extends AbstractModule {
    @Override
    public void install() {
        // Set the money related thing in the config (planCalcScore) file to 0.
        getConfig().planCalcScore().getModes().get(TransportMode.pt).setDailyMonetaryConstant(0);
        getConfig().planCalcScore().getModes().get(TransportMode.pt).setMarginalUtilityOfDistance(0);

        // Initialize config group (and also write in the output config)
        PtFareConfigGroup ptFareConfigGroup = ConfigUtils.addOrGetModule(this.getConfig(), PtFareConfigGroup.class);
        DistanceBasedPtFareParams distanceBasedPtFareParams = ConfigUtils.addOrGetModule(this.getConfig(), DistanceBasedPtFareParams.class);

        // Set parameters
        ptFareConfigGroup.setApplyUpperBound(true);
        ptFareConfigGroup.setUpperBoundFactor(1.5);

        distanceBasedPtFareParams.setMinFare(2.0);  // Minimum fare (e.g. short trip or 1 zone ticket)
        distanceBasedPtFareParams.setLongTripThreshold(50000); // Division between long trip and short trip (unit: m)
        distanceBasedPtFareParams.setShortTripSlope(0.00017); // y = ax + b --> a value, for short trips
        distanceBasedPtFareParams.setShortTripIntercept(1.6); // y = ax + b --> b value, for short trips
        distanceBasedPtFareParams.setLongTripSlope(0.00025); // y = ax + b --> a value, for long trips
        distanceBasedPtFareParams.setLongTripIntercept(30); // y = ax + b --> b value, for long trips


        // Add bindings
        addEventHandlerBinding().toInstance(new DistanceBasedPtFareHandler(distanceBasedPtFareParams));
        if (ptFareConfigGroup.getApplyUpperBound()) {
            PtFareUpperBoundHandler ptFareUpperBoundHandler = new PtFareUpperBoundHandler(ptFareConfigGroup.getUpperBoundFactor());
            addEventHandlerBinding().toInstance(ptFareUpperBoundHandler);
            addControlerListenerBinding().toInstance(ptFareUpperBoundHandler);
        }
    }
}
