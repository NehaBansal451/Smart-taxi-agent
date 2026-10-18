package com.smarttaxi.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ModelTest {

    @Test
    void movesChangeRowAndColumn() {
        Position p = new Position(2, 2);
        assertEquals(new Position(3, 2), p.move(Action.SOUTH));
        assertEquals(new Position(1, 2), p.move(Action.NORTH));
        assertEquals(new Position(2, 3), p.move(Action.EAST));
        assertEquals(new Position(2, 1), p.move(Action.WEST));
    }

    @Test
    void pickupAndDropoffDoNotMove() {
        Position p = new Position(2, 2);
        assertEquals(p, p.move(Action.PICKUP));
        assertEquals(p, p.move(Action.DROPOFF));
    }

    @Test
    void equalStatesWorkAsMapKeys() {
        State a = new State(new Position(0, 0), Optional.of(new Position(1, 1)), new Position(4, 4));
        State b = new State(new Position(0, 0), Optional.of(new Position(1, 1)), new Position(4, 4));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        Map<State, Integer> map = new HashMap<>();
        map.put(a, 1);
        assertEquals(Integer.valueOf(1), map.get(b));
    }

    @Test
    void passengerInsideTaxiIsDifferentFromWaiting() {
        Position taxi = new Position(0, 0);
        Position destination = new Position(4, 4);
        State waiting = new State(taxi, Optional.of(new Position(1, 1)), destination);
        State inside = new State(taxi, Optional.empty(), destination);
        assertNotEquals(waiting, inside);
        assertTrue(inside.isPassengerInTaxi());
        assertFalse(waiting.isPassengerInTaxi());
        assertTrue(inside.getPassenger().isEmpty());
    }
}
