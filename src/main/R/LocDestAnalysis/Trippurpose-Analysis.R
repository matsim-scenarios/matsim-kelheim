library(tidyverse)
library(dplyr)
library(ggplot2)
library(plotly)
library(hrbrthemes)
library(ggpubr)
library(patchwork)
library(RColorBrewer)

require(readr)

##################################################################################################################################
##INPUTS

#  !!!INCLUDE TRAILING SLASH!!!
#the output_trips file should be there

#runDirectory <- "D:/svn/public-svn/matsim/scenarios/countries/de/kelheim/projects/KelRide/AVServiceAreas/output/KEXI-base-case/"
runDirectory <- "Z:/net/ils/matsim-kelheim/kelheim-case-study/KEXI-with-av/run-asv-0.7_marginal-5e-5-BAUERNSIEDLUNG/" 

interactiveMode <-FALSE

#setwd(runDirectory) # !not necessary but try if something fails!

##################################################################################################################################
##output directory is automatically set and created
outputDir <- paste(runDirectory, "analysis-trip-purposes", sep = "") # the plots are going to be saved here
if(!file.exists(outputDir)){
  print("creating analysis sub-directory")
  dir.create(outputDir)  
}


TripDataframe <- read.csv(list.files(runDirectory, pattern = "*.output_trips.csv.gz", full.names = T, include.dirs = F), stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", sep= ";")
#integer am Ende der en

TripDataframe <- TripDataframe %>%
    rowwise() %>%
        mutate(end_activity_type=strsplit(end_activity_type, "_(?!.*_)", perl=TRUE)[[1]][1])

counts <- TripDataframe %>%
    group_by(main_mode,end_activity_type) %>%
        summarise(count = n())




##################################################################################################################################################
#######################


#analyze drt mode
drtanalyse <- TripDataframe %>%
  filter(main_mode=="drt")
print(length(drtanalyse$main_mode))

####################
#drt Balkendiagramm
plot <-ggplot(drtanalyse,aes(x=end_activity_type))+
  geom_bar(fill= "steelblue")+
  xlab("Trip purpose")+
  ggtitle("Trip purposes of rides with conventional KEXI")+
  ylab("Number of trips ")+
  theme(axis.text.x = element_text(angle = 45,hjust=1))+
  theme(plot.title = element_text(hjust = 0.5))+
  #theme_ipsum()+
  theme(text=element_text(size=20))+
  geom_text(aes(label=stat(count)),stat="count",position = position_stack(vjust = 0.5),text=element_text(size=20),color="white")


plotFile <-paste(outputDir,"/drt_trip_purposes.png",sep="")
paste("printing plot to ", plotFile)
png(plotFile, width = 1200, height = 800)
plot
dev.off()
if(interactiveMode){
  ggplotly(plot)
}

##############
#drt Stackbalkendiagramm
plot1 <-ggplot(drtanalyse,aes(x=main_mode,y=stat(count), group=factor(end_activity_type), fill=factor(end_activity_type)))+
  geom_bar()+
  xlab("Trip purpose")+
  ylab("Traveling mode")+
  ggtitle("Trip purposes of trips with conventional KEXI")+
  scale_fill_manual(values = c("#A6CEE3", "#6EAACF", "#3687BC", "#4190AA" ,"#7EBA98" ,"#AADB84" ,"#76C15D", "#41A737", "#6D9E4C", "#C09B78", "#F88A8A", "#EE5656", "#E42123", "#EC5439", "#F6985B", "#FDB35B" ,"#FE992D", "#FF7F00")) +
  #theme_ipsum()+
  theme(text=element_text(size=20))+
  theme(plot.title = element_text(hjust = 0.5))+
  #theme(axis.text.x = element_text(angle = 45,hjust=1))+
  geom_text(aes(label=stat(count)),stat="count",position = position_stack(vjust = 0.5))


plotFile <-paste(outputDir,"/drt_trip_purposes_stacked.png",sep="")
paste("printing plot to ", plotFile)
#ggsave(plotFile)
png(plotFile, width = 1200, height = 800)
plot1
dev.off()
if(interactiveMode){
  ggplotly(plot1)
}

#analyze av mode
avanalyse <- TripDataframe %>%
  filter(main_mode=="av")
print(length(drtanalyse$main_mode))

####################
#drt Balkendiagramm
plot <-ggplot(avanalyse,aes(x=end_activity_type))+
  geom_bar(fill= "steelblue")+
  xlab("Trip purpose")+
  ggtitle("Trip purposes of rides with KEXI AV")+
  ylab("Number of trips ")+
  theme(axis.text.x = element_text(angle = 45,hjust=1))+
  theme(plot.title = element_text(hjust = 0.5))+
  #theme_ipsum()+
  theme(text=element_text(size=20))+
  geom_text(aes(label=stat(count)),stat="count",position = position_stack(vjust = 0.5),text=element_text(size=20),color="white")


plotFile <-paste(outputDir,"/av_trip_purposes.png",sep="")
paste("printing plot to ", plotFile)
png(plotFile, width = 1200, height = 800)
plot
dev.off()
if(interactiveMode){
  ggplotly(plot)
}

##############
#drt Stackbalkendiagramm
plot1 <-ggplot(avanalyse,aes(x=main_mode,y=stat(count), group=factor(end_activity_type), fill=factor(end_activity_type)))+
  geom_bar()+
  xlab("Trip purpose")+
  ylab("Traveling mode")+
  ggtitle("Trip purposes of trips with KEXI AV")+
  scale_fill_manual(values = c("#A6CEE3", "#6EAACF", "#3687BC", "#4190AA" ,"#7EBA98" ,"#AADB84" ,"#76C15D", "#41A737", "#6D9E4C", "#C09B78", "#F88A8A", "#EE5656", "#E42123", "#EC5439", "#F6985B", "#FDB35B" ,"#FE992D", "#FF7F00")) +
  #theme_ipsum()+
  theme(text=element_text(size=20))+
  theme(plot.title = element_text(hjust = 0.5))+
  #theme(axis.text.x = element_text(angle = 45,hjust=1))+
  geom_text(aes(label=stat(count)),stat="count",position = position_stack(vjust = 0.5))


plotFile <-paste(outputDir,"/av_trip_purposes_stacked.png",sep="")
paste("printing plot to ", plotFile)
#ggsave(plotFile)
png(plotFile, width = 1200, height = 800)
plot1
dev.off()
if(interactiveMode){
  ggplotly(plot1)
}





###############################################################################################################################################

################
# Veranschauling der Fortbewegungsarten im Bezug auf einen Wegzweck
p3<-ggplot(counts, aes(x=main_mode, y=count, fill=end_activity_type))+
  geom_bar( stat="identity",position ="stack")+
  xlab("Main mode")+
  ylab("Number of trips")+
  #theme_ipsum()+
  theme(text=element_text(size=20))+
  ggtitle("traveling modes per trip purpose")+
  theme(plot.title = element_text(hjust = 0.5))+
  facet_wrap(~end_activity_type, scale="free")

plotFile <-paste(outputDir,"/modes_per_trip_purpose.png",sep="")
paste("printing plot to ", plotFile)
png(plotFile, width = 1200, height = 800)
p3
dev.off()
if(interactiveMode){
  ggplotly(p3)
}
################
#Veranschaulichung der Wegezwecke im Bezug auf eine Fortbewegungsart

p4 <-ggplot(counts, aes(x=end_activity_type,y=count, fill=main_mode))+
  geom_bar( stat="identity",position ="stack")+
  ylab("Number of trips ")+
  xlab("Trip purposes")+
  ggtitle("Trip purposes per traveling mode")+
  facet_wrap(~main_mode, scale="free")+
  #theme_ipsum()+
  theme(text=element_text(size=20))+
  theme(plot.title = element_text(hjust = 0.5))+
  #theme(legend.position = "none")+
  theme(axis.text.x = element_text(angle = 45,hjust=1,size=8))


#ggtitle("Trip purposes refernced to the traveling mode")
plotFile <-paste(outputDir,"/trip_purpose_per_mode_split.png",sep="")
paste("printing plot to ", plotFile)
png(plotFile, width = 1200, height = 800)
p4
dev.off()
if(interactiveMode){
  ggplotly(p4)
}



#########
p5 <-ggplot(counts, aes(x=end_activity_type,y=count, fill=main_mode))+
  geom_bar( stat="identity",position ="stack")+
  #ylab("Mainmodes")+
  #lab("Number of trips")+
  theme(legend.position = "none")+
  #ggtitle("Trip purposes refernced to the traveling mode")+
  #facet_wrap(~main_mode, scale="free")+
  #theme_ipsum()+
  theme(text=element_text(size=20))+
  theme(plot.title = element_text(hjust = 0.5))+
  theme(axis.text.x = element_text(angle = 45,hjust=1))

#figure <- p4 + inset_element(p5,left=0.5,bottom=0, top=0.5,right=0)
#ggtitle("Trip purposes refernced to the traveling mode")
dev.off()
layout <- c(
  area(t = 1, l = 1, b = 5, r = 5),
  area(t = 5, l = 3, b = 5, r = 5)
)
figure<- p4+p5+ plot_layout(design=layout)
#figure=p4 + inset_element(p5, left = 0.6, bottom = 1, right = 1, top = 0.6)
ggtitle("Trip purposes per traveling mode")
plotFile <-paste(outputDir,"/trip_purpose_per_mode_all.png",sep="")
paste("printing plot to ", plotFile)
png(plotFile, width = 1200, height = 800)
figure
dev.off()
if(interactiveMode){
  ggplotly(figure)
}



#######
p5 <-ggplot(counts, aes(x=end_activity_type,y=count, fill=main_mode))+
  geom_bar( stat="identity",position ="stack")+
  ylab("Number of trips")+
  xlab("Trip purposes")+

  ggtitle("Trip purposes referenced to the traveling mode")+

  theme(text=element_text(size=20))+
  theme(plot.title = element_text(hjust = 0.5))+
  theme(axis.text.x = element_text(angle = 45,hjust=1))


plotFile <-paste(outputDir,"/modes_per_trip_purpose_stack.png",sep="")
paste("printing plot to ", plotFile)
png(plotFile, width = 1200, height = 800)
p5
dev.off()
if(interactiveMode){
  ggplotly(p5)
}


##############
#Stackbalkendiagramm, die Wegzwecke auf Fortbewegungart aufgetragen
#drt sehr schlecht erkennbar, da so geringe Anzahl

p2 <-ggplot(TripDataframe, aes(x=main_mode, y=stat(count), group=factor(end_activity_type), fill=factor(end_activity_type)))+
  geom_bar()+
  xlab("Traveling mode")+
  ylab("Number of trips")+
  ggtitle("Trip purposes of traveling modes")+
  labs(fill="Wegezwecke")+
  scale_fill_manual(values = c("#A6CEE3", "#6EAACF", "#3687BC", "#4190AA" ,"#7EBA98" ,"#AADB84" ,"#76C15D", "#41A737", "#6D9E4C", "#C09B78", "#F88A8A", "#EE5656", "#E42123", "#EC5439", "#F6985B", "#FDB35B" ,"#FE992D", "#FF7F00")) +
  theme(text=element_text(size=20))+
  theme(plot.title = element_text(hjust = 0.5))


plotFile <-paste(outputDir,"/trip_purpose_per_mode_stack.png",sep="")
paste("printing plot to ", plotFile)
png(plotFile, width = 1200, height = 800)
p2
dev.off()
if(interactiveMode){
  ggplotly(p2)
}

