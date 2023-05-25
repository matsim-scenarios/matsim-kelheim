

import pandas as pd
import numpy as np
from scipy.optimize import minimize

# Calculates the adjusted modal split by simply solving objective function

if __name__ == "__main__":

    mid = pd.read_csv("../R/mid.csv")
    sim = pd.read_csv("../R/sim.csv")


    sagg = sim.groupby("dist_group").sum()
    sagg['share'] = sagg.trips / np.sum(sagg.trips)

    print("Start")
    print(sagg)

    print("Mid")
    print(mid)

    # Rescale the distance groups of the survey data so that it matches the distance group distribution of the simulation
    # The overall mode share after this adjustment will the resulting adjusted mode share
    def f(x, p=False):
        adj = mid.copy()

        for i, t in enumerate(x):
            adj.loc[adj.dist_group == sagg.index[i], "share"] *= t

        adj.share = adj.share / np.sum(adj.share)

        agg = adj.groupby("dist_group").sum()

        # Minimized difference between adjusted and simulated distribution
        err = sum((sagg.share - agg.share)**2)

        if p:
            print(agg)
            print(err)
            return adj

        return err

    # One variable for each distance group
    x0 = np.ones(6) / 6

    # Sum of weights need to be smaller than one
    cons = [{'type': 'ineq', 'fun': lambda x:  1 - sum(x)}]
    bnds = tuple((0, 1) for x in x0)

    res = minimize(f, x0, method='SLSQP', bounds=bnds, constraints=cons)

    print("Result")
    print(res)

    print("Result scaled", res.x * 6)

    df = f(res.x, True)

    df.to_csv("../R/mid_adj.csv", index=False)

    print(df.groupby("mode").sum())
