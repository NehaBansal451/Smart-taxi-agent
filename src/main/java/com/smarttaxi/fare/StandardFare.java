package com.smarttaxi.fare;

import java.time.LocalTime;

/** Base fare plus a fixed price per cell of distance; cost is a fixed price per step driven. */
public class StandardFare implements FareCalculator {

    private final double baseFare;
    private final double perCell;
    private final double costPerStep;

    public StandardFare() {
        this(5.0, 2.0, 0.5);
    }

    public StandardFare(double baseFare, double perCell, double costPerStep) {
        this.baseFare = baseFare;
        this.perCell = perCell;
        this.costPerStep = costPerStep;
    }

    @Override
    public double calculateFare(int distance, LocalTime pickupTime) {
        return baseFare + perCell * distance;
    }

    @Override
    public double calculateCost(int steps) {
        return costPerStep * steps;
    }
}
