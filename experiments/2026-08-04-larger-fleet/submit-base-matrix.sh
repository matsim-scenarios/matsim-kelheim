#!/usr/bin/env bash
# Submit no-DRT base scenarios.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
RUNNER="$SCRIPT_DIR/runBase.sh"
DRY_RUN=false

# Matrix options. Edit these lists to select the submitted base experiments.
DEMANDS=(base counterfactual)
SEEDS=(4711 5822)

if [[ "${1:-}" == "--dry-run" ]]; then
	DRY_RUN=true
elif [[ $# -gt 0 ]]; then
	echo "Usage: $0 [--dry-run]" >&2
	exit 2
fi

mkdir -p "$SCRIPT_DIR/logs"
cd "$SCRIPT_DIR"
MANIFEST="$SCRIPT_DIR/base-submission.tsv"
if [[ "$DRY_RUN" == false ]]; then
	printf 'submitted_at\tscenario_id\tdemand\tseed\tjob_id\n' > "$MANIFEST"
fi

for seed in "${SEEDS[@]}"; do
	for demand in "${DEMANDS[@]}"; do
		scenario_id="base__demand-${demand}__seed-${seed}"
		exports="ALL,DEMAND=${demand},SEED=${seed},SCENARIO_ID=${scenario_id}"
		if [[ "$DRY_RUN" == true ]]; then
			printf 'sbatch --job-name=%q --export=%q %q\n' "$scenario_id" "$exports" "$RUNNER"
			job_id=DRY_RUN
		else
			job_id="$(sbatch --parsable --job-name="$scenario_id" --export="$exports" "$RUNNER")"
		fi
		if [[ "$DRY_RUN" == false ]]; then
			printf '%s\t%s\t%s\t%s\t%s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$scenario_id" "$demand" "$seed" "$job_id" >> "$MANIFEST"
		fi
	done
done

if [[ "$DRY_RUN" == false ]]; then
	echo "Wrote $MANIFEST"
fi
