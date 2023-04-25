package org.matsim.analysis.postAnalysis.drt;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.util.DrtEventsReaders;
import org.matsim.contrib.dvrp.fleet.FleetReader;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecificationImpl;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import picocli.CommandLine;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.matsim.application.ApplicationUtils.globFile;

/**
 * See description.
 *
 * @author Chengqi Lu (luchengqi7)
 */
@CommandLine.Command(
		name = "road-usage",
		description = "Analyze road usage by drt vehicles"
)
public class DrtVehiclesRoadUsageAnalysis implements MATSimAppCommand {
	@CommandLine.Option(names = "--directory", description = "path to the directory of the simulation output", required = true)
	private Path directory;

	@CommandLine.Option(names = "--time-bin", description = "Time bin sie in second", defaultValue = "3600")
	private int timeBinSize;

	public static void main(String[] args) {
		new DrtVehiclesRoadUsageAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		Path configPath = globFile(directory, "*output_config.*");
		Path networkPath = globFile(directory, "*output_network.*");
		Path eventsFilePath = globFile(directory, "*output_events.*");
		Path outputFolder = Path.of(directory.toString() + "/analysis-road-usage");

		if (!Files.exists(outputFolder)) {
			Files.createDirectory(outputFolder);
		}

		Network network = NetworkUtils.readNetwork(networkPath.toString());
		EventsManager eventsManager = EventsUtils.createEventsManager();

		Config config = ConfigUtils.loadConfig(configPath.toString());
		MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		Map<String, VehicleLinkUsageRecorder> handlerMap = new HashMap<>();

		for (DrtConfigGroup drtCfg : multiModeDrtConfigGroup.getModalElements()) {
			String mode = drtCfg.getMode();
			Path vehicleFilePath = globFile(directory, "*" + mode + "_vehicles.*");
			FleetSpecification fleetSpecification = new FleetSpecificationImpl();
			new FleetReader(fleetSpecification).parse(vehicleFilePath.toUri().toURL());
			List<String> vehicleIdStrings = fleetSpecification.getVehicleSpecifications().keySet().
					stream().map(Object::toString).collect(Collectors.toList());
			VehicleLinkUsageRecorder vehicleLinkUsageRecorder = new VehicleLinkUsageRecorder(network, timeBinSize, TransportMode.drt, vehicleIdStrings);
			handlerMap.put(mode, vehicleLinkUsageRecorder);
			eventsManager.addHandler(vehicleLinkUsageRecorder);
		}

		MatsimEventsReader eventsReader = DrtEventsReaders.createEventsReader(eventsManager);
		eventsReader.readFile(eventsFilePath.toString());

		// Write results
		for (String mode : handlerMap.keySet()) {
			Map<String, Map<Integer, MutableInt>> vehicleRoadUsageRecordMap = handlerMap.get(mode).getVehicleRoadUsageRecordMap();
			Map<String, Map<Integer, MutableInt>> passengerRoadUsageMap = handlerMap.get(mode).getPassengerRoadUsageMap();

			String vehicleRoadUsageFile = outputFolder + "/" + mode + "_vehicle_road_usage.tsv";
			String passengerRoadUsageFile = outputFolder + "/" + mode + "_passenger_road_usage.tsv";
			CSVPrinter vehicleRoadUsageWriter = new CSVPrinter(new FileWriter(vehicleRoadUsageFile), CSVFormat.TDF);
			CSVPrinter passengerRoadUsageWriter = new CSVPrinter(new FileWriter(passengerRoadUsageFile), CSVFormat.TDF);

			List<String> header = new ArrayList<>();
			header.add("link_id");
			int numOfTimeBins = 86400 / timeBinSize;
			for (int i = 0; i < numOfTimeBins; i++) {
				int time = i * timeBinSize;
				String formattedTime = DurationFormatUtils.formatDuration(time * 1000L, "HH:MM:SS", true);
				header.add(formattedTime);
			}
			header.add("sum");
			vehicleRoadUsageWriter.printRecord(header);
			passengerRoadUsageWriter.printRecord(header);

			for (Link link : network.getLinks().values()) {
				if (link.getAllowedModes().contains(TransportMode.drt)) {
					List<String> vehicleEntry = new ArrayList<>();
					List<String> passengerEntry = new ArrayList<>();
					vehicleEntry.add(link.getId().toString());
					passengerEntry.add(link.getId().toString());
					MutableInt vehicleSum = new MutableInt(0);
					MutableInt passengerSum = new MutableInt(0);
					for (int i = 0; i < numOfTimeBins; i++) {
						int vehicleRoadUsage = vehicleRoadUsageRecordMap.get(link.getId().toString()).get(i).intValue();
						vehicleSum.add(vehicleRoadUsage);
						vehicleEntry.add(Integer.toString(vehicleRoadUsage));
						int passengerRoadUsage = passengerRoadUsageMap.get(link.getId().toString()).get(i).intValue();
						passengerSum.add(passengerRoadUsage);
						passengerEntry.add(Integer.toString(passengerRoadUsage));
					}
					vehicleEntry.add(Integer.toString(vehicleSum.intValue()));
					passengerEntry.add(Integer.toString(passengerSum.intValue()));
					vehicleRoadUsageWriter.printRecord(vehicleEntry);
					passengerRoadUsageWriter.printRecord(passengerEntry);
				}
			}
			vehicleRoadUsageWriter.close();
			passengerRoadUsageWriter.close();
		}
		return 0;
	}

