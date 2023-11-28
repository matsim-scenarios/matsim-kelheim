#!/usr/bin/env python
# -*- coding: utf-8 -*-

import geopandas as gpd


from matsim.scenariogen.data import read_all

if __name__ == "__main__":
    hh, p, t = read_all("/Volumes/Untitled/B3_Lokal-Datensatzpaket/CSV")

    region = gpd.read_file("../../../input/shp/dilutionArea.shp").set_crs("EPSG:25832")
    hh = gpd.GeoDataFrame(hh, geometry=gpd.geoseries.from_wkt(hh.geom), crs="EPSG:4326").to_crs("EPSG:25832")

    hh = gpd.sjoin(hh, region, how="inner", predicate="intersects")

    hh.to_csv("hh.csv")
