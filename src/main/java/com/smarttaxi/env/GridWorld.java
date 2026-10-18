package com.smarttaxi.env;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import com.smarttaxi.exception.InvalidMoveException;
import com.smarttaxi.map.CityMap;
import com.smarttaxi.model.Action;
import com.smarttaxi.model.Position;
import com.smarttaxi.model.State;

/**
 * The taxi world. The taxi starts on a random free cell, a passenger waits at one stop and wants to go
 * to a different stop. Walls and the map edge block movement.
 */
public class GridWorld implements Environment {

    public static final double STEP_REWARD = -1.0;
    public static final double ILLEGAL_REWARD = -10.0;
    public static final double DROPOFF_REWARD = 20.0;
    public static final int DEFAULT_MAX_STEPS = 200;

    private final CityMap map;
    private final Random random;
    private final int maxSteps;

    private Position taxi;
    private Position passenger; // null while the passenger is inside the taxi
    private Position destination;
    private int stepCount;
    private boolean done = true; // nothing to step until reset() is called

    public GridWorld(CityMap map, Random random, int maxSteps) {
        if (map.getStops().size() < 2) {
            throw new IllegalArgumentException("A map needs at least 2 stops");
        }
        if (maxSteps < 1) {
            throw new IllegalArgumentException("maxSteps must be at least 1");
        }
        this.map = map;
        this.random = random;
        this.maxSteps = maxSteps;
    }

    public GridWorld(CityMap map, Random random) {
        this(map, random, DEFAULT_MAX_STEPS);
    }

    @Override
    public State reset() {
        List<Position> stops = map.getStops();
        Position pickup = stops.get(random.nextInt(stops.size()));
        Position drop;
        do {
            drop = stops.get(random.nextInt(stops.size()));
        } while (drop.equals(pickup));

        List<Position> free = new ArrayList<>();
        for (int r = 0; r < map.getSize(); r++) {
            for (int c = 0; c < map.getSize(); c++) {
                Position p = new Position(r, c);
                if (!map.isWall(p)) {
                    free.add(p);
                }
            }
        }
        taxi = free.get(random.nextInt(free.size()));
        passenger = pickup;
        destination = drop;
        stepCount = 0;
        done = false;
        return getState();
    }

    @Override
    public State reset(State startState) {
        if (!map.isInside(startState.getTaxi()) || map.isWall(startState.getTaxi())) {
            throw new IllegalArgumentException("Taxi start must be on a free cell");
        }
        taxi = startState.getTaxi();
        passenger = startState.getPassenger().orElse(null);
        destination = startState.getDestination();
        stepCount = 0;
        done = false;
        return getState();
    }

    @Override
    public StepResult step(Action action) throws InvalidMoveException {
        if (action == null) {
            throw new InvalidMoveException("Action must not be null");
        }
        if (done) {
            throw new InvalidMoveException("The episode has ended; call reset() first");
        }
        stepCount++;
        double reward = STEP_REWARD;
        boolean success = false;

        switch (action) {
            case SOUTH, NORTH, EAST, WEST -> {
                Position next = taxi.move(action);
                if (map.isInside(next) && !map.isWall(next)) {
                    taxi = next;
                }
            }
            case PICKUP -> {
                if (passenger != null && passenger.equals(taxi)) {
                    passenger = null;
                } else {
                    reward = ILLEGAL_REWARD;
                }
            }
            case DROPOFF -> {
                if (passenger == null && taxi.equals(destination)) {
                    reward = DROPOFF_REWARD;
                    success = true;
                    done = true;
                } else {
                    reward = ILLEGAL_REWARD;
                }
            }
            default -> throw new InvalidMoveException("Unknown action " + action);
        }

        if (!done && stepCount >= maxSteps) {
            done = true;
        }
        return new StepResult(getState(), reward, done, success);
    }

    /** The current state (the same value reset and step return). */
    public State getState() {
        return new State(taxi, Optional.ofNullable(passenger), destination);
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public int getStepCount() {
        return stepCount;
    }

    @Override
    public CityMap getMap() {
        return map;
    }
}
