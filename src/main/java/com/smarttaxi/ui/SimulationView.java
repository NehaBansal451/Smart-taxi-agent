package com.smarttaxi.ui;

import java.util.Random;
import java.util.function.Supplier;

import com.smarttaxi.agent.Agent;
import com.smarttaxi.agent.QLearningAgent;
import com.smarttaxi.agent.RandomAgent;
import com.smarttaxi.env.GridWorld;
import com.smarttaxi.exception.InvalidMoveException;
import com.smarttaxi.map.CityMap;
import com.smarttaxi.model.Action;
import com.smarttaxi.model.State;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Simulation tab: watch the Random or the Q-learning agent drive one trip at a time.
 * It only displays the environment; all game and learning logic stays in env and agent.
 */
public class SimulationView extends BorderPane {

    private static final String RANDOM = "Random";
    private static final String LEARNED = "Q-learning";

    private GridWorld env;
    private CityGridView grid;
    private final Supplier<Agent> learnedAgent;
    private final Agent randomAgent = new RandomAgent(new Random());
    private final ComboBox<String> agentChoice = new ComboBox<>();
    private final Slider delay = new Slider(50, 1000, 300);
    private final Label info = new Label("Press New trip");
    private final Button newTripButton = new Button("New trip");
    private final Button stepButton = new Button("Step");
    private final Button runButton = new Button("Run trip");
    private final CheckBox heatmapBox = new CheckBox("Show heatmap");
    private Timeline timeline;

    /**
     * Creates the tab.
     *
     * @param map city to show
     * @param learnedAgent supplies the Q-learning agent each time it is needed
     */
    public SimulationView(CityMap map, Supplier<Agent> learnedAgent) {
        this.env = new GridWorld(map, new Random());
        this.grid = new CityGridView(map);
        this.learnedAgent = learnedAgent;

        agentChoice.getItems().addAll(RANDOM, LEARNED);
        agentChoice.setValue(RANDOM);
        newTripButton.setOnAction(e -> startTrip());
        stepButton.setOnAction(e -> onStep());
        runButton.setOnAction(e -> onRun());
        heatmapBox.setOnAction(e -> updateHeatmap(env.getState()));

        VBox controls = new VBox(10, new Label("Agent"), agentChoice, newTripButton, stepButton, runButton,
                new Label("Delay per step (ms)"), delay, heatmapBox, info);
        controls.setPadding(new Insets(10));
        setCenter(grid);
        setRight(controls);
        startTrip();
    }

    /** Shows another city: builds a new environment and grid, then starts a fresh trip. */
    public void setMap(CityMap map) {
        stopTimeline();
        this.env = new GridWorld(map, new Random());
        this.grid = new CityGridView(map);
        setCenter(grid);
        startTrip();
    }

    private Agent currentAgent() {
        return LEARNED.equals(agentChoice.getValue()) ? learnedAgent.get() : randomAgent;
    }

    private void startTrip() {
        stopTimeline();
        show(env.reset());
        info.setText("New trip, 0 steps");
    }

    private void onStep() {
        stopTimeline();
        advance();
    }

    private void onRun() {
        stopTimeline();
        if (env.isDone()) {
            startTrip();
        }
        timeline = new Timeline(new KeyFrame(Duration.millis(delay.getValue()), e -> advance()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void advance() {
        if (env.isDone()) {
            stopTimeline();
            return;
        }
        try {
            Action action = currentAgent().chooseAction(env.getState());
            env.step(action);
            show(env.getState());
            info.setText(action + ", " + env.getStepCount() + " steps" + (env.isDone() ? " (trip ended)" : ""));
            if (env.isDone()) {
                stopTimeline();
            }
        } catch (InvalidMoveException ex) {
            stopTimeline();
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    private void show(State state) {
        grid.setTaxi(state.getTaxi());
        grid.setPassenger(state.getPassenger().orElse(null));
        grid.setDestination(state.getDestination());
        updateHeatmap(state);
    }

    /** Repaints or clears the heatmap for the situation of the current trip. */
    private void updateHeatmap(State state) {
        grid.clearOverlay();
        if (!heatmapBox.isSelected()) {
            return;
        }
        Agent agent = learnedAgent.get();
        if (agent instanceof QLearningAgent q) {
            HeatmapOverlay.apply(grid, env.getMap(), q.getQTable(), state);
        }
    }

    private void stopTimeline() {
        if (timeline != null) {
            timeline.stop();
        }
    }
}
