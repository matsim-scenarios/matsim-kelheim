package org.matsim.run.prepare;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import java.util.ArrayList;
import java.util.List;

public class PrepareNetworkChangeEvents {
    private static final String NETWORK = "/Users/luchengqi/Documents/MATSimScenarios/Kelheim/output/047/047.output_network.xml.gz"; // TODO
    private static final String EVENTS_FILE = "/Users/luchengqi/Documents/MATSimScenarios/Kelheim/output/047/047.output_events.xml.gz"; //TODO
    private static final String OUTPUT = "/Users/luchengqi/Documents/MATSimScenarios/Kelheim/output/047/network-change-events.xml.gz"; // TODO


    public static void main(String[] args) {
        Network network = NetworkUtils.readNetwork(NETWORK);
        TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder(network);
        TravelTimeCalculator travelTimeCalculator = builder.build();

        // event reader add event handeler travelTimeCalculator
        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(travelTimeCalculator);
        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
        eventsReader.readFile(EVENTS_FILE);

        // Actual TravelTime based on the events file
        TravelTime travelTime = travelTimeCalculator.getLinkTravelTimes();

        //TODO delete afterwards
        // STARTS
        double freeTravelTime1 = network.getLinks().get(Id.createLinkId("26526533#0")).getLength()/ network.getLinks().get(Id.createLinkId("26526533#0")).getFreespeed();
        System.out.println("Free speed of link 26526533#0 is " + freeTravelTime1);
        System.out.println("Travel time at link 26526533#0 at 02:00:00 is " + travelTime.getLinkTravelTime(network.getLinks().get(Id.createLinkId("26526533#0")), 7200, null, null));
        System.out.println("Travel time at link 26526533#0 at 08:20:00 is " + travelTime.getLinkTravelTime(network.getLinks().get(Id.createLinkId("26526533#0")), 30000, null, null));
        System.out.println("Travel time at link 26526533#0 at 08:30:00 is " + travelTime.getLinkTravelTime(network.getLinks().get(Id.createLinkId("26526533#0")), 30600, null, null));
        System.out.println("Travel time at link 26526533#0 at 08:35:00 is " + travelTime.getLinkTravelTime(network.getLinks().get(Id.createLinkId("26526533#0")), 30900, null, null));
        // TODO ENDS

        // write network change events
        List<NetworkChangeEvent> networkChangeEvents = new ArrayList<>();
        for (Link link : network.getLinks().values()) {
            double freeSpeed = link.getFreespeed();
            double previousTravelTime = Math.floor(link.getLength() / freeSpeed) + 1;
            for (int i = 0; i < 86400; i += 900) {
                double actualTravelTime = travelTime.getLinkTravelTime(link, i, null, null);
                if (actualTravelTime != previousTravelTime) {
                    double actualSpeed = link.getLength() / actualTravelTime;
                    NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(i);
                    networkChangeEvent.addLink(link);
                    networkChangeEvent.setFreespeedChange(new NetworkChangeEvent.ChangeValue
                            (NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, actualSpeed));
                    networkChangeEvents.add(networkChangeEvent);
                    previousTravelTime = actualTravelTime;
                }
            }
        }

        // write network change events
        NetworkChangeEventsWriter writer = new NetworkChangeEventsWriter();
        writer.write(OUTPUT, networkChangeEvents);

    }
}
