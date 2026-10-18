package com.smarttaxi.fare;

import java.time.LocalTime;

/** Strategy for pricing a trip. */
public interface FareCalculator {

    /** Fare for a delivered trip; distance is the Manhattan distance from pickup to destination. */
    double calculateFare(int distance, LocalTime pickupTime);

    /** Fuel and time cost of driving the given number of steps. */
    double calculateCost(int steps);
}
