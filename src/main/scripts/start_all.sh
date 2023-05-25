#!/bin/bash

# Iterate options to generate all runs

for tm in "--no-time-mutation" ""; do
  for it in "--iterations 500" ""; do

    ./start.sh --mc none --plans choices-top5 $tm "$it"
    ./start.sh --mc none --plans choices-top5-st $tm "$it"

    ./start.sh --mc changeSingleTrip --plans car $tm "$it"
    ./start.sh --mc changeSingleTrip --plans walk $tm "$it"

    ./start.sh --mc subTourModeChoice --plans car $tm "$it"
    ./start.sh --mc subTourModeChoice --plans walk $tm "$it"


    for mc in "--mass-conservation" ""; do

      true
   #   ./start.sh --mc bestChoice --plans car $tm "$it" "$mc"
   #   ./start.sh --mc bestChoice --plans walk $tm "$it" "$mc"

   #   ./start.sh --mc bestKSelection --plans car $tm "$it" "$mc"
   #   ./start.sh --mc bestKSelection --plans walk $tm "$it" "$mc"

    done

  done

done
