package com.smarttaxi.fare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class FareTest {

    @Test
    void standardFareIsBasePlusDistance() {
        StandardFare fare = new StandardFare();
        assertEquals(5.0, fare.calculateFare(0, LocalTime.NOON), 1e-9);
        assertEquals(13.0, fare.calculateFare(4, LocalTime.NOON), 1e-9);
    }

    @Test
    void costGrowsWithSteps() {
        assertEquals(5.0, new StandardFare().calculateCost(10), 1e-9);
    }

    @Test
    void peakHourBoundaries() {
        assertTrue(PeakHourFare.isPeak(LocalTime.of(8, 0)));
        assertTrue(PeakHourFare.isPeak(LocalTime.of(9, 59)));
        assertFalse(PeakHourFare.isPeak(LocalTime.of(10, 0)));
        assertTrue(PeakHourFare.isPeak(LocalTime.of(17, 30)));
        assertFalse(PeakHourFare.isPeak(LocalTime.of(20, 0)));
        assertFalse(PeakHourFare.isPeak(LocalTime.of(3, 0)));
    }

    @Test
    void peakFareIsOnePointFiveTimesStandard() {
        PeakHourFare peak = new PeakHourFare();
        assertEquals(19.5, peak.calculateFare(4, LocalTime.of(8, 30)), 1e-9);
        assertEquals(13.0, peak.calculateFare(4, LocalTime.of(12, 0)), 1e-9);
        assertEquals(5.0, peak.calculateCost(10), 1e-9);
    }

    @Test
    void profitIsFareMinusCost() {
        StandardFare fare = new StandardFare();
        double profit = fare.calculateFare(4, LocalTime.NOON) - fare.calculateCost(10);
        assertEquals(8.0, profit, 1e-9);
    }
}
