package org.matsim.run;

import com.google.common.base.Preconditions;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.optimizer.rebalancing.NoRebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.DefaultAnalysisMainModeIdentifier;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.rebalancing.WaitingPointsBasedRebalancingModule;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.matsim.application.ApplicationUtils.globFile;

/***
 * This run class can identify the actual minimum required fleet size for a given AV demands from the output
 * of a standard MATSim run (where the fleet is usually more than enough).
 */
public class DrtFleetSizing implements MATSimAppCommand {
	@CommandLine.Option(names = "--run-folder", description = "Output folder of MATsim run", required = true)
	private String matsimRunFolderPath;

	@CommandLine.Option(names = "--target-wait-time", description = "the target average waiting time", required = true)
	private double meanWaitTime;

	@CommandLine.Option(names = "--drt-config", description = "config path", required = true)
	private String drtConfigPath;

	@CommandLine.Option(names = "--output", description = "output root folder", required = true)
	private String outputFolderPath;

	@CommandLine.Option(names = "--vehicles-folder", description = "folders of the vehicles files", required = true)
	private String vehiclesFolderPath;

	@CommandLine.Option(names = "--transit-stops", description = "transit stops of av service", required = true)
	private String transitStopFilePath;

	@CommandLine.Option(names = "--rebalancing", description = "enable waiting point based rebalancing strategy or not", defaultValue = "false")
	private boolean rebalancing;

	@CommandLine.Option(names = "--waiting-points", description = "waiting points for rebalancing strategy. If unspecified, the starting" +
		"points of the fleet will be set as waiting points", defaultValue = "")
	private String waitingPointsPath;

	@CommandLine.Option(names = "--fleet-sizing", description = "a triplet: [from max interval]. ", arity = "1..*", defaultValue = "10 50 5")
	private List<Integer> fleetSizing;


	public static void main(String[] args) {
		new DrtFleetSizing().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		// write output root folder
		if (!Files.exists(Path.of(outputFolderPath))) {
			Files.createDirectories(Path.of(outputFolderPath));
		}

		// read DRT trips and generate plans
		String drtNetworkPath = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/kelheim-v3.1/input/av-fleet-sizing/kelheim-v3.0-drt-network.xml.gz";
		Network drtNetwork = NetworkUtils.readNetwork(drtNetworkPath);
		Path outputPopulationPath = globFile(Path.of(matsimRunFolderPath), "*output_plans.xml.gz*");
		MainModeIdentifier modeIdentifier = new DefaultAnalysisMainModeIdentifier();
		Population outputPlans = PopulationUtils.readPopulation(outputPopulationPath.toString());
		Population avPlans = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		PopulationFactory pf = avPlans.getFactory();

		int counter = 0;
		for (Person person : outputPlans.getPersons().values()) {
			Plan selectedPlan = person.getSelectedPlan();
			List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(selectedPlan);
			for (TripStructureUtils.Trip trip : trips) {
				String mode = modeIdentifier.identifyMainMode(trip.getTripElements());
				if (mode.equals("av")) {
					Person avPerson = pf.createPerson(Id.createPersonId("dummy-" + counter));
					Plan avPlan = pf.createPlan();

					Activity act0 = trip.getOriginActivity();
					act0.setType("dummy");
					Coord fromCoord = act0.getCoord();
					Link fromLinkOnDrtNetwork = NetworkUtils.getNearestLink(drtNetwork, fromCoord);
					act0.setLinkId(fromLinkOnDrtNetwork.getId());

					Leg leg = pf.createLeg("av");

					Activity act1 = trip.getDestinationActivity();
					act1.setType("dummy");
					act1.setEndTime(30 * 3600);
					Coord toCoord = act1.getCoord();
					Link toLinkOnDrtNetwork = NetworkUtils.getNearestLink(drtNetwork, toCoord);
					act1.setLinkId(toLinkOnDrtNetwork.getId());

					act0.setStartTime(0);
					avPlan.addActivity(act0);
					avPlan.addLeg(leg);
					avPlan.addActivity(act1);
					avPerson.addPlan(avPlan);
					avPlans.addPerson(avPerson);

					counter++;
				}
			}
		}
		new PopulationWriter(avPlans).write(outputFolderPath + "/av-plans.xml.gz");

		// run DRT simulations
		Preconditions.checkArgument(fleetSizing.size() == 3);
		int fleetFrom = fleetSizing.get(0);
		int fleetMax = fleetSizing.get(1);
		int fleetInterval = fleetSizing.get(2);

		for (int fleetSize = fleetFrom; fleetSize <= fleetMax; fleetSize += fleetInterval) {
			// setup DRT run
			Config config = ConfigUtils.loadConfig(drtConfigPath, new MultiModeDrtConfigGroup(DrtWithExtensionsConfigGroup::new), new DvrpConfigGroup());
			config.network().setInputFile(drtNetworkPath);
			config.plans().setInputFile(outputFolderPath + "/av-plans.xml.gz");
			config.controller().setLastIteration(1);
			config.controller().setOutputDirectory(outputFolderPath + "/" + fleetSize + "-veh");
			config.vehicles().setVehiclesFile(vehiclesFolderPath + "/" + fleetSize + "-veh.xml");
			String singleDrtRunOutputDirectory = config.controller().getOutputDirectory();
			MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);
			Controler controler = DrtControlerCreator.createControler(config, false);

			for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
				if (drtCfg.getMode().equals("av")) {
					drtCfg.transitStopFile = transitStopFilePath;
					controler.addOverridingModule(new WaitingPointsBasedRebalancingModule(drtCfg, waitingPointsPath));
				}

				if (drtCfg.getMode().equals(TransportMode.drt)) {
					controler.addOverridingModule(new AbstractDvrpModeModule(drtCfg.mode) {
						@Override
						public void install() {
							bindModal(RebalancingStrategy.class).to(NoRebalancingStrategy.class).asEagerSingleton();
						}
					});
				}
			}

			// run simulation
			controler.run();

			// analyze mean waiting time
			Path waitTimeStatsPath = globFile(Path.of(singleDrtRunOutputDirectory), "*drt_customer_stats_av.csv*");
			double waitingTime = 0;
			CSVFormat.Builder format = CSVFormat.DEFAULT.builder().setDelimiter(';').setHeader().setSkipHeaderRecord(true);
			try (CSVParser parser = new CSVParser(IOUtils.getBufferedReader(waitTimeStatsPath.toString()), format.build())) {
				for (CSVRecord row : parser) {
					waitingTime = Double.parseDouble(row.get("wait_average"));
					// we take the value of the last row (Probably not the best way, but it should do the job...).
				}
			}

			if (waitingTime < meanWaitTime) {
				break;
			}
		}

		return 0;
	}
}
