package org.matsim.analysis.postAnalysis.traffic;

import org.apache.commons.lang.mutable.MutableInt;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;

import java.util.HashMap;
import java.util.Map;

public class LinkFilter implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler {
    private final Geometry studyArea;
    private final int minimumDailyTrafficCount;
    Map<Id<Link>, MutableInt> linksTrafficCountMap = new HashMap<>();

    public LinkFilter(Geometry studyArea, int minimumDailyTrafficCount) {
        this.studyArea = studyArea;
        this.minimumDailyTrafficCount = minimumDailyTrafficCount;
    }

    public boolean checkIfConsiderTheLink(Link link) {
        if (!link.getAllowedModes().contains(TransportMode.car)) {
            return false;
        }

        // Links that are too short may produce extreme values, which may influence the results
        if (link.getLength() < 10) {
            return false;
        }

        // Remove the links that is outside the study area
        if (studyArea != null) {
            boolean fromNodeIsInStudyArea = MGC.coord2Point(link.getFromNode().getCoord()).within(studyArea);
            boolean toNodeIsInStudyArea = MGC.coord2Point(link.getToNode().getCoord()).within(studyArea);
            if (!fromNodeIsInStudyArea && !toNodeIsInStudyArea) {
                return false;
            }
        }
        return linksTrafficCountMap.getOrDefault(link.getId(), new MutableInt()).intValue() >= minimumDailyTrafficCount;
    }

    @Override
    public void handleEvent(LinkEnterEvent linkEnterEvent) {
        Id<Link> linkId = linkEnterEvent.getLinkId();
        linksTrafficCountMap.computeIfAbsent(linkId, v -> new MutableInt()).increment();
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent vehicleEntersTrafficEvent) {
        Id<Link> linkId = vehicleEntersTrafficEvent.getLinkId();
        linksTrafficCountMap.computeIfAbsent(linkId, v -> new MutableInt()).increment();
    }

    @Override
    public void reset(int iteration) {
        linksTrafficCountMap.clear();
    }
}
