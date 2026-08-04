#!/usr/bin/env bash
# Submit the DRT policy matrix.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
RUNNER="$SCRIPT_DIR/runPolicy.sh"
DRY_RUN=false

# Matrix options. Edit these lists to select the submitted DRT policy experiments.
DEMANDS=(base counterfactual)
FLEET_SIZES=(1000 2000 5000 10000)
CAPACITIES=(1 8)
SEEDS=(4711 5822)

if [[ "${1:-}" == "--dry-run" ]]; then
	DRY_RUN=true
elif [[ $# -gt 0 ]]; then
	echo "Usage: $0 [--dry-run]" >&2
	exit 2
fi

mkdir -p "$SCRIPT_DIR/logs"
cd "$SCRIPT_DIR"

MANIFEST="$SCRIPT_DIR/submission.tsv"
if [[ "$DRY_RUN" == false ]]; then
	printf 'submitted_at\tscenario_id\tdemand\tfleet_size\tcapacity\tseed\tjob_id\n' > "$MANIFEST"
fi

submit_job() {
	local demand=$1 fleet_size=$2 capacity=$3 seed=$4
	local scenario_id="policy__demand-${demand}__fleet-${fleet_size}__capacity-${capacity}__seed-${seed}"
	local exports="ALL,DEMAND=${demand},FLEET_SIZE=${fleet_size},CAPACITY=${capacity},SEED=${seed},SCENARIO_ID=${scenario_id}"
	local job_id
	if [[ "$DRY_RUN" == true ]]; then
		printf 'sbatch --job-name=%q --export=%q %q\n' "$scenario_id" "$exports" "$RUNNER"
		job_id=DRY_RUN
	else
		job_id="$(sbatch --parsable --job-name="$scenario_id" --export="$exports" "$RUNNER")"
	fi
	if [[ "$DRY_RUN" == false ]]; then
		printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$scenario_id" "$demand" "$fleet_size" "$capacity" "$seed" "$job_id" >> "$MANIFEST"
	fi
}

for seed in "${SEEDS[@]}"; do
	for demand in "${DEMANDS[@]}"; do
		for fleet_size in "${FLEET_SIZES[@]}"; do
			for capacity in "${CAPACITIES[@]}"; do
				submit_job "$demand" "$fleet_size" "$capacity" "$seed"
			done
		done
	done
done

if [[ "$DRY_RUN" == false ]]; then
	echo "Wrote $MANIFEST"
fi
