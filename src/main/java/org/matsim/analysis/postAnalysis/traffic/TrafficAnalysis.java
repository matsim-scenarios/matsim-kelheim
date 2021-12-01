package org.matsim.analysis.postAnalysis.traffic;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

public class TrafficAnalysis {
    public static TravelTime analyzeTravelTimeFromEvents(Network network, String eventsFile) {
        TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder(network);
        TravelTimeCalculator travelTimeCalculator = builder.build();

        // event reader add event handeler travelTimeCalculator
        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(travelTimeCalculator);
        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
        eventsReader.readFile(eventsFile);

        return travelTimeCalculator.getLinkTravelTimes();
    }
}
