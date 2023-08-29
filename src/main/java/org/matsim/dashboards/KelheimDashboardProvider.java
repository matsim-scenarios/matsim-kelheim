package org.matsim.dashboards;

import org.matsim.application.ApplicationUtils;
import org.matsim.core.config.Config;
import org.matsim.run.RunKelheimScenario;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.DashboardProvider;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.dashboard.TravelTimeComparisonDashboard;
import org.matsim.simwrapper.dashboard.TripDashboard;

import java.util.List;

/**
 * Provider for default dashboards in the scenario.
 * Declared in META-INF/services
 */
public class KelheimDashboardProvider implements DashboardProvider {

	@Override
	public List<Dashboard> getDashboards(Config config, SimWrapper simWrapper) {

		TripDashboard trips = new TripDashboard("kelheim_mode_share.csv", "kelheim_mode_share_per_dist.csv", null);

		trips.setAnalysisArgs("--dist-groups", "0,1000,2000,5000,10000,20000");
		return List.of(
			trips,
			new TravelTimeComparisonDashboard(ApplicationUtils.resolve(config.getContext(), "kelheim-v" + RunKelheimScenario.VERSION + "-routes-ref.csv.gz"))
		);
	}

}
