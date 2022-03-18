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

#auch gut, aber zu groß,
ggplot(counts, aes(x=main_mode, y=Anzahl, fill=end_activity_type))+
    geom_bar( stat="identity",position ="stack")+
    xlab("Mainmodes")+
    ylab("Anzahl der Wegzwecke")+
    facet_grid(~end_activity_type)


ggsave("example.pdf",width=10,height=10)


# gar nicht so schlecht
ggplot(TripDataframe, aes(x=main_mode, y=stat(count), group=factor(end_activity_type), fill=factor(end_activity_type)))+
  geom_bar( )+
  xlab("Fortbewegungsart")+
  ylab("Anzahl der Wegzwecke")+
  scale_y_continuous(breaks= seq(0,100000,5000))+
  labs(fill="Wegezwecke")+
  scale_fill_hue(l=40)
  #geom_text(aes(label=stat(count)),stat="count",position=position_stack(vjust=0.5))

ggsave("try.png",width=10,height=10)


# das ist gut
ggplot(counts, aes(x=main_mode, y=Anzahl, fill=end_activity_type))+
  geom_bar( stat="identity",position ="stack")+
  xlab("Mainmodes")+
  ylab("Anzahl der Wegzwecke")+
  facet_wrap(~end_activity_type, scale="free")

################ drt


