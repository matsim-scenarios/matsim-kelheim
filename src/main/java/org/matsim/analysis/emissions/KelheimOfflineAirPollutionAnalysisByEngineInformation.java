/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.analysis.emissions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.preAnalysis.ActivityLengthAnalysis;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.emissions.*;
import org.matsim.contrib.emissions.analysis.EmissionsOnLinkEventHandler;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.*;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * processes MATSim output leveraging the emission contrib.<br>
 * needs input tables from/according to HBEFA.<br>
 * produces output tables (csv files) that contain emission values per link (per meter) as well as emission events.
 *
 */
class KelheimOfflineAirPollutionAnalysisByEngineInformation implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(KelheimOfflineAirPollutionAnalysisByEngineInformation.class);

	//Please set MATSIM_DECRYPTION_PASSWORD as environment variable to decrypt the files. ask VSP for access.
	private static final String HBEFA_2020_PATH = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/";
	private static final String HBEFA_FILE_COLD_DETAILED = "../../svn/shared-svn/projects/matsim-germany/hbefa/hbefa-files/v4.1/EFA_ColdStart_Concept_2020_detailed_perTechAverage_withHGVetc.csv.enc"; //TODO adjust to public svn encrypted version
	private static final String HBEFA_FILE_WARM_DETAILED = HBEFA_2020_PATH + "944637571c833ddcf1d0dfcccb59838509f397e6.enc";
	private static final String HBEFA_FILE_COLD_AVERAGE = "../../svn/shared-svn/projects/matsim-germany/hbefa/hbefa-files/v4.1/EFA_ColdStart_Vehcat_2020_Average_withHGVetc.csv.enc"; //TODO adjust to public svn encrypted version
	private static final String HBEFA_FILE_WARM_AVERAGE = HBEFA_2020_PATH + "7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc";

	@CommandLine.Option(names = "--runDir", description = "Path to MATSim output directory containing network, events, ....", required = true)
	private String runDirectory;
	@CommandLine.Option(names = "--runId", description = "runId of the corresponding MATSim run to analyzed", required = true)
	private String runId;
	@CommandLine.Option(names = "--output", description = "output directory (must not pre-exist)", required = true)
	private String analysisOutputDirectory;

