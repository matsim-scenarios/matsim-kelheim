library(gridExtra)
library(tidyverse)
library(lubridate)
library(viridis)
library(ggsci)
library(sf)


# setwd("C:/Users/chris/Development/matsim-scenarios/matsim-kelheim/src/main/R")

read_runs <- function(files, name, base_dir="") {

  bar <- imap(files, function(elem, entry) {
    print(paste("Processing", entry))
    
    f <- paste(base_dir, elem, sep="")
    
    df <- read_delim(list.files(f, paste("*.\\.", name, sep=""), full.names = T, include.dirs = F), delim = "\t", trim_ws = T) %>%
        pivot_longer(!Iteration)
    
  })
  
  bind_rows(bar, .id = "file")
}


d <- "\\\\sshfs.kr\\rakow@cluster.math.tu-berlin.de\\net\\ils\\schlenther\\"

files <- list(
  fromCar="hamburg-v3.0\\output\\output-hh-base-10pct-c",
  fromWalk="hamburg-v3.0\\output\\output-hh-base-10pct-w"
)

df <- read_runs(files, "modestats.txt", d) %>%
    filter(name != "freight") %>%
    filter(!str_starts(name, "commercial"))

df_diff <- df %>% 
  group_by(Iteration, name) %>%
  summarise(value=diff(value))



ggplot(df, aes(Iteration, value, color=name)) +
  labs(title = "ChangeSingleTrip", subtitle = "Hamburg") +
  geom_line(size=1.2) +
  facet_grid(cols=vars(file)) +
  theme_classic(base_size = 12) +
  xlim(0, 500) +
  ylim(0, 0.85) +
  scale_color_startrek() +
  ylab("Share")


ggsave("file.png", dpi = 300, width = 10, height = 4)

ggplot(df_diff, aes(Iteration, value, color=name)) +
  labs(title = "Differences FromWalk/FromCar", subtitle = "ForceInnovation=10") +
  geom_line(size=1.2) +
  theme_classic(base_size = 12) +
  xlim(0, 500) +
  ylim(-0.4, 0.4) +
  scale_color_startrek() +
  ylab("Share")


ggsave("file_diff.png", dpi = 300, width = 10, height = 4)


pbinom(3, size = 13, prob = 1 / 6)
plot(0:10, pbinom(0:10, size = 10, prob = 1 / 6), type = "l")
