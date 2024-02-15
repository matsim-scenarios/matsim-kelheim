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

#read data



testdata <- "D:/svn/shared-svn/projects/KelRide/data/KEXI/Via_data_sample_2023_12_20/Fahrtanfragen-2023-12-20.csv"
data_feb_14 <- "D:/svn/shared-svn/projects/KelRide/data/KEXI/Via_data_2024_02_14/Fahrtanfragen-2024-02-14.csv"
data <- read.csv2(data_feb_14, sep = ";", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8")

#Id s for test booking
testingCustomerIds <- c(1, 43, 649, 3432, 3847, 3887, 12777)
testingCustomerIds_extended <- c(1, 43, 649, 3432, 3847, 3887, 12777, 673, 4589, 7409, 7477, 9808, 9809, 10718, 13288)

#prepare data
data2 <- data %>% 
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
         isTestBooking = Fahrgast.ID %in% testingCustomerIds_extended
  )


data_noTests2 <- data2 %>% 
  filter(isTestBooking == FALSE)

data_2024 <- data_noTests %>% 
  filter(Erstellungszeit >= ymd_hms("2024-01-01 00:00:00"))

hist(data_noTests$Anzahl.der.Fahrgäste)
hist(data_2024$Anzahl.der.Fahrgäste)

plot(data_2024$Erstellungszeit, data_2024$Anzahl.der.Fahrgäste)

gg <- ggplot(data_2024, aes(x = as.factor(Erstellungsdatum), y = Anzahl.der.Fahrgäste)) +
  geom_boxplot() +
  stat_summary(fun.y = "mean", geom = "point", shape = 18, size = 1, color = "red",
               aes(label = round(..y.., 2))) + # Runde auf zwei Dezimalstellen
  #geom_point(stat = "summary", fun = "mean", shape = 18, size = 3, color = "red") + # Verwende geom_point statt stat_summary
  labs(title = "Boxplot Fahrgäste",
       x = "Datum",
       y = "Fahrgäste") +
  theme_minimal() +
  theme(axis.text.x = element_text(angle = 45, hjust = 1), # Rotate x-axis labels for better visibility
        plot.title = element_text(size = 14, face = "bold", hjust = 0.5), 
        plot.subtitle = element_text(size = 16)
  )  

ggplotly(gg)

tt <- data_2024 %>% 
  filter(!is.na(Anzahl.der.Fahrgäste)) %>%
  group_by(Erstellungsdatum, Anzahl.der.Fahrgäste) %>%
  summarise(Frequency = n())

# Barplot erstellen
gg <- ggplot(tt, aes(x = as.factor(Erstellungsdatum), y = Frequency, fill = as.factor(Anzahl.der.Fahrgäste))) +
  geom_bar(stat = "identity") +
  labs(title = "Häufigkeit der Ausprägungen von Anzahl.der.Fahrgäste",
       x = "Datum",
       y = "Häufigkeit",
       fill = "Anzahl der Fahrgäste") +
  theme_minimal() +
  theme(axis.text.x = element_text(angle = 45, hjust = 1), 
        plot.title = element_text(size = 14, face = "bold", hjust = 0.5),
        legend.position = "bottom"
  )

gg

# Daten gruppieren und Häufigkeit zählen
tt <- data_2024 %>% 
  filter(!is.na(Anzahl.der.Fahrgäste)) %>%
  group_by(Erstellungsdatum, Anzahl.der.Fahrgäste) %>%
  summarise(Frequency = n())

# Berechne den Durchschnitt pro Erstellungsdatum
avg_values <- tt %>%
  group_by(Erstellungsdatum) %>%
  summarise(avg = mean(Anzahl.der.Fahrgäste))

# Barplot erstellen
gg <- ggplot(tt, aes(x = as.factor(Erstellungsdatum), y = Frequency, fill = as.factor(Anzahl.der.Fahrgäste))) +
  geom_bar(stat = "identity") +
  geom_line(data = avg_values, aSes(x = as.factor(Erstellungsdatum), y = avg), color = "red", size = 1.5) + # Linie für den Durchschnitt hinzufügen
  labs(title = "Häufigkeit der Ausprägungen von Anzahl.der.Fahrgäste",
       x = "Datum",
       y = "Häufigkeit",
       fill = "Anzahl der Fahrgäste") +
  theme_minimal() +
  theme(axis.text.x = element_text(angle = 45, hjust = 1), 
        plot.title = element_text(size = 14, face = "bold", hjust = 0.5),
        legend.position = "bottom"
  )

