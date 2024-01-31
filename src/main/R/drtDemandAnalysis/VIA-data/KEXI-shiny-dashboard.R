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


 
#data <- read.csv2("D:/svn/shared-svn/projects/KelRide/data/KEXI/Via_data_2024_01_29/KEXI Daten ioki LK 11_2023 bis 01_2024_raw.csv", sep = ";", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8")
data <- read.csv2("D:/svn/shared-svn/projects/KelRide/data/KEXI/Via_data_sample_2023_12_20/Fahrtanfragen-2023-12-20.csv", sep = ";", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8")

#prepare data
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
         Zielort.Längengrad = as.numeric(Zielort.Längengrad)
         )
###

##TODO :
# 1) filter AV data
# filter test bookings. -> by IDs? 

# Shiny-App erstellen
ui <- fluidPage(
  navbarPage(
    title = "KEXI Dashboard",
    fluidRow(
      column(3,
             selectInput("anbieterFilter", label = "Filter Anbieter",
                         choices = unique(data$Anbietername),
                         selected = c("AV","no vendor"),
                         multiple = TRUE)
      ),
      column(3,
        dateRangeInput("dateRange", label = "Filter nach Erstellungsdatum:",
                       start = min(data$Erstellungszeit),
                       end = max(data$Erstellungszeit),
                       min = min(data$Erstellungszeit),
                       max = max(data$Erstellungszeit),
                       separator = " - ")
      ),
      column(3,
        selectInput("statusFilter", label = "Filter nach Status der Fahrtanfrage:",
                    choices = unique(data$Status.der.Fahrtanfrage),
                    selected = "Completed",
                    multiple = TRUE)
      )
    ),
    tabPanel(
      "Auslastung / Nachfrage",
        fluidRow(
          column(12,
              # Hier fügen Sie Ihre Diagramme oder Tabellen hinzu
              leafletOutput("map", height = 600), # Karte für Standorte der Fahrten
              plotlyOutput("rideRequestsOverTime"),
              plotlyOutput("totalPassengersOverTime"),
              plotlyOutput("distances_walking"),
              plotlyOutput("travelStats")
            )   
        )      
    ),
    tabPanel(
      "Service Level",
      fluidRow(
        column(12,
          plotlyOutput("vehiclesOverTime"),
          textOutput("vehicleStatsPerDay"),
          plotlyOutput("einstieg_diff_angefragt"),
          plotlyOutput("einstieg_diff_geplant"),
          plotlyOutput("speed")
        )
      )
    )
  )
)


