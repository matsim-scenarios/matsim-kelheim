package org.matsim.rebalancing;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.utils.geometry.CoordUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author Chengqi Lu
 */
class WaitingPointsBasedRebalancingStrategy implements RebalancingStrategy {

	private final Network network;
	private final RebalancingParams params;
	private final Fleet fleet;
	private final Map<Id<Link>, Integer> waitingPointsCapcityMap = new HashMap<>();
	private static final Logger log = LogManager.getLogger(WaitingPointsBasedRebalancingStrategy.class);

	WaitingPointsBasedRebalancingStrategy(Network network, String waitingPointsPath, RebalancingParams params, Fleet fleet) throws IOException {
		this.network = network;
		this.params = params;
		this.fleet = fleet;
		initialize(waitingPointsPath);
	}

	private void initialize(String waitingPointsPath) throws IOException {
		if (!waitingPointsPath.isEmpty()) {
			// read the waiting point locations from the provided tsv file: link_id	capacity
			log.info("Reading waiting points from the file...");
			try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(waitingPointsPath), StandardCharsets.UTF_8),
				CSVFormat.TDF.builder().setHeader().setSkipHeaderRecord(true).build())) {
				for (CSVRecord record : parser) {
					Link waitingPointLink = network.getLinks().get(Id.createLinkId(record.get("link_id")));
					Integer capacity = Integer.parseInt(record.get("capacity"));
					waitingPointsCapcityMap.put(waitingPointLink.getId(), capacity);
				}
			}
		} else {
			// using the starting points of the vehicle as waiting points, capacity of the waiting point is set to the count of the vehicles starting
			// at that point (i.e., link).
			log.info("No waiting points file is provided. Assume all the starting locations of vehicles to be waiting points...");
			for (DvrpVehicle vehicle : fleet.getVehicles().values()) {
				Id<Link> startLinkId = vehicle.getStartLink().getId();
				int currentCount = waitingPointsCapcityMap.getOrDefault(startLinkId, 0);
				waitingPointsCapcityMap.put(startLinkId, currentCount + 1);
			}
		}
	}

	@Override
	public List<Relocation> calcRelocations(Stream<? extends DvrpVehicle> rebalancableVehicles, double time) {
		List<Relocation> relocations = new ArrayList<>();
		List<? extends DvrpVehicle> idleVehicles = rebalancableVehicles.filter(v -> v.getServiceEndTime() > time + params.minServiceTime).toList();
		if (!idleVehicles.isEmpty()) {
			// when there are idling vehicles that can be rebalanced
			// we first identify the distribution of the vehicles
			Map<Id<Link>, MutableInt> waitingPointsOccupancyMap = new HashMap<>();
			Map<Id<DvrpVehicle>, Id<Link>> vehicleLocationMap = new HashMap<>();
			waitingPointsCapcityMap.keySet().forEach(linkId -> waitingPointsOccupancyMap.put(linkId, new MutableInt()));
			for (DvrpVehicle v : fleet.getVehicles().values()) {
				Schedule s = v.getSchedule();
				Task finalTask = Schedules.getLastTask(s);
				int finalTaskIdx = finalTask.getTaskIdx();
				int currentTaskIdx = s.getCurrentTask().getTaskIdx();
				if ((currentTaskIdx == finalTaskIdx || currentTaskIdx == finalTaskIdx - 1) && finalTask instanceof StayTask) {
					// The vehicle is staying at the final location (i.e., idling) or driving to the final stay task (i.e., being relocated)
					Link finalStayTaskLink = ((StayTask) finalTask).getLink();
					// store the location of the (idling) vehicle for later use
					vehicleLocationMap.put(v.getId(), finalStayTaskLink.getId());
					if (waitingPointsOccupancyMap.containsKey(finalStayTaskLink.getId())) {
						waitingPointsOccupancyMap.get(finalStayTaskLink.getId()).increment();
					}
				}
			}

			for (DvrpVehicle idleVehicle : idleVehicles) {
				Id<Link> currentLinkId = vehicleLocationMap.get(idleVehicle.getId());
				if (!waitingPointsOccupancyMap.containsKey(currentLinkId)) {
					// vehicle is not at any waiting point -> go to the nearest waiting point that is not yet full
					Link nearestAvailableWaitingPointLink = findNearestAvailableWaitingPoint(currentLinkId, waitingPointsOccupancyMap);
					if (nearestAvailableWaitingPointLink == null) {
						continue;
					}
					relocations.add(new Relocation(idleVehicle, nearestAvailableWaitingPointLink));
					waitingPointsOccupancyMap.get(nearestAvailableWaitingPointLink.getId()).increment();
				} else if (waitingPointsOccupancyMap.get(currentLinkId).intValue() > waitingPointsCapcityMap.get(currentLinkId)) {
					// it is stopping at a waiting point, but there is no extra space -> go to next waiting point that is not yet full
					Link nearestAvailableWaitingPointLink = findNearestAvailableWaitingPoint(currentLinkId, waitingPointsOccupancyMap);
					if (nearestAvailableWaitingPointLink == null) {
						continue;
					}
					relocations.add(new Relocation(idleVehicle, nearestAvailableWaitingPointLink));
					waitingPointsOccupancyMap.get(nearestAvailableWaitingPointLink.getId()).increment();
					waitingPointsOccupancyMap.get(currentLinkId).decrement();
				}
				// else, stay where it is now.
			}
		}
		return relocations;
	}

	private Link findNearestAvailableWaitingPoint(Id<Link> currentLinkId, Map<Id<Link>, MutableInt> waitingPointsOccupancyMap) {
		double shortestDistance = Double.POSITIVE_INFINITY;
		Link nearestWaitingPoint = null;

		for (Id<Link> waitingPoint : waitingPointsOccupancyMap.keySet()) {
			if (waitingPointsOccupancyMap.get(waitingPoint).intValue() < waitingPointsCapcityMap.get(waitingPoint)) {
				double distance = CoordUtils.calcEuclideanDistance(network.getLinks().get(currentLinkId).getToNode().getCoord(),
					network.getLinks().get(waitingPoint).getToNode().getCoord());
				if (distance < shortestDistance) {
					nearestWaitingPoint = network.getLinks().get(waitingPoint);
					shortestDistance = distance;
				}
			}
		}

		if (nearestWaitingPoint == null) {
			log.warn("No suitable waiting point can be found! Probably because the sum of the capacities in the waiting points is smaller than " +
				"the fleet size. Please double check that! The vehicle will not be relocated");
		}

		return nearestWaitingPoint;
	}
}
