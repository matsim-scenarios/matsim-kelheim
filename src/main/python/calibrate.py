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
    "bike": -2.135,
    "pt": 2.6879,
    "car": -0.2666,
    "ride": -3.233
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

def f(persons):    
    df = gpd.sjoin(persons.set_crs("EPSG:25832"), city, how="inner", op="intersects")    
    return df

def filter_freight(df):
    return df[df.main_mode != "freight"]

study, obj = calibration.create_mode_share_study("calib", "matsim-kelheim-1.0-SNAPSHOT.jar", 
                                        "../scenarios/input/kelheim-v1.0-25pct.calib.xml", 
                                        modes, target, 
                                        initial_asc=initial,
                                        args="--25pct",
                                        jvm_args="-Xmx46G -Xms46G -XX:+AlwaysPreTouch",
                                        person_filter=f, map_trips=filter_freight, chain_runs=True)


#%%

study.optimize(obj, 10)
