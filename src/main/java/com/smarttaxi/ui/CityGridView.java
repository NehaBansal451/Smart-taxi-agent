package com.smarttaxi.ui;

import com.smarttaxi.map.CityMap;
import com.smarttaxi.model.Position;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

/** Draws the city: walls, stops, taxi, passenger and destination. */
public class CityGridView extends GridPane {

    private static final int CELL = 56;

    private final CityMap map;
    private final StackPane[][] cells;
    private final Label[][] labels;
    private Position taxi;
    private Position passenger;
    private Position destination;

    public CityGridView(CityMap map) {
        this.map = map;
        int n = map.getSize();
        cells = new StackPane[n][n];
        labels = new Label[n][n];
        setHgap(3);
        setVgap(3);
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                Label label = new Label();
                StackPane cell = new StackPane(label);
                cell.setPrefSize(CELL, CELL);
                labels[r][c] = label;
                cells[r][c] = cell;
                add(cell, c, r);
            }
        }
        refresh();
    }

    /** Shows a situation. Pass null for the passenger while the passenger is inside the taxi. */
    public void show(Position taxi, Position passenger, Position destination) {
        this.taxi = taxi;
        this.passenger = passenger;
        this.destination = destination;
        refresh();
    }

    private void refresh() {
        for (int r = 0; r < map.getSize(); r++) {
            for (int c = 0; c < map.getSize(); c++) {
                Position p = new Position(r, c);
                String bg = "#141414";
                String fg = "#f5f5f5";
                String text = "";
                if (map.isWall(p)) {
                    bg = "#3a3a3a";
                } else {
                    if (map.getStops().contains(p)) { bg = "#2a2112"; text = "S"; fg = "#9a9a9a"; }
                    if (p.equals(destination)) { bg = "#166534"; text = "D"; }
                    if (p.equals(passenger)) { bg = "#1d4ed8"; text = "P"; }
                    if (p.equals(taxi)) { bg = "#f59e0b"; text = passenger == null ? "T+P" : "T"; fg = "#0a0a0a"; }
                }
                cells[r][c].setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 8;");
                labels[r][c].setText(text);
                labels[r][c].setStyle("-fx-text-fill: " + fg + "; -fx-font-weight: bold; -fx-font-size: 16px;");
            }
        }
    }
}
