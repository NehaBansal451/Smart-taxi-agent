package com.smarttaxi.agent;

import java.util.Random;

import com.smarttaxi.model.Action;
import com.smarttaxi.model.State;

/** The baseline: picks a random action every time and never learns. */
public class RandomAgent extends AbstractAgent {

    public RandomAgent(Random random) {
        super("Random", random);
    }

    @Override
    public Action chooseAction(State state) {
        Action[] all = Action.values();
        return all[random.nextInt(all.length)];
    }

    @Override
    public void learn(State state, Action action, double reward, State nextState, boolean done) {
        // a random agent does not learn
    }

    @Override
    public void endEpisode() {
        // nothing to update
    }

    @Override
    public double getEpsilon() {
        return 0.0;
    }
}
