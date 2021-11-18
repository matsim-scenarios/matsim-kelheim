library(tidyverse)

population_attributes <- read.csv(file = '/Users/luchengqi/Documents/MATSimScenarios/Kelheim/population-analysis/persons-attributes.csv')

ggplot(data = population_attributes) + 
  geom_histogram(mapping = aes(x= estimated_personal_allowance), binwidth = 500)

population_attributes <- population_attributes %>%
  mutate(age_group = cut(age, 
                         breaks = c(0, 18, 25, 35, 45, 55, 65, 75, Inf), 
                         labels = c("0-18", "18-25", "25-35", "35-45", "45-55","55-65", "65-75","75+")))

population_attributes <- population_attributes %>%
  mutate(household_total_income = household_size * estimated_personal_allowance)

ggplot(data=population_attributes, mapping = aes(x = age_group, y = household_total_income)) + 
  geom_boxplot() +
  ggtitle("Househod total income distribution per age groups") +
  theme(plot.title = element_text(hjust = 0.5))

ggplot(data=population_attributes, mapping = aes(x = age_group, y = estimated_personal_allowance)) + 
  geom_boxplot() + 
  ggtitle("Personal allowance distribution per age groups")+
  theme(plot.title = element_text(hjust = 0.5))

ggplot(data=population_attributes, mapping = aes(x = age_group, y = household_size)) + 
  geom_boxplot() + 
  ggtitle("Household size distribution per age groups")+
  theme(plot.title = element_text(hjust = 0.5))