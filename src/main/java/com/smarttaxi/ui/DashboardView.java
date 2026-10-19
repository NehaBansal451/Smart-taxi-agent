package com.smarttaxi.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

import com.smarttaxi.agent.EvaluationSummary;
import com.smarttaxi.agent.Evaluator;
import com.smarttaxi.agent.QLearningAgent;
import com.smarttaxi.agent.RandomAgent;
import com.smarttaxi.config.AppConfig;
import com.smarttaxi.env.GridWorld;
import com.smarttaxi.exception.InvalidConfigException;
import com.smarttaxi.fare.FareCalculator;
import com.smarttaxi.fare.StandardFare;
import com.smarttaxi.map.CityMap;
import com.smarttaxi.service.EpisodeResult;
import com.smarttaxi.exception.StorageException;
import com.smarttaxi.service.TrainingService;
import com.smarttaxi.storage.AgentSnapshot;
import com.smarttaxi.storage.AgentStorage;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Training dashboard: start, pause and resume training, watch reward, steps, epsilon and profit,
 * and compare Q-learning with Random.
 */
public class DashboardView extends BorderPane {

    private static final int REFRESH_EVERY = 20;
    private static final int PAUSE_POLL_MS = 50;
    private static final int EVALUATION_EPISODES = 200;

    private CityMap map = CityMap.defaultMap();
    private final TextField episodesField = new TextField("5000");
    private final Button trainButton = new Button("Train");
    private final Button pauseButton = new Button("Pause");
    private final Button evaluateButton = new Button("Evaluate");
    private final Button resetButton = new Button("Reset");
    private final Button saveRunButton = new Button("Save run");
    private final Button saveAgentButton = new Button("Save agent");
    private final Button loadAgentButton = new Button("Load agent");
    private final ProgressBar progress = new ProgressBar(0);
    private final Label status = new Label("Untrained agent");
    private final XYChart.Series<Number, Number> rewardSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> stepsSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> epsilonSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> profitSeries = new XYChart.Series<>();
    private final TableView<EvaluationSummary> table = new TableView<>();
    private volatile boolean paused;
    private volatile List<EpisodeResult> lastResults = new ArrayList<>();
    private QLearningAgent agent;
    private TrainingService service;

    /** Builds the dashboard with an untrained agent on the default city. */
    public DashboardView() {
        resetAgent();
        GridPane charts = new GridPane();
        charts.setHgap(8);
        charts.setVgap(8);
        addChart(charts, "Average reward", "Reward", rewardSeries, 0, 0);
        addChart(charts, "Average steps", "Steps", stepsSeries, 1, 0);
        addChart(charts, "Epsilon", "Epsilon", epsilonSeries, 0, 1);
        addChart(charts, "Average profit", "Profit", profitSeries, 1, 1);

        trainButton.setOnAction(e -> onTrain());
        pauseButton.setOnAction(e -> onPause());
        pauseButton.setDisable(true);
        evaluateButton.setOnAction(e -> onEvaluate());
        resetButton.setOnAction(e -> onReset());
        saveAgentButton.setOnAction(e -> onSaveAgent());
        loadAgentButton.setOnAction(e -> onLoadAgent());
        HBox controls = new HBox(10, new Label("Episodes:"), episodesField, trainButton, pauseButton,
                evaluateButton, resetButton, saveRunButton, saveAgentButton, loadAgentButton, progress);
        controls.setPadding(new Insets(10));
        setTop(controls);
        setCenter(charts);
        buildTable();
        VBox bottom = new VBox(6, table, status);
        bottom.setPadding(new Insets(10));
        setBottom(bottom);
    }

    private void addChart(GridPane grid, String title, String yLabel, XYChart.Series<Number, Number> series,
                          int col, int row) {
        NumberAxis x = new NumberAxis();
        x.setLabel("Episode");
        NumberAxis y = new NumberAxis();
        y.setLabel(yLabel);
        y.setForceZeroInRange(false);
        LineChart<Number, Number> chart = new LineChart<>(x, y);
        chart.setTitle(title);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setLegendVisible(false);
        chart.getData().add(series);
        GridPane.setHgrow(chart, Priority.ALWAYS);
        GridPane.setVgrow(chart, Priority.ALWAYS);
        grid.add(chart, col, row);
    }

    /** The agent that is currently being trained. */
    public QLearningAgent getAgent() {
        return agent;
    }

    /** The city this dashboard trains on. */
    public CityMap getMap() {
        return map;
    }

    /** Sets what happens when Save run is pressed; the saver receives the results of the last training. */
    public void setRunSaver(Consumer<List<EpisodeResult>> saver) {
        saveRunButton.setOnAction(e -> {
            if (lastResults == null || lastResults.isEmpty()) {
                showError("Train the agent first.");
                return;
            }
            saver.accept(lastResults);
        });
    }

