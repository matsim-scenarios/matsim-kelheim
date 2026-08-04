# Larger-fleet DRT capacity experiment

This directory contains separate submitters for two no-DRT base runs (base and counterfactual demand) and the DRT policy matrix: base and counterfactual demand, fleet sizes 1000/2000/5000/10000, and vehicle capacities 1/8. Prebooking is disabled for every run. All counterfactual cases use the same already-generated population; no demand is regenerated while varying fleet size or vehicle capacity.

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

From this directory, inspect or submit the two no-DRT base cases:

```bash
./submit-base-matrix.sh --dry-run
./submit-base-matrix.sh
```

Inspect or submit the DRT policy matrix:

```bash
./submit-policy-matrix.sh --dry-run
./submit-policy-matrix.sh
```

The command records submitted Slurm IDs in `submission.tsv`. Output directories are deterministic, for example `output/2026-08-04-larger-fleet/policy__demand-counterfactual__fleet-5000__capacity-8__seed-4711`.

Submit the matrix from the upload directory. The job runner uses Slurm's `SLURM_SUBMIT_DIR`, rather than the temporary spool directory where Slurm copies the batch script, to resolve all relative input paths.

All base controls are at the top of `submit-base-matrix.sh`: `DEMANDS=(base counterfactual)` and `SEEDS=(4711 5822)`. Policy controls are independently configured at the top of `submit-policy-matrix.sh`: `DEMANDS`, `FLEET_SIZES`, `CAPACITIES`, and `SEEDS`. Extend either seed list to submit independent replications.

To submit a single case, use:

```bash
mkdir -p logs
sbatch --job-name=policy__demand-counterfactual__fleet-5000__capacity-8__seed-4711 \
  --export=ALL,DEMAND=counterfactual,FLEET_SIZE=5000,CAPACITY=8,SEED=4711,SCENARIO_ID=policy__demand-counterfactual__fleet-5000__capacity-8__seed-4711 \
  runPolicy.sh
```
