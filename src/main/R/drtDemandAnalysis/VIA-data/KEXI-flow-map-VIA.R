# Installieren Sie die benötigten Pakete, wenn noch nicht installiert
# install.packages(c("shiny", "dplyr", "ggplot2"))

# Laden Sie die Bibliotheken
library(shiny)
library(dplyr)
library(ggplot2)
library(lubridate)
library(plotly)
library(leaflet)
library(leaflet.extras) # for heatmap
library(geosphere) # for flow chart
library(RColorBrewer)


#### read data.
##### you have to download the data in Excel format and then export to csv !!with semi-colon as separator!! because the addresses have commata in them and then commata does not work as delimiter!!

#input files
testdata <- "D:/svn/shared-svn/projects/KelRide/data/KEXI/Via_data_sample_2023_12_20/Fahrtanfragen-2023-12-20.csv"
data_feb_14 <- "D:/svn/shared-svn/projects/KelRide/data/KEXI/Via_data_2024_02_14/Fahrtanfragen-2024-02-14.csv"
data_jan_01_feb_27 <- "D:/svn/shared-svn/projects/KelRide/data/KEXI/Via_data_2024_02_27/Fahrtanfragen-2024-02-27.csv"

#parse data
data <- read.csv2(data_jan_01_feb_27, sep = ";", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8")


### prepare data

## filter out test bookings

#10718 is a real customer
#10031 too
testingCustomerIds_extended <- c(1, 
                                 43, 
                                 649, 
                                 673,
                                 3432, 
                                 3847, 
                                 3887, 
                                 4589, 
                                 7409,
                                 7477,
                                 9808, 
                                 9809, 
                                 8320,
                                 12777, 
                                 13288
)

#pepare data tyopes
data <- data %>% 
  mutate(Erstellungszeit = ymd_hms(Erstellungszeit.der.Fahrtanfrage),
         Erstellungsdatum = date(Erstellungsdatum.der.Fahrtanfrage),
         Angefragte.Einstiegszeit = ymd_hms(Angefragte.Einstiegszeit),
         Angefragte.Ausstiegszeit = ymd_hms(Angefragte.Ausstiegszeit),
         Tatsächliche.Einstiegszeit = ymd_hms(Tatsächliche.Einstiegszeit),
         Tatsächliche.Ausstiegszeit = ymd_hms(Tatsächliche.Ausstiegszeit),
         Ursprünglich.geplante.Einstiegszeit = ymd_hms(Ursprünglich.geplante.Einstiegszeit),
         Laufdistanz..Einstieg. = as.numeric(Laufdistanz..Einstieg.),
         Laufdistanz..Ausstieg. = as.numeric(Laufdistanz..Ausstieg.),
         Fahrtdistanz = as.numeric(Fahrtdistanz),
         Fahrtdauer = as.numeric(Fahrtdauer),
         Start.Breitengrad = as.numeric(Start.Breitengrad),
         Start.Längengrad = as.numeric(Start.Längengrad),
         Zielort.Breitengrad = as.numeric(Zielort.Breitengrad),
         Zielort.Längengrad = as.numeric(Zielort.Längengrad),
         Fahrtbewertung..1.5. = as.numeric(Fahrtbewertung..1.5.),
         isTestBooking = Fahrgast.ID %in% testingCustomerIds_extended
  )


flow_data <- data %>% 
  filter(Tatsächliche.Einstiegsadresse != "") %>% 
  # die Daten liegen bereits in der VIA-Datenplattform in falschen Spalten vor
  mutate(from.x = as.numeric(Reise.Endzeitstempel),
         from.y = as.numeric(Laufdistanz..Abholung.),
         to.x = as.numeric(Zur.Benutzerbestellung.vorgelegt),
         to.y = as.numeric(Anzahl.der.Abschnitte),
  ) %>% 
  select(Tatsächliche.Einstiegsadresse, Tatsächliche.Ausstiegsadresse, from.x, from.y, to.x, to.y)

origins <- flow_data %>% 
  group_by(from.x) %>% 
  select(Tatsächliche.Einstiegsadresse, from.x, from.y)

destinations <- flow_data %>% 
  group_by(to.x) %>% 
  select(Tatsächliche.Ausstiegsadresse, to.x, to.y)

ff <- flow_data %>% 
  group_by(Tatsächliche.Einstiegsadresse, Tatsächliche.Ausstiegsadresse) %>% 
  summarise(counts = n()) %>% 
  ungroup() %>% 
  left_join(origins, by = "Tatsächliche.Einstiegsadresse") %>%
  left_join(destinations, by = "Tatsächliche.Ausstiegsadresse")

flows <- gcIntermediate(ff[,4:5], ff[,6:7], sp = TRUE, addStartEnd = TRUE)

flows$counts <- ff$counts

flows$origins <- ff$Tatsächliche.Einstiegsadresse

flows$destinations <- ff$Tatsächliche.Ausstiegsadresse

flows



hover <- paste0(flows$origins, " to ", 
                flows$destinations, ': ', 
                as.character(flows$counts))

pal <- colorFactor(brewer.pal(4, 'Set2'), flows$origins)

leaflet() %>%
  addProviderTiles('CartoDB.Positron') %>%
  addPolylines(data = flows, weight = ~counts, label = hover, 
               group = ~origins, color = ~pal(origins)) %>%
  addLayersControl(overlayGroups = unique(flows$origins), 
                   options = layersControlOptions(collapsed = FALSE))
