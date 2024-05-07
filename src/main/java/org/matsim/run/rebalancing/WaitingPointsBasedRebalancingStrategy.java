package org.matsim.run.rebalancing;

import com.google.common.base.Preconditions;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.mutable.MutableInt;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Chengqi Lu
 */
public class WaitingPointsBasedRebalancingStrategy implements RebalancingStrategy {

	private final Network network;
	private final RebalancingParams params;
	private final Fleet fleet;
	private final Map<Id<Link>, Integer> waitingPointsCapcityMap = new HashMap<>();

	public WaitingPointsBasedRebalancingStrategy(Network network, String waitingPointsPath, RebalancingParams params, Fleet fleet) throws IOException {
		this.network = network;
		this.params = params;
		this.fleet = fleet;
		initialize(waitingPointsPath);
	}

	private void initialize(String waitingPointsPath) throws IOException {
		// read tsv file: link_id capacity
		try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(waitingPointsPath), StandardCharsets.UTF_8),
			CSVFormat.TDF.builder().setHeader().setSkipHeaderRecord(true).build())) {
			for (CSVRecord record : parser) {
				Link waitingPointLink = network.getLinks().get(Id.createLinkId(record.get("link_id")));
				Integer capacity = Integer.parseInt(record.get("capacity"));
				waitingPointsCapcityMap.put(waitingPointLink.getId(), capacity);
			}
		}
	}

	@Override
	public List<Relocation> calcRelocations(Stream<? extends DvrpVehicle> rebalancableVehicles, double time) {
		List<Relocation> relocations = new ArrayList<>();
		List<? extends DvrpVehicle> idleVehicles = rebalancableVehicles.filter(v -> v.getServiceEndTime() > time + params.minServiceTime).toList();
		if (!idleVehicles.isEmpty()) {
			// when there are idling vehicle that can be rebalanced
			// we first identify the distribution of the vehicles
			Map<Id<Link>, MutableInt> waitingPointsOccupancyMap = new HashMap<>();
			Map<Id<DvrpVehicle>, Id<Link>> vehicleLocationMap = new HashMap<>();
			waitingPointsCapcityMap.keySet().forEach(linkId -> waitingPointsOccupancyMap.put(linkId, new MutableInt()));
			for (DvrpVehicle v : fleet.getVehicles().values()) {
				Schedule s = v.getSchedule();
				StayTask stayTask = (StayTask) Schedules.getLastTask(s);
				if (stayTask.getStatus() == Task.TaskStatus.PLANNED
					&& stayTask.getBeginTime() < time + params.maxTimeBeforeIdle
					&& v.getServiceEndTime() > time + params.minServiceTime) {
					Link finalStayTaskLink = stayTask.getLink();
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
					relocations.add(new Relocation(idleVehicle, nearestAvailableWaitingPointLink));
					waitingPointsOccupancyMap.get(nearestAvailableWaitingPointLink.getId()).increment();
				} else if (waitingPointsOccupancyMap.get(currentLinkId).intValue() > waitingPointsCapcityMap.get(currentLinkId)) {
					// it is stopping at a waiting point, but there is no extra space -> go to next waiting point that is not yet full
					Link nearestAvailableWaitingPointLink = findNearestAvailableWaitingPoint(currentLinkId, waitingPointsOccupancyMap);
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
			if (waitingPointsOccupancyMap.get(currentLinkId).intValue() < waitingPointsCapcityMap.get(currentLinkId)) {
				double distance = CoordUtils.calcEuclideanDistance(network.getLinks().get(currentLinkId).getToNode().getCoord(),
					network.getLinks().get(waitingPoint).getToNode().getCoord());
				if (distance < shortestDistance) {
					nearestWaitingPoint = network.getLinks().get(waitingPoint);
					shortestDistance = distance;
				}
			}
		}
		Preconditions.checkArgument(nearestWaitingPoint != null, "Not able to find nearest waiting point! Please make sure the sum capacity" +
			"of waiting points is greater than or equal to the fleet size");
		return nearestWaitingPoint;
	}
}
