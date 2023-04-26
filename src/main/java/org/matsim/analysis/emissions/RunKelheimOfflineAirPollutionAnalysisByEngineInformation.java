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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;


public class RunKelheimOfflineAirPollutionAnalysisByEngineInformation {

	private static final Logger log = LogManager.getLogger(RunKelheimOfflineAirPollutionAnalysisByEngineInformation.class);

	private static final String HBEFA_2020_PATH = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/";
	private static final String HBEFA_FILE_COLD_DETAILED = "../../svn/shared-svn/projects/matsim-germany/hbefa/hbefa-files/v4.1/EFA_ColdStart_Concept_2020_detailed_perTechAverage_withHGVetc.csv";
	private static final String HBEFA_FILE_WARM_DETAILED = HBEFA_2020_PATH + "944637571c833ddcf1d0dfcccb59838509f397e6.enc";
	private static final String HBEFA_FILE_COLD_AVERAGE = HBEFA_2020_PATH + "22823adc0ee6a0e231f35ae897f7b224a86f3a7a.enc";
	private static final String HBEFA_FILE_WARM_AVERAGE = HBEFA_2020_PATH + "7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc";

	private final String runDirectory;
	private final String runId;
	private final String analysisOutputDirectory;

//	static List<Pollutant> pollutants2Output = Arrays.asList(CO2_TOTAL, NOx, PM, PM_non_exhaust);
	static List<Pollutant> pollutants2Output = Arrays.asList(Pollutant.values()); //dump out all pollutants

	RunKelheimOfflineAirPollutionAnalysisByEngineInformation(String runDirectory,
															 String runId,
															 String analysisOutputDirectory) {
		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";
		this.runDirectory = runDirectory;

		this.runId = runId;

		if (!analysisOutputDirectory.endsWith("/")) analysisOutputDirectory = analysisOutputDirectory + "/";
		this.analysisOutputDirectory = analysisOutputDirectory;

	}

	public static void main(String[] args) throws IOException {

		//TODO: Please set MATSIM_DECRYPTION_PASSWORD as environment variable to decrypt the files. ask VSP for access.

		//actually the hbefa files need to be set relative to the config or by absolute path...
//		final String hbefaFileColdDetailed = hbefa2020Path + "0e73947443d68f95202b71a156b337f7f71604ae/5a297db51545335b2f7899002a1ea6c45d4511a3.enc";



		final String runId = "008" ;
		String runDirectory = "//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/matsim-kelheim/calibration-v3/runs/008";
		RunKelheimOfflineAirPollutionAnalysisByEngineInformation analysis = new RunKelheimOfflineAirPollutionAnalysisByEngineInformation(
				runDirectory,
				runId,
				runDirectory + "emission-analysis-hbefa-v4.1-2020");
		try {
			analysis.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void run() throws IOException {

		Config config = prepareConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		prepareNetwork(scenario);

//		NetworkUtils.writeNetwork(scenario.getNetwork(), "D:/KelRide/2023-04-26-testEmissionAnalyis/kelheim-v3-net-osmhbefamapping.xml.gz");
//		NetworkUtils.writeNetwork(scenario.getNetwork(), "D:/KelRide/2023-04-26-testEmissionAnalyis/kelheim-v3-net-vsphbefamapping.xml.gz");



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
	}

	private void process(Config config, Scenario scenario) throws IOException {
		//------------------------------------------------------------------------------
		// the following is copied from the example and supplemented...
		//------------------------------------------------------------------------------

		File folder = new File(analysisOutputDirectory);
		folder.mkdirs();

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
						+" = " + entry.getValue() + " (equals " + ((double)entry.getValue()/(double)totalVehicles) + "% overall)"));

//		scenario.getVehicles().getVehicles().values().stream()
//				.map(vehicle -> {
//					return VehicleUtils.getHbefaEmissionsConcept(vehicle.getType().getEngineInformation()) == null ? "NONE" : VehicleUtils.getHbefaEmissionsConcept(vehicle.getType().getEngineInformation());
//				})
//				.collect(Collectors.groupingBy(category -> category, Collectors.counting()))
//				.entrySet()
//				.forEach(entry -> log.info("nr of " + entry.getKey() + " vehicles = " + entry.getValue() + " (equals " + ((double)entry.getValue()/(double)totalVehicles) + "%)"));
	}

	private Config prepareConfig() {
		//TODO: let's load the actual output config instead of filling a dummy one. Hopefully this does not size up the scenario too much. This way, we can get access to actually used values
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
		eConfig.setHbefaTableConsistencyCheckingLevel(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.consistent); //TODO ?????
		return config;
	}

	private void prepareNetwork(Scenario scenario) {
		//prepare the network
//		HbefaRoadTypeMapping roadTypeMapping = new OsmHbefaMapping(); //alternatively, use new VspHbefaRoadTypeMapping() // TODO compare
		//OsmHbefaMapping currently does not work because our link types
		// 1) have the prefix "highway."
		// 2) have suffixes "_link"
		// 3) are not handled, specifically "highway.primary|railway.tram"
		HbefaRoadTypeMapping roadTypeMapping = new VspHbefaRoadTypeMapping();

		roadTypeMapping.addHbefaMappings(scenario.getNetwork());
	}

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
				VehicleUtils.setHbefaEmissionsConcept(engineInformation, "average"); //TODO?
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
				VehicleUtils.setHbefaEmissionsConcept(engineInformation, "average"); //TODO ?
			} else {
				throw new IllegalArgumentException("does not know how to handle vehicleType " + type.getId().toString());
			}
		}
	}

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
