package com.smarttaxi.fare;

import java.time.LocalTime;

/** Multiplies the standard fare during the morning peak (08:00-10:00) and evening peak (17:00-20:00). */
public class PeakHourFare implements FareCalculator {

    private final FareCalculator standard;
    private final double multiplier;

    public PeakHourFare() {
        this(new StandardFare(), 1.5);
    }

    public PeakHourFare(FareCalculator standard, double multiplier) {
        this.standard = standard;
        this.multiplier = multiplier;
    }

    /** True from 08:00 up to (not including) 10:00 and from 17:00 up to (not including) 20:00. */
    public static boolean isPeak(LocalTime time) {
        int hour = time.getHour();
        return (hour >= 8 && hour < 10) || (hour >= 17 && hour < 20);
    }

    @Override
    public double calculateFare(int distance, LocalTime pickupTime) {
        double fare = standard.calculateFare(distance, pickupTime);
        return isPeak(pickupTime) ? fare * multiplier : fare;
    }

    @Override
    public double calculateCost(int steps) {
        return standard.calculateCost(steps);
    }
}
