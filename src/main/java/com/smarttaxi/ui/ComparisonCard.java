package com.smarttaxi.ui;

import java.util.Locale;

import com.smarttaxi.agent.EvaluationSummary;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Random driver against Q-learning agent: success rate, steps per trip and average reward as paired bars. */
public class ComparisonCard extends VBox {

    private final ProgressBar successRandom = bar("bar-random");
    private final ProgressBar successLearned = bar("bar-q");
    private final ProgressBar stepsRandom = bar("bar-random");
    private final ProgressBar stepsLearned = bar("bar-q");
    private final ProgressBar rewardRandom = bar("bar-random");
    private final ProgressBar rewardLearned = bar("bar-q");
    private final Label successRandomText = new Label("-");
    private final Label successLearnedText = new Label("-");
    private final Label stepsRandomText = new Label("-");
    private final Label stepsLearnedText = new Label("-");
    private final Label rewardRandomText = new Label("-");
    private final Label rewardLearnedText = new Label("-");

    /** Builds the card with empty bars. */
    public ComparisonCard() {
        super(4);
        getStyleClass().add("card");
        Label title = new Label("Random vs Q-learning");
        title.getStyleClass().add("card-title");
        getChildren().addAll(title,
                group("SUCCESS RATE", successRandom, successRandomText, successLearned, successLearnedText),
                group("STEPS PER TRIP (fewer is better)", stepsRandom, stepsRandomText, stepsLearned, stepsLearnedText),
                group("AVERAGE REWARD", rewardRandom, rewardRandomText, rewardLearned, rewardLearnedText));
    }

    /** Fills the bars from two evaluation runs. */
    public void update(EvaluationSummary learned, EvaluationSummary random) {
        set(successRandom, successRandomText, random.successRate(), 1.0,
                String.format(Locale.US, "%.0f%%", random.successRate() * 100));
        set(successLearned, successLearnedText, learned.successRate(), 1.0,
                String.format(Locale.US, "%.0f%%", learned.successRate() * 100));
        double stepsMax = Math.max(1e-9, Math.max(random.averageSteps(), learned.averageSteps()));
        set(stepsRandom, stepsRandomText, random.averageSteps(), stepsMax,
                String.format(Locale.US, "%.1f", random.averageSteps()));
        set(stepsLearned, stepsLearnedText, learned.averageSteps(), stepsMax,
                String.format(Locale.US, "%.1f", learned.averageSteps()));
        double rewardMax = Math.max(1e-9, Math.max(Math.abs(random.averageReward()), Math.abs(learned.averageReward())));
        set(rewardRandom, rewardRandomText, Math.abs(random.averageReward()), rewardMax,
                String.format(Locale.US, "%+.0f", random.averageReward()));
        set(rewardLearned, rewardLearnedText, Math.abs(learned.averageReward()), rewardMax,
                String.format(Locale.US, "%+.1f", learned.averageReward()));
    }

    private static void set(ProgressBar bar, Label text, double value, double max, String shown) {
        bar.setProgress(Math.max(0.0, Math.min(1.0, value / max)));
        text.setText(shown);
    }

    private static ProgressBar bar(String style) {
        ProgressBar b = new ProgressBar(0);
        b.getStyleClass().add(style);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setPrefHeight(9);
        HBox.setHgrow(b, Priority.ALWAYS);
        return b;
    }

    private static VBox group(String title, ProgressBar randomBar, Label randomText, ProgressBar learnedBar,
                              Label learnedText) {
        Label t = new Label(title);
        t.getStyleClass().add("section-label");
        return new VBox(2, t, row("Random", randomBar, randomText), row("Q-learning", learnedBar, learnedText));
    }

    private static HBox row(String name, ProgressBar bar, Label value) {
        Label n = new Label(name);
        n.setMinWidth(78);
        value.getStyleClass().add("mono");
        value.setMinWidth(56);
        value.setAlignment(Pos.CENTER_RIGHT);
        HBox row = new HBox(10, n, bar, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
