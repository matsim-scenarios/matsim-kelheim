#!/bin/bash

name=$( echo "$*" | sed -e 's/ //g' -e 's/--//g')

export RUN_ARGS="$*"

echo "Starting run kh-$name"
echo "$*"

#qsub -V -N kh-"$name" job.sh