	static class VehicleLinkUsageRecorder implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler,
			PassengerPickedUpEventHandler, PassengerDroppedOffEventHandler {
		private final Network network;
		private final int timeBinSize;
		private final Map<String, Map<Integer, MutableInt>> vehicleRoadUsageRecordMap = new HashMap<>();
		private final Map<String, MutableInt> vehiclesOccupancyTracker = new HashMap<>();
		private final Map<String, Map<Integer, MutableInt>> passengerRoadUsageMap = new HashMap<>();

		private final String mode;
		private final List<String> vehicleIdStrings;


		VehicleLinkUsageRecorder(Network network, int timeBinSize, String mode, List<String> vehicleIdStrings) {
			this.network = network;
			this.timeBinSize = timeBinSize;
			this.mode = mode;
			this.vehicleIdStrings = vehicleIdStrings;
			reset(0);
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			if (vehicleIdStrings.contains(event.getVehicleId().toString())) {
				String linkIdString = event.getLinkId().toString();
				double time = event.getTime();
				if (time <= 86400 && vehicleRoadUsageRecordMap.containsKey(linkIdString)) {
					int timeBin = (int) (time / timeBinSize);
					vehicleRoadUsageRecordMap.get(linkIdString).get(timeBin).increment();
					int passengerOnBoard = vehiclesOccupancyTracker.get(event.getVehicleId().toString()).intValue();
					assert passengerOnBoard >= 0 : "Passenger onboard is smaller than 0! Something has went wrong!";
					for (int i = 0; i < passengerOnBoard; i++) {
						passengerRoadUsageMap.get(linkIdString).get(timeBin).increment();
					}
				}
			}
		}

		@Override
		public void handleEvent(VehicleEntersTrafficEvent event) {
			if (vehicleIdStrings.contains(event.getVehicleId().toString())) {
				String linkIdString = event.getLinkId().toString();
				double time = event.getTime();
				if (time <= 86400 && vehicleRoadUsageRecordMap.containsKey(linkIdString)) {
					int timeBin = (int) (time / timeBinSize);
					vehicleRoadUsageRecordMap.get(linkIdString).get(timeBin).increment();

					// Should we also include the passenger road usage for this event?
				}
			}
		}

		@Override
		public void handleEvent(PassengerDroppedOffEvent event) {
			if (vehicleIdStrings.contains(event.getVehicleId().toString())) {
				String vehicleIdString = event.getVehicleId().toString();
				vehiclesOccupancyTracker.get(vehicleIdString).decrement();
			}
		}

		@Override
		public void handleEvent(PassengerPickedUpEvent event) {
			if (vehicleIdStrings.contains(event.getVehicleId().toString())) {
				String vehicleIdString = event.getVehicleId().toString();
				vehiclesOccupancyTracker.get(vehicleIdString).increment();
			}
		}

		@Override
		public void reset(int iteration) {
			initialize();
		}

		private void initialize() {
			vehicleRoadUsageRecordMap.clear();
			passengerRoadUsageMap.clear();
			vehiclesOccupancyTracker.clear();

			for (Link link : network.getLinks().values()) {
				if (link.getAllowedModes().contains(mode)) {
					int timeBins = 86400 / timeBinSize;

					Map<Integer, MutableInt> counterMap = new HashMap<>();
					for (int i = 0; i < timeBins; i++) {
						counterMap.put(i, new MutableInt(0));
					}
					vehicleRoadUsageRecordMap.put(link.getId().toString(), counterMap);

					Map<Integer, MutableInt> counterMap1 = new HashMap<>();
					for (int i = 0; i < timeBins; i++) {
						counterMap1.put(i, new MutableInt(0));
					}
					passengerRoadUsageMap.put(link.getId().toString(), counterMap1);
				}
			}

			for (String vehicleIdString : vehicleIdStrings) {
				vehiclesOccupancyTracker.put(vehicleIdString, new MutableInt(0));
			}
		}

		public Map<String, Map<Integer, MutableInt>> getVehicleRoadUsageRecordMap() {
			return vehicleRoadUsageRecordMap;
		}

		public Map<String, Map<Integer, MutableInt>> getPassengerRoadUsageMap() {
			return passengerRoadUsageMap;
		}
	}
}