    /** Switches training to another city: the agent starts again because the old Q-table no longer fits. */
    public void setMap(CityMap newMap) {
        if (trainButton.isDisabled()) {
            showError("Wait for the training to finish before changing the map.");
            return;
        }
        map = newMap;
        resetAgent();
        clearCharts();
        lastResults = new ArrayList<>();
        table.getItems().clear();
        progress.setProgress(0);
        status.setText("New map loaded (" + newMap.getSize() + " x " + newMap.getSize() + "), agent reset");
    }

    private void onSaveAgent() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save agent");
        chooser.setInitialFileName("agent.ser");
        chooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            AgentStorage.save(new AgentSnapshot(agent.getQTable(), agent.getAlpha(), agent.getGamma(),
                    agent.getEpsilon(), map.getSize()), file.toPath());
            status.setText("Agent saved to " + file.getName());
        } catch (StorageException e) {
            showError(e.getMessage());
        }
    }

    private void onLoadAgent() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load agent");
        chooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            installSnapshot(AgentStorage.load(file.toPath(), map.getSize()));
            table.getItems().clear();
        } catch (StorageException e) {
            showError(e.getMessage());
        }
    }

    /** Replaces the current agent with one restored from a saved snapshot. */
    public void installSnapshot(AgentSnapshot snapshot) {
        agent = new QLearningAgent(new Random(), snapshot.getQTable(), snapshot.getAlpha(), snapshot.getGamma(),
                snapshot.getEpsilon(), 0.05, 0.999);
        buildService();
        clearCharts();
        status.setText("Loaded agent, epsilon " + String.format("%.3f", snapshot.getEpsilon()));
    }

    private void resetAgent() {
        agent = new QLearningAgent(new Random(), 0.1, 0.99, 1.0, 0.05, 0.999);
        buildService();
    }

    private void buildService() {
        FareCalculator fare = new StandardFare();
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
        paused = false;
        setBusy(true);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<EpisodeResult> batch = new ArrayList<>();
                lastResults = service.train(episodes, r -> {
                    waitWhilePaused();
                    batch.add(r);
                    updateProgress(r.episodeNo() - firstEpisode, episodes);
                    if (batch.size() == REFRESH_EVERY) {
                        double reward = batch.stream().mapToDouble(EpisodeResult::totalReward).average().orElse(0);
                        double steps = batch.stream().mapToInt(EpisodeResult::steps).average().orElse(0);
                        double profit = batch.stream().mapToDouble(EpisodeResult::profit).average().orElse(0);
                        int no = r.episodeNo();
                        double eps = r.epsilon();
                        batch.clear();
                        Platform.runLater(() -> {
                            rewardSeries.getData().add(new XYChart.Data<>(no, reward));
                            stepsSeries.getData().add(new XYChart.Data<>(no, steps));
                            epsilonSeries.getData().add(new XYChart.Data<>(no, eps));
                            profitSeries.getData().add(new XYChart.Data<>(no, profit));
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

    /** Called on the training thread after each episode; blocks while the user has paused. */
    private void waitWhilePaused() {
        while (paused) {
            try {
                Thread.sleep(PAUSE_POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void onPause() {
        paused = !paused;
        pauseButton.setText(paused ? "Resume" : "Pause");
        status.setText(paused ? "Paused" : "Resumed");
    }

    private void finishTraining() {
        paused = false;
        pauseButton.setText("Pause");
        progress.progressProperty().unbind();
        progress.setProgress(1);
        setBusy(false);
        double totalProfit = 0;
        for (EpisodeResult r : lastResults) {
            totalProfit += r.profit();
        }
        status.setText("Trained " + lastResults.size() + " episodes, total profit " + String.format("%.2f", totalProfit));
    }

    private void onEvaluate() {
        try {
            EvaluationSummary learned = Evaluator.evaluate(new GridWorld(map, new Random()), agent, EVALUATION_EPISODES);
            EvaluationSummary random = Evaluator.evaluate(new GridWorld(map, new Random()),
                    new RandomAgent(new Random()), EVALUATION_EPISODES);
            table.getItems().setAll(random, learned);
            status.setText("Evaluated " + EVALUATION_EPISODES + " episodes per agent");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void onReset() {
        resetAgent();
        clearCharts();
        lastResults = new ArrayList<>();
        table.getItems().clear();
        progress.setProgress(0);
        status.setText("Untrained agent");
    }

    private void clearCharts() {
        rewardSeries.getData().clear();
        stepsSeries.getData().clear();
        epsilonSeries.getData().clear();
        profitSeries.getData().clear();
    }

    private void setBusy(boolean busy) {
        trainButton.setDisable(busy);
        evaluateButton.setDisable(busy);
        resetButton.setDisable(busy);
        saveRunButton.setDisable(busy);
        saveAgentButton.setDisable(busy);
        loadAgentButton.setDisable(busy);
        pauseButton.setDisable(!busy);
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }
}