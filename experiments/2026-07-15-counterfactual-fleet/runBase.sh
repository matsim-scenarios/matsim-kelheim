#!/bin/bash --login
#SBATCH --nodes=1
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=16
#SBATCH --mem=64G
#SBATCH --time=48:00:00
#SBATCH --mail-type=END,FAIL
#SBATCH --mail-user=rehmann@vsp.tu-berlin.de
#SBATCH --output=logs/%x-%j.out

# Required job variables: DEMAND=base|counterfactual and SEED.
set -euo pipefail
set -x

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
RUN_ROOT="${SLURM_SUBMIT_DIR:-$SCRIPT_DIR}"
cd "$RUN_ROOT"

EXPERIMENT_ID="2026-07-15-counterfactual-fleet"
INPUT_DIR="$RUN_ROOT/input/v3.1/expanded-service-area"
CONFIG="$RUN_ROOT/input/v3.1/kelheim-v3.1-config.xml"
COUNTERFACTUAL_PLANS="$INPUT_DIR/counterfactual-25pct-plans.xml.gz"
ITERATIONS=300
JAVA_HEAP=60G

: "${DEMAND:?Set DEMAND to base or counterfactual.}"
: "${SEED:?Set SEED to a non-negative integer.}"
case "$DEMAND" in
	base|counterfactual) ;;
	*) echo "DEMAND must be base or counterfactual, got: $DEMAND" >&2; exit 2 ;;
esac
if ! [[ "$SEED" =~ ^[0-9]+$ ]]; then
	echo "SEED must be a non-negative integer, got: $SEED" >&2
	exit 2
fi

SCENARIO_ID="${SCENARIO_ID:-base__demand-${DEMAND}__seed-${SEED}}"
OUTPUT_DIR="output/${EXPERIMENT_ID}/${SCENARIO_ID}"
[[ -f "$CONFIG" ]] || { echo "Missing required input: $CONFIG" >&2; exit 2; }
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
	--iterations="$ITERATIONS"
	--random-seed "$SEED"
	--config:controller.overwriteFiles=deleteDirectoryIfExists
	--output "$OUTPUT_DIR"
)
if [[ "$DEMAND" == counterfactual ]]; then
	command+=(--plans "$COUNTERFACTUAL_PLANS")
fi

"${command[@]}"
chmod -R g+rwX "$OUTPUT_DIR"
