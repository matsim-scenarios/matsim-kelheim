package org.matsim.run;

import org.matsim.core.controler.AbstractModule;
import playground.vsp.pt.fare.DistanceBasedPtFareHandler;
import playground.vsp.pt.fare.DistanceBasedPtFareParams;
import playground.vsp.pt.fare.PtFareConfigGroup;
import playground.vsp.pt.fare.PtFareUpperBoundHandler;

public class KelheimPtFareModule extends AbstractModule {
    @Override
    public void install() {
        // Initialize config group
        PtFareConfigGroup ptFareConfigGroup = new PtFareConfigGroup();
        DistanceBasedPtFareParams distanceBasedPtFareParams = new DistanceBasedPtFareParams();

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
