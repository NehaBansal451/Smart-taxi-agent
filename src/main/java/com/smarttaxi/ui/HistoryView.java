package com.smarttaxi.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;

import com.smarttaxi.db.TrainingRun;
import com.smarttaxi.db.Trip;
import com.smarttaxi.env.GridWorld;
import com.smarttaxi.exception.DataAccessException;
import com.smarttaxi.exception.InvalidMoveException;
import com.smarttaxi.map.CityMap;
import com.smarttaxi.model.Action;
import com.smarttaxi.model.Position;
import com.smarttaxi.model.State;
import com.smarttaxi.service.HistoryService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * History tab: saved training runs, a leaderboard, editable notes, delete, and replay of a stored trip.
 * All database access goes through {@link HistoryService}.
 */
public class HistoryView extends BorderPane {

    /** Turns the stored map text back into a CityMap. */
    @FunctionalInterface
    public interface MapParser {
        CityMap parse(String text) throws Exception;
    }

    private static final String NEWEST = "Newest first";
    private static final String BY_REWARD = "Top by average reward";
    private static final String BY_SUCCESS = "Top by success rate";
    private static final String BY_PROFIT = "Top by profit";
    private static final int LEADERBOARD_SIZE = 10;
    private static final double REPLAY_STEP_MS = 350;

    private final HistoryService service;
    private final MapParser mapParser;
    private final ComboBox<String> sortChoice = new ComboBox<>();
    private final TableView<TrainingRun> runTable = new TableView<>();
    private final TableView<Trip> tripTable = new TableView<>();
    private final TextArea notesArea = new TextArea();
    private final StackPane replayHolder = new StackPane();
    private final Label replayInfo = new Label("Select a run and a trip, then press Replay");
    private final Label status = new Label();
    private Timeline timeline;

    /**
     * Creates the tab.
     *
     * @param service reads and writes stored history
     * @param mapParser converts a stored map text back into a CityMap
     */
    public HistoryView(HistoryService service, MapParser mapParser) {
        this.service = service;
        this.mapParser = mapParser;

        sortChoice.getItems().addAll(NEWEST, BY_REWARD, BY_SUCCESS, BY_PROFIT);
        sortChoice.setValue(NEWEST);
        sortChoice.valueProperty().addListener((o, oldValue, newValue) -> refresh());
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refresh());
        HBox top = new HBox(10, new Label("Show:"), sortChoice, refreshButton, status);
        top.setPadding(new Insets(10));
        setTop(top);

        buildRunTable();
        buildTripTable();
        runTable.getSelectionModel().selectedItemProperty()
                .addListener((o, oldRun, newRun) -> onRunSelected(newRun));

        notesArea.setPromptText("Notes for the selected run");
        notesArea.setPrefRowCount(3);
        Button saveNotes = new Button("Save notes");
        saveNotes.setOnAction(e -> onSaveNotes());
        Button deleteRun = new Button("Delete run");
        deleteRun.setOnAction(e -> onDelete());
  VBox right = new VBox(8, new Label("Trips of the selected run"), tripTable, notesArea,
                new HBox(10, saveNotes, deleteRun));
        right.setPadding(new Insets(0, 0, 0, 10));
        VBox left = new VBox(8, new Label("Training runs"), runTable);
        SplitPane split = new SplitPane(left, right);
        setCenter(split);

        Button replayButton = new Button("Replay selected trip");
        replayButton.setOnAction(e -> onReplay());
        VBox bottom = new VBox(8, new HBox(10, replayButton, replayInfo), replayHolder);
        bottom.setPadding(new Insets(10));
        setBottom(bottom);

