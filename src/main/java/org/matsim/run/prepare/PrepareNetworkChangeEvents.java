package org.matsim.run.prepare;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

public class PrepareNetworkChangeEvents implements MATSimAppCommand {

    @CommandLine.Option(names= "--network", description = "path to drt stop xml file", required = true)
    private String networkFile;

    @CommandLine.Option(names= "--events", description = "path to real drt demand csv file", required = true)
    private String eventsFile;

    @CommandLine.Option(names= "--output", description = "path to real drt demand csv file", required = true)
    private String output;

    public static void main(String[] args) {
        new PrepareNetworkChangeEvents().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        Network network = NetworkUtils.readNetwork(networkFile);
        TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder(network);
        TravelTimeCalculator travelTimeCalculator = builder.build();

        // event reader add event handeler travelTimeCalculator
        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(travelTimeCalculator);
        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
        eventsReader.readFile(eventsFile);

        // Actual TravelTime based on the events file
        TravelTime travelTime = travelTimeCalculator.getLinkTravelTimes();

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
        writer.write(output, networkChangeEvents);

        return 0;
    }
}
