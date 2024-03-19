
V := v3.1
CRS := EPSG:25832

MEMORY ?= 20G
JAR := matsim-kelheim-*.jar

ifndef SUMO_HOME
	export SUMO_HOME := $(abspath ../../sumo-1.15.0/)
endif

osmosis := osmosis/bin/osmosis

# Scenario creation tool
sc := java -Xmx$(MEMORY) -jar $(JAR)

.PHONY: prepare

$(JAR):
	mvn package

# Required files
input/network.osm.pbf:
	curl https://download.geofabrik.de/europe/germany-220101.osm.pbf\
	  -o input/network.osm.pbf


#input/gtfs-lvb.zip:
#	curl https://opendata.kelheim.de/dataset/8803f612-2ce1-4643-82d1-213434889200/resource/b38955c4-431c-4e8b-a4ef-9964a3a2c95d/download/gtfsmdvlvb.zip\
#	  -o $@

input/network.osm: input/network.osm.pbf

	$(osmosis) --rb file=$<\
	 --tf accept-ways highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary_link,secondary,tertiary,motorway_junction,residential,unclassified,living_street,service\
	 --bounding-box top=48.977 left=11.779 bottom=48.854 right=12.019\
	 --used-node --wb network-service.osm.pbf

	$(osmosis) --rb file=$<\
	 --tf accept-ways highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary_link,secondary,tertiary,motorway_junction,residential,unclassified,living_street\
	 --bounding-box top=48.994 left=11.574 bottom=48.584 right=12.095\
	 --used-node --wb network-detailed.osm.pbf

	$(osmosis) --rb file=$<\
	 --tf accept-ways highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary_link,secondary,tertiary,motorway_junction\
	 --bounding-box top=49.08 left=11.31 bottom=48.50 right=12.24\
	 --used-node --wb network-coarse.osm.pbf

	$(osmosis) --rb file=$<\
	 --tf accept-ways highway=motorway,motorway_link,motorway_junction,trunk,trunk_link,primary,primary_link\
	 --used-node --wb network-germany.osm.pbf

	$(osmosis) --rb file=network-service.osm.pbf --rb file=network-germany.osm.pbf --rb file=network-coarse.osm.pbf --rb file=network-detailed.osm.pbf\
  	 --merge --merge --merge --wx $@

	rm network-service.osm.pbf
	rm network-detailed.osm.pbf
	rm network-coarse.osm.pbf
	rm network-germany.osm.pbf


input/sumo.net.xml: input/network.osm

	$(SUMO_HOME)/bin/netconvert --geometry.remove --ramps.guess --ramps.no-split\
	 --type-files $(SUMO_HOME)/data/typemap/osmNetconvert.typ.xml,$(SUMO_HOME)/data/typemap/osmNetconvertUrbanDe.typ.xml\
	 --tls.guess-signals true --tls.discard-simple --tls.join --tls.default-type actuated\
	 --junctions.join --junctions.corner-detail 5\
	 --roundabouts.guess --remove-edges.isolated\
	 --no-internal-links --keep-edges.by-vclass passenger,bicycle\
	 --remove-edges.by-vclass hov,tram,rail,rail_urban,rail_fast,pedestrian\
	 --output.original-names --output.street-names\
	 --proj "+proj=utm +zone=32 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs"\
	 --osm-files $< -o=$@


input/$V/kelheim-$V-network.xml.gz: input/sumo.net.xml
	$(sc) prepare network-from-sumo $<\
	 --output $@

	$(sc) prepare network\
     --shp ../public-svn/matsim/scenarios/countries/de/kelheim/shp/prepare-network/av-and-drt-area.shp\
	 --network $@\
	 --output $@

input/$V/kelheim-$V-network-with-pt.xml.gz: input/$V/kelheim-$V-network.xml.gz
	$(sc) prepare transit-from-gtfs --network $<\
	 --name kelheim-$V --date "2021-08-18" --target-crs $(CRS) \
	 --output input/$V\
	 ../shared-svn/projects/KelRide/data/20210816_regio.zip\
	 ../shared-svn/projects/KelRide/data/20210816_train_short.zip\
	 ../shared-svn/projects/KelRide/data/20210816_train_long.zip\
	 --prefix regio_,short_,long_\
	 --shp ../shared-svn/projects/KelRide/data/pt-area/pt-area.shp\
	 --shp ../shared-svn/projects/KelRide/data/Bayern.zip\
	 --shp ../shared-svn/projects/KelRide/data/germany-area/germany-area.shp\

input/freight-trips.xml.gz: input/$V/kelheim-$V-network.xml.gz
	$(sc) prepare extract-freight-trips ../public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/german_freight.25pct.plans.xml.gz\
	 --network ../public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/germany-europe-network.xml.gz\
	 --input-crs $(CRS)\
	 --target-crs $(CRS)\
	 --shp ../shared-svn/projects/KelRide/matsim-input-files/20211217_kelheim/20211217_kehlheim/kehlheim.shp --shp-crs $(CRS)\
	 --output $@

input/$V/kelheim-$V-25pct.plans-initial.xml.gz: input/freight-trips.xml.gz input/$V/kelheim-$V-network.xml.gz
	$(sc) prepare trajectory-to-plans\
	 --name prepare --sample-size 0.25\
	 --max-typical-duration 0\
	 --output input/\
	 --population ../shared-svn/projects/KelRide/matsim-input-files/20211217_kelheim/20211217_kehlheim/population.xml.gz\
	 --attributes  ../shared-svn/projects/KelRide/matsim-input-files/20211217_kelheim/20211217_kehlheim/personAttributes.xml.gz

	$(sc) prepare resolve-grid-coords\
	 input/prepare-25pct.plans.xml.gz\
	 --input-crs $(CRS)\
	 --grid-resolution 300\
	 --landuse ../shared-svn/projects/matsim-germany/landuse/landuse.shp\
	 --output input/prepare-25pct.plans.xml.gz

	$(sc) prepare population input/prepare-25pct.plans.xml.gz\
	 --output input/prepare-25pct.plans.xml.gz

	$(sc) prepare generate-short-distance-trips\
 	 --population input/prepare-25pct.plans.xml.gz\
 	 --input-crs $(CRS)\
 	 --shp ../shared-svn/projects/KelRide/matsim-input-files/20211217_kelheim/20211217_kehlheim/kehlheim.shp --shp-crs $(CRS)\
 	 --num-trips 15216

	$(sc) prepare split-activity-types-duration\
		--input input/prepare-25pct.plans-with-trips.xml.gz\
		--output input/prepare-25pct.plans-with-trips.xml.gz

	$(sc) prepare fix-subtour-modes --input input/prepare-25pct.plans-with-trips.xml.gz --coord-dist 100 --output $@

	$(sc) prepare merge-populations $@ $< --output $@

	$(sc) prepare extract-home-coordinates $@ --csv input/$V/kelheim-$V-homes.csv

	$(sc) prepare downsample-population $@\
    	 --sample-size 0.25\
    	 --samples 0.1 0.01\


check: input/$V/kelheim-$V-25pct.plans-initial.xml.gz
	$(sc) analysis check-population $<\
 	 --input-crs $(CRS)\
 	 --shp ../shared-svn/projects/KelRide/matsim-input-files/20211217_kelheim/20211217_kehlheim/kehlheim.shp --shp-crs $(CRS)

# Aggregated target
prepare: input/$V/kelheim-$V-25pct.plans-initial.xml.gz input/$V/kelheim-$V-network-with-pt.xml.gz
	echo "Done"