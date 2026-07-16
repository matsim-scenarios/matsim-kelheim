#!/bin/bash --login
#SBATCH --nodes=1
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=16
#SBATCH --mem=128G
#SBATCH --time=12:00:00
#SBATCH --mail-type=END,FAIL
#SBATCH --mail-user=rehmann@vsp.tu-berlin.de
#SBATCH --output=logs/%x-%j.out

# Required variables: OUTPUT_DIR and either POI or POIS.
# RUN_MODE is calculate by default and may be dashboard-only.
set -euo pipefail
set -x

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
RUN_ROOT="${SLURM_SUBMIT_DIR:-$SCRIPT_DIR}"
cd "$RUN_ROOT"

JAVA_HEAP="${JAVA_HEAP:-100G}"
RUN_MODE="${RUN_MODE:-calculate}"
MODES="${MODES:-estimatedDrt teleportedWalk pt car}"
TIMES="${TIMES:-21600 25200 28800 32400 36000 39600 43200 46800 50400 54000 57600 61200 64800 68400 72000 75600}"

: "${OUTPUT_DIR:?Set OUTPUT_DIR to a MATSim output directory.}"

case "$RUN_MODE" in
	calculate)
		: "${POI:?Set POI for calculation mode.}"
		POIS="$POI"
		;;
	dashboard-only)
		: "${POIS:?Set POIS for dashboard-only mode.}"
		;;
	*)
		echo "RUN_MODE must be calculate or dashboard-only, got: $RUN_MODE" >&2
		exit 2
		;;
esac

if [[ "$OUTPUT_DIR" != /* ]]; then
	OUTPUT_DIR="$RUN_ROOT/$OUTPUT_DIR"
fi
[[ -d "$OUTPUT_DIR" ]] || { echo "Missing MATSim output directory: $OUTPUT_DIR" >&2; exit 2; }

# Slurm --export uses commas as separators, so the submitter passes lists as
# space-separated values. Commas are also accepted for convenient direct use.
MODES_CSV="${MODES// /,}"
TIMES_CSV="${TIMES// /,}"
POIS_CSV="${POIS// /,}"

shopt -s nullglob
jars=(matsim-kelheim-2024.2*accessibility.jar)
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
	java "-Xmx${JAVA_HEAP}" "-Xms${JAVA_HEAP}"
	-cp "${jars[0]}"
	org.matsim.analysis.postAnalysis.accessibility.run.RunOfflineAccessibilityKelheim
	--directory "$OUTPUT_DIR"
	--pois "$POIS_CSV"
	--modes "$MODES_CSV"
	--times "$TIMES_CSV"
)

if [[ "$RUN_MODE" == dashboard-only ]]; then
	command+=(--dashboard-only)
fi

"${command[@]}"
chmod -R g+rwX "$OUTPUT_DIR"
