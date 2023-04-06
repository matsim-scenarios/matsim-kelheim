require(matsim) #new version needed. currently, you need to install from personsOutput branch in order to have readPersonsTable available
##
library(matsim)
library(tidyverse)
library(dplyr)
library(ggalluvial)

#######
baseCaseDirectory <- "//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/matsim-kelheim/calibration/runs/052"
policyCaseDirectory <- "//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/matsim-kelheim/kelheim-case-study/v2.0/KEXI-with-av/output-ASC-0.15-dist-0.00006-5_av-seed5678-BAUERNSIEDLUNG"

basePersons <- readPersonsTable(baseCaseDirectory)
baseTrips <- readTripsTable(baseCaseDirectory)

policyPersons <- readPersonsTable(policyCaseDirectory)
policyTrips <- readTripsTable(policyCaseDirectory)

baseNonZero <- basePersons %>%
  filter(executed_score != 0)

policyNonZero <- policyPersons %>%
  filter(executed_score != 0)

if(! count(baseNonZero) == count(policyNonZero) ) {
  warning("base case has a different number of non-active/non-mobile persons than policy case !!")
}

## boxplot of all non-negative score differences
boxplotScoreDifferences(baseNonZero, policyNonZero) +
  labs(
    #subtitle = "score_delta = score(policy) - score(base)",
    caption = "ALL MOBILE AGENTS",
    #y = "score_delta"
  )

##let's get into the details. create a joined table first
personsJoined <- inner_join(basePersons, policyPersons, by = "person", suffix = c("_base", "_policy")) %>%
  select(person,
         executed_score_base,
         executed_score_policy) %>%
  mutate(score_diff = executed_score_policy - executed_score_base)


mean(personsJoined$score_diff)
sd(personsJoined$score_diff)

positiveChanges <- personsJoined %>%
  filter(score_diff > 0)

negativeChanges <- personsJoined %>%
  filter(score_diff < 0)

zeroChanges <- personsJoined %>%
  filter(score_diff == 0)

