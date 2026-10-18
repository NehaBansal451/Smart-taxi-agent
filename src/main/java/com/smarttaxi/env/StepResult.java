package com.smarttaxi.env;

import com.smarttaxi.model.State;

/** The outcome of one environment step. */
public final class StepResult {
    private final State nextState;
    private final double reward;
    private final boolean done;
    private final boolean success;

    public StepResult(State nextState, double reward, boolean done, boolean success) {
        this.nextState = nextState;
        this.reward = reward;
        this.done = done;
        this.success = success;
    }

    public State getNextState() {
        return nextState;
    }

    public double getReward() {
        return reward;
    }

    public boolean isDone() {
        return done;
    }

    public boolean isSuccess() {
        return success;
    }
}