//	static List<Pollutant> pollutants2Output = Arrays.asList(CO2_TOTAL, NOx, PM, PM_non_exhaust);
	static List<Pollutant> pollutants2Output = Arrays.asList(Pollutant.values()); //dump out all pollutants

	@Override
	public Integer call() throws Exception {
		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";
		if (!analysisOutputDirectory.endsWith("/")) analysisOutputDirectory = analysisOutputDirectory + "/";

		Config config = prepareConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		prepareNetwork(scenario);
		prepareVehicleTypes(scenario);

		{//analyze current distribution. TODO: could be integrated above in order to save lines?
			scenario.getVehicles().getVehicles().values().stream()
					.map(vehicle -> { //map vehicle to category
						if(scenario.getTransitVehicles().getVehicles().get(vehicle.getId()) != null) return "transit";
						else if(vehicle.getId().toString().contains("freight") || vehicle.getId().toString().contains("commercial")) return "freight";
						else if(vehicle.getId().toString().contains("conventional_vehicle"))  return "KEXI conv.";
						else if(vehicle.getId().toString().contains("autonomous_vehicle"))  return "KEXI auton.";
						else if(vehicle.getType().getId().toString().equals("car")) return "standard (private)";
						else return "unknown";
					})
					.collect(Collectors.groupingBy(category -> category, Collectors.counting()))
					.entrySet()
					.forEach(entry -> log.info("nr of " + entry.getKey() + " vehicles = " + entry.getValue()));
		}

		process(config, scenario);
		return 0;
	}

	public static void main(String[] args) throws IOException {
		new KelheimOfflineAirPollutionAnalysisByEngineInformation().execute(args);
	}

	/**
	 * process output events, compute emission events and dump output.
	 * @param config
	 * @param scenario
	 * @throws IOException
	 */
	private void process(Config config, Scenario scenario) throws IOException {
		//------------------------------------------------------------------------------
		// the following is copied from the example and supplemented...
		//------------------------------------------------------------------------------

		File folder = new File(analysisOutputDirectory);
		folder.mkdirs();

		String outputNetworkFile = analysisOutputDirectory + runId + ".emissionNetwork.xml.gz";
		NetworkUtils.writeNetwork(scenario.getNetwork(), outputNetworkFile);

		final String eventsFile = runDirectory + runId + ".output_events.xml.gz";

		final String emissionEventOutputFile = analysisOutputDirectory + runId + ".emission.events.offline.xml.gz";
		final String linkEmissionAnalysisFile = analysisOutputDirectory + runId + ".emissionsPerLink.csv";
		final String linkEmissionPerMAnalysisFile = analysisOutputDirectory + runId + ".emissionsPerLinkPerM.csv";
		final String vehicleTypeFile = analysisOutputDirectory + runId + ".emissionVehicleInformation.csv";


		EventsManager eventsManager = EventsUtils.createEventsManager();
		AbstractModule module = new AbstractModule(){
			@Override
			public void install(){
				bind( Scenario.class ).toInstance(scenario);
				bind( EventsManager.class ).toInstance( eventsManager );
				bind( EmissionModule.class ) ;
			}
		};

		com.google.inject.Injector injector = Injector.createInjector(config, module);
		EmissionModule emissionModule = injector.getInstance(EmissionModule.class);

		EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
		emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

		EmissionsOnLinkEventHandler emissionsEventHandler = new EmissionsOnLinkEventHandler(3600);
		eventsManager.addHandler(emissionsEventHandler);
		eventsManager.initProcessing();
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);
		log.info("Done reading the events file.");
		log.info("Finish processing...");
		eventsManager.finishProcessing();

		log.info("Closing events file...");
		emissionEventWriter.closeFile();

		writeOutput(linkEmissionAnalysisFile, linkEmissionPerMAnalysisFile, vehicleTypeFile, scenario, emissionsEventHandler);

		int totalVehicles = scenario.getVehicles().getVehicles().size();
		log.info("Total number of vehicles: " + totalVehicles);

		scenario.getVehicles().getVehicles().values().stream()
				.map(vehicle -> vehicle.getType())
				.collect(Collectors.groupingBy(category -> category, Collectors.counting()))
				.entrySet()
				.forEach(entry -> log.info("nr of " + VehicleUtils.getHbefaVehicleCategory(entry.getKey().getEngineInformation()) + " vehicles running on " + VehicleUtils.getHbefaEmissionsConcept(entry.getKey().getEngineInformation())
						+" = " + entry.getValue() + " (equals " + (100.0d*(double)entry.getValue()/(double)totalVehicles) + "% overall)"));

//		scenario.getVehicles().getVehicles().values().stream()
//				.map(vehicle -> {
//					return VehicleUtils.getHbefaEmissionsConcept(vehicle.getType().getEngineInformation()) == null ? "NONE" : VehicleUtils.getHbefaEmissionsConcept(vehicle.getType().getEngineInformation());
//				})
//				.collect(Collectors.groupingBy(category -> category, Collectors.counting()))
//				.entrySet()
//				.forEach(entry -> log.info("nr of " + entry.getKey() + " vehicles = " + entry.getValue() + " (equals " + ((double)entry.getValue()/(double)totalVehicles) + "%)"));
	}

	/**
	 * set all input files in EmissionConfigGroup as well as input from the MATSim run.
	 * @return
	 */
	private Config prepareConfig() {
		Config config = ConfigUtils.createConfig();
//		Config config = ConfigUtils.loadConfig(runDirectory + runId + ".output_config.xml");

		config.vehicles().setVehiclesFile( runDirectory + runId + ".output_allVehicles.xml.gz");
		config.network().setInputFile( runDirectory +runId + ".output_network.xml.gz");
		config.transit().setTransitScheduleFile( runDirectory +runId + ".output_transitSchedule.xml.gz");
		config.transit().setVehiclesFile( runDirectory + runId + ".output_transitVehicles.xml.gz");
		config.global().setCoordinateSystem("EPSG:25832");
		config.plans().setInputFile(null);
		config.parallelEventHandling().setNumberOfThreads(null);
		config.parallelEventHandling().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(1);

		EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
		eConfig.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable);
		eConfig.setDetailedColdEmissionFactorsFile(HBEFA_FILE_COLD_DETAILED);
		eConfig.setDetailedWarmEmissionFactorsFile(HBEFA_FILE_WARM_DETAILED);
		eConfig.setAverageColdEmissionFactorsFile(HBEFA_FILE_COLD_AVERAGE);
		eConfig.setAverageWarmEmissionFactorsFile(HBEFA_FILE_WARM_AVERAGE);
