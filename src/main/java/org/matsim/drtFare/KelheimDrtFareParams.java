package org.matsim.drtFare;

import org.matsim.core.config.ReflectiveConfigGroup;

import jakarta.validation.constraints.PositiveOrZero;
import java.util.Map;

public final class KelheimDrtFareParams extends ReflectiveConfigGroup {
    public static final String SET_NAME = "kelheimDrtFare";
    public static final String BASEFARE = "baseFare";
    public static final String ZONE_2_SURCHARGE = "zone2Surcharge";
    public static final String MINFARE_PER_TRIP = "minFarePerTrip";
    public static final String DAILY_FEE = "dailySubscriptionFee";
    public static final String TIMEFARE = "timeFare_h";
    public static final String DISTANCEFARE = "distanceFare_m";
    public static final String MODE = "mode";
    public static final String SHAPEFILE = "shapeFile";

    @PositiveOrZero
    private double baseFare;
    @PositiveOrZero
    private double zone2Surcharge;
    private String mode;

    @PositiveOrZero
    private double minFarePerTrip = 0.0;
    @PositiveOrZero
    private double dailySubscriptionFee = 0.0;
    @PositiveOrZero
    private double timeFare_h = 0.0;
    @PositiveOrZero
    private double distanceFare_m = 0.0;

    private String shapeFile;

    public KelheimDrtFareParams(double baseFare, double zone2Surcharge, String mode) {
        super(SET_NAME);
        this.baseFare = baseFare;
        this.zone2Surcharge = zone2Surcharge;
        this.mode = mode;
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(BASEFARE, "Basefare per trip: 2 EUR (For trips within zone 1 base fare will be charged)");
        map.put(ZONE_2_SURCHARGE, "Surcharge for trips traveling to/within zone 2: 1 EUR");
        map.put(MINFARE_PER_TRIP,
                "Minimum fare per trip (paid instead of the sum of base, time and distance fare if that sum would be lower than the minimum fare, positive or zero value).");
        map.put(DAILY_FEE, "Daily subscription fee (positive or zero value)");
        map.put(TIMEFARE, "drt fare per hour (positive or zero value)");
        map.put(DISTANCEFARE, "drt fare per meter (positive or zero value)");
        map.put(MODE, "transport mode for which the fare applies. Default: drt");
        map.put(SHAPEFILE, "shape file of the DRT fare zonal system");
        return map;
    }

    @StringGetter(BASEFARE)
    public double getBaseFare() {
        return baseFare;
    }

    @StringSetter(BASEFARE)
    public void setBaseFare(double baseFare) {
        this.baseFare = baseFare;
    }

    @StringGetter(ZONE_2_SURCHARGE)
    public double getZone2Surcharge() {
        return zone2Surcharge;
    }

    @StringSetter(ZONE_2_SURCHARGE)
    public void setZone2Surcharge(double zone2Surcharge) {
        this.zone2Surcharge = zone2Surcharge;
    }

    @StringGetter(MINFARE_PER_TRIP)
    public double getMinFarePerTrip() {
        return minFarePerTrip;
    }

    @StringSetter(MINFARE_PER_TRIP)
    public void setMinFarePerTrip(double minFarePerTrip) {
        this.minFarePerTrip = minFarePerTrip;
    }

    @StringGetter(DAILY_FEE)
    public double getDailySubscriptionFee() {
        return dailySubscriptionFee;
    }

    @StringSetter(DAILY_FEE)
    public void setDailySubscriptionFee(double dailySubscriptionFee) {
        this.dailySubscriptionFee = dailySubscriptionFee;
    }

    @StringGetter(TIMEFARE)
    public double getTimeFare_h() {
        return timeFare_h;
    }

    @StringSetter(TIMEFARE)
    public void setTimeFare_h(double timeFare_h) {
        this.timeFare_h = timeFare_h;
    }

    @StringGetter(DISTANCEFARE)
    public double getDistanceFare_m() {
        return distanceFare_m;
    }

    @StringSetter(DISTANCEFARE)
    public void setDistanceFare_m(double distanceFare_m) {
        this.distanceFare_m = distanceFare_m;
    }

    @StringGetter(SHAPEFILE)
    public String getShapeFile() {
        return shapeFile;
    }

    @StringSetter(SHAPEFILE)
    public void setShapeFile(String shapeFile) {
        this.shapeFile = shapeFile;
    }

    @StringGetter(MODE)
    public String getMode() {
        return mode;
    }

    @StringSetter(MODE)
    public void setMode(String mode) {
        this.mode = mode;
    }
}
