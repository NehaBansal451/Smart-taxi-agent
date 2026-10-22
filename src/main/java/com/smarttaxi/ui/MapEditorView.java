package com.smarttaxi.ui;

import java.io.File;
import java.util.function.Consumer;

import com.smarttaxi.exception.InvalidMapException;
import com.smarttaxi.exception.StorageException;
import com.smarttaxi.map.CityMap;
import com.smarttaxi.map.MapFileHandler;
import com.smarttaxi.model.Position;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

/**
 * City map editor. Click a cell to place a wall or a stop (or erase it), resize the city,
 * check that every stop can be reached, and save or load the map as a text file.
 * It uses only CityMap and MapFileHandler, so it does not depend on the other screens.
 */
public class MapEditorView extends BorderPane {

    private static final int CELL = 52;

    private final Consumer<CityMap> onApply;
    private final GridPane board = new GridPane();
    private final ToggleButton wallTool = new ToggleButton("Wall");
    private final ToggleButton stopTool = new ToggleButton("Stop");
    private final ToggleButton eraseTool = new ToggleButton("Erase");
    private final Spinner<Integer> sizeSpinner = new Spinner<>(CityMap.MIN_SIZE, CityMap.MAX_SIZE, 5);
    private final Label status = new Label("Click a cell to edit the city");
    private CityMap current;

    /**
     * @param start   the map to edit first
     * @param onApply called when the user presses "Use this map"; may be null
     */
    public MapEditorView(CityMap start, Consumer<CityMap> onApply) {
        this.onApply = onApply;
        this.current = start;

        ToggleGroup tools = new ToggleGroup();
        wallTool.setToggleGroup(tools);
        stopTool.setToggleGroup(tools);
        eraseTool.setToggleGroup(tools);
        wallTool.setSelected(true);

        sizeSpinner.getValueFactory().setValue(start.getSize());
        sizeSpinner.valueProperty().addListener((obs, oldSize, newSize) -> resize(newSize));

        Button checkButton = new Button("Check map");
        checkButton.setOnAction(e -> check());
        Button saveButton = new Button("Save to file");
        saveButton.setOnAction(e -> save());
        Button loadButton = new Button("Load from file");
        loadButton.setOnAction(e -> load());
        Button useButton = new Button("Use this map");
        useButton.setOnAction(e -> use());

        HBox bar = new HBox(10, new Label("Tool:"), wallTool, stopTool, eraseTool,
                new Label("Size:"), sizeSpinner, checkButton, saveButton, loadButton, useButton);
        bar.setPadding(new Insets(10));
        bar.setAlignment(Pos.CENTER_LEFT);
        setTop(bar);

        board.setHgap(3);
        board.setVgap(3);
        board.setAlignment(Pos.CENTER);
        setCenter(board);

        status.setPadding(new Insets(10));
        setBottom(status);

        redraw();
    }

    private void redraw() {
        board.getChildren().clear();
        for (int r = 0; r < current.getSize(); r++) {
            for (int c = 0; c < current.getSize(); c++) {
                Position p = new Position(r, c);
                Button cell = new Button();
                cell.setMinSize(CELL, CELL);
                cell.setMaxSize(CELL, CELL);
                if (current.isWall(p)) {
                    cell.setText("BLDG");
                    cell.setStyle("-fx-background-color: #46464f; -fx-text-fill: white; -fx-font-size: 10px;");
                } else if (current.getStops().contains(p)) {
                    cell.setText("STOP");
                    cell.setStyle("-fx-background-color: #f9d71c; -fx-text-fill: black; -fx-font-weight: bold;"
                            + " -fx-font-size: 10px;");
                } else {
                    cell.setStyle("-fx-background-color: white; -fx-border-color: #dddddd;");
                }
                cell.setOnAction(e -> click(p));
                board.add(cell, c, r);
            }
        }
    }

    private void click(Position p) {
        try {
            CityMap.Builder builder = current.toBuilder();
            if (wallTool.isSelected()) {
                builder.addWall(p);
            } else if (stopTool.isSelected()) {
                builder.addStop(p);
            } else {
                builder.removeWall(p).removeStop(p);
            }
            current = builder.build();
            status.setText("Edited " + p);
            redraw();
        } catch (IllegalArgumentException e) {
            status.setText(e.getMessage());
        }
    }

    private void resize(int newSize) {
        try {
            CityMap.Builder builder = CityMap.builder(newSize);
            for (Position wall : current.getWalls()) {
                if (wall.getRow() < newSize && wall.getCol() < newSize) {
                    builder.addWall(wall);
                }
            }
            for (Position stop : current.getStops()) {
                if (stop.getRow() < newSize && stop.getCol() < newSize) {
                    builder.addStop(stop);
                }
            }
            current = builder.build();
            status.setText("City is now " + newSize + " x " + newSize);
            redraw();
        } catch (IllegalArgumentException e) {
            status.setText(e.getMessage());
        }
    }

    private boolean check() {
        try {
            MapFileHandler.validate(current);
            status.setText("Map is valid: " + current.getStops().size() + " stops, all reachable");
            return true;
        } catch (InvalidMapException e) {
            status.setText("Map problem: " + e.getMessage());
            return false;
        }
    }

    private void save() {
        if (!check()) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save map");
        chooser.setInitialFileName("my-city.csv");
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            MapFileHandler.save(current, file.toPath());
            status.setText("Saved to " + file.getName());
        } catch (StorageException e) {
            status.setText(e.getMessage());
        }
    }

    private void load() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load map");
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            current = MapFileHandler.load(file.toPath());
            sizeSpinner.getValueFactory().setValue(current.getSize());
            status.setText("Loaded " + file.getName());
            redraw();
            if (onApply != null) {
                use();
            }
        } catch (InvalidMapException | StorageException e) {
            status.setText(e.getMessage());
        }
    }

    private void use() {
        if (!check()) {
            return;
        }
        if (onApply == null) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Map saved only. Save it to a file and set map.file in config.properties to use it.").showAndWait();
            return;
        }
        onApply.accept(current);
        status.setText("Map handed to the application");
    }
}
