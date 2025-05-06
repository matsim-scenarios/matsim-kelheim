#!/bin/bash --login

#SBATCH --job-name=gatherResults            # Job-Name
#SBATCH --output=./logfiles-dashboards/logfile_%x-%j.log    # file into which the stdout of your job is written
#SBATCH --nodes=1                               # How many computing nodes do you need (for MATSim usually 1)
#SBATCH --ntasks=1                              # How many tasks should be run (For MATSim usually 1)   
#SBATCH --cpus-per-task=1                      # Number of CPUs per task (For MATSim usually 8 - 12)
#SBATCH --mem=12G                              # RAM per node for the job
#SBATCH --time=01:00:00                         # Time limit hrs:min:sec
#SBATCH --mail-type=FAIL                         # Send email on begin, end, and fail
#SBATCH --mail-user=schlenther@vsp.tu-berlin.de # Your email address


module load R                                           # R-Modul laden
Rscript readValuesFromRunSummaries.R                    # Skript ausführen

echo "JOB ABOUT TO FINISH"