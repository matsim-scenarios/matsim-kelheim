
JAR := matsim-kelheim-*.jar
V := v1.0
CRS := EPSG:25832

export SUMO_HOME := $(abspath ../../sumo-1.8.0/)
osmosis := osmosis\bin\osmosis

REGIONS := baden-wuerttemberg bayern brandenburg bremen hamburg hessen mecklenburg-vorpommern niedersachsen nordrhein-westfalen\
	rheinland-pfalz saarland sachsen sachsen-anhalt schleswig-holstein thueringen

SHP_FILES=$(patsubst %, scenarios/shp/%-latest-free.shp.zip, $(REGIONS))

.PHONY: prepare

$(JAR):
	mvn package

# Required files
scenarios/input/network.osm.pbf:
	curl https://download.geofabrik.de/europe/germany-210101.osm.pbf\
	  -o scenarios/input/network.osm.pbf

${SHP_FILES} :
	mkdir -p scenarios/shp
	curl https://download.geofabrik.de/europe/germany/$(@:scenarios/shp/%=%) -o $@

#scenarios/input/gtfs-lvb.zip:
#	curl https://opendata.kelheim.de/dataset/8803f612-2ce1-4643-82d1-213434889200/resource/b38955c4-431c-4e8b-a4ef-9964a3a2c95d/download/gtfsmdvlvb.zip\
#	  -o $@

scenarios/input/network.osm: scenarios/input/network.osm.pbf

	$(osmosis) --rb file=$<\
	 --tf accept-ways highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary_link,secondary,tertiary,motorway_junction,residential,unclassified,living_street\
	 --bounding-box top=49.003 left=11.556 bottom=48.591 right=12.119\
	 --used-node --wb network-detailed.osm.pbf

	$(osmosis) --rb file=$<\
	 --tf accept-ways highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary_link,secondary,tertiary,motorway_junction\
	 --bounding-box top=49.08 left=11.31 bottom=48.50 right=12.24\
	 --used-node --wb network-coarse.osm.pbf

	$(osmosis) --rb file=$<\
	 --tf accept-ways highway=motorway,motorway_link,motorway_junction,trunk,trunk_link,primary,primary_link\
	 --used-node --wb network-germany.osm.pbf

	$(osmosis) --rb file=network-germany.osm.pbf --rb file=network-coarse.osm.pbf --rb file=network-detailed.osm.pbf\
  	 --merge --merge --wx $@

	rm network-detailed.osm.pbf
	rm network-coarse.osm.pbf
	rm network-germany.osm.pbf


scenarios/input/sumo.net.xml: scenarios/input/network.osm

	$(SUMO_HOME)/bin/netconvert --geometry.remove --ramps.guess --ramps.no-split\
	 --type-files $(SUMO_HOME)/data/typemap/osmNetconvert.typ.xml,$(SUMO_HOME)/data/typemap/osmNetconvertUrbanDe.typ.xml\
	 --tls.guess-signals true --tls.discard-simple --tls.join --tls.default-type actuated\
	 --junctions.join --junctions.corner-detail 5\
	 --roundabouts.guess --remove-edges.isolated\
	 --no-internal-links --keep-edges.by-vclass passenger,bicycle --remove-edges.by-type highway.track,highway.services,highway.unsurfaced\
	 --remove-edges.by-vclass hov,tram,rail,rail_urban,rail_fast,pedestrian\
	 --output.original-names --output.street-names\
	 --proj "+proj=utm +zone=32 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs"\
	 --osm-files $< -o=$@


scenarios/input/kelheim-$V-network.xml.gz: scenarios/input/sumo.net.xml
	java -jar $(JAR) prepare network-from-sumo $<\
	 --output $@

#scenarios/input/kelheim-$V-network-with-pt.xml.gz: scenarios/input/kelheim-$V-network.xml.gz scenarios/input/gtfs-lvb.zip
#	java -jar $(JAR) prepare transit-from-gtfs --network $< $(filter-out $<,$^)\
#	 --name kelheim-$V --date "2019-06-05" --target-crs $(CRS)

scenarios/input/freight-trips.xml.gz: scenarios/input/kelheim-$V-network.xml.gz
	java -jar $(JAR) prepare extract-freight-trips ../../shared-svn/komodnext/data/freight/German-freight-25pct.plans.xml.gz\
	 --network ../../shared-svn/komodnext/data/freight/original_data/german-primary-road.network.xml.gz\
	 --input-crs EPSG:5677\
	 --target-crs $(CRS)\
	 --shp ../../shared-svn/NaMAV/data/freight-area/freight-area.shp --shp-crs $(CRS)\
	 --output $@

scenarios/input/landuse/landuse.shp: ${SHP_FILES}
	mkdir -p scenarios/input/landuse
	java -Xmx20G -jar $(JAR) prepare create-landuse-shp $^\
	 --target-crs ${CRS}\
	 --output $@

scenarios/input/kelheim-$V-25pct.plans.xml.gz: scenarios/input/freight-trips.xml.gz scenarios/input/landuse/landuse.shp
	java -jar $(JAR) prepare trajectory-to-plans\
	 --name prepare --sample-size 0.25\
	 --population ../../shared-svn/NaMAV/matsim-input-files/senozon/20210309_kelheim/optimizedPopulation_filtered.xml.gz\
	 --attributes  ../../shared-svn/NaMAV/matsim-input-files/senozon/20210309_kelheim/personAttributes.xml.gz

	java -jar $(JAR) prepare resolve-grid-coords\
	 scenarios/input/prepare-25pct.plans.xml.gz\
	 --input-crs $(CRS)\
	 --grid-resolution 500\
	 --landuse scenarios/input/landuse/landuse.shp\
	 --output scenarios/input/prepare-25pct.plans.xml.gz

	java -jar $(JAR) prepare generate-short-distance-trips\
 	 --population scenarios/input/prepare-25pct.plans.xml.gz\
 	 --input-crs $(CRS)\
 	 --shp ../../shared-svn/NaMAV/data/kelheim-utm32n/kelheim-utm32n.shp --shp-crs $(CRS)\
 	 --num-trips 49200

	java -jar $(JAR) prepare merge-populations scenarios/input/prepare-25pct.plans-with-trips-with-trips.xml.gz $<\
     --output scenarios/input/kelheim-$V-25pct.plans.xml.gz

	java -jar $(JAR) prepare downsample-population scenarios/input/kelheim-$V-25pct.plans.xml.gz\
    	 --sample-size 0.25\
    	 --samples 0.1 0.01\


check: scenarios/input/kelheim-$V-25pct.plans.xml.gz
	java -jar $(JAR) analysis check-population $<\
 	 --input-crs $(CRS)\
 	 --shp ../../shared-svn/NaMAV/data/kelheim-utm32n/kelheim-utm32n.shp\

# Aggregated target
prepare: scenarios/input/kelheim-$V-25pct.plans.xml.gz scenarios/input/kelheim-$V-network-with-pt.xml.gz
	echo "Done"