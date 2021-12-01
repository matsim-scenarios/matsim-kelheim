library(tidyverse)

population_attributes <- read.csv(file = 'C:/Users/Simon/Documents/KelRide/persons-attributes.tsv', sep = "\t")

population_attributes <- population_attributes %>%
  mutate(household_total_income = household_size * estimated_personal_allowance) %>%
  #one could actually think about splitting up 0-18 grup into groups of 0-6,7-10,11-13,14-17 like MID data does; SM 11-21
  mutate(age_group = cut(age,
                         breaks = c(0, 18, 29, 39, 49, 59, 64, 74, 79, Inf),
                         labels = c("0-18", "19-29", "30-39", "40-49", "50-59", "60-64", "65-74", "75-79","80+"))) %>%
  mutate(household_size_group = cut(household_size,
                                    breaks = c(0, 1, 2, 3, 4, Inf),
                                    labels = c("1", "2", "3", "4", "5 or more"))) 

ggplot(data = population_attributes) + 
  geom_histogram(mapping = aes(x= estimated_personal_allowance), binwidth = 100) +
  ggtitle("Person income (estimated allowance) distribution") +
  theme(plot.title = element_text(hjust = 0.5)) +
  xlab("Income (estimated allowance)")
  
ggplot(data=population_attributes, mapping = aes(x = age_group, y = estimated_personal_allowance)) + 
  geom_boxplot(outlier.shape = NA) + 
  ggtitle("Personal allowance distribution per age groups")+
  stat_summary(fun=mean, geom="point", shape=20, size=5, color="red", fill="red") +
  theme(plot.title = element_text(hjust = 0.5)) +
  xlab("Age group") + 
  ylab("Income (estimated allowance)")+
  ylim(0,4000)

ggplot(data=population_attributes, mapping = aes(x = sex, y = estimated_personal_allowance)) + 
  geom_boxplot(outlier.shape = NA) + 
  stat_summary(fun=mean, geom="point", shape=20, size=5, color="red", fill="red") +
  ggtitle("Personal allowance distribution per age sex")+
  theme(plot.title = element_text(hjust = 0.5))+
  ylab("Income (estimated allowance)")+
  ylim(0,4000)

ggplot(data=population_attributes, mapping = aes(x = age_group, y = household_total_income)) + 
  geom_boxplot(outlier.shape = NA) +
  stat_summary(fun=mean, geom="point", shape=20, size=5, color="red", fill="red") +
  ggtitle("Household total income distribution per age groups") +
  theme(plot.title = element_text(hjust = 0.5)) + 
  xlab("Age group") +
  ylab("Household income")

ggplot(data=population_attributes, mapping = aes(x = age_group, y = household_size)) + 
  geom_boxplot(outlier.shape = NA) + 
  stat_summary(fun=mean, geom="point", shape=20, size=5, color="red", fill="red") +
  ggtitle("Household size distribution per age groups")+
  theme(plot.title = element_text(hjust = 0.5))+
  xlab("Age group")+
  ylab("Household size")

ggplot(data=population_attributes, mapping = aes(x = age_group, y = number_of_trips_per_day)) + 
  geom_boxplot(outlier.shape = NA) +
  ggtitle("Number of trips per age groups")+
  theme(plot.title = element_text(hjust = 0.5))+
  stat_summary(fun=mean, geom="point", shape=20, size=5, color="red", fill="red") +
  xlab("Age group")+
  ylab("Number of trips per day")+
  ylim(0,10)

ggplot(data=population_attributes, mapping = aes(x = sex, y = number_of_trips_per_day)) + 
  geom_boxplot(outlier.shape = NA, varwidth=TRUE) +
  stat_summary(fun=mean, geom="point", shape=20, size=5, color="red", fill="red") +
  ggtitle("Number of trips per sex")+
  theme(plot.title = element_text(hjust = 0.5))+
  ylab("Number of trips per day")+
  ylim(0,10)

ggplot(population_attributes, mapping = aes(x = household_size_group, y = number_of_trips_per_day)) + 
  geom_boxplot(outlier.shape = NA) +
  ggtitle("Number of trips per houshold sizes")+
  stat_summary(fun=mean, geom="point", shape=20, size=5, color="red", fill="red") +
  theme(plot.title = element_text(hjust = 0.5))+
  ylab("Number of trips per day")+
  ylim(0,10)

