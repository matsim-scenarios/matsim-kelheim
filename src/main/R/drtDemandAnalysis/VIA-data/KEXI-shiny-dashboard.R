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
library(zoo) #for moving averages



#### read data.


##### you have to download the demand data in Excel format and then export to csv !!with semi-colon as separator!!
##### Because the addresses have commata in them and then commata might not work as delimiter!!
##### for the driver shift data, you can/should directly download in csv format !!
# fuer den datensatz vom april 24 geht das wohl doch mit dem komma als trennzeichen -- datensatz hatte Jan Eller heruntergeladen


#input files

data_jan_01_apr_24 <- "D:/svn/shared-svn/projects/KelRide/data/KEXI/Via_data_2024_04_24/Fahrtanfragen-2024-04-24.csv"
data_jan_01_apr_24_fahrerschichten <- "D:/svn/shared-svn/projects/KelRide/data/KEXI/Via_data_2024_04_24/Fahrerschichten-2024-04-24.csv"

requests_file <- "D:/svn/shared-svn/projects/KelRide/data/KEXI/VIA_data_2024_05_06/Fahrtanfragen-2024-05-06.csv"
shifts_file <- "D:/svn/shared-svn/projects/KelRide/data/KEXI/VIA_data_2024_05_06/Fahrerschichten-2024-05-06.csv"

