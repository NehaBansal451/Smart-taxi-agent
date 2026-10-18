package com.smarttaxi.ui;

import java.util.Random;
import java.util.function.Supplier;

import com.smarttaxi.agent.Agent;
import com.smarttaxi.agent.QLearningAgent;
import com.smarttaxi.agent.QTable;
import com.smarttaxi.agent.RandomAgent;
import com.smarttaxi.env.GridWorld;
import com.smarttaxi.env.StepResult;
import com.smarttaxi.map.CityMap;
import com.smarttaxi.model.Action;
import com.smarttaxi.model.Position;
import com.smarttaxi.model.State;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Simulation tab: watch an agent drive one trip on the city grid. */
public class SimulationView extends BorderPane {

    private final Supplier<QLearningAgent> agentSource;
    private final GridWorld env;
    private final CityGridView grid;
    private final Agent randomAgent = new RandomAgent(new Random());
    private final ComboBox<String> agentChoice = new ComboBox<>();
    private final Button newTripButton = new Button("New trip");
    private final Button stepButton = new Button("Step");
    private final Button runButton = new Button("Run trip");
    private final Slider speed = new Slider(50, 800, 300);
    private final CheckBox heatToggle = new CheckBox("Show heatmap");
    private final Label info = new Label("Press New trip");
    private Timeline timeline;
    private State state;
    private double totalReward;
    private boolean finished = true;

    public SimulationView(CityMap map, Supplier<QLearningAgent> agentSource) {
        this.agentSource = agentSource;
        this.env = new GridWorld(map, new Random());
        this.grid = new CityGridView(map);
        agentChoice.getItems().addAll("Random", "Q-learning");
        agentChoice.setValue("Random");
        newTripButton.getStyleClass().add("primary");
        newTripButton.setOnAction(e -> newTrip());
        stepButton.setOnAction(e -> step());
        runButton.setOnAction(e -> runTrip());
        stepButton.setDisable(true);
        runButton.setDisable(true);
        heatToggle.setOnAction(e -> updateHeat());
        info.setWrapText(true);
        VBox panel = new VBox(12, new Label("Agent"), agentChoice, heatToggle, newTripButton, stepButton, runButton, new Label("Delay per move (ms)"), speed, info);
        panel.setPadding(new Insets(20));
        panel.setPrefWidth(240);
        setCenter(grid);
        setRight(panel);
        BorderPane.setAlignment(grid, Pos.CENTER);
        BorderPane.setMargin(grid, new Insets(20));
    }

    private void newTrip() {
        stopTimeline();
        state = env.reset();
        totalReward = 0;
        finished = false;
        showState();
        info.setText("Trip started");
        stepButton.setDisable(false);
        runButton.setDisable(false);
    }

    private void showState() {
        grid.show(state.getTaxi(), state.getPassenger().orElse(null), state.getDestination());
        updateHeat();
    }

    private void updateHeat() {
        if (!heatToggle.isSelected() || state == null) {
            grid.setHeat(null, null);
            return;
        }
        QTable table = agentSource.get().getQTable();
        int n = env.getMap().getSize();
        double[][] values = new double[n][n];
        String[][] arrows = new String[n][n];
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                State s = new State(new Position(r, c), state.getPassenger(), state.getDestination());
                values[r][c] = table.getMaxValue(s);
                arrows[r][c] = arrow(table.getBestAction(s));
            }
        }
        grid.setHeat(values, arrows);
    }

    private static String arrow(Action a) {
        switch (a) {
            case SOUTH: return "\u2193";
            case NORTH: return "\u2191";
            case EAST: return "\u2192";
            case WEST: return "\u2190";
            case PICKUP: return "PK";
            default: return "DR";
        }
    }

    private Action choose() {
        if ("Random".equals(agentChoice.getValue())) {
            return randomAgent.chooseAction(state);
        }
        QLearningAgent agent = agentSource.get();
        agent.setGreedyMode(true);
        try {
            return agent.chooseAction(state);
        } finally {
            agent.setGreedyMode(false);
        }
    }

    private void step() {
        if (finished) {
            return;
        }
        try {
            Action action = choose();
            StepResult result = env.step(action);
            state = result.getNextState();
            totalReward += result.getReward();
            showState();
            info.setText(action + " | steps " + env.getStepCount() + " | reward " + totalReward);
            if (result.isDone()) {
                finished = true;
                stopTimeline();
                stepButton.setDisable(true);
                runButton.setDisable(true);
                info.setText((result.isSuccess() ? "Delivered! " : "Step limit reached. ") + "Steps " + env.getStepCount() + ", reward " + totalReward);
            }
        } catch (Exception e) {
            stopTimeline();
            new Alert(Alert.AlertType.ERROR, String.valueOf(e.getMessage())).show();
        }
    }

    private void runTrip() {
        stopTimeline();
        timeline = new Timeline(new KeyFrame(Duration.millis(speed.getValue()), e -> step()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void stopTimeline() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }
}

