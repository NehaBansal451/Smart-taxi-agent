package com.smarttaxi.ui;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.function.Function;

import com.smarttaxi.agent.EvaluationSummary;
import com.smarttaxi.agent.Evaluator;
import com.smarttaxi.agent.QLearningAgent;
import com.smarttaxi.agent.RandomAgent;
import com.smarttaxi.config.AppConfig;
import com.smarttaxi.env.GridWorld;
import com.smarttaxi.exception.InvalidConfigException;
import com.smarttaxi.fare.FareCalculator;
import com.smarttaxi.map.CityMap;
import com.smarttaxi.service.EpisodeResult;
import com.smarttaxi.service.TrainingService;
import com.smarttaxi.util.Validator;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Training dashboard: start training, watch the reward curve, compare Q-learning with Random. */
public class DashboardView extends BorderPane {

    private static final int REFRESH_EVERY = 20;

    private final CityMap map = CityMap.defaultMap();
    private final TextField episodesField = new TextField("5000");
    private final Button trainButton = new Button("Train");
    private final Button evaluateButton = new Button("Evaluate");
    private final Button resetButton = new Button("Reset");
    private final ProgressBar progress = new ProgressBar(0);
    private final Label status = new Label("Untrained agent");
    private final XYChart.Series<Number, Number> rewardSeries = new XYChart.Series<>();
    private final TableView<EvaluationSummary> table = new TableView<>();
    private QLearningAgent agent;
    private TrainingService service;

    public DashboardView() {
        resetAgent();
        rewardSeries.setName("Average reward per 20 episodes");
        NumberAxis x = new NumberAxis();
        x.setLabel("Episode");
        NumberAxis y = new NumberAxis();
        y.setLabel("Reward");
        LineChart<Number, Number> chart = new LineChart<>(x, y);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.getData().add(rewardSeries);
        trainButton.getStyleClass().add("primary"); trainButton.setOnAction(e -> onTrain());
        evaluateButton.setOnAction(e -> onEvaluate());
        resetButton.setOnAction(e -> onReset());
        HBox controls = new HBox(10, new Label("Episodes:"), episodesField, trainButton, evaluateButton, resetButton, progress);
        controls.setPadding(new Insets(10));
        setTop(controls);
        setCenter(chart);
        buildTable();
        VBox bottom = new VBox(6, table, status);
        bottom.setPadding(new Insets(10));
        setBottom(bottom);
    }

    public CityMap getMap() {
        return map;
    }

    public QLearningAgent getAgent() {
        return agent;
    }

    private void resetAgent() {
        agent = new QLearningAgent(new Random(), 0.1, 0.99, 1.0, 0.05, 0.999);
        FareCalculator fare = new FareCalculator() {
            @Override
            public double calculateFare(int distance, LocalTime time) {
                return 5 + 2 * distance;
            }

            @Override
            public double calculateCost(int steps) {
                return 0.5 * steps;
            }
        };
        try {
            service = new TrainingService(new GridWorld(map, new Random()), agent, fare, new AppConfig(new Properties()));
        } catch (InvalidConfigException e) {
            throw new IllegalStateException(e);
        }
    }

    private void buildTable() {
        table.getColumns().add(column("Agent", s -> s.agentName()));
        table.getColumns().add(column("Avg reward", s -> String.format("%.2f", s.averageReward())));
        table.getColumns().add(column("Avg steps", s -> String.format("%.2f", s.averageSteps())));
        table.getColumns().add(column("Success rate", s -> String.format("%.1f%%", s.successRate() * 100)));
        table.setPrefHeight(110);
    }

    private TableColumn<EvaluationSummary, String> column(String title, Function<EvaluationSummary, String> value) {
        TableColumn<EvaluationSummary, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return c;
    }

    private void onTrain() {
        int parsed;
        try {
            parsed = Validator.parseEpisodes(episodesField.getText());
        } catch (InvalidConfigException e) {
            showError(e.getMessage());
            return;
        }
        final int episodes = parsed;
        final int firstEpisode = service.getEpisodeCounter();
        setBusy(true);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<EpisodeResult> batch = new ArrayList<>();
                service.train(episodes, r -> {
                    batch.add(r);
                    updateProgress(r.episodeNo() - firstEpisode, episodes);
                    if (batch.size() == REFRESH_EVERY) {
                        double avg = batch.stream().mapToDouble(EpisodeResult::totalReward).average().orElse(0);
                        int no = r.episodeNo();
                        double eps = r.epsilon();
                        batch.clear();
                        Platform.runLater(() -> {
                            rewardSeries.getData().add(new XYChart.Data<>(no, avg));
                            status.setText("Episode " + no + ", epsilon " + String.format("%.3f", eps));
                        });
                    }
                }, this::isCancelled);
                return null;
            }
        };
        progress.progressProperty().bind(task.progressProperty());
        task.setOnSucceeded(e -> finishTraining());
        task.setOnFailed(e -> {
            finishTraining();
            showError(String.valueOf(task.getException()));
        });
        Thread thread = new Thread(task, "training");
        thread.setDaemon(true);
        thread.start();
    }

    private void finishTraining() {
        progress.progressProperty().unbind();
        progress.setProgress(1);
        setBusy(false);
    }

    private void onEvaluate() {
        try {
            EvaluationSummary learned = Evaluator.evaluate(new GridWorld(map, new Random()), agent, 200);
            EvaluationSummary random = Evaluator.evaluate(new GridWorld(map, new Random()), new RandomAgent(new Random()), 200);
            table.getItems().setAll(random, learned);
            status.setText("Evaluated 200 episodes per agent");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void onReset() {
        resetAgent();
        rewardSeries.getData().clear();
        table.getItems().clear();
        progress.setProgress(0);
        status.setText("Untrained agent");
    }

    private void setBusy(boolean busy) {
        trainButton.setDisable(busy);
        evaluateButton.setDisable(busy);
        resetButton.setDisable(busy);
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }
}

