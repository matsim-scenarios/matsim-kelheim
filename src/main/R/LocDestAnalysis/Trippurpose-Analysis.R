library(tidyverse)
library(dplyr)
library(ggplot2)

require(readr)
filePath<-"/Users/tomkelouisa/Documents/VSP/Kehlheimfiles"
filename <- "kelheim.output_trips.csv.gz"

setwd(filePath)
TripDataframe <- read.csv(filename, stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", sep= ";")
#integer am Ende der en

TripDataframe <- TripDataframe %>%
    rowwise() %>%
        mutate(end_activity_type=strsplit(end_activity_type, "_(?!.*_)", perl=TRUE)[[1]][1])

counts <- TripDataframe %>%
    group_by(main_mode,end_activity_type) %>%
        summarise(Anzahl = n())

ggplot(counts, aes(x=main_mode, fill=end_activity_type))+
    geom_bar( aes(y=..count..))+
    xlab("Mainmodes")+
    ylab("Anzahl der Wegzwecke")

ggsave("plot.png",width=5,height=5)



