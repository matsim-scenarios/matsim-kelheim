package org.matsim.drtFare;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;

public class KelheimDrtFareModule extends AbstractDvrpModeModule {
    private final DrtConfigGroup drtCfg;
    private final Network network;

    public KelheimDrtFareModule(DrtConfigGroup drtCfg, Network network) {
        super(drtCfg.getMode());
        this.drtCfg = drtCfg;
        this.network = network;
    }

    @Override
    public void install() {
        // Default pricing scheme
        KelheimDrtFareParams kelheimDrtFareParams = new KelheimDrtFareParams(2.0, 1.0, getMode());
        kelheimDrtFareParams.setShapeFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/shp/KEXI-fare-shp/DrtFareZonalSystem2.shp");

        // Special price for Autonomous vehicles
        if (getMode().equals("AV")){
            kelheimDrtFareParams.setBaseFare(1.0);
            kelheimDrtFareParams.setZone2Surcharge(0.0);
            kelheimDrtFareParams.setMode("AV");
        }
        addEventHandlerBinding().toInstance(new KelheimDrtFareHandler(getMode(), network, kelheimDrtFareParams));
    }
}