ggplot(positiveChanges, aes(y = score_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of score differences",
    subtitle = "score_delta = score(policy) - score(base)",
    caption = "positive score development",
    y = "score_delta"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.ticks.x = element_blank(),
    axis.title.x = element_blank(),
    axis.text.x = element_blank()
  )

ggplot(negativeChanges, aes(y = score_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of score differences",
    subtitle = "score_delta = score(policy) - score(base)",
    caption = "negative score development",
    y = "score_delta"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.ticks.x = element_blank(),
    axis.title.x = element_blank(),
    axis.text.x = element_blank()
  )

sd(negativeChanges$score_diff)
mean(negativeChanges$score_diff)

hist(negativeChanges$score_diff)

########################################
### where do AV users and drt users come from? especially the losing ones?

av_trips <- policyTrips %>%
  filter(main_mode == "av")

drt_trips <- policyTrips %>%
  filter(grepl("drt", modes, fixed = TRUE)) #check if drt is in the mode chain (we can have intermodal trips where drt is not the main mode)

plotModalShiftSankey(baseTrips, av_trips) +
  labs(
    title = "Modal Shift for all AV Users"
  )

plotModalShiftSankey(baseTrips, drt_trips) +
  labs(
    title = "Modal Shift for all conv. KEXI Users"
  )

drtUsers <- personsJoined %>%
  filter(person %in% drt_trips$person)

avUsers <- personsJoined %>%
  filter(person %in% av_trips$person)

ggplot(drtUsers, aes(y = score_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of score differences",
    subtitle = "score_delta = score(policy) - score(base)",
    caption = "conv. KEXI users",
    y = "score_delta"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.ticks.x = element_blank(),
    axis.title.x = element_blank(),
    axis.text.x = element_blank()
  )

hist(drtUsers$score_diff, breaks = seq(min(drtUsers$score_diff), max(drtUsers$score_diff), length.out = abs(min(drtUsers$score_diff) - max(drtUsers$score_diff))))

ggplot(avUsers, aes(y = score_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of score differences",
    subtitle = "score_delta = score(policy) - score(base)",
    caption = "AV users",
    y = "score_delta"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.ticks.x = element_blank(),
    axis.title.x = element_blank(),
    axis.text.x = element_blank()
  )

hist(avUsers$score_diff, breaks = seq(min(avUsers$score_diff), max(avUsers$score_diff), length.out = abs(min(avUsers$score_diff) - max(avUsers$score_diff))))

##### filter av trips where users have a negative score diff

avUsers_neg <- avUsers %>%
  filter(score_diff < 0)

av_trips_neg <- av_trips %>%
  filter(person %in% avUsers_neg$person)

plotModalShiftSankey(baseTrips, av_trips_neg) +
  labs(
    title = "Modal Shift for AV Users who have a negative score_diff"
  )

drtUsers_neg <- drtUsers %>%
  filter(score_diff < 0)

drt_trips_neg <- drt_trips %>%
  filter(person %in% drtUsers_neg$person)

plotModalShiftSankey(baseTrips, drt_trips_neg) +
  labs(
    title = "Modal Shift for conv. KEXI Users who have a negative score_diff"
  )

#####################################################################
### analyse travel times and distances

### generally compare travel time an distances
compareAverageTravelWait(baseTrips,policyTrips)
compareModalDistanceDistribution(baseTrips, policyTrips)
###


### look at AV and drt trips specifically

##DRT

##aggregated statistics

#get those trips in base case that turn into drt trips in policy case
drt_base_trips <- baseTrips %>%
  filter(trip_id %in% drt_trips$trip_id)

compareModalDistanceDistribution(drt_base_trips, drt_trips) +
  labs(
    title = "Modal Distance Base - Policy"
  )
mean(drt_base_trips$traveled_distance)
mean(drt_trips$traveled_distance)
compareAverageTravelWait(drt_base_trips, drt_trips)
mean(drt_base_trips$trav_time)
mean(drt_trips$trav_time)

##trip-based

drt_joined_trips <- full_join(drt_base_trips, drt_trips, by = "trip_id", suffix = c("_base", "_policy")) %>%
  mutate(traveled_distance_diff = traveled_distance_policy - traveled_distance_base) %>%
  mutate(trav_time_diff = trav_time_policy - trav_time_base) %>%
  mutate(person = person_base) %>%
  select(trip_id, person, trav_time_diff, traveled_distance_diff)

ggplot(drt_joined_trips, aes(y = (trav_time_diff) )) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of traveltime differences",
    subtitle = "trav_time_diff = trav_time_policy - trav_time_base",
    caption = "all DRT users",
    y = "trav_time_diff [s]"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.ticks.x = element_blank(),
    axis.title.x = element_blank(),
    axis.text.x = element_blank()
  )

hist(seconds(drt_joined_trips$trav_time_diff))

ggplot(drt_joined_trips, aes(y = (traveled_distance_diff) )) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of travel distance differences",
    subtitle = "traveled_distance_diff = traveled_distance_policy - traveled_distance_base",
    caption = "all DRT users",
    y = "traveled_distance_diff [m]"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.ticks.x = element_blank(),
    axis.title.x = element_blank(),
    axis.text.x = element_blank()
  )
hist(drt_joined_trips$traveled_distance_diff)

## drt losers
drt_joined_trips_neg <- drt_joined_trips %>%
  filter(trip_id %in% drt_trips_neg$trip_id)

hist(drt_joined_trips_neg$traveled_distance_diff)
hist(seconds(drt_joined_trips_neg$trav_time_diff))

### there are X=3 cases where agents travel less distance and faster but still have a worse score...
### none of them is an ex-car user.... maybe they now have longer pt waiting time or something..

####TODOS

# analyse TT change for drt+av trips
# analyse TD change for drt+av trips
# dump output

