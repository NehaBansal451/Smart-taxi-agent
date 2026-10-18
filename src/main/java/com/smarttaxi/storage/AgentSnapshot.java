package com.smarttaxi.storage;

import java.io.Serializable;

import com.smarttaxi.agent.QTable;

/** Everything needed to restore a trained agent: the Q-table, its settings and the map size. */
public class AgentSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private final QTable qTable;
    private final double alpha;
    private final double gamma;
    private final double epsilon;
    private final int mapSize;

    public AgentSnapshot(QTable qTable, double alpha, double gamma, double epsilon, int mapSize) {
        this.qTable = qTable;
        this.alpha = alpha;
        this.gamma = gamma;
        this.epsilon = epsilon;
        this.mapSize = mapSize;
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

    public double getEpsilon() {
        return epsilon;
    }

    public int getMapSize() {
        return mapSize;
    }
}
