#!/bin/bash --login
#SBATCH --nodes=1
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=16
#SBATCH --mem=64G
#SBATCH --time=48:00:00
#SBATCH --mail-type=END,FAIL
#SBATCH --mail-user=rehmann@vsp.tu-berlin.de
#SBATCH --output=logs/%x-%j.out

# Required job variables are supplied by submit-policy-matrix.sh (or with sbatch
# --export): DEMAND=base|counterfactual, FLEET_SIZE, CAPACITY, and SEED.
set -euo pipefail
set -x

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# Slurm executes a spooled copy of this script. SLURM_SUBMIT_DIR therefore
# identifies the uploaded experiment directory, while SCRIPT_DIR is only the
# correct location when invoking the script directly outside Slurm.
RUN_ROOT="${SLURM_SUBMIT_DIR:-$SCRIPT_DIR}"
cd "$RUN_ROOT"

# Shared experiment settings. The config and shared study-area shape retain
# their normal scenario locations; only experiment-specific inputs use INPUT_DIR.
EXPERIMENT_ID="2026-08-04-larger-fleet"
INPUT_DIR="$RUN_ROOT/input/v3.1/expanded-service-area"
CONFIG="$RUN_ROOT/input/v3.1/kelheim-v3.1-25pct.kexi.config.xml"
COUNTERFACTUAL_PLANS="$INPUT_DIR/counterfactual-25pct-plans.xml.gz"
PROBE_STOP_PAIRS="$INPUT_DIR/drt_stops_queried_train_station.csv,$INPUT_DIR/drt_stops_queried_supermarket.csv,$INPUT_DIR/drt_stops_queried_logistic.csv"
FLEET_START_LINK_WEIGHTS="$INPUT_DIR/populationLinkWeights.csv"
EXPANDED_DRT_STOPS="$INPUT_DIR/drt_stops_landkreis.xml"
STUDY_AREA_SHP="$RUN_ROOT/input/shp/lk-kelheim/lk-kelheim.shp"
ITERATIONS=300
JAVA_HEAP=60G

: "${DEMAND:?Set DEMAND to base or counterfactual.}"
: "${FLEET_SIZE:?Set FLEET_SIZE to a positive integer.}"
: "${CAPACITY:?Set CAPACITY to a positive integer.}"
: "${SEED:?Set SEED to a non-negative integer.}"

case "$DEMAND" in
	base|counterfactual) ;;
	*) echo "DEMAND must be base or counterfactual, got: $DEMAND" >&2; exit 2 ;;
esac
if ! [[ "$SEED" =~ ^[0-9]+$ ]]; then
	echo "SEED must be a non-negative integer, got: $SEED" >&2
	exit 2
fi
if ! [[ "$FLEET_SIZE" =~ ^[1-9][0-9]*$ ]]; then
	echo "FLEET_SIZE must be a positive integer, got: $FLEET_SIZE" >&2
	exit 2
fi
if ! [[ "$CAPACITY" =~ ^[1-9][0-9]*$ ]]; then
	echo "CAPACITY must be a positive integer, got: $CAPACITY" >&2
	exit 2
fi

if [[ -z "${SCENARIO_ID:-}" ]]; then
	SCENARIO_ID="policy__demand-${DEMAND}__fleet-${FLEET_SIZE}__capacity-${CAPACITY}__seed-${SEED}"
fi
OUTPUT_DIR="output/${EXPERIMENT_ID}/${SCENARIO_ID}"

[[ -f "$CONFIG" ]] || { echo "Missing required input: $CONFIG" >&2; exit 2; }
	for required_file in "$FLEET_START_LINK_WEIGHTS" "$EXPANDED_DRT_STOPS" "$STUDY_AREA_SHP"; do
	[[ -f "$required_file" ]] || { echo "Missing required input: $required_file" >&2; exit 2; }
done
IFS=',' read -r -a probe_stop_pair_files <<< "$PROBE_STOP_PAIRS"
for required_file in "${probe_stop_pair_files[@]}"; do
	[[ -f "$required_file" ]] || { echo "Missing DRT stop-pair input: $required_file" >&2; exit 2; }
done
if [[ "$DEMAND" == counterfactual && ! -f "$COUNTERFACTUAL_PLANS" ]]; then
	echo "Missing counterfactual plans: $COUNTERFACTUAL_PLANS" >&2
	exit 2
fi

shopt -s nullglob
jars=(matsim-kelheim-2024.2*.jar)
if (( ${#jars[@]} != 1 )); then
	echo "Expected exactly one matsim-kelheim-2024.2*.jar in $RUN_ROOT, found ${#jars[@]}." >&2
	exit 2
fi

date
hostname
module load java/25
which java
java -version
echo "$JAVA_HOME"

command=(
	java "-Xmx${JAVA_HEAP}" "-Xms${JAVA_HEAP}" -cp "${jars[0]}" org.matsim.run.RunKelheimScenario run
	--config "$CONFIG"
	--25pct
	--with-drt
	--with-drt-expandedServiceArea
	--iterations="$ITERATIONS"
	--random-seed "$SEED"
	--drt-fleet-size="$FLEET_SIZE"
	--drt-vehicle-capacity="$CAPACITY"
	--drt-expanded-service-area-stops "$EXPANDED_DRT_STOPS"
	--drt-study-area-shp "$STUDY_AREA_SHP"
	--write-drt-service-quality-probe
	--drt-service-quality-probe-stop-pair-input-files "$PROBE_STOP_PAIRS"
	--drt-fleet-start-link-weights "$FLEET_START_LINK_WEIGHTS"
	--config:controller.overwriteFiles=deleteDirectoryIfExists
	--config:swissRailRaptor.useIntermodalAccessEgress=false
	--output "$OUTPUT_DIR"
)

if [[ "$DEMAND" == counterfactual ]]; then
	command+=(--plans "$COUNTERFACTUAL_PLANS")
fi
"${command[@]}"
chmod -R g+rwX "$OUTPUT_DIR"
