package com.smarttaxi.ui;

import java.util.function.Consumer;

import com.smarttaxi.map.CityMap;
import com.smarttaxi.model.Position;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Draws a {@link CityMap} as a grid and shows the taxi, passenger and destination.
 * Reused by the simulation tab, the replay screen and the map editor.
 * It only displays state; it contains no game or learning logic.
 */
public class CityGridView extends GridPane {

    private static final double CELL_SIZE = 64;
    private static final Color WALL_COLOR = Color.rgb(70, 70, 80);
    private static final Color STOP_COLOR = Color.rgb(200, 225, 250);
    private static final Color ROAD_COLOR = Color.WHITE;

    private final CityMap map;
    private final Cell[][] cells;
    private Position taxi;
    private Position passenger;
    private Position destination;
    private Consumer<Position> cellClickHandler;

    /** Creates a view for the given map. */
    public CityGridView(CityMap map) {
        this.map = map;
        int size = map.getSize();
        this.cells = new Cell[size][size];
        setHgap(1);
        setVgap(1);
        setAlignment(Pos.CENTER);
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Position p = new Position(r, c);
                Cell cell = new Cell();
                cell.setOnMouseClicked(e -> {
                    if (cellClickHandler != null) {
                        cellClickHandler.accept(p);
                    }
                });
                cells[r][c] = cell;
                add(cell, c, r);
            }
        }
        refresh();
    }

    /** Moves the taxi marker. Pass null to hide it. */
    public void setTaxi(Position taxi) {
        this.taxi = taxi;
        refresh();
    }

    /** Sets where the passenger waits. Pass null while the passenger is inside the taxi. */
    public void setPassenger(Position passenger) {
        this.passenger = passenger;
        refresh();
    }

    /** Sets the destination marker. Pass null to hide it. */
    public void setDestination(Position destination) {
        this.destination = destination;
        refresh();
    }

    /** Registers a handler that receives the clicked cell position. */
    public void setCellClickHandler(Consumer<Position> handler) {
        this.cellClickHandler = handler;
    }

    /** Overlay hook for the heatmap: tints one cell and shows a short text such as an arrow. */
    public void setCellOverlay(Position p, Color tint, String text) {
        if (map.isInside(p)) {
            Cell cell = cells[p.getRow()][p.getCol()];
            cell.heat.setFill(tint == null ? Color.TRANSPARENT : tint);
            cell.hint.setText(text == null ? "" : text);
        }
    }

    /** Removes every overlay tint and text. */
    public void clearOverlay() {
        for (Cell[] row : cells) {
            for (Cell cell : row) {
                cell.heat.setFill(Color.TRANSPARENT);
                cell.hint.setText("");
            }
        }
    }

    private void refresh() {
        for (int r = 0; r < map.getSize(); r++) {
            for (int c = 0; c < map.getSize(); c++) {
                Position p = new Position(r, c);
                Cell cell = cells[r][c];
                if (map.isWall(p)) {
                    cell.base.setFill(WALL_COLOR);
                } else if (map.getStops().contains(p)) {
                    cell.base.setFill(STOP_COLOR);
                } else {
                    cell.base.setFill(ROAD_COLOR);
                }
                cell.marker.setText(markerText(p));
            }
        }
    }

    private String markerText(Position p) {
        StringBuilder sb = new StringBuilder();
        if (p.equals(taxi)) {
            sb.append("T");
        }
        if (p.equals(passenger)) {
            sb.append("P");
        }
        if (p.equals(destination)) {
            sb.append("D");
        }
        return sb.toString();
    }

    /** One square of the grid: base colour, heat tint, arrow text and a marker. */
    private static final class Cell extends StackPane {
        private final Rectangle base = new Rectangle(CELL_SIZE, CELL_SIZE);
        private final Rectangle heat = new Rectangle(CELL_SIZE, CELL_SIZE, Color.TRANSPARENT);
        private final Label hint = new Label();
        private final Label marker = new Label();

        Cell() {
            hint.setFont(Font.font(22));
            marker.setFont(Font.font("System", FontWeight.BOLD, 14));
            StackPane.setAlignment(marker, Pos.TOP_LEFT);
            getChildren().addAll(base, heat, hint, marker);
        }
    }
}