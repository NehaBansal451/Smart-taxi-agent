package com.smarttaxi.ui;

import com.smarttaxi.agent.QTable;
import com.smarttaxi.map.CityMap;
import com.smarttaxi.model.Action;
import com.smarttaxi.model.Position;
import com.smarttaxi.model.State;

import javafx.scene.paint.Color;

/**
 * Colours every free cell by how valuable the agent believes it is and shows the preferred move as an arrow.
 * It only reads the Q-table and never changes it.
 */
public final class HeatmapOverlay {

    private static final double SAME_VALUE_LIMIT = 1e-9;
    private static final double GREEN_HUE = 120.0;
    private static final double OPACITY = 0.55;

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
                Color tint = Color.hsb(GREEN_HUE * t, 0.6, 1.0, OPACITY);
                grid.setCellOverlay(p, tint, uniform ? "" : arrow(best[r][c]));
            }
        }
    }

    private static String arrow(Action action) {
        return switch (action) {
            case SOUTH -> "\u2193";
            case NORTH -> "\u2191";
            case EAST -> "\u2192";
            case WEST -> "\u2190";
            case PICKUP -> "P";
            case DROPOFF -> "D";
        };
    }
}