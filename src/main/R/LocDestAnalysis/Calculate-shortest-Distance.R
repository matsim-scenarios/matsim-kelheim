library(geosphere)
list1 <- data.frame(longitude = c(80.15998, 72.89125, 77.65032, 77.60599,
                                  72.88120, 76.65460, 72.88232, 77.49186,
                                  72.82228, 72.88871),
                    latitude = c(12.90524, 19.08120, 12.97238, 12.90927,
                                 19.08225, 12.81447, 19.08241, 13.00984,
                                 18.99347, 19.07990))
list2 <- data.frame(longitude = c(72.89537, 77.65094, 73.95325, 72.96746,
                                  77.65058, 77.66715, 77.64214, 77.58415,
                                  77.76180, 76.65460),
                    latitude = c(19.07726, 13.03902, 18.50330, 19.16764,
                                 12.90871, 13.01693, 13.00954, 12.92079,
                                 13.02212, 12.81447),
                    locality = c("A", "A", "B", "B", "C", "C", "C", "D", "D", "E"))
mat <- distm(list1[,c('longitude','latitude')], list2[,c('longitude','latitude')], fun=distVincentyEllipsoid)
print(mat)
list1$locality <- list2$locality[max.col(-mat)]
