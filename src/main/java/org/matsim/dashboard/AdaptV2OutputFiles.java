package org.matsim.dashboard;

import com.univocity.parsers.common.input.EOFException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import org.matsim.contrib.dvrp.fleet.FleetReader;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecificationImpl;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import picocli.CommandLine;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.io.csv.CsvWriteOptions;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.matsim.application.ApplicationUtils.globFile;

@CommandLine.Command(
	name = "adapt-v2-output",
	description = "Adapt run output from a v2 kelheim sim run, such that standard dashboards can be created for the run."
)
public class AdaptV2OutputFiles implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(AdaptV2OutputFiles.class);

	@CommandLine.Option(names = "--runDir", description = "Path of V2 run directory with files to adapt.", required = true)
	private String dir;

	@CommandLine.Option(names = "--drt-modes", description = "Drt modes to be analyzed. Separated by comma.", defaultValue = "drt,av")
	private String drtModes;

	@CommandLine.Option(names = "--drt-stops-files", description = "File names for drt stops files. This has to be adapted when param --drt-modes is adapted. Needs to be an absolute path Separated by comma.",
		defaultValue = "/net/ils/matsim-kelheim/scenarios/input/kelheim-v2.0/kelheim-v2.0-drt-stops.xml,/net/ils/matsim-kelheim/scenarios/input/kelheim-v2.0/av-stops-DP-AS.xml")
	private String drtStops;

	public static void main(String[] args) {
		new AdaptV2OutputFiles().execute(args);
	}

	@Override
	public Integer call() throws Exception {

//		copy stops files to run dir
		List<String> stopsFiles = Arrays.stream(drtStops.split(",")).toList();

		stopsFiles.forEach(path -> {
			File oldFile = new File(path);
			Path targetPath = Path.of(dir + "/" + oldFile.getName());

			copyFile(targetPath, oldFile);
		});

//		copy original config file
		File inputConfigFile = new File(globFile(Path.of(dir), "*output_config.xml").toString());
		Path targetConfigPath = Path.of(inputConfigFile.getPath().split(".xml")[0] + "_withZonalSystemParams_withTravelTimeCalculatorParam.xml");
		copyFile(targetConfigPath, inputConfigFile);

//		comment out config params which produce errors
		adaptConfigAndWriteXml(inputConfigFile);

		List<String> modes = Arrays.stream(drtModes.split(",")).toList();

		Map<String, List<String>> filePaths = new HashMap<>();

//		get all relevant filePaths and save to map for iterating over them
		modes.forEach(m -> {
			String legsPath = globFile(Path.of(dir + "/ITERS/it.999"), "*drt_legs_" + m + ".csv").toString();
			String vehicleStatsPath = globFile(Path.of(dir), "*drt_vehicle_stats_" + m + ".csv").toString();
			String customerStatsPath = globFile(Path.of(dir), "*drt_customer_stats_" + m + ".csv").toString();

			filePaths.put(m, List.of(legsPath, vehicleStatsPath, customerStatsPath));
			});


//		copy drt and av legs file from iters folder to global output
		filePaths.values().forEach(v -> {
			File legsFile = new File(v.getFirst());
			Path targetLegsPath = Path.of(dir + "/" + legsFile.getName());

			copyFile(targetLegsPath, legsFile);
		});

//		add totalServiceDuration column to vehicle stats file
		filePaths.forEach((k, v) -> {
			File vehicleStatsFile = new File(v.get(1));

			Table vehicleStats;
			try {
				vehicleStats = Table.read().csv(CsvReadOptions.builder(vehicleStatsFile)
					.separator(CsvOptions.detectDelimiter(vehicleStatsFile.getPath()))
					.build());
			} catch (IOException e) {
				throw new EOFException();
			}

			if (!vehicleStats.containsColumn("totalServiceDuration")) {
				FleetSpecification fleet = new FleetSpecificationImpl();
				new FleetReader(fleet).readFile(globFile(Path.of(dir), "*" + k + "_vehicles.xml*").toString());

				AtomicReference<Double> serviceDuration = new AtomicReference<>(0.);

				fleet.getVehicleSpecifications().values().forEach(veh -> serviceDuration.updateAndGet(v1 -> v1 + veh.getServiceEndTime() - veh.getServiceBeginTime()));

				double[] values = new double[vehicleStats.rowCount()];
				Arrays.fill(values, serviceDuration.get());

				DoubleColumn totalServiceDuration = DoubleColumn.create("totalServiceDuration", values);
				vehicleStats.addColumns(totalServiceDuration);
			}

			vehicleStats.write().csv(CsvWriteOptions.builder(vehicleStatsFile)
				.separator(';')
				.build());
		});

//		add rides_pax and dummy groupSize_mean columns to customer stats file
		filePaths.values().forEach(v -> {
			File customerStatsFile = new File(v.get(2));

			Table customerStats;
			try {
				customerStats = Table.read().csv(CsvReadOptions.builder(customerStatsFile)
					.separator(CsvOptions.detectDelimiter(customerStatsFile.getPath()))
					.build());
			} catch (IOException e) {
				throw new EOFException();
			}

			if (!customerStats.containsColumn("rides_pax") || !customerStats.containsColumn("groupSize_mean")) {
				if (customerStats.containsColumn("rides")) {
					IntColumn rides = customerStats.intColumn("rides");
					IntColumn ridesPax = rides.copy().setName("rides_pax");
					customerStats.addColumns(ridesPax);
				}
				double[] defaultValues = new double[customerStats.rowCount()];

				Arrays.fill(defaultValues, -1);
				DoubleColumn groupSizeMean = DoubleColumn.create("groupSize_mean", defaultValues);
				customerStats.addColumns(groupSizeMean);
			}

			customerStats.write().csv(CsvWriteOptions.builder(customerStatsFile)
				.separator(';')
				.build());
		});

		return 0;
	}

	private static void adaptConfigAndWriteXml(File inputConfigFile) throws ParserConfigurationException, SAXException, IOException, TransformerException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(inputConfigFile);
		doc.getDocumentElement().normalize();

		DocumentType doctype = doc.getDoctype();

		NodeList moduleList = doc.getElementsByTagName("module");
		for (int i = 0; i < moduleList.getLength(); i++) {
			Element moduleElement = (Element) moduleList.item(i);

			if (moduleElement.getAttribute("name").equals("travelTimeCalculator")) {
				NodeList paramList = moduleElement.getElementsByTagName("param");
				for (int j = 0; j < paramList.getLength(); j++) {
					Element paramElement = (Element) paramList.item(j);
					if (paramElement.getAttribute("name").equals("travelTimeCalculator")) {
						// Comment out the param element
						Comment comment = doc.createComment(paramElement.getTextContent());
						moduleElement.replaceChild(comment, paramElement);
						break;
					}
				}
				break;
			}

			if (moduleElement.getAttribute("name").equals("multiModeDrt")) {
				NodeList paramSetList = moduleElement.getElementsByTagName("parameterset");

				for (int k = 0; k < paramSetList.getLength(); k++) {
					Element paramSetElement = (Element) paramSetList.item(k);

					if (paramSetElement.getAttribute("type").equals("drt")) {
						NodeList innerParamSetList = paramSetElement.getElementsByTagName("parameterset");

						for (int l = 0; l < innerParamSetList.getLength(); l++) {
							Element innerParamSetElement = (Element) innerParamSetList.item(l);
							if (innerParamSetElement.getAttribute("type").equals("zonalSystem")) {

								// Comment out the "parameterset" of type "zonalSystem"
								Comment comment = doc.createComment(innerParamSetElement.getTextContent());
								paramSetElement.replaceChild(comment, innerParamSetElement);
								break;
							}
						}
					}
				}
			}
		}

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();

		transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());

		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(inputConfigFile);
		transformer.setOutputProperty(OutputKeys.INDENT, "no");
		transformer.transform(source, result);
	}

	private static void copyFile(Path targetPath, File inputFile) {
		if (Files.notExists(targetPath) && inputFile.exists() && inputFile.isFile()) {
			try {
				Files.copy(inputFile.toPath(), targetPath);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else {
			log.warn("File {} was not copied to target path {}. Please check if file already exists in target dir, the input file exists and the input file is not a directory."
				, inputFile.getAbsolutePath(), targetPath);
		}
	}
}
