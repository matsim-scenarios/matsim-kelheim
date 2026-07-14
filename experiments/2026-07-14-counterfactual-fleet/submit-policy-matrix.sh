#!/usr/bin/env bash
# Submit base/counterfactual demand, four fleet sizes, prebooking off/on, and configured seeds.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
RUNNER="$SCRIPT_DIR/runPolicy.sh"
DRY_RUN=false

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
	printf 'submitted_at\tscenario_id\tdemand\tfleet_size\tprebooking\tjob_id\n' > "$MANIFEST"
fi

# Extend this list to add independent random-seed replications.
SEEDS=(4711)

for seed in "${SEEDS[@]}"; do
	for demand in base counterfactual; do
		for fleet_size in 100 250 500 1000; do
			for prebooking in off on; do
				scenario_id="demand-${demand}__prebooking-${prebooking}__fleet-${fleet_size}__seed-${seed}"
				exports="ALL,DEMAND=${demand},FLEET_SIZE=${fleet_size},PREBOOKING=${prebooking},SEED=${seed},SCENARIO_ID=${scenario_id}"
				if [[ "$DRY_RUN" == true ]]; then
					printf 'sbatch --job-name=%q --export=%q %q\n' "$scenario_id" "$exports" "$RUNNER"
					job_id=DRY_RUN
				else
					job_id="$(sbatch --parsable --job-name="$scenario_id" --export="$exports" "$RUNNER")"
				fi
				if [[ "$DRY_RUN" == false ]]; then
					printf '%s\t%s\t%s\t%s\t%s\t%s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$scenario_id" "$demand" "$fleet_size" "$prebooking" "$job_id" >> "$MANIFEST"
				fi
			done
		done
	done
done

if [[ "$DRY_RUN" == false ]]; then
	echo "Wrote $MANIFEST"
fi
