#!/usr/bin/env bash
# Submit accessibility calculations and, optionally, dependent dashboards.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
RUN_ROOT="${SLURM_SUBMIT_DIR:-$SCRIPT_DIR}"
RUNNER="$SCRIPT_DIR/runAccessibility.sh"
DRY_RUN=false

# Matrix options. Edit these lists to choose exactly which existing runs to analyze.
SCENARIO_DIRS=(
  "output/2026-07-15-counterfactual-fleet/policy__demand-base__prebooking-off__fleet-1000__seed-4711",
  "output/2026-07-15-counterfactual-fleet/policy__demand-counterfactual__prebooking-off__fleet-500__seed-4711",
	"output/2026-07-15-counterfactual-fleet/policy__demand-base__prebooking-off__fleet-500__seed-4711"
)
POIS=(train_station logistic supermarket)

# Space-separated values are intentional: Slurm uses commas to delimit --export fields.
MODES="estimatedDrt teleportedWalk pt car"
TIMES="21600 25200 28800 32400 36000 39600 43200 46800 50400 54000 57600 61200 64800 68400 72000 75600"
JAVA_HEAP="60G"

# Submit one dashboard-only job per scenario after all of its POI jobs succeed.
SUBMIT_DASHBOARDS=true

if [[ "${1:-}" == "--dry-run" ]]; then
	DRY_RUN=true
elif [[ $# -gt 0 ]]; then
	echo "Usage: $0 [--dry-run]" >&2
	exit 2
fi

[[ -x "$RUNNER" ]] || { echo "Runner is not executable: $RUNNER" >&2; exit 2; }
(( ${#SCENARIO_DIRS[@]} > 0 )) || { echo "SCENARIO_DIRS must not be empty." >&2; exit 2; }
(( ${#POIS[@]} > 0 )) || { echo "POIS must not be empty." >&2; exit 2; }

mkdir -p "$SCRIPT_DIR/logs"
cd "$RUN_ROOT"

MANIFEST="$SCRIPT_DIR/accessibility-submission.tsv"
if [[ "$DRY_RUN" == false ]]; then
	printf 'submitted_at\tscenario_dir\trun_mode\tpoi\tjob_id\tdependency\n' > "$MANIFEST"
fi

join_by_space() {
	local IFS=' '
	printf '%s' "$*"
}

submit_calculation() {
	local scenario_dir=$1 output_dir=$2 poi=$3 job_name exports job_id
	job_name="access-$(basename "$scenario_dir")-${poi}"
	exports="ALL,OUTPUT_DIR=${output_dir},POI=${poi},MODES=$(join_by_space $MODES),TIMES=$(join_by_space $TIMES),JAVA_HEAP=${JAVA_HEAP},RUN_MODE=calculate"
	if [[ "$DRY_RUN" == true ]]; then
		printf 'sbatch --job-name=%q --export=%q %q\n' "$job_name" "$exports" "$RUNNER" >&2
		job_id="DRY_RUN_${poi}"
	else
		job_id="$(sbatch --parsable --job-name="$job_name" --export="$exports" "$RUNNER")"
	fi
	if [[ "$DRY_RUN" == false ]]; then
		printf '%s\t%s\tcalculate\t%s\t%s\t%s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$scenario_dir" "$poi" "$job_id" "" >> "$MANIFEST"
	fi
	printf '%s' "$job_id"
}

submit_dashboard() {
	local scenario_dir=$1 output_dir=$2 dependency=$3 job_name exports job_id
	job_name="access-dashboard-$(basename "$scenario_dir")"
	exports="ALL,OUTPUT_DIR=${output_dir},POIS=$(join_by_space "${POIS[@]}"),MODES=$(join_by_space $MODES),TIMES=$(join_by_space $TIMES),JAVA_HEAP=${JAVA_HEAP},RUN_MODE=dashboard-only"
	if [[ "$DRY_RUN" == true ]]; then
		printf 'sbatch --dependency=%q --job-name=%q --export=%q %q\n' "afterok:$dependency" "$job_name" "$exports" "$RUNNER" >&2
		job_id="DRY_RUN_dashboard"
	else
		job_id="$(sbatch --parsable --dependency="afterok:$dependency" --job-name="$job_name" --export="$exports" "$RUNNER")"
	fi
	if [[ "$DRY_RUN" == false ]]; then
		printf '%s\t%s\tdashboard-only\t%s\t%s\t%s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$scenario_dir" "${POIS[*]}" "$job_id" "afterok:$dependency" >> "$MANIFEST"
	fi
}

for scenario_dir in "${SCENARIO_DIRS[@]}"; do
	if [[ "$scenario_dir" == /* ]]; then
		output_dir="$scenario_dir"
	else
		output_dir="$RUN_ROOT/$scenario_dir"
	fi
	if [[ "$DRY_RUN" == false ]]; then
		[[ -d "$output_dir" ]] || { echo "Missing MATSim output directory: $output_dir" >&2; exit 2; }
	fi

	job_ids=()
	for poi in "${POIS[@]}"; do
		job_ids+=("$(submit_calculation "$scenario_dir" "$output_dir" "$poi")")
	done

	if [[ "$SUBMIT_DASHBOARDS" == true ]]; then
		dependency=$(IFS=:; printf '%s' "${job_ids[*]}")
		submit_dashboard "$scenario_dir" "$output_dir" "$dependency"
	fi
done

if [[ "$DRY_RUN" == false ]]; then
	echo "Wrote $MANIFEST"
fi
