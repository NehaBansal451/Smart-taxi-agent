package com.smarttaxi.agent;

import com.smarttaxi.model.Action;
import com.smarttaxi.model.State;

/** Anything that can choose actions and (optionally) learn from the results. */
public interface Agent {

    String getName();

    Action chooseAction(State state);

    void learn(State state, Action action, double reward, State nextState, boolean done);

    /** Called once after every episode (for example to decay epsilon). */
    void endEpisode();

    /** Current exploration rate; 0 for agents that do not explore. */
    double getEpsilon();
}