        refresh();
    }

    /** Reloads the runs from the database. Call it after a new run has been saved. */
    public final void refresh() {
        try {
            List<TrainingRun> runs;
            String sort = sortChoice.getValue();
            if (BY_REWARD.equals(sort)) {
                runs = service.getLeaderboard("avg_reward", LEADERBOARD_SIZE);
            } else if (BY_SUCCESS.equals(sort)) {
                runs = service.getLeaderboard("success_rate", LEADERBOARD_SIZE);
            } else if (BY_PROFIT.equals(sort)) {
                runs = service.getLeaderboard("total_profit", LEADERBOARD_SIZE);
            } else {
                runs = new ArrayList<>(service.getRuns());
                runs.sort(Comparator.comparing(TrainingRun::startedAt).reversed());
            }
            runTable.getItems().setAll(runs);
            tripTable.getItems().clear();
            notesArea.clear();
            status.setText(runs.size() + " runs");
        } catch (DataAccessException e) {
            showError(e.getMessage());
        }
    }

    private void buildRunTable() {
        runTable.getColumns().add(column("ID", (TrainingRun r) -> String.valueOf(r.id())));
        runTable.getColumns().add(column("Started", (TrainingRun r) -> r.startedAt().toString()));
        runTable.getColumns().add(column("Agent", (TrainingRun r) -> r.agentType()));
        runTable.getColumns().add(column("Episodes", (TrainingRun r) -> String.valueOf(r.episodes())));
        runTable.getColumns().add(column("Avg reward", (TrainingRun r) -> String.format("%.2f", r.avgReward())));
        runTable.getColumns().add(column("Success", (TrainingRun r) -> String.format("%.1f%%", r.successRate() * 100)));
        runTable.getColumns().add(column("Profit", (TrainingRun r) -> String.format("%.2f", r.totalProfit())));
    }

    private void buildTripTable() {
        tripTable.getColumns().add(column("Episode", (Trip t) -> String.valueOf(t.episodeNo())));
        tripTable.getColumns().add(column("Reward", (Trip t) -> String.format("%.1f", t.totalReward())));
        tripTable.getColumns().add(column("Steps", (Trip t) -> String.valueOf(t.steps())));
        tripTable.getColumns().add(column("Delivered", (Trip t) -> t.success() ? "yes" : "no"));
        tripTable.getColumns().add(column("Profit", (Trip t) -> String.format("%.2f", t.profit())));
    }

    private static <T> TableColumn<T, String> column(String title, Function<T, String> value) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return c;
    }

    private void onRunSelected(TrainingRun run) {
        stopTimeline();
        if (run == null) {
            tripTable.getItems().clear();
            notesArea.clear();
            return;
        }
        notesArea.setText(run.notes() == null ? "" : run.notes());
        try {
            tripTable.getItems().setAll(service.getTrips(run.id()));
        } catch (DataAccessException e) {
            showError(e.getMessage());
        }
    }

    private void onSaveNotes() {
        TrainingRun run = runTable.getSelectionModel().getSelectedItem();
        if (run == null) {
            showInfo("Select a run first.");
            return;
        }
        try {
            service.updateNotes(run.id(), notesArea.getText());
            refresh();
        } catch (DataAccessException e) {
            showError(e.getMessage());
        }
    }

    private void onDelete() {
        TrainingRun run = runTable.getSelectionModel().getSelectedItem();
        if (run == null) {
            showInfo("Select a run first.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete run " + run.id() + " and all its trips?", ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isPresent() && answer.get() == ButtonType.OK) {
            try {
                service.deleteRun(run.id());
                replayHolder.getChildren().clear();
                refresh();
            } catch (DataAccessException e) {
                showError(e.getMessage());
            }
        }
    }

    private void onReplay() {
        stopTimeline();
        TrainingRun run = runTable.getSelectionModel().getSelectedItem();
        Trip trip = tripTable.getSelectionModel().getSelectedItem();
        if (run == null || trip == null) {
            showInfo("Select a run and a trip first.");
            return;
        }
        try {
            CityMap map = mapParser.parse(run.mapData());
            Position start = Trip.parsePosition(trip.startTaxi());
            Position pickup = Trip.parsePosition(trip.pickup());
            Position destination = Trip.parsePosition(trip.destination());
            GridWorld env = new GridWorld(map, new Random());
            env.reset(new State(start, Optional.of(pickup), destination));

            CityGridView grid = new CityGridView(map);
            grid.setTaxi(start);
            grid.setPassenger(pickup);
            grid.setDestination(destination);
            replayHolder.getChildren().setAll(grid);

            List<Action> actions = trip.actionList();
            replayInfo.setText("Replaying " + actions.size() + " steps");
            if (actions.isEmpty()) {
                return;
            }
            int[] index = {0};
            timeline = new Timeline(new KeyFrame(Duration.millis(REPLAY_STEP_MS), e -> {
                Action action = actions.get(index[0]);
                try {
                    env.step(action);
                } catch (InvalidMoveException ex) {
                    stopTimeline();
                    showError(ex.getMessage());
                    return;
                }
                State s = env.getState();
                grid.setTaxi(s.getTaxi());
                grid.setPassenger(s.getPassenger().orElse(null));
                index[0]++;
                replayInfo.setText("Step " + index[0] + " of " + actions.size() + ": " + action);
            }));
            timeline.setCycleCount(actions.size());
            timeline.play();
        } catch (Exception e) {
            showError("Could not replay this trip: " + e.getMessage());
        }
    }

    private void stopTimeline() {
        if (timeline != null) {
            timeline.stop();
        }
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
    }
}