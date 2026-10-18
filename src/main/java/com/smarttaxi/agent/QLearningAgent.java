package com.smarttaxi.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.smarttaxi.model.Action;
import com.smarttaxi.model.State;

/** Tabular Q-learning with epsilon-greedy exploration and epsilon decay. */
public class QLearningAgent extends AbstractAgent {

    private final QTable qTable;
    private final double alpha;
    private final double gamma;
    private final double epsilonMin;
    private final double epsilonDecay;
    private double epsilon;
    private boolean greedyMode;

    public QLearningAgent(Random random, double alpha, double gamma,
                          double epsilonStart, double epsilonMin, double epsilonDecay) {
        this(random, new QTable(), alpha, gamma, epsilonStart, epsilonMin, epsilonDecay);
    }

    /** Use this constructor to continue from a loaded Q-table. */
    public QLearningAgent(Random random, QTable qTable, double alpha, double gamma,
                          double epsilonStart, double epsilonMin, double epsilonDecay) {
        super("Q-learning", random);
        this.qTable = qTable;
        this.alpha = alpha;
        this.gamma = gamma;
        this.epsilon = epsilonStart;
        this.epsilonMin = epsilonMin;
        this.epsilonDecay = epsilonDecay;
    }

    @Override
    public Action chooseAction(State state) {
        Action[] all = Action.values();
        if (!greedyMode && random.nextDouble() < epsilon) {
            return all[random.nextInt(all.length)];
        }
        double best = qTable.getMaxValue(state);
        List<Action> ties = new ArrayList<>();
        for (Action a : all) {
            if (qTable.getQ(state, a) == best) {
                ties.add(a);
            }
        }
        return ties.get(random.nextInt(ties.size()));
    }

    @Override
    public void learn(State state, Action action, double reward, State nextState, boolean done) {
        double target = done ? reward : reward + gamma * qTable.getMaxValue(nextState);
        double old = qTable.getQ(state, action);
        qTable.setQ(state, action, old + alpha * (target - old));
    }

    @Override
    public void endEpisode() {
        epsilon = Math.max(epsilonMin, epsilon * epsilonDecay);
    }

    @Override
    public double getEpsilon() {
        return epsilon;
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

    /** In greedy mode the agent never explores; used when evaluating. */
    public void setGreedyMode(boolean greedyMode) {
        this.greedyMode = greedyMode;
    }

    public QTable getQTable() {
        return qTable;
    }

    public double getAlpha() {
        return alpha;
    }

    public double getGamma() {
        return gamma;
    }
}
