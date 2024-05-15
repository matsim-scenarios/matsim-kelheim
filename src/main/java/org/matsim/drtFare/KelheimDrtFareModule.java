package org.matsim.drtFare;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;

/**
 * Module to bind fare handlers.
 */
public class KelheimDrtFareModule extends AbstractDvrpModeModule {
	private final DrtConfigGroup drtCfg;
	private final Network network;
	private final double avFare;
	private final double baseFare;
	private final double surcharge;

	public KelheimDrtFareModule(DrtConfigGroup drtCfg, Network network, double avFare, double baseFare, double surcharge) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
		this.network = network;
		this.avFare = avFare;
		this.baseFare = baseFare;
		this.surcharge = surcharge;
	}

	@Override
	public void install() {
		// Default pricing scheme
		KelheimDrtFareParams kelheimDrtFareParams = new KelheimDrtFareParams(baseFare, surcharge, getMode());
		kelheimDrtFareParams.setShapeFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/shp/KEXI-fare-shp/DrtFareZonalSystem2.shp");

		// Special price for Autonomous vehicles
		if (getMode().equals("av")) {
			kelheimDrtFareParams.setBaseFare(avFare);
			kelheimDrtFareParams.setZone2Surcharge(0.0);
			kelheimDrtFareParams.setMode("av");
		}
		addEventHandlerBinding().toInstance(new KelheimDrtFareHandler(getMode(), network, kelheimDrtFareParams));
	}
}