## SERVER
server <- function(input, output) {
  
  ####################################################################
  ## Pre-Processing
  ####################################################################
  
  # Hier können Sie die Reaktionen auf Benutzereingaben hinzufügen
  filtered_data <- reactive({
    req(input$dateRange, input$statusFilter, input$anbieterFilter)
    data %>%
      filter(Erstellungszeit >= input$dateRange[1] & Erstellungszeit <= input$dateRange[2],
             Status.der.Fahrtanfrage %in% input$statusFilter,
             Anbietername %in% input$anbieterFilter)
  })
  
  timeDifferenceData <- reactive({
    filtered_data() %>%
      mutate(einstieg_diff_angefragt = as.numeric(difftime(Tatsächliche.Einstiegszeit, Angefragte.Einstiegszeit, units = "mins")),
             einstieg_diff_geplant = as.numeric(difftime(Ursprünglich.geplante.Einstiegszeit, Angefragte.Einstiegszeit, units = "mins")),)
  })
  
  distances_einstieg_data <- reactive({
    filtered_data() %>% 
      filter(!is.na(Laufdistanz..Einstieg.)) %>%
      group_by(Erstellungsdatum) %>%
      summarise(mean_distance_einstieg = mean(Laufdistanz..Einstieg.))
  })
  
  distances_ausstieg_data <- reactive({
    filtered_data() %>% 
      filter(!is.na(Laufdistanz..Ausstieg.)) %>%
      group_by(Erstellungsdatum) %>%
      summarise(mean_distance_ausstieg = mean(Laufdistanz..Ausstieg.))
  })
  
  traveled_distances_data <- reactive({
    filtered_data() %>% 
      filter(!is.na(Fahrtdistanz)) %>%
      group_by(Erstellungsdatum) %>%
      summarise(mean_traveled_distance = mean(Fahrtdistanz))
  })
  
  traveled_time_data <- reactive({
    filtered_data() %>% 
      filter(!is.na(Fahrtdauer)) %>%
      group_by(Erstellungsdatum) %>%
      summarise(mean_traveled_time = mean(Fahrtdauer))
  })
  
  grouped_data <- reactive({
    filtered_data() %>%
      group_by(Erstellungsdatum, `Status.der.Fahrtanfrage`) %>%
      summarise(Fahrtanfragen = n(),
                TotalPassengers = sum(`Anzahl.der.Fahrgäste`))
  })
  
  vehiclesPerDay <- reactive({
    filtered_data() %>%
      filter(!is.na(Fahrzeug.ID)) %>%  # Filtere fehlende Werte
      group_by(Erstellungsdatum, `Fahrzeug.ID`) %>%
      summarise(ii=1) %>% 
      group_by(Erstellungsdatum) %>% 
      summarise(n = sum(ii))
  })
  
  # Berechnung der durchschnittlichen Anzahl Fahrzeuge pro Tag
  output$vehicleStatsPerDay <- renderText({
    avg_vehicles <- mean(vehiclesPerDay()$n)
    min_vehicles <- min(vehiclesPerDay()$n)
    max_vehicles <- max(vehiclesPerDay()$n)
    paste("Anzahl zugewiesene Fahrzeuge pro Tag. Durchschnitt: ", round(avg_vehicles, 2),
          "Minimum: ", min_vehicles,
          "Maximum: ", max_vehicles)
  })
  
  ####################################################################
  ## Nachfrage Reiter Plots
  ####################################################################
  
  # Karte für Standorte der Fahrten
  output$map <- renderLeaflet({
    leaflet() %>%
      addTiles() %>%
      addHeatmap(data = filtered_data(), 
                 lat = ~Start.Breitengrad, 
                 lng = ~Start.Längengrad,
                 blur = 20, max = 1,
                 radius = 10, intensity = 2, 
                 gradient = heat.colors(10))
  })
  
  # Beispiel: Stacked Area Plot für den Status der Fahrtanfrage pro Tag
  output$rideRequestsOverTime <- renderPlotly({
    gg <- ggplot(filtered_data(), aes(x = Erstellungsdatum, fill = `Status.der.Fahrtanfrage`)) +
      geom_area(stat = "count") +
      labs(title = "Anzahl der Fahrtanfragen pro Tag",
           subtitle="für obige Filterauswahl",
           x = "Datum",
           y = "Anzahl der Fahrtanfragen") +
      theme_minimal() + 
      theme(legend.position = "top", legend.justification = "center",
            plot.title = element_text(size = 14, face = "bold", hjust = 0.5), 
            plot.subtitle = element_text(size = 16, hjust = 0.5)
            )
    
    ggplotly(gg)
  })
  
  'Passagiere am Tag'
  output$totalPassengersOverTime <- renderPlotly({
    gg <- ggplot(grouped_data(), aes(x = Erstellungsdatum)) +
      geom_line(aes(y = `TotalPassengers`, color = "Total Passengers")) +
      geom_line(aes(y = Fahrtanfragen, color = "Fahrtanfragen")) +
      labs(title = "Anzahl der Fahrgäste und Fahrtanfragen pro Tag",
           subtitle="für obige Filterauswahl",
           x = "Datum",
           y = "Anzahl") +
      theme_minimal() +
      scale_color_manual(values = c("Total Passengers" = "blue", "Fahrtanfragen" = "red"))+
      theme(legend.position = "top", legend.justification = "center",
            plot.title = element_text(size = 14, face = "bold", hjust = 0.5),
            plot.subtitle = element_text(size = 16, hjust = 0.5))
    
    ggplotly(gg)
  })
  
  # Beispiel: Line Plot für durchschnittliche Distanzen pro Tag
  output$distances_walking <- renderPlotly({
    gg <- ggplot() +
      geom_line(data = distances_einstieg_data(), aes(x = Erstellungsdatum, y = mean_distance_einstieg, color = "Einstieg"), linetype = "solid") +
      geom_line(data = distances_ausstieg_data(), aes(x = Erstellungsdatum, y = mean_distance_ausstieg, color = "Ausstieg"), linetype = "dashed") +
      labs(title = "Durchschnittliche Laufdistanzen",
           subtitle="für obige Filterauswahl",
           x = "Datum",
           y = "Durchschn. Distanz") +
      theme_minimal() + 
      theme(legend.position = "top", legend.justification = "center",
            plot.title = element_text(size = 14, face = "bold", hjust = 0.5), 
            plot.subtitle = element_text(size = 16, hjust = 0.5))
    
    ggplotly(gg)
  })
  
  # Plot TravelStats mit getrennten Y-Achsen
  output$travelStats <- renderPlotly({
    fig <- plot_ly()
    
    # Linie für die Durchschnittliche zurückgelegte Entfernung
    fig <- fig %>% 
      add_trace(
        x = traveled_distances_data()$Erstellungsdatum,
        y = traveled_distances_data()$mean_traveled_distance,
        name = "Durchschnittliche Distanz",
        type = "scatter",
        mode = "lines",
        yaxis = "y1"
      )
    
    # Linie für die Durchschnittliche Reisezeit
    fig <- fig %>% 
      add_trace(
        x = traveled_time_data()$Erstellungsdatum,
        y = traveled_time_data()$mean_traveled_time,
        name = "Durchschnittliche Zeit",
        type = "scatter",
        mode = "lines",
        yaxis = "y2"
      )
    
    # Layout-Anpassungen
    fig <- fig %>% 
      layout(
        title = list(
          text = "Durchschnittliche Distanz und Zeit",
          font = list(size = 14, color = "black", family = "Arial", weight = "bold"),
          x = 0.5  # Zentriert den Titel
        ),
        xaxis = list(title = "Datum"),
        yaxis = list(title = "Durchschnittliche Distanz", side = "left"),
        yaxis2 = list(title = "Durchschnittliche Zeit", overlaying = "y", side = "right")#,
        #plot.subtitle = list(
        #  text = "Ihr Untertitel hier",
        #  font = list(size = 16, color = "grey", family = "Arial"),
        #  x = 0.5  # Zentriert den Untertitel
        #)
      )
    
    fig
  })
  
  ####################################################################
  ## Service Level Reiter Plots
  ####################################################################
  # Line plot for 'Fahrzeug ID' per 'Erstellungsdatum'
  output$vehiclesOverTime <- renderPlotly({
    #grouped_vehicles()$`Fahrzeug.ID` <- as.factor(grouped_vehicles()$`Fahrzeug.ID`)
    #
    #plot_ly(grouped_vehicles(), x = ~Erstellungsdatum, y = ~Count, color = ~`Fahrzeug.ID`, type = 'scatter', mode = 'markers') %>%
    #  layout(title = 'Anzahl der Fahrzeuge pro Tag',
    #         xaxis = list(title = 'Datum', type = 'date', tickformat = '%Y-%m-%d', range = c(input$dateRange[1], input$dateRange[2])),
    #         yaxis = list(title = 'Anzahl'))
    gg <- ggplot(vehiclesPerDay(), aes(x = Erstellungsdatum)) +
      geom_line(aes(y = n)) +
      labs(title = "Anzahl der eingesetzten Fzge (mit Auftrag) pro Tag",
           x = "Datum",
           y = "Anzahl") +
      theme_minimal() +
      theme(legend.position = "top", legend.justification = "center",
            plot.title = element_text(size = 14, face = "bold", hjust = 0.5), 
            plot.subtitle = element_text(size = 16, hjust = 0.5))
    
    ggplotly(gg)
  })
  
  # Boxplot für die Differenz zwischen 'Angefragte.Einstiegszeit' und 'Tatsächliche.Einstiegszeit'
  output$einstieg_diff_angefragt <- renderPlotly({
    gg <- ggplot(timeDifferenceData(), aes(x = as.factor(Erstellungsdatum), y = einstieg_diff_angefragt)) +
      geom_boxplot() +
      stat_summary(fun.y = "mean", geom = "point", shape = 18, size = 1, color = "red") +
      labs(title = "Boxplot der Differenz zwischen Angefragte und Tatsächliche Einstiegszeit",
           subtitle = "Positiv = Verspätung, Negativ = früher",
           x = "Datum",
           y = "Zeitdifferenz (Minuten)") +
      theme_minimal() +
      theme(axis.text.x = element_text(angle = 45, hjust = 1), # Rotate x-axis labels for better visibility
            plot.title = element_text(size = 14, face = "bold", hjust = 0.5), 
            plot.subtitle = element_text(size = 16)
            )  
    
    ggplotly(gg)
  })
  
  # Boxplot für die Differenz zwischen 'Angefragte.Einstiegszeit' und 'Tatsächliche.Einstiegszeit'
  output$einstieg_diff_geplant <- renderPlotly({
    gg <- ggplot(timeDifferenceData(), aes(x = as.factor(Erstellungsdatum), y = einstieg_diff_geplant)) +
      geom_boxplot() +
      stat_summary(fun.y = "mean", geom = "point", shape = 18, size = 1, color = "red") +
      labs(title = "Boxplot der Differenz zwischen ursprünglich geplanter und tatsächlicher Einstiegszeit",
           subtitle = "Positiv = Verspätung, Negativ = früher",
           x = "Datum",
           y = "Zeitdifferenz (Minuten)") +
      theme_minimal() +
      theme(axis.text.x = element_text(angle = 45, hjust = 1),   # Rotate x-axis labels for better visibility
            plot.title = element_text(size = 14, face = "bold", hjust = 0.5), 
            plot.subtitle = element_text(size = 16)
            )
    
    ggplotly(gg)
  })
  
  #Spped
  output$speed <- renderPlotly({
    # Merge der Dataframes basierend auf dem Erstellungsdatum
    merged_data <- merge(traveled_distances_data(), traveled_time_data(), by = "Erstellungsdatum", all = TRUE)
    
    # Berechnung des Quotienten
    merged_data$quotient <- 60 * merged_data$mean_traveled_distance / merged_data$mean_traveled_time
    
    # Plot mit dem Quotienten
    fig <- plot_ly(
      x = merged_data$Erstellungsdatum,
      y = merged_data$quotient,
      type = "scatter",
      mode = "lines",
      name = "Quotient",
      yaxis = "y"
    )
    
    # Layout-Anpassungen
    fig <- fig %>% 
      layout(
        title = list(
          text = "Durchschnittlicher Speed mit Pax an Board",
          font = list(size = 14, color = "black", family = "Arial", weight = "bold"),
          x = 0.5  # Zentriert den Titel
        ),
        xaxis = list(title = "Datum"),
        yaxis = list(title = "Speed [km/h]")
      )
    
    fig
  })
  
  
}

# Shiny-App starten
shinyApp(ui = ui, server = server)
