package com.smarttaxi.env;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

import com.smarttaxi.exception.InvalidMoveException;
import com.smarttaxi.map.CityMap;
import com.smarttaxi.model.Action;
import com.smarttaxi.model.Position;
import com.smarttaxi.model.State;

class GridWorldTest {

    private final CityMap map = CityMap.defaultMap();

    private GridWorld worldAt(Position taxi, Optional<Position> passenger, Position destination) {
        GridWorld w = new GridWorld(map, new Random(1));
        w.reset(new State(taxi, passenger, destination));
        return w;
    }

    @Test
    void bumpingIntoEdgeKeepsPositionAndCostsOne() throws Exception {
        GridWorld w = worldAt(new Position(0, 0), Optional.of(new Position(0, 4)), new Position(4, 4));
        StepResult r = w.step(Action.NORTH);
        assertEquals(new Position(0, 0), r.getNextState().getTaxi());
        assertEquals(-1.0, r.getReward());
    }

    @Test
    void bumpingIntoWallKeepsPosition() throws Exception {
        GridWorld w = worldAt(new Position(2, 1), Optional.of(new Position(0, 4)), new Position(4, 4));
        StepResult r = w.step(Action.EAST);
        assertEquals(new Position(2, 1), r.getNextState().getTaxi());
    }

    @Test
    void normalMoveChangesPosition() throws Exception {
        GridWorld w = worldAt(new Position(0, 0), Optional.of(new Position(0, 4)), new Position(4, 4));
        assertEquals(new Position(1, 0), w.step(Action.SOUTH).getNextState().getTaxi());
    }

    @Test
    void pickupAwayFromPassengerIsPenalised() throws Exception {
        GridWorld w = worldAt(new Position(0, 0), Optional.of(new Position(0, 4)), new Position(4, 4));
        assertEquals(-10.0, w.step(Action.PICKUP).getReward());
    }

    @Test
    void correctPickupPutsPassengerInTaxi() throws Exception {
        GridWorld w = worldAt(new Position(0, 4), Optional.of(new Position(0, 4)), new Position(4, 4));
        StepResult r = w.step(Action.PICKUP);
        assertEquals(-1.0, r.getReward());
        assertTrue(r.getNextState().isPassengerInTaxi());
    }

    @Test
    void dropoffWithoutPassengerIsPenalised() throws Exception {
        GridWorld w = worldAt(new Position(4, 4), Optional.of(new Position(0, 4)), new Position(4, 4));
        assertEquals(-10.0, w.step(Action.DROPOFF).getReward());
        assertFalse(w.isDone());
    }

    @Test
    void correctDropoffEndsEpisodeWithSuccess() throws Exception {
        GridWorld w = worldAt(new Position(4, 4), Optional.empty(), new Position(4, 4));
        StepResult r = w.step(Action.DROPOFF);
        assertEquals(20.0, r.getReward());
        assertTrue(r.isDone());
        assertTrue(r.isSuccess());
    }

    @Test
    void stepAfterDoneThrows() throws Exception {
        GridWorld w = worldAt(new Position(4, 4), Optional.empty(), new Position(4, 4));
        w.step(Action.DROPOFF);
        assertThrows(InvalidMoveException.class, () -> w.step(Action.NORTH));
    }

    @Test
    void nullActionThrows() {
        GridWorld w = worldAt(new Position(0, 0), Optional.of(new Position(0, 4)), new Position(4, 4));
        assertThrows(InvalidMoveException.class, () -> w.step(null));
    }

    @Test
    void episodeEndsAtStepLimitWithoutSuccess() throws Exception {
        GridWorld w = new GridWorld(map, new Random(1), 3);
        w.reset(new State(new Position(0, 0), Optional.of(new Position(0, 4)), new Position(4, 4)));
        w.step(Action.NORTH);
        w.step(Action.NORTH);
        StepResult r = w.step(Action.NORTH);
        assertTrue(r.isDone());
        assertFalse(r.isSuccess());
    }

    @Test
    void resetPicksDifferentStopsAndFreeCell() {
        GridWorld w = new GridWorld(map, new Random(7));
        for (int i = 0; i < 50; i++) {
            State s = w.reset();
            assertTrue(map.getStops().contains(s.getPassenger().get()));
            assertTrue(map.getStops().contains(s.getDestination()));
            assertNotEquals(s.getPassenger().get(), s.getDestination());
            assertFalse(map.isWall(s.getTaxi()));
        }
    }

    @Test
    void sameSeedGivesSameStart() {
        State a = new GridWorld(map, new Random(42)).reset();
        State b = new GridWorld(map, new Random(42)).reset();
        assertEquals(a, b);
    }
}
