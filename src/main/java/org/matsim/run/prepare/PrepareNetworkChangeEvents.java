package org.matsim.run.prepare;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.prepare.scenario.CreateScenarioCutOut;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(
		name = "prepare-network-change-events",
		description = "Write network change events based on output events"
)
public class PrepareNetworkChangeEvents implements MATSimAppCommand {
	@CommandLine.Option(names = "--network", description = "path to network file", required = true)
	private String networkFile;

	@CommandLine.Option(names = "--events", description = "path to events file", required = true)
	private String eventsFile;

	@CommandLine.Option(names = "--output", description = "output path", required = true)
	private String output;

	@CommandLine.Option(names = "--interval", description = "Interval of network change events in seconds.", defaultValue = "900")
	private double interval;

	public static void main(String[] args) {
		new PrepareNetworkChangeEvents().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		if (interval <= 0) {
			throw new IllegalArgumentException("--interval must be greater than zero");
		}

		Network network = NetworkUtils.readNetwork(networkFile);

		TravelTimeCalculator travelTimeCalculator = createTravelTimeCalculator(network);
		LastTimeEvaluator lastTimeEvaluator = new LastTimeEvaluator();
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(travelTimeCalculator);
		eventsManager.addHandler(lastTimeEvaluator);
		eventsManager.initProcessing();
		EventsUtils.readEvents(eventsManager, eventsFile);
		eventsManager.finishProcessing();

		List<NetworkChangeEvent> networkChangeEvents = CreateScenarioCutOut.generateNetworkChangeEvents(
			network, travelTimeCalculator, null, null, true, lastTimeEvaluator.lastTime, interval);
		new NetworkChangeEventsWriter().write(output, networkChangeEvents);

		return 0;
	}

	private TravelTimeCalculator createTravelTimeCalculator(Network network) {

		TravelTimeCalculator.Builder ttcb = new TravelTimeCalculator.Builder(network);
		ttcb.setTimeslice(interval);
		return ttcb.build();
	}

	private static final class LastTimeEvaluator implements BasicEventHandler {
		private double lastTime = -1;

		@Override
		public void handleEvent(Event event) {
			lastTime = Math.max(lastTime, event.getTime());
		}
	}
}
