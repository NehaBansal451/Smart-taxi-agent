package com.smarttaxi.ui;

import java.util.List;

import com.smarttaxi.service.EpisodeResult;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Average reward per block of training episodes, drawn as one glowing line. */
public class LearningCurveCard extends VBox {

    private final XYChart.Series<Number, Number> series = new XYChart.Series<>();

    /** Builds an empty chart. */
    public LearningCurveCard() {
        super(4);
        getStyleClass().add("card");
        Label title = new Label("Learning curve");
        title.getStyleClass().add("card-title");
        Label note = new Label("Average reward per block of episodes. The line rises as the agent learns.");
        note.getStyleClass().add("muted");
        NumberAxis x = new NumberAxis();
        x.setLabel("Episode");
        NumberAxis y = new NumberAxis();
        y.setLabel("Reward");
        y.setForceZeroInRange(false);
        LineChart<Number, Number> chart = new LineChart<>(x, y);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setLegendVisible(false);
        chart.getData().add(series);
        chart.setPrefHeight(110);
        VBox.setVgrow(chart, Priority.ALWAYS);
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, Priority.ALWAYS);
        getChildren().addAll(new javafx.scene.layout.HBox(10, title, spacer, note), chart);
    }

    /** Replaces the curve with the results of a training run. */
    public void update(List<EpisodeResult> results) {
        series.getData().clear();
        if (results.isEmpty()) {
            return;
        }
        int block = Math.max(20, results.size() / 100);
        int first = results.get(0).episodeNo();
        double sum = 0;
        int count = 0;
        for (EpisodeResult r : results) {
            sum += r.totalReward();
            count++;
            if (count == block) {
                series.getData().add(new XYChart.Data<>(r.episodeNo() - first + 1, sum / count));
                sum = 0;
                count = 0;
            }
        }
    }
}
