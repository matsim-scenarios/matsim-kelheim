package org.matsim.drtFare;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.fare.DrtFareHandler;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles monetary fare for drt rides.
 */
public class KelheimDrtFareHandler implements DrtRequestSubmittedEventHandler, PassengerDroppedOffEventHandler, PassengerRequestRejectedEventHandler {

	private static final Logger log = LogManager.getLogger(KelheimDrtFareHandler.class);

	@Inject
	private EventsManager events;

	//    public static final String PERSON_MONEY_EVENT_PURPOSE_DRT_FARE = "drtFare"; // Use the public static String in the DrtFareHandler instead.
	private final double baseFare;
	private final double zone2Surcharge;
	private final String mode;
	private final String shapeFIle;
	private final Network network;
	private final Map<String, Geometry> zonalSystem;

	private final Map<Id<Request>, Boolean> surchargeMap = new HashMap<>();

	public KelheimDrtFareHandler(String mode, Network network, KelheimDrtFareParams params) {
		this.baseFare = params.getBaseFare();
		this.zone2Surcharge = params.getZone2Surcharge();
		this.mode = mode;
		this.network = network;
		this.shapeFIle = params.getShapeFile();

		this.zonalSystem = new HashMap<>();
		for (SimpleFeature feature : getFeatures(shapeFIle)) {
			zonalSystem.put(feature.getAttribute("Region_ID").toString(), (Geometry) feature.getDefaultGeometry());
		}
	}

	// Constructor that does not require injection (can be used for testing)
	KelheimDrtFareHandler(String mode, KelheimDrtFareParams params, Network network, EventsManager events) {
		this.baseFare = params.getBaseFare();
		this.zone2Surcharge = params.getZone2Surcharge();
		this.mode = mode;
		this.network = network;
		this.shapeFIle = params.getShapeFile();
		this.zonalSystem = new HashMap<>();
		this.events = events;
		for (SimpleFeature feature : getFeatures(shapeFIle)) {
			zonalSystem.put(feature.getAttribute("Region_ID").toString(), (Geometry) feature.getDefaultGeometry());
		}
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent drtRequestSubmittedEvent) {
		if (drtRequestSubmittedEvent.getMode().equals(mode)) {
			Link fromLink = network.getLinks().get(drtRequestSubmittedEvent.getFromLinkId());
			Link toLink = network.getLinks().get(drtRequestSubmittedEvent.getToLinkId());
			if (!zonalSystem.isEmpty()) {
				if (zonalSystem.get("1") == null) {
					throw new RuntimeException("The shape file data entry is not prepared correctly. " +
							"Please make sure the attribute of the shape file are in the correct format: " +
							"Region_ID --> 1 or 2.");
				}
				boolean fromZone1 = zonalSystem.get("1").contains(MGC.coord2Point(fromLink.getToNode().getCoord()));
				boolean toZone1 = zonalSystem.get("1").contains(MGC.coord2Point(toLink.getToNode().getCoord()));
				if (fromZone1 && toZone1) {
					// trip within zone 1
					surchargeMap.put(drtRequestSubmittedEvent.getRequestId(), false);
				} else {
					// otherwise
					surchargeMap.put(drtRequestSubmittedEvent.getRequestId(), true);
				}
			} else {
				// If no shape file is provided, all the trip will be charged base price
				surchargeMap.put(drtRequestSubmittedEvent.getRequestId(), false);
			}
		}
	}

	@Override
	public void handleEvent(PassengerDroppedOffEvent event) {
		if (event.getMode().equals(mode)) {
			double actualFare = baseFare;
			boolean doesSurchargeApply = surchargeMap.get(event.getRequestId());
			if (doesSurchargeApply) {
				actualFare = actualFare + zone2Surcharge;
			}
			events.processEvent(
					new PersonMoneyEvent(event.getTime(), event.getPersonId(),
							-actualFare, DrtFareHandler.PERSON_MONEY_EVENT_PURPOSE_DRT_FARE, mode, event.getRequestId().toString()));
			surchargeMap.remove(event.getRequestId());
		}
	}

	@Override
	public void handleEvent(PassengerRequestRejectedEvent passengerRequestRejectedEvent) {
		if (passengerRequestRejectedEvent.getMode().equals(mode)) {
			surchargeMap.remove(passengerRequestRejectedEvent.getRequestId());
		}
	}

	@Override
	public void reset(int iteration) {
		surchargeMap.clear();
	}

	private Collection<SimpleFeature> getFeatures(String pathToShapeFile) {
		log.info("Reading shape file...");
		if (pathToShapeFile != null) {
			Collection<SimpleFeature> features;
			if (pathToShapeFile.startsWith("http")) {
				URL shapeFileAsURL = null;
				try {
					shapeFileAsURL = new URL(pathToShapeFile);
				} catch (MalformedURLException e) {
					log.error(e);
				}
				features = GeoFileReader.getAllFeatures(shapeFileAsURL);
			} else {
				features = GeoFileReader.getAllFeatures(pathToShapeFile);
			}
			return features;
		} else {
			log.error("Warning: Shapefile Path is null! All the trip will be charged the base price");
			return null;
		}
	}
}