gg


# Daten gruppieren und Häufigkeit zählen
tt <- data_2024 %>% 
  filter(!is.na(Anzahl.der.Fahrgäste)) %>%
  group_by(Erstellungsdatum, Anzahl.der.Fahrgäste) %>%
  summarise(Frequency = n())

# Berechne den Durchschnitt pro Erstellungsdatum
avg_values <- tt %>%
  group_by(Erstellungsdatum) %>%
  summarise(avg = mean(Anzahl.der.Fahrgäste), Anzahl.der.Fahrgäste = 0) # Hier den Durchschnitt direkt berechnen

# Barplot erstellen
gg <- ggplot(tt, aes(x = as.factor(Erstellungsdatum), y = Frequency, fill = as.factor(Anzahl.der.Fahrgäste))) +
  geom_bar(stat = "identity") +
  geom_line(data = avg_values, aes(x = as.factor(Erstellungsdatum), y = avg), color = "red", size = 1.5) + # Linie für den Durchschnitt hinzufügen
  labs(title = "Häufigkeit der Ausprägungen von Anzahl.der.Fahrgäste",
       x = "Datum",
       y = "Häufigkeit",
       fill = "Anzahl der Fahrgäste") +
  theme_minimal() +
  theme(axis.text.x = element_text(angle = 45, hjust = 1), 
        plot.title = element_text(size = 14, face = "bold", hjust = 0.5),
        legend.position = "bottom"
  )

gg


# Daten gruppieren und Häufigkeit zählen
tt <- data_2024 %>% 
  filter(!is.na(Anzahl.der.Fahrgäste)) %>%
  group_by(Erstellungsdatum, Anzahl.der.Fahrgäste) %>%
  summarise(Frequency = n())

# Berechne den Durchschnitt pro Erstellungsdatum
avg_values <- tt %>%
  group_by(Erstellungsdatum) %>%
  summarise(avg = mean(Anzahl.der.Fahrgäste), Anzahl.der.Fahrgäste = "avg")

#tt <- tt %>%
#  arrange(desc(Anzahl.der.Fahrgäste))


# Barplot erstellen
gg <- ggplot(tt, aes(x = as.factor(Erstellungsdatum), y = Frequency, fill = as.factor( Anzahl.der.Fahrgäste ))) +
  geom_bar(stat = "identity") +
  geom_line(data = avg_values, aes(x = as.factor(Erstellungsdatum), y = avg, group = 1), color = "black", size = 1.5) + # Linie für den Durchschnitt hinzufügen
  labs(title = "Häufigkeit der Ausprägungen von Anzahl.der.Fahrgäste",
       x = "Datum",
       y = "Häufigkeit",
       fill = "Anzahl der Fahrgäste") +
  theme_minimal() +
  theme(axis.text.x = element_text(angle = 45, hjust = 1), 
        plot.title = element_text(size = 14, face = "bold", hjust = 0.5),
        legend.position = "bottom"
  )

ggplotly(gg)
  


test <- data_2024 %>% 
  filter(Erstellungszeit >= ymd_hms("2024-01-23 00:00:00"), Erstellungszeit <= ymd_hms("2024-01-23 23:59:59"))

test

test2 <- data %>% 
  filter(Anzahl.der.Fahrgäste == 6)


ggplot(test2, aes(x = as.factor(Fahrgast.ID))) +
  geom_bar(fill = "skyblue") +
  labs(title = "Wie oft mit 6 Fahrgästen gebucht wurde",
       x = "Fahrgast.ID",
       y = "Häufigkeit") +
  theme_minimal()


tt <- test2 %>% 
  group_by(Fahrgast.ID) %>% 
  tally()

ggplot(tt, aes(x = Fahrgast.ID)) +
  geom_bar(fill = "skyblue") +
  labs(title = "Wie oft mit 6 Fahrgästen gebucht wurde",
       x = "Fahrgast.ID",
       y = "Häufigkeit") +
  theme_minimal()



zzz <- data_noTests2 %>% 
  group_by(Fahrgast.ID) %>% 
  tally()

z <- zzz %>% 
  filter(n > 1)

g <- ggplot(data_noTests2, aes(x = as.factor(Fahrgast.ID))) +
  geom_bar(fill = "skyblue") +
  labs(title = "Wie oft von wem gebucht wurde",
       x = "Fahrgast.ID",
       y = "Häufigkeit") +
  theme_minimal()


ggplotly(g)


personTest <- data_noTests2 %>% 
  filter(Fahrgast.ID == 2296)


