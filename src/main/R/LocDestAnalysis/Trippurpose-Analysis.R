library(tidyverse)
library(dplyr)
library(ggplot2)
library(plotly)
library(hrbrthemes)

require(readr)
filePath<-"/Users/tomkelouisa/Documents/VSP/Kehlheimfiles/Wegezweckeplots" # the plots are going to be saved here
filePaths<-"/Users/tomkelouisa/Documents/VSP/Kehlheimfiles" # the output_trips file should be there
filename <- "kelheim.output_trips.csv.gz"


setwd(filePaths)
TripDataframe <- read.csv(filename, stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", sep= ";")
#integer am Ende der en

TripDataframe <- TripDataframe %>%
    rowwise() %>%
        mutate(end_activity_type=strsplit(end_activity_type, "_(?!.*_)", perl=TRUE)[[1]][1])

counts <- TripDataframe %>%
    group_by(main_mode,end_activity_type) %>%
        summarise(Anzahl = n())

interactiveMode<-FALSE
#######################
#Analyse nur für drt
drtanalyse <- TripDataframe %>%
  filter(main_mode=="drt")
print(length(drtanalyse$main_mode))

####################
#drt Balkendiagramm
plot <-ggplot(drtanalyse,aes(x=end_activity_type))+
  geom_bar(fill= "steelblue")+
  xlab("Trippurpose")+
  ggtitle("Trippurposes of drt usage")+
  ylab("Number of trippurposes ")+
  theme(axis.text.x = element_text(angle = 45,hjust=1))+
  theme(plot.title = element_text(hjust = 0.5))+
  #theme_ipsum()+
  theme(text=element_text(size=20))+
  geom_text(aes(label=stat(count)),stat="count",position = position_stack(vjust = 0.5),text=element_text(size=20),color="white")


plotFile <-paste(filePath,"/drt-Wegezwecke.png",sep="")
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
  xlab("Trippurpose")+
  ylab("Traveling mode")+
  ggtitle("Trippurposes of drt usage")+
  scale_fill_hue(l=40)+
  #theme_ipsum()+
  theme(text=element_text(size=20))+
  theme(plot.title = element_text(hjust = 0.5))+
  #theme(axis.text.x = element_text(angle = 45,hjust=1))+
  geom_text(aes(label=stat(count)),stat="count",position = position_stack(vjust = 0.5))


plotFile <-paste(filePath,"/drt_as_Stack_Wegzwecke.png",sep="")
paste("printing plot to ", plotFile)
#ggsave(plotFile)
png(plotFile, width = 1200, height = 800)
plot1
dev.off()
if(interactiveMode){
  ggplotly(plot1)
}

#ggsave("example.pdf",width=10,height=10)


##############
#Stackbalkendiagramm, die Wegzwecke auf Fortbewegungart aufgetragen
#drt sehr schlecht erkennbar, da so geringe Anzahl
p2 <-ggplot(TripDataframe, aes(x=main_mode, y=stat(count), group=factor(end_activity_type), fill=factor(end_activity_type)))+
  geom_bar( )+
  xlab("Traveling mode")+
  ylab("Number of Trippurposes")+
  ggtitle("Trippurposes of traveling modes")+
  scale_y_continuous(breaks= seq(0,100000,5000))+
  labs(fill="Wegezwecke")+
  #theme_ipsum()+
  theme(text=element_text(size=20))+
  theme(plot.title = element_text(hjust = 0.5))+
  scale_fill_hue(l=40)

plotFile <-paste(filePath,"/Wegezwecke_Fortbewegungsmittel.png",sep="")
paste("printing plot to ", plotFile)
png(plotFile, width = 1200, height = 800)
p2
dev.off()
if(interactiveMode){
  ggplotly(p2)
}



################
# Veranschauling der Fortbewegungsarten im Bezug auf einen Wegzweck
p3<-ggplot(counts, aes(x=main_mode, y=Anzahl, fill=end_activity_type))+
  geom_bar( stat="identity",position ="stack")+
  xlab("Mainmodes")+
  ylab("Number of Trippurposes")+
  #theme_ipsum()+
  theme(text=element_text(size=20))+
  ggtitle("traveling modes refenced to trippurpose")+
  theme(plot.title = element_text(hjust = 0.5))+
  facet_wrap(~end_activity_type, scale="free")

plotFile <-paste(filePath,"/Fortbewegungsmittel_pro_Wegzweck.png",sep="")
paste("printing plot to ", plotFile)
png(plotFile, width = 1200, height = 800)
p3
dev.off()
if(interactiveMode){
  ggplotly(p3)
}
################
#Veranschaulichung der Wegezwecke im Bezug auf eine Fortbewegungsart

p4 <-ggplot(counts, aes(x=end_activity_type,y=Anzahl, fill=main_mode))+
  geom_bar( stat="identity",position ="stack")+
  ylab("Mainmodes")+
  xlab("Numbr of Trippurposes")+
  ggtitle("Trippurposes refernced to the traveling mode")+
  facet_wrap(~main_mode, scale="free")+
  #theme_ipsum()+
  theme(text=element_text(size=20))+
  theme(plot.title = element_text(hjust = 0.5))+
  theme(axis.text.x = element_text(angle = 45,hjust=1))



plotFile <-paste(filePath,"/Weckzweck_pro_Fortbewegungsmittel.png",sep="")
paste("printing plot to ", plotFile)
png(plotFile, width = 1200, height = 800)
p4
dev.off()
if(interactiveMode){
  ggplotly(p4)
}



## simplify the plot edu=combine

