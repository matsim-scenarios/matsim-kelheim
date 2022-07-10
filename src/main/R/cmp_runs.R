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


d <- "\\\\sshfs.kr\\rakow@cluster.math.tu-berlin.de\\net\\ils\\matsim-kelheim\\mode-choice\\output\\"

files <- list(
  forceInnovation="kh-mc_subTourModeChoice-no-tm-car",
  default="kh-mc_subTourModeChoice-no-tm-car-f-inv_0"
)

df <- read_runs(files, "modestats.txt", d) %>%
    filter(name != "freight")

df_diff <- df %>% 
  group_by(Iteration, name) %>%
  summarise(value=diff(value))



ggplot(df, aes(Iteration, value, color=name)) +
  labs(title = "ChangeSingleTrip", subtitle = "ForceInnovation=10") +
  geom_line(size=1.2) +
  facet_grid(cols=vars(file)) +
  theme_classic(base_size = 12) +
  xlim(0, 1500) +
  ylim(0, 0.85) +
  scale_color_startrek() +
  ylab("Share")


ggsave("file.png", dpi = 300, width = 10, height = 4)

ggplot(df_diff, aes(Iteration, value, color=name)) +
  labs(title = "ChangeSingleTrip Differences FromWalk/FromCar", subtitle = "ForceInnovation=10") +
  geom_line(size=1.2) +
  theme_classic(base_size = 12) +
  xlim(0, 1500) +
  ylim(-0.4, 0.4) +
  scale_color_startrek() +
  ylab("Share")


ggsave("file_diff.png", dpi = 300, width = 10, height = 4)
