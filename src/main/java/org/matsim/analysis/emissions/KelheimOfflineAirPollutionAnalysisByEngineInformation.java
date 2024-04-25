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

import it.unimi.dsi.fastutil.objects.Object2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.*;
import org.matsim.contrib.emissions.analysis.EmissionsByPollutant;
import org.matsim.contrib.emissions.analysis.EmissionsOnLinkEventHandler;
import org.matsim.contrib.emissions.analysis.FastEmissionGridAnalyzer;
import org.matsim.contrib.emissions.analysis.Raster;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.*;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(
	name = "kelheim-air-pollution",
	description = "processes MATSim output leveraging the emission contrib.\n" +
		"Needs input tables from/according to HBEFA.\n" +
		"Produces output tables (csv files) that contain emission values per link (per meter) as well as emission events.",
	mixinStandardHelpOptions = true, showDefaultValues = true
)
@CommandSpec(requireRunDirectory = true,
	requireEvents = true,
	requireNetwork = true,
	produces = {
		"emissions_total.csv", "emissions_grid_per_day.xyt.csv", "emissions_per_link.csv",
		"emissions_per_link_per_m.csv",
		"emissions_grid_per_hour.csv",
		"emissions_vehicle_info.csv",
		"emissionNetwork.xml.gz"
	}
)
public class KelheimOfflineAirPollutionAnalysisByEngineInformation implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(KelheimOfflineAirPollutionAnalysisByEngineInformation.class);

	//Please set MATSIM_DECRYPTION_PASSWORD as environment variable to decrypt the files. ask VSP for access.
	private static final String HBEFA_2020_PATH = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/";
	private static final String HBEFA_FILE_COLD_DETAILED = HBEFA_2020_PATH + "82t7b02rc0rji2kmsahfwp933u2rfjlkhfpi2u9r20.enc";
	private static final String HBEFA_FILE_WARM_DETAILED = HBEFA_2020_PATH + "944637571c833ddcf1d0dfcccb59838509f397e6.enc";
	private static final String HBEFA_FILE_COLD_AVERAGE = HBEFA_2020_PATH + "r9230ru2n209r30u2fn0c9rn20n2rujkhkjhoewt84202.enc" ;
	private static final String HBEFA_FILE_WARM_AVERAGE = HBEFA_2020_PATH + "7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc";

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(KelheimOfflineAirPollutionAnalysisByEngineInformation.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(KelheimOfflineAirPollutionAnalysisByEngineInformation.class);
	@CommandLine.Mixin
	//TODO delete
	private final ShpOptions shp = new ShpOptions();
	@CommandLine.Mixin
	private SampleOptions sample;
	@CommandLine.Option(names = "--grid-size", description = "Grid size in meter", defaultValue = "250")
	private double gridSize;


	//dump out all pollutants. to include only a subset of pollutants, adjust!
	static List<Pollutant> pollutants2Output = Arrays.asList(Pollutant.values());

	@Override
	public Integer call() throws Exception {

		Config config = prepareConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		prepareNetwork(scenario);
		prepareVehicleTypes(scenario);
		process(config, scenario);

		return 0;
	}

	public static void main(String[] args) throws IOException {
		new KelheimOfflineAirPollutionAnalysisByEngineInformation().execute(args);
	}

	/**
	 * process output events, compute emission events and dump output.
	 * @param config input config
	 * @param scenario object to operate on (analyze)
	 * @throws IOException if output can't be written
	 */
	private void process(Config config, Scenario scenario) throws IOException {
		//------------------------------------------------------------------------------
		// the following is copied from the example and supplemented...
		//------------------------------------------------------------------------------


		NetworkUtils.writeNetwork(scenario.getNetwork(), output.getPath( "emissionNetwork.xml.gz").toString());

		final String eventsFile = input.getEventsPath();

		final String linkEmissionAnalysisFile = output.getPath("emissions_per_link.csv").toString();
		final String linkEmissionPerMAnalysisFile = output.getPath("emissions_per_link_per_m.csv").toString();
		final String vehicleTypeFile = output.getPath("emissions_vehicle_info.csv").toString();


		EventsManager eventsManager = EventsUtils.createEventsManager();
		AbstractModule module = new AbstractModule(){
			@Override
			public void install(){
				bind( Scenario.class ).toInstance(scenario);
				bind( EventsManager.class ).toInstance( eventsManager );
				bind( EmissionModule.class ) ;
			}
		};

		EmissionsOnLinkEventHandler emissionsEventHandler = new EmissionsOnLinkEventHandler(3600);
		eventsManager.addHandler(emissionsEventHandler);
		eventsManager.initProcessing();
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);
		log.info("Done reading the events file.");
		log.info("Finish processing...");
		eventsManager.finishProcessing();

		//we only output values for a subnetwork, if shp is defined. this speeds up vizes.
		Network filteredNetwork;
		if (shp.isDefined()) {
			ShpOptions.Index index = shp.createIndex(ProjectionUtils.getCRS(scenario.getNetwork()), "_");

			NetworkFilterManager manager = new NetworkFilterManager(scenario.getNetwork(), config.network());
			manager.addLinkFilter(l -> index.contains(l.getCoord()));

			filteredNetwork = manager.applyFilters();
		} else {
			filteredNetwork = scenario.getNetwork();
		}

		log.info("write basic output");
		writeTotal(filteredNetwork, emissionsEventHandler);
		writeVehicleInfo(scenario, vehicleTypeFile);
		log.info("write link output");
		writeLinkOutput(linkEmissionAnalysisFile, linkEmissionPerMAnalysisFile, filteredNetwork, emissionsEventHandler);


		log.info("write daily raster");
		writeRaster(scenario.getNetwork(), filteredNetwork, config, emissionsEventHandler);
		log.info("write hourly raster");
		writeTimeDependentRaster(scenario.getNetwork(), filteredNetwork, config, emissionsEventHandler);


		int totalVehicles = scenario.getVehicles().getVehicles().size();
		log.info("Total number of vehicles: " + totalVehicles);

		scenario.getVehicles().getVehicles().values().stream()
				.map(vehicle -> vehicle.getType())
				.collect(Collectors.groupingBy(category -> category, Collectors.counting()))
				.entrySet()
				.forEach(entry -> log.info("nr of " + VehicleUtils.getHbefaVehicleCategory(entry.getKey().getEngineInformation()) + " vehicles running on " + VehicleUtils.getHbefaEmissionsConcept(entry.getKey().getEngineInformation())
						+" = " + entry.getValue() + " (equals " + (100.0d * ((double) entry.getValue()) / ((double) totalVehicles)) + "% overall)"));
	}

	/**
	 * set all input files in EmissionConfigGroup as well as input from the MATSim run.
	 * @return the adjusted config
	 */
	private Config prepareConfig() {
		Config config = ConfigUtils.createConfig();
		config.vehicles().setVehiclesFile(ApplicationUtils.matchInput("allVehicles.xml.gz", input.getRunDirectory()).toAbsolutePath().toString());
		config.network().setInputFile(ApplicationUtils.matchInput("network", input.getRunDirectory()).toAbsolutePath().toString());
		config.transit().setTransitScheduleFile(ApplicationUtils.matchInput("transitSchedule", input.getRunDirectory()).toAbsolutePath().toString());
		config.transit().setVehiclesFile(ApplicationUtils.matchInput("transitVehicles", input.getRunDirectory()).toAbsolutePath().toString());
		config.global().setCoordinateSystem("EPSG:25832");
		config.plans().setInputFile(null);
		config.eventsManager().setNumberOfThreads(null);
		config.eventsManager().setEstimatedNumberOfEvents(null);
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
	 * changes/adds link attributes of the network in the given scenario.
	 * @param scenario for which to prepare the network
	 */
	private void prepareNetwork(Scenario scenario) {
		//prepare the network

		//do not use VspHbefaRoadTypeMapping() as it results in almost every road to mapped to "highway"!
		HbefaRoadTypeMapping roadTypeMapping = OsmHbefaMapping.build();
//		the type attribute in our network has the prefix "highway" for all links but pt links. we need to delete that because OsmHbefaMapping does not handle that.
		for (Link link : scenario.getNetwork().getLinks().values()) {
			//pt links can be disregarded
			if (!link.getAllowedModes().contains("pt")) {
				NetworkUtils.setType(link, NetworkUtils.getType(link).replaceFirst("highway.", ""));
			}
		}
		roadTypeMapping.addHbefaMappings(scenario.getNetwork());

	}


	/**
	 * we set all vehicles to average except for KEXI vehicles, i.e. drt. Drt vehicles are set to electric light commercial vehicles.
	 * @param scenario scenario object for which to prepare vehicle types
	 */
	private void prepareVehicleTypes(Scenario scenario) {
		for (VehicleType type : scenario.getVehicles().getVehicleTypes().values()) {
			EngineInformation engineInformation = type.getEngineInformation();
			VehicleUtils.setHbefaTechnology(engineInformation, "average");
			VehicleUtils.setHbefaSizeClass(engineInformation, "average");
			if (scenario.getTransitVehicles().getVehicleTypes().containsKey(type.getId())) {
				// consider transit vehicles as non-hbefa vehicles, i.e. ignore them
				VehicleUtils.setHbefaVehicleCategory( engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
			} else if (type.getId().toString().equals("car")){
				VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
				VehicleUtils.setHbefaEmissionsConcept(engineInformation, "average");
			} else if (type.getId().toString().equals("conventional_vehicle") || type.getId().toString().equals("autonomous_vehicle")){
				VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.LIGHT_COMMERCIAL_VEHICLE.toString());
				VehicleUtils.setHbefaEmissionsConcept(engineInformation, "electricity");
			} else if (type.getId().toString().equals("freight")){
				VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
				VehicleUtils.setHbefaEmissionsConcept(engineInformation, "average");
			} else {
				throw new IllegalArgumentException("does not know how to handle vehicleType " + type.getId().toString());
			}
		}
	}

	/**
	 * dumps the output.
	 * @param linkEmissionAnalysisFile path including file name and ending (csv) for the output file containing absolute emission values per link
	 * @param linkEmissionPerMAnalysisFile path including file name and ending (csv) for the output file containing emission values per meter, per link
	 * @param network the network for which utput is createdS
	 * @param emissionsEventHandler handler holding the emission data (from events-processing)
	 * @throws IOException if output can't be written
	 */
	private void writeLinkOutput(String linkEmissionAnalysisFile, String linkEmissionPerMAnalysisFile, Network network, EmissionsOnLinkEventHandler emissionsEventHandler) throws IOException {

		log.info("Emission analysis completed.");

		log.info("Writing output...");

		NumberFormat nf = NumberFormat.getInstance(Locale.US);
		nf.setMaximumFractionDigits(4);
		nf.setGroupingUsed(false);

		{
			//dump link-based output files
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

				// Link might be filtered
				if (!network.getLinks().containsKey(linkId))
					continue;

				absolutWriter.write(linkId.toString());
				perMeterWriter.write(linkId.toString());

				for (Pollutant pollutant : pollutants2Output) {
					double emissionValue = 0.;
					if (link2pollutants.get(linkId).get(pollutant) != null) {
						emissionValue = link2pollutants.get(linkId).get(pollutant);
					}
					absolutWriter.write(";" + nf.format(emissionValue));

					double emissionPerM = Double.NaN;
					Link link = network.getLinks().get(linkId);
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

	}

	private static void writeVehicleInfo(Scenario scenario, String vehicleTypeFileStr) throws IOException {
		//dump used vehicle types. in our (Kelheim) case not really needed as we did not change anything. But generally useful.
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

	private void writeTotal(Network network, EmissionsOnLinkEventHandler emissionsEventHandler) {

		Object2DoubleMap<Pollutant> sum = new Object2DoubleLinkedOpenHashMap<>();

		DecimalFormat simple = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		simple.setMaximumFractionDigits(2);
		simple.setMaximumIntegerDigits(5);

		DecimalFormat scientific = new DecimalFormat("0.###E0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

		for (Map.Entry<Id<Link>, Map<Pollutant, Double>> e : emissionsEventHandler.getLink2pollutants().entrySet()) {

			if (!network.getLinks().containsKey(e.getKey()))
				continue;
			for (Map.Entry<Pollutant, Double> p : e.getValue().entrySet()) {
				sum.mergeDouble(p.getKey(), p.getValue(), Double::sum);
			}
		}

		try (CSVPrinter total = new CSVPrinter(Files.newBufferedWriter(output.getPath("emissions_total.csv")), CSVFormat.DEFAULT)) {

			total.printRecord("Pollutant", "kg");
			for (Pollutant p : Pollutant.values()) {
				double val = (sum.getDouble(p) / sample.getSample()) / 1000;
				total.printRecord(p, val < 100_000 && val > 100 ? simple.format(val) : scientific.format(val));
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Creates the data for the XY-Time plot. The time is fixed and the data is summarized over the run.
	 * Currently only the CO2_Total Values is printed because Simwrapper can handle only one value.
	 */
	//ts, april 24:
	// this method produces (x,y,t) for the full network = entire germany, which is too big to be loaded into simwrapper.
	//we can not feed a filtered network, because otherwise exceptions are thrown because the rastering methods find links which are not in the filtered network
	//so we need to do some stupid filtering afterwards, which means that we produce and calculate more data than we dump out....
	private void writeRaster(Network fullNetwork, Network filteredNetwork, Config config, EmissionsOnLinkEventHandler emissionsEventHandler) {



		Map<Pollutant, Raster> rasterMap = FastEmissionGridAnalyzer.processHandlerEmissions(emissionsEventHandler.getLink2pollutants(), fullNetwork, gridSize, 20);

		Raster raster = rasterMap.values().stream().findFirst().orElseThrow();

		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("emissions_grid_per_day.xyt.csv")),
			CSVFormat.DEFAULT.builder().setCommentMarker('#').build())) {

			String crs = ProjectionUtils.getCRS(fullNetwork);
			if (crs == null)
				crs = config.network().getInputCRS();
			if (crs == null)
				crs = config.global().getCoordinateSystem();

			// print coordinate system
			printer.printComment(crs);

			// print header
			printer.print("time");
			printer.print("x");
			printer.print("y");

			printer.print("value");

			printer.println();

			Set<Coord> coords = filteredNetwork.getNodes().values().stream()
				.map(BasicLocation::getCoord)
				.collect(Collectors.toSet());
			Double minX = coords.stream().map(coord -> coord.getX())
				.min(Double::compare).orElse(Double.NEGATIVE_INFINITY);
			Double maxX = coords.stream().map(coord -> coord.getX())
				.max(Double::compare).orElse(Double.POSITIVE_INFINITY);
			Double minY = coords.stream().map(coord -> coord.getY())
				.min(Double::compare).orElse(Double.NEGATIVE_INFINITY);
			Double maxY = coords.stream().map(coord -> coord.getY())
				.max(Double::compare).orElse(Double.POSITIVE_INFINITY);

			//we only want to print raster data for the bounding box of the filtered network
			for (int xi = raster.getXIndex(minX); xi <= raster.getXIndex(maxX); xi++) {
				for (int yi = raster.getYIndex(minY); yi < raster.getYIndex(maxY); yi++) {

					Coord coord = raster.getCoordForIndex(xi, yi);

					printer.print(0.0);
					printer.print(coord.getX());
					printer.print(coord.getY());

					double value = rasterMap.get(Pollutant.CO2_TOTAL).getValueByIndex(xi, yi);
					printer.print(value);

					printer.println();
				}
			}

		} catch (IOException e) {
			log.error("Error writing results", e);
		}
	}

	//ts, april 24:
	// this method produces (x,y,t) for the full network = entire germany, which is too big to be loaded into simwrapper.
	//we can not feed a filtered network, because otherwise exceptions are thrown because the rastering methods find links which are not in the filtered network
	//so we need to do some stupid filtering afterwards, which means that we produce and calculate more data than we dump out....
	private void writeTimeDependentRaster(Network fullNetwork, Network filteredNetwork, Config config, EmissionsOnLinkEventHandler emissionsEventHandler) {

		//later we print C02_total only. so we pass corresponding data into the rasterization - in order to save resources (i had RAM problems before)
		Set<Pollutant> otherPollutants = new HashSet<>(pollutants2Output);
		otherPollutants.remove(Pollutant.CO2_TOTAL);
		TimeBinMap<Map<Id<Link>, EmissionsByPollutant>> handlerTimeBinMap = emissionsEventHandler.getTimeBins();
		for (TimeBinMap.TimeBin<Map<Id<Link>, EmissionsByPollutant>> perLink : handlerTimeBinMap.getTimeBins()) {
			Double time = perLink.getStartTime();
			for (Map.Entry<Id<Link>, EmissionsByPollutant> emissionsByPollutantEntry : perLink.getValue().entrySet()) {
				otherPollutants.forEach(pollutant -> emissionsByPollutantEntry.getValue().getEmissions().remove(pollutant));
				}
			}

		TimeBinMap<Map<Pollutant, Raster>> timeBinMap = FastEmissionGridAnalyzer.processHandlerEmissionsPerTimeBin(handlerTimeBinMap, fullNetwork, gridSize, 20);

		Map<Pollutant, Raster> firstBin = timeBinMap.getTimeBin(timeBinMap.getStartTime()).getValue();

		Raster raster = firstBin.values().stream().findFirst().orElseThrow();

		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("emissions_grid_per_hour.csv").toString()),
			CSVFormat.DEFAULT.builder().setCommentMarker('#').build())) {

			String crs = ProjectionUtils.getCRS(fullNetwork);
			if (crs == null)
				crs = config.network().getInputCRS();
			if (crs == null)
				crs = config.global().getCoordinateSystem();

			// print coordinate system
			printer.printComment(crs);

			// print header
			printer.print("time");
			printer.print("x");
			printer.print("y");

			printer.print("value");

			printer.println();

			Set<Coord> coords = filteredNetwork.getNodes().values().stream()
				.map(BasicLocation::getCoord)
				.collect(Collectors.toSet());
			Double minX = coords.stream().map(coord -> coord.getX())
				.min(Double::compare).orElse(Double.NEGATIVE_INFINITY);
			Double maxX = coords.stream().map(coord -> coord.getX())
				.max(Double::compare).orElse(Double.POSITIVE_INFINITY);
			Double minY = coords.stream().map(coord -> coord.getY())
				.min(Double::compare).orElse(Double.NEGATIVE_INFINITY);
			Double maxY = coords.stream().map(coord -> coord.getY())
				.max(Double::compare).orElse(Double.POSITIVE_INFINITY);

			//we only want to print raster data for the bounding box of the filtered network
			for (int xi = raster.getXIndex(minX); xi <= raster.getXIndex(maxX); xi++) {
				for (int yi = raster.getYIndex(minY); yi < raster.getYIndex(maxY); yi++) {
					for (TimeBinMap.TimeBin<Map<Pollutant, Raster>> timeBin : timeBinMap.getTimeBins()) {

						Coord coord = raster.getCoordForIndex(xi, yi);
						double value = timeBin.getValue().get(Pollutant.CO2_TOTAL).getValueByIndex(xi, yi);

//						if (value == 0)
//							continue;

						printer.print(timeBin.getStartTime());
						printer.print(coord.getX());
						printer.print(coord.getY());

						printer.print(value);

						printer.println();
					}
				}
			}

		} catch (IOException e) {
			log.error("Error writing results", e);
		}

	}

}
