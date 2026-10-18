package com.smarttaxi.env;

import com.smarttaxi.exception.InvalidMoveException;
import com.smarttaxi.map.CityMap;
import com.smarttaxi.model.Action;
import com.smarttaxi.model.State;

/** A world an agent can act in. */
public interface Environment {

    /** Starts a new episode with a random pickup stop and a different random destination stop. */
    State reset();

    /** Starts a new episode from a given state (used by replay and tests). */
    State reset(State startState);

    /** Applies an action. Throws InvalidMoveException for a null action or a step after the episode ended. */
    StepResult step(Action action) throws InvalidMoveException;

    boolean isDone();

    int getStepCount();

    CityMap getMap();
}
