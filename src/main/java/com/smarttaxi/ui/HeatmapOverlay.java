package com.smarttaxi.ui;

import com.smarttaxi.agent.QTable;
import com.smarttaxi.map.CityMap;
import com.smarttaxi.model.Action;
import com.smarttaxi.model.Position;
import com.smarttaxi.model.State;

import javafx.scene.paint.Color;

/**
 * Colours every free cell by how valuable the agent believes it is and shows the preferred move as an arrow.
 * Low value is deep navy, then pink, orange and yellow, and the best cells glow mint.
 * It only reads the Q-table and never changes it.
 */
public final class HeatmapOverlay {

    private static final double SAME_VALUE_LIMIT = 1e-9;
    private static final double OPACITY = 0.82;

    /** Colour stops of the ramp: position 0..1, then red, green, blue (0..255). */
    private static final double[][] RAMP = {
        {0.00, 17, 27, 82},
        {0.30, 255, 46, 151},
        {0.60, 255, 159, 28},
        {0.85, 255, 240, 80},
        {1.00, 45, 255, 208}
    };

    private HeatmapOverlay() {
    }

    /**
     * Paints the overlay for one fixed situation: the passenger and destination of {@code context}.
     * If all cells have the same value (an untrained table) every cell gets the same colour and no arrow.
     */
    public static void apply(CityGridView grid, CityMap map, QTable table, State context) {
        int size = map.getSize();
        double[][] values = new double[size][size];
        Action[][] best = new Action[size][size];
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Position p = new Position(r, c);
                if (map.isWall(p)) {
                    continue;
                }
                State s = new State(p, context.getPassenger(), context.getDestination());
                values[r][c] = table.getMaxValue(s);
                best[r][c] = table.getBestAction(s);
                min = Math.min(min, values[r][c]);
                max = Math.max(max, values[r][c]);
            }
        }
        boolean uniform = max - min < SAME_VALUE_LIMIT;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Position p = new Position(r, c);
                if (map.isWall(p)) {
                    continue;
                }
                double t = uniform ? 0.5 : (values[r][c] - min) / (max - min);
                grid.setCellOverlay(p, colourFor(t), uniform ? "" : arrow(best[r][c]));
            }
        }
    }

    /** Maps a value between 0 (worst) and 1 (best) to a colour on the ramp. */
    static Color colourFor(double t) {
        double x = Math.max(0.0, Math.min(1.0, t));
        for (int i = 1; i < RAMP.length; i++) {
            if (x <= RAMP[i][0]) {
                double[] a = RAMP[i - 1];
                double[] b = RAMP[i];
                double f = (x - a[0]) / (b[0] - a[0]);
                return Color.rgb(mix(a[1], b[1], f), mix(a[2], b[2], f), mix(a[3], b[3], f), OPACITY);
            }
        }
        return Color.rgb(45, 255, 208, OPACITY);
    }

    private static int mix(double from, double to, double f) {
        return (int) Math.round(from + (to - from) * f);
    }

    private static String arrow(Action action) {
        return switch (action) {
            case SOUTH -> "↓";
            case NORTH -> "↑";
            case EAST -> "→";
            case WEST -> "←";
            case PICKUP -> "P";
            case DROPOFF -> "D";
        };
    }
}
