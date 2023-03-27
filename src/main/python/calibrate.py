#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os

import pandas as pd
import geopandas as gpd
import numpy as np

import calibration

#%%

modes = ["walk", "car", "ride", "pt", "bike"]
fixed_mode = "walk"
initial = {
    "bike": -2,
    "pt": -0,
    "car": -0.2,
    "ride": -4
}

# Original modal split
target = {
    "walk": 0.13,
    "bike": 0.08,
    "pt": 0.03,
    "car": 0.59,
    "ride": 0.17    
}

# Use adjusted modal split for our distance distribution
target = {
    "walk":  0.111505,
    "bike":  0.068790,
    "pt":    0.038063,
    "car":   0.612060,
    "ride":  0.169581
}

city = gpd.read_file("../scenarios/shape-file/dilutionArea.shp").set_crs("EPSG:25832")
homes = pd.read_csv("kelheim-v3.0-homes.csv", dtype={"person": "str"})

def f(persons):
    persons = pd.merge(persons, homes, how="inner", left_on="person", right_on="person")
    persons = gpd.GeoDataFrame(persons, geometry=gpd.points_from_xy(persons.home_x, persons.home_y))

    df = gpd.sjoin(persons.set_crs("EPSG:25832"), city, how="inner", op="intersects")

    print("Filtered %s persons" % len(df))

    return df

def filter_modes(df):
    return df[df.main_mode.isin(modes)]

study, obj = calibration.create_mode_share_study("calib", "matsim-kelheim-3.x-SNAPSHOT.jar",
                                        "../scenarios/input/kelheim-v3.0-25pct.config.xml",
                                        modes, target, 
                                        initial_asc=initial,
                                        args="--25pct",
                                        jvm_args="-Xmx46G -Xms46G -XX:+AlwaysPreTouch",
                                        person_filter=f, map_trips=filter_modes, chain_runs=True)


#%%

study.optimize(obj, 8)