//		eConfig.setHbefaRoadTypeSource(HbefaRoadTypeSource.fromLinkAttributes);
		eConfig.setNonScenarioVehicles(EmissionsConfigGroup.NonScenarioVehicles.abort);
		eConfig.setWritingEmissionsEvents(true);
		eConfig.setHbefaTableConsistencyCheckingLevel(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.consistent);
		return config;
	}

	/**
	 * changes/adds link attributes of the network in the given scenario
	 * @param scenario
	 */
	private void prepareNetwork(Scenario scenario) {
		//prepare the network

		HbefaRoadTypeMapping roadTypeMapping = OsmHbefaMapping.build(); //alternatively, use new VspHbefaRoadTypeMapping() //
//		the type attribute in our network has the prefix "highway" for all links but pt links. we need to delete that because OsmHbefaMapping does not handle that.
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if(!link.getAllowedModes().contains("pt")) { //pt links can be disregarded
				NetworkUtils.setType(link, NetworkUtils.getType(link).replaceFirst("highway.", ""));
			}
		}
		//do not use VspHbefaRoadTypeMapping() as it results in almost every road to mapped to "highway"!
//		HbefaRoadTypeMapping roadTypeMapping = new VspHbefaRoadTypeMapping();

		roadTypeMapping.addHbefaMappings(scenario.getNetwork());
	}

	/**
	 * we set all vehicles to average except for KEXI vehicles, i.e. drt. Drt vehicles are set to electric light commercial vehicles.
	 *
	 * @param scenario
	 */
	private void prepareVehicleTypes(Scenario scenario) {
		for (VehicleType type : scenario.getVehicles().getVehicleTypes().values()) {
			EngineInformation engineInformation = type.getEngineInformation();
			if (scenario.getTransitVehicles().getVehicleTypes().containsKey(type.getId())) {
				// consider transit vehicles as non-hbefa vehicles, i.e. ignore them
				VehicleUtils.setHbefaVehicleCategory( engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
			} else if(type.getId().toString().equals("car")){
				VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
				VehicleUtils.setHbefaTechnology(engineInformation, "average");
				VehicleUtils.setHbefaSizeClass(engineInformation, "average");
				VehicleUtils.setHbefaEmissionsConcept(engineInformation, "average");
			} else if (type.getId().toString().equals("conventional_vehicle")){
				VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.LIGHT_COMMERCIAL_VEHICLE.toString());
				VehicleUtils.setHbefaTechnology(engineInformation, "average");
				VehicleUtils.setHbefaSizeClass(engineInformation, "average");
				VehicleUtils.setHbefaEmissionsConcept(engineInformation, "electricity");
			} else if (type.getId().toString().equals("autonomous_vehicle")){
				VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.LIGHT_COMMERCIAL_VEHICLE.toString());
				VehicleUtils.setHbefaTechnology(engineInformation, "average");
				VehicleUtils.setHbefaSizeClass(engineInformation, "average");
				VehicleUtils.setHbefaEmissionsConcept(engineInformation, "electricity");
			} else if (type.getId().toString().equals("freight")){
				VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
				VehicleUtils.setHbefaTechnology(engineInformation, "average");
				VehicleUtils.setHbefaSizeClass(engineInformation, "average");
				VehicleUtils.setHbefaEmissionsConcept(engineInformation, "average");
			} else {
				throw new IllegalArgumentException("does not know how to handle vehicleType " + type.getId().toString());
			}
		}
	}

	/**
	 *
	 * @param linkEmissionAnalysisFile
	 * @param linkEmissionPerMAnalysisFile
	 * @param vehicleTypeFileStr
	 * @param scenario
	 * @param emissionsEventHandler
	 * @throws IOException
	 */
	private void writeOutput(String linkEmissionAnalysisFile, String linkEmissionPerMAnalysisFile, String vehicleTypeFileStr, Scenario scenario, EmissionsOnLinkEventHandler emissionsEventHandler) throws IOException {

		log.info("Emission analysis completed.");

		log.info("Writing output...");

		NumberFormat nf = NumberFormat.getInstance(Locale.US);
		nf.setMaximumFractionDigits(4);
		nf.setGroupingUsed(false);

		{ //dump link-based output files
			File absolutFile = new File(linkEmissionAnalysisFile);
			File perMeterFile = new File(linkEmissionPerMAnalysisFile);

			BufferedWriter absolutWriter = new BufferedWriter(new FileWriter(absolutFile));
			BufferedWriter perMeterWriter = new BufferedWriter(new FileWriter(perMeterFile));

			absolutWriter.write("linkId");
			perMeterWriter.write("linkId");

			for (Pollutant pollutant : pollutants2Output) {
				absolutWriter.write(";" + pollutant);
				perMeterWriter.write(";" + pollutant + " [g/m]");

			}
			absolutWriter.newLine();
			perMeterWriter.newLine();


			Map<Id<Link>, Map<Pollutant, Double>> link2pollutants = emissionsEventHandler.getLink2pollutants();

			for (Id<Link> linkId : link2pollutants.keySet()) {
				absolutWriter.write(linkId.toString());
				perMeterWriter.write(linkId.toString());

				for (Pollutant pollutant : pollutants2Output) {
					double emissionValue = 0.;
					if (link2pollutants.get(linkId).get(pollutant) != null) {
						emissionValue = link2pollutants.get(linkId).get(pollutant);
					}
					absolutWriter.write(";" + nf.format(emissionValue));

					double emissionPerM = Double.NaN;
					Link link = scenario.getNetwork().getLinks().get(linkId);
					if (link != null) {
						emissionPerM = emissionValue / link.getLength();
					}
					perMeterWriter.write(";" + nf.format(emissionPerM));

				}
				absolutWriter.newLine();
				perMeterWriter.newLine();

			}

			absolutWriter.close();
			log.info("Output written to " + linkEmissionAnalysisFile);
			perMeterWriter.close();
			log.info("Output written to " + linkEmissionPerMAnalysisFile);
		}

		{ //dump used vehicle types. in our (Kelheim) case not really needed as we did not change anything. But generally useful.
			File vehicleTypeFile = new File(vehicleTypeFileStr);

			BufferedWriter vehicleTypeWriter = new BufferedWriter(new FileWriter(vehicleTypeFile));

			vehicleTypeWriter.write("vehicleId;vehicleType;emissionsConcept");
			vehicleTypeWriter.newLine();

			for (Vehicle vehicle : scenario.getVehicles().getVehicles().values()) {
				String emissionsConcept = "null";
				if (vehicle.getType().getEngineInformation() != null && VehicleUtils.getHbefaEmissionsConcept(vehicle.getType().getEngineInformation()) != null) {
					emissionsConcept = VehicleUtils.getHbefaEmissionsConcept(vehicle.getType().getEngineInformation());
				}

				vehicleTypeWriter.write(vehicle.getId() + ";" + vehicle.getType().getId().toString() + ";" + emissionsConcept);
				vehicleTypeWriter.newLine();
			}

			vehicleTypeWriter.close();
			log.info("Output written to " + vehicleTypeFileStr);
		}
	}


}