#parse data
data <- read.csv2(requests_file, sep = ";", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8")
#data_fahrerschichten  <- read.csv2(shifts_file, sep = ",", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8") %>% 
#  mutate(time = ymd_hms(Datum),
#         date = date(time))

data_fahrerschichten <- read.csv2(shifts_file, sep = ",", stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8") %>% 
  mutate(Start = ymd_hms(Schichtstart),
         Ende = ymd_hms(Schichtende),
         Dauer_h = as.numeric(Ende - Start, units="hours"),
         Tag = date(Start))


### prepare data

## filter out test bookings
#10718 is a real customer
#10031 too
testingCustomerIds_extended <- c(1,  # Testrider
                                 43, # Stefan
                                 649,# Salah
                                 673,# Markus
                                 3432,# ??
                                 3847, # CS Test
                                 3887, # Jonathan
                                 4589, # Gerlinde
                                 7409, # Jalal
                                 7477, # Bus31
                                 9808, # Marina
                                 9809, # Günter
                                 8320, # Bus28
                                 12777, # Salah
                                 13288, #Bus47
                                 13498  #kam von Jan Eller
)


#pepare data types
data <- data %>% 
  mutate(
         Erstellungszeit = ymd_hms(Erstellungszeit.der.Fahrtanfrage),
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
#for some reason, doing this with an ifelse clause does not work, so making sure time is not N/A in a separate step
data <- data %>% 
  mutate(time = if_else(is.na(Angefragte.Einstiegszeit), Angefragte.Ausstiegszeit, Angefragte.Einstiegszeit),
         date = date(time),
         isWeekend = wday(date) >= 6)


## TODO:
#Anbietername wieder aufnehmen und filtern!

test <- data %>%
  select(Fahrgast.ID, isTestBooking)
###

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
        dateRangeInput("dateRange", label = "Filter nach Angefragtem Einstieg:",
                       #start = min(data$time),
                       start = "2024-01-01",
                       end = max(data$time),
                       min = min(data$time),
                       max = max(data$time),
                       separator = " - ")
      ),
      #column(3,
      #  selectInput("statusFilter", label = "Filter nach Status der Fahrtanfrage:",
      #              choices = unique(data$Status.der.Fahrtanfrage),
      #              selected = "Completed",
      #              multiple = TRUE)
      #),
      column(3,
             selectInput("testbookingFilter", label = "Filter Testbuchungen",
                         choices = unique(data$isTestBooking),
                         selected = "FALSE",
                         multiple = TRUE)
      ),
      column(3,
             checkboxInput("filterWeekend", "Filter Wochenende", FALSE))
    ),
    tabPanel(
      "O1: Auslastung",
        fluidRow(
          column(12,
              # Hier fügen Sie Ihre Diagramme oder Tabellen hinzu
              plotlyOutput("passengersWeekly"),
              plotlyOutput("totalPassengersOverTime"),
              #plotlyOutput("rideRequestsOverTime"),
              plotlyOutput("passengerCountDistribution"),
              plotlyOutput("pooledRides"),
              leafletOutput("map", height = 600) # Karte für Standorte der Fahrten
           
          )
        )      
    ),
    tabPanel(
      "O2: Kundenzufriedenheit",
      fluidRow(
        column(12,
               plotlyOutput("customerRating"),
               plotlyOutput("ratingFrequencies")
        )
      )
    ),
    tabPanel(
      "O3: Erreichung Zielgruppen",
      fluidRow(
        column(12,
               
        )
      )
    ),
    tabPanel(
      "O4: Service Level",
      fluidRow(
        column(12,
          plotlyOutput("vehiclesOverTime"),
          textOutput("vehicleStatsPerDay"),
          
          plotlyOutput("rideRequestsRelativeOverTime"),
          plotlyOutput("rideRequestsOverTime"),
          
          plotlyOutput("einstieg_diff_geplant"),
          plotlyOutput("einstieg_diff_angefragt")
        )
      )
    ),
    tabPanel(
      "Extras",
      fluidRow(
        column(12,
               plotlyOutput("distances_walking"),
               plotlyOutput("travelStats"),
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
    req(input$dateRange,
        #input$statusFilter, 
        input$anbieterFilter,
        input$testbookingFilter)
    data %>%
      filter(time >= input$dateRange[1] & time <= input$dateRange[2],
             #Status.der.Fahrtanfrage %in% input$statusFilter,
             Status.der.Fahrtanfrage == "Completed",
             Anbietername %in% input$anbieterFilter,
             isTestBooking %in% input$testbookingFilter)
  })
  
  # Definieren Sie die Reihenfolge der Faktoren
  status_order <- c("Completed","No Show", "Cancel", "Invalid","Unaccepted Proposal", "Seat Unavailable")  
  
  filtered_fahrerschichten <- reactive({
    req(input$dateRange)
    data_fahrerschichten %>%
      filter(Start >= input$dateRange[1] & Ende <= input$dateRange[2])
  })
  
  
  filtered_requests <- reactive({
    req(input$dateRange,
        #input$statusFilter, 
        input$anbieterFilter, input$testbookingFilter)
    data %>%
      filter(time >= input$dateRange[1] & time <= input$dateRange[2],
             Anbietername %in% input$anbieterFilter,
             isTestBooking %in% input$testbookingFilter) %>% 
      mutate(Status.der.Fahrtanfrage = factor(Status.der.Fahrtanfrage, levels = status_order))
  })
  
  timeDifferenceData <- reactive({
    filtered_data() %>%
      mutate(einstieg_diff_angefragt = as.numeric(difftime(Tatsächliche.Einstiegszeit, Angefragte.Einstiegszeit, units = "mins")),
             einstieg_diff_geplant = as.numeric(difftime(Ursprünglich.geplante.Einstiegszeit, Angefragte.Einstiegszeit, units = "mins")),)
  })
  
  distances_einstieg_data <- reactive({
    filtered_data() %>% 
      filter(!is.na(Laufdistanz..Einstieg.)) %>%
      group_by(date) %>%
      summarise(mean_distance_einstieg = mean(Laufdistanz..Einstieg.))
  })
  
  distances_ausstieg_data <- reactive({
    filtered_data() %>% 
      filter(!is.na(Laufdistanz..Ausstieg.)) %>%
      group_by(date) %>%
      summarise(mean_distance_ausstieg = mean(Laufdistanz..Ausstieg.))
  })
  
  traveled_distances_data <- reactive({
    filtered_data() %>% 
      filter(!is.na(Fahrtdistanz)) %>%
      group_by(date) %>%
      summarise(mean_traveled_distance = mean(Fahrtdistanz))
  })
  
  traveled_time_data <- reactive({
    filtered_data() %>% 
      filter(!is.na(Fahrtdauer)) %>%
      group_by(date) %>%
      summarise(mean_traveled_time = mean(Fahrtdauer))
  })
  
  grouped_data <- reactive({
    filtered_data() %>%
      group_by(date, `Status.der.Fahrtanfrage`) %>%
      summarise(Fahrtanfragen = n(),
                TotalPassengers = sum(`Anzahl.der.Fahrgäste`)) %>% 
      ungroup
  })
  
  passengerCount <- reactive({
    filtered_data() %>%
      group_by(date, Anzahl.der.Fahrgäste) %>%
      summarise(Frequency = n()) %>% 
      ungroup
  })

  avg_passengerCount <- reactive({
    filtered_data() %>%
      group_by(date) %>%
      summarise(avg = mean(Anzahl.der.Fahrgäste), Anzahl.der.Fahrgäste = "avg") %>% 
      ungroup
  })

  #avg_passengerCount <- passengerCount %>%
  #  group_by(date) %>%
  #  summarise(avg = mean(Anzahl.der.Fahrgäste), Anzahl.der.Fahrgäste = "avg")

  # Grouped data by week with total passengers
  grouped_data_weekly <- reactive({
    filtered_data() %>%
      mutate(week = lubridate::week(time)) %>%  # Extract week number
      group_by(week) %>%
      summarise(Fahrtanfragen = n(),
                TotalPassengers = sum(`Anzahl.der.Fahrgäste`))
  })

  dailyServiceHours <- reactive({
    # 9am - 4 pm
    totalServiceHoursPerDay = 7
    dailyServiceHours <- filtered_fahrerschichten() %>%
        group_by(Tag) %>% 
        summarise(Summe_Schichtdauer_h = sum(Dauer_h),
                  Mittl_Fahrzeugverfuegbarkeit_h = Summe_Schichtdauer_h / totalServiceHoursPerDay)
      
    # Calculate the 7-day moving average
    dailyServiceHours <- dailyServiceHours %>%
      mutate(Moving_Average_h = rollmean(Mittl_Fahrzeugverfuegbarkeit_h, k = 7, align = "right", fill = NA))
      
  })
  
  
  # Berechnung der durchschnittlichen Anzahl Fahrzeuge pro Tag
  output$vehicleStatsPerDay <- renderText({
    avg_vehicles <- mean(dailyServiceHours()$Mittl_Fahrzeugverfuegbarkeit_h)
    min_vehicles <- min(dailyServiceHours()$Mittl_Fahrzeugverfuegbarkeit_h)
    max_vehicles <- max(dailyServiceHours()$Mittl_Fahrzeugverfuegbarkeit_h)
    avg_mov_avg <- mean(dailyServiceHours()$Moving_Average_h, na.rm = TRUE)
    paste("Anzahl aktiver Fahrerschichten pro Tag: ", round(avg_vehicles, 2),
          "Minimum: ", round(min_vehicles,2),
          "Maximum: ", round(max_vehicles,2),
          "Mittelwert des gleitenden 7-Tages-Mittelwertes: ", round(avg_mov_avg, 2))
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
                 gradient = heat.colors(10)) %>%   
      addMarkers(lng = filtered_data()$Start.Längengrad, lat = filtered_data()$Start.Breitengrad,
                 popup = paste("Startadresse:", filtered_data()$Startadresse))
  })

  output$rideRequestsOverTime <- renderPlotly({
    gg <- ggplot(filtered_requests(), aes(x = date, fill = `Status.der.Fahrtanfrage`)) +
      #geom_area(stat = "count") +
      geom_bar() +
      labs(title = "Anzahl der Fahrtanfragen pro Tag",
           subtitle="für obige Filterauswahl",
           x = "Datum",
           y = "Anzahl der Fahrtanfragen") +
      theme_minimal() + 
      theme(legend.position = "top", legend.justification = "center",
            plot.title = element_text(size = 14, face = "bold", hjust = 0.5), 
            plot.subtitle = element_text(size = 16, hjust = 0.5)
            ) +
      scale_x_date(date_breaks = "1 week", date_labels = "%d.%m.")
    
    ggplotly(gg)
  })
  

  
  # Plot für ride requests und total passengers pro Woche
  output$rideRequestsRelativeOverTime <- renderPlotly({
    gg <- ggplot(filtered_requests(), aes(x = date, fill = `Status.der.Fahrtanfrage`)) +
      geom_bar(position = "fill") +  # Gestapelte Balken mit normierten Werten
      labs(title = "Anteil der Fahrtanfragen pro Tag nach Status",
           subtitle = "für obige Filterauswahl",
           x = "Datum",
           y = "Anteil") +
      theme_minimal() + 
      theme(legend.position = "top", legend.justification = "center",
            plot.title = element_text(size = 14, face = "bold", hjust = 0.5), 
            plot.subtitle = element_text(size = 16, hjust = 0.5)) + 
      scale_x_date(date_breaks = "1 week", date_labels = "%d.%m.") +
      geom_hline(yintercept = 0.05, linetype = "dashed", color = "red", alpha = 0.45) # Referenzlinie hinzufügen
    
    
    ggplotly(gg)
  })
  
  # Plot für ride requests und total passengers pro Woche
  output$pooledRides <- renderPlotly({
    gg <- ggplot(filtered_data(), aes(x = date, fill = `Geteilte.Fahrt`)) +
      geom_bar() +
      labs(title = "Anzahl geteilter Fahrten",
           subtitle = "für obige Filterauswahl",
           x = "Datum",
           y = "Anteil") +
      theme_minimal() + 
      theme(legend.position = "top", legend.justification = "center",
            plot.title = element_text(size = 14, face = "bold", hjust = 0.5), 
            plot.subtitle = element_text(size = 16, hjust = 0.5)) + 
      scale_x_date(date_breaks = "1 week", date_labels = "%d.%m.")
    
    
    ggplotly(gg)
  })
  
  'Passagiere am Tag'
  output$totalPassengersOverTime <- renderPlotly({
   #gg <- ggplot(grouped_data(), aes(x = date, y = TotalPassengers, fill = `Status.der.Fahrtanfrage`)) +
  #  geom_bar(stat = "identity") +  # Stacked Bar mit TotalPassengers
  #  labs(title = "Anzahl der Fahrgäste pro Tag",
  #       subtitle = "für obige Filterauswahl",
  #       x = "Datum",
  #       y = "Anzahl") +
  #  theme_minimal() +
  #  scale_fill_manual(values = c("Fahrtanfragen" = "red", "Completed" = "blue")) +  # Legende anpassen
  #  theme(legend.position = "right", legend.justification = "top",
  #        plot.title = element_text(size = 14, face = "bold", hjust = 0.5),
  #        plot.subtitle = element_text(size = 16, hjust = 0.5)) + 
  #   scale_x_date(date_breaks = "1 week", date_labels = "%d.%m.")
   

   gg <- ggplot(grouped_data(), aes(x = date, y = TotalPassengers, fill = `Status.der.Fahrtanfrage`)) +
     geom_bar(stat = "identity") +  # Stacked Bar mit TotalPassengers
     labs(title = "Anzahl der Fahrgäste pro Tag",
          subtitle = "für obige Filterauswahl",
          x = "Datum",
          y = "Anzahl") +
     theme_minimal() +
     scale_fill_manual(values = c("Fahrtanfragen" = "red", "Completed" = "blue")) +  # Legende anpassen
     theme(legend.position = "right", legend.justification = "top",
           plot.title = element_text(size = 14, face = "bold", hjust = 0.5),
           plot.subtitle = element_text(size = 16, hjust = 0.5)) + 
     scale_x_date(date_breaks = "1 week", date_labels = "%d.%m.")

    ggplotly(gg)
  })


  # Plot für ride requests und total passengers pro Woche
  output$passengersWeekly <- renderPlotly({
    gg <- ggplot(grouped_data_weekly(), aes(x = factor(week))) +
      geom_bar(aes(y = TotalPassengers, fill = "Fahrgäste"), stat = "identity", position = "dodge", width = 0.9) +
      labs(title = "Anzahl der Fahrgäste pro Woche",
           x = "Woche",
           y = "Anzahl") +
      scale_fill_manual(values = c("Fahrgäste" = "green"),
                        name = "Legende",
                        labels = c( "Fahrgäste")) +
      theme_minimal() +
      theme(legend.position = "top", legend.justification = "center",
            plot.title = element_text(size = 14, face = "bold", hjust = 0.5))

    ggplotly(gg)
  })



  output$passengerCountDistribution <- renderPlotly({
    gg <- ggplot(passengerCount(), aes(x = as.factor(date), y = Frequency, fill = as.factor( Anzahl.der.Fahrgäste ))) +
      geom_bar(stat = "identity") +
      geom_line(data = avg_passengerCount(), aes(x = as.factor(date), y = avg, group = 1), color = "black", size = 1.5) + # Linie für den Durchschnitt hinzufügen
      labs(title = "Verteilung der Gruppengröße",
           x = "Datum",
           y = "Häufigkeit",
           fill = "Anzahl der Fahrgäste") +
      theme_minimal() +
      theme(axis.text.x = element_text(angle = 45, hjust = 1),
            plot.title = element_text(size = 14, face = "bold", hjust = 0.5),
            legend.position = "bottom"
      )
    
    ggplotly(gg)
  })
  

  # Beispiel: Line Plot für durchschnittliche Distanzen pro Tag
  output$distances_walking <- renderPlotly({
    gg <- ggplot() +
      geom_line(data = distances_einstieg_data(), aes(x = date, y = mean_distance_einstieg, color = "Einstieg"), linetype = "solid") +
      geom_line(data = distances_ausstieg_data(), aes(x = date, y = mean_distance_ausstieg, color = "Ausstieg"), linetype = "dashed") +
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
        x = traveled_distances_data()$date,
        y = traveled_distances_data()$mean_traveled_distance,
        name = "Durchschnittliche Distanz",
        type = "scatter",
        mode = "lines",
        yaxis = "y1"
      )
    
    # Linie für die Durchschnittliche Reisezeit
    fig <- fig %>% 
      add_trace(
        x = traveled_time_data()$date,
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
  ## Kundenzufriedenheit Reiter Plots
  ####################################################################
  
  
  # Berechnung des Durchschnitts von 'Fahrtbewertung..1.5.' exklusive 'NA' für jede Woche
  customerRatingAvg <- reactive({
    filtered_data() %>%
      mutate(Rating = Fahrtbewertung..1.5.) %>% 
      #filter(!is.na(Rating)) %>%
      group_by(week = lubridate::week(time)) %>%
      summarise(avg_rating = mean(Rating, na.rm = TRUE))
  })
  
  ratingsPerWeek <- reactive({
    filtered_data() %>%
      mutate(week = lubridate::week(time)) %>%
      group_by(week, `Fahrtbewertung..1.5.`) %>%
      summarise(frequency = n())
  })
  
  # Plot für die Kundenzufriedenheit
  output$customerRating <- renderPlotly({
  
    
    avg_plot <- ggplot(customerRatingAvg(), aes(x = week, y = avg_rating)) +
      geom_line(color = "green") +
      labs(title = "Durchschnittliche Fahrtbewertung pro Woche",
           x = "Woche",
           y = "Durchschnittliche Bewertung",
           color = "Bewertung") +
      theme_minimal() + 
      theme(legend.position = "top", legend.justification = "center",
            plot.title = element_text(size = 14, face = "bold", hjust = 0.5))
    
  #  subplot(ggplotly(frequencies), ggplotly(avg_plot), nrows = 2)
    ggplotly(avg_plot)
  })
  
  output$ratingFrequencies <- renderPlotly({
    frequencies <- ggplot(ratingsPerWeek(), aes(x = week, y = frequency, fill = factor(`Fahrtbewertung..1.5.`))) +
      geom_bar(stat = "identity") +
      labs(title = "Häufigkeit der Fahrtbewertungen pro Woche",
           x = "Woche",
           y = "Häufigkeit",
           fill = "Bewertung") +
      theme_minimal() + 
      theme(legend.position = "top", legend.justification = "center",
            plot.title = element_text(size = 14, face = "bold", hjust = 0.5))
    
    ggplotly(frequencies)
    
    
  })
  
  ####################################################################
  ## Service Level Reiter Plots
  ####################################################################
  # Line plot for 'Fahrzeug ID' per 'date'
  output$vehiclesOverTime <- renderPlotly({
    dailyServiceHours <- dailyServiceHours()
    # Berechne die Werte für maximalen, minimalen und Median aller Werte von Moving_Average_h
    max_val <- max(dailyServiceHours$Moving_Average_h, na.rm = TRUE)
    min_val <- min(dailyServiceHours$Moving_Average_h, na.rm = TRUE)
    median_val <- median(dailyServiceHours$Moving_Average_h, na.rm = TRUE)
    
    #Finde den Index des maximalen, minimalen und Medianwerts des Moving_Average_h
    max_val <- max(dailyServiceHours$Moving_Average_h, na.rm = TRUE)
    max_date <- dailyServiceHours$Tag[which.max(dailyServiceHours$Moving_Average_h)]
    min_val <- min(dailyServiceHours$Moving_Average_h, na.rm = TRUE)
    min_date <- dailyServiceHours$Tag[which.min(dailyServiceHours$Moving_Average_h)]
    median_val <- median(dailyServiceHours$Moving_Average_h, na.rm = TRUE)
    median_date <- dailyServiceHours$Tag[which(dailyServiceHours$Moving_Average_h == median_val)]
    
    # Erstellen des ggplot
    p <- ggplot(dailyServiceHours, aes(x = Tag)) +
      geom_line(aes(y = Mittl_Fahrzeugverfuegbarkeit_h, color = "Mittlere Fahrzeugverfuegbarkeit pro Tag"), size = 1) +
      geom_line(aes(y = Moving_Average_h, color = "7-Tages-Schnitt gleitend"), size = 1, linetype = "dashed") +
      geom_text(data = tibble(x = max_date, y = max_val, label = paste("Max:", round(max_val, 2))),
                aes(x = x, y = y, label = label), vjust = -0.5, hjust = 0) +
      geom_text(data = tibble(x = min_date, y = min_val, label = paste("Min:", round(min_val, 2))),
                aes(x = x, y = y, label = label), vjust = 1.5, hjust = 0) +
      geom_text(data = tibble(x = median_date, y = median_val, label = paste("Median:", round(median_val, 2))),
                aes(x = x, y = y, label = label), vjust = -0.5, hjust = 0) +
      #geom_label(data = tibble(x = max_date, y = max_val, label = paste("Max:", round(max_val, 2))),
      #           aes(x = x, y = y, label = label), vjust = -0.5, hjust = 0, fill = "grey90", color="red") +
      #geom_label(data = tibble(x = min_date, y = min_val, label = paste("Min:", round(min_val, 2))),
      #           aes(x = x, y = y, label = label), vjust = 1.5, hjust = 0, fill = "grey90", color="red") +
      #geom_label(data = tibble(x = median_date, y = median_val, label = paste("Median:", round(median_val, 2))),
      #           aes(x = x, y = y, label = label), vjust = -0.5, hjust = 0, fill = "grey90", color="red") +
      labs(x = "Datum", y = "Fahrzeuge", title = "Mittlere Anzahl verfügbarer Fahrzeuge pro Tag") +
      scale_color_manual(values = c("Mittlere Fahrzeugverfuegbarkeit pro Tag" = "black", 
                                    "7-Tages-Schnitt gleitend" = "red")) +
      theme_minimal()
    
    #p <- plotly::ggplotly(p) %>%
    #  plotly::add_trace(x = max_date, y = max_val, text = paste("Max:", round(max_val, 2)),
    #                    type = "scatter", mode = "text", textfont = list(color = "red"),
    #                    hoverinfo = "none", showlegend = FALSE) %>%
    #  plotly::add_trace(x = min_date, y = min_val, text = paste("Min:", round(min_val, 2)),
    #                    type = "scatter", mode = "text", textfont = list(color = "red"),
    #                    hoverinfo = "none", showlegend = FALSE) %>%
    #  plotly::add_trace(x = median_date, y = median_val, text = paste("Median:", round(median_val, 2)),
    #                    type = "scatter", mode = "text", textfont = list(color = "red"),
    #                    hoverinfo = "none", showlegend = FALSE)
    
    return(p)
    
  })
  
  # Boxplot für die Differenz zwischen 'Angefragte.Einstiegszeit' und 'Tatsächliche.Einstiegszeit'
  output$einstieg_diff_angefragt <- renderPlotly({
    gg <- ggplot(timeDifferenceData(), aes(x = as.factor(date), y = einstieg_diff_angefragt)) +
      geom_boxplot() +
      stat_summary(fun.y = "mean", geom = "point", shape = 18, size = 1, color = "blue",
                   aes(label = round(..y.., 2))) + # Runde auf zwei Dezimalstellen
      labs(title = "Boxplot der Differenz zwischen Angefragte und Tatsächliche Einstiegszeit",
           subtitle = "Positiv = Verspätung, Negativ = früher",
           x = "Datum",
           y = "Zeitdifferenz (Minuten)") +
      theme_minimal() +
      theme(axis.text.x = element_text(angle = 45, hjust = 1), # Rotate x-axis labels for better visibility
            plot.title = element_text(size = 14, face = "bold", hjust = 0.5), 
            plot.subtitle = element_text(size = 16)
            ) + 
      geom_hline(yintercept = 20, linetype = "dashed", color = "red", alpha = 0.35) # Referenzlinie hinzufügen
    
    ggplotly(gg)
  })
  
  # Boxplot für die Differenz zwischen 'Angefragte.Einstiegszeit' und 'Tatsächliche.Einstiegszeit'
  output$einstieg_diff_geplant <- renderPlotly({
    gg <- ggplot(timeDifferenceData(), aes(x = as.factor(date), y = einstieg_diff_geplant)) +
      geom_boxplot() +
      stat_summary(fun.y = "mean", geom = "point", shape = 18, size = 1, color = "blue",
                   aes(label = round(..y.., 2))) + # Runde auf zwei Dezimalstellen
      labs(title = "Boxplot der Differenz zwischen ursprünglich geplanter und tatsächlicher Einstiegszeit",
           subtitle = "Positiv = Verspätung, Negativ = früher",
           x = "Datum",
           y = "Zeitdifferenz (Minuten)") +
      theme_minimal() +
      theme(axis.text.x = element_text(angle = 45, hjust = 1),   # Rotate x-axis labels for better visibility
            plot.title = element_text(size = 14, face = "bold", hjust = 0.5), 
            plot.subtitle = element_text(size = 16)
            ) + 
      geom_hline(yintercept = 10, linetype = "dashed", color = "red", alpha = 0.35) # Referenzlinie hinzufügen
    
    ggplotly(gg)
  })
  
  #Speed
  output$speed <- renderPlotly({
    # Merge der Dataframes basierend auf dem date
    merged_data <- merge(traveled_distances_data(), traveled_time_data(), by = "date", all = TRUE)
    
    # Berechnung des Quotienten
    merged_data$quotient <- 60 * merged_data$mean_traveled_distance / merged_data$mean_traveled_time
    
    # Plot mit dem Quotienten
    fig <- plot_ly(
      x = merged_data$date,
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
