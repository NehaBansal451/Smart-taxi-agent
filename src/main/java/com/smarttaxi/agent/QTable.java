package com.smarttaxi.agent;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.smarttaxi.model.Action;
import com.smarttaxi.model.State;

/** Maps each State to one learned value per Action. Unseen states start with all zeros. */
public class QTable implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<State, double[]> values = new HashMap<>();

    private double[] row(State s) {
        return values.computeIfAbsent(s, k -> new double[Action.values().length]);
    }

    public double getQ(State s, Action a) {
        double[] r = values.get(s);
        return r == null ? 0.0 : r[a.ordinal()];
    }

    public void setQ(State s, Action a, double value) {
        row(s)[a.ordinal()] = value;
    }

    public double getMaxValue(State s) {
        double[] r = values.get(s);
        if (r == null) {
            return 0.0;
        }
        double max = r[0];
        for (double v : r) {
            max = Math.max(max, v);
        }
        return max;
    }

    /** Action with the highest value; the first one wins a tie. */
    public Action getBestAction(State s) {
        double[] r = values.get(s);
        if (r == null) {
            return Action.values()[0];
        }
        int best = 0;
        for (int i = 1; i < r.length; i++) {
            if (r[i] > r[best]) {
                best = i;
            }
        }
        return Action.values()[best];
    }

    public int size() {
        return values.size();
    }
}
