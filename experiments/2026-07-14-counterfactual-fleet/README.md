# Counterfactual-demand DRT fleet experiment

This directory submits 16 independent Slurm jobs: base and counterfactual demand, fleet sizes 100/250/500/1000, and probability-based DRT prebooking off/on. All counterfactual cases use the same already-generated population; no demand is regenerated while varying fleet size.

## Upload layout

Create one cluster upload directory containing `runPolicy.sh`, `submit-policy-matrix.sh`, this README, and one `matsim-kelheim-2024.2*.jar`. Keep the existing scenario layout for the config and the shared study-area shape:

- `input/v3.1/kelheim-v3.1-25pct.kexi.config.xml`
- `input/shp/lk-kelheim/lk-kelheim.shp` and its required shapefile sidecars (`.dbf`, `.shx`, `.prj`, and any others)

Upload only experiment-specific inputs to `input/v3.1/expanded-service-area/`:

- `counterfactual-25pct-plans.xml.gz`
- `populationLinkWeights.csv`
- `drt_stops_landkreis.xml`
- all three `drt_stops_queried_{train_station,supermarket,logistic}.csv` files

The config continues to load the base network, population, vehicles, and transit inputs from its configured remote URLs. Adjust shared paths, iterations, heap, or probe inputs only in `runPolicy.sh`.

## Submit

From this directory, inspect the 16 commands without submitting:

```bash
./submit-policy-matrix.sh --dry-run
```

Submit all jobs:

```bash
./submit-policy-matrix.sh
```

The command records submitted Slurm IDs in `submission.tsv`. Output directories are deterministic, for example `output/2026-07-14-counterfactual-fleet/demand-counterfactual__prebooking-on__fleet-500__seed-4711`.

The seed dimension is defined once as `SEEDS=(4711)` in `submit-policy-matrix.sh`. Extend that list, for example to `SEEDS=(4711 1234)`, to submit independent replications.

To submit a single case, use:

```bash
mkdir -p logs
sbatch --job-name=demand-counterfactual__prebooking-on__fleet-500__seed-4711 \
  --export=ALL,DEMAND=counterfactual,FLEET_SIZE=500,PREBOOKING=on,SEED=4711,SCENARIO_ID=demand-counterfactual__prebooking-on__fleet-500__seed-4711 \
  runPolicy.sh
```

With `PREBOOKING=on`, `RunKelheimScenario` enables probability-based prebooking for conventional `drt` only: every DRT trip is submitted 1,800 seconds (30 minutes) before its planned departure. The AV DRT mode is unchanged.
