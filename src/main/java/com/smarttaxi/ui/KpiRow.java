package com.smarttaxi.ui;

import java.util.Locale;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Four headline numbers: success rate, average reward, average steps per trip and total profit.
 * Each tile compares the Q-learning agent with the Random driver. It only displays numbers.
 */
public class KpiRow extends HBox {

    private final Label success = new Label("-");
    private final Label reward = new Label("-");
    private final Label steps = new Label("-");
    private final Label profit = new Label("-");
    private final Label successSub = new Label("Train the agent to see results");
    private final Label rewardSub = new Label(" ");
    private final Label stepsSub = new Label(" ");
    private final Label profitSub = new Label("Fares minus running cost");

    /** Builds the row with empty values. */
    public KpiRow() {
        super(10);
        getChildren().addAll(
                tile("SUCCESS RATE", success, successSub, "#2DFFD0"),
                tile("AVERAGE REWARD", reward, rewardSub, "#FF9F1C"),
                tile("AVERAGE STEPS PER TRIP", steps, stepsSub, "#37D4FF"),
                tile("AVERAGE PROFIT PER TRIP", profit, profitSub, "#F4F6FF"));
        for (javafx.scene.Node n : getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
        }
    }

    /** The four headline numbers for one driver. successRate is between 0 and 1. */
    public record Values(double successRate, double reward, double steps, double profit) {
    }

    /**
     * Shows the Q-learning numbers with the random driver and the last trip next to them.
     *
     * @param trips number of trips driven in this session (0 while the numbers still come from the evaluation)
     * @param last the trip that just ended, or null before the first trip
     */
    public void update(Values learned, Values random, int trips, Values last) {
        success.setText(String.format(Locale.US, "%.1f%%", learned.successRate() * 100));
        successSub.setText((last == null ? "" : "Last trip: " + (last.successRate() > 0 ? "delivered" : "gave up") + "  |  ")
                + String.format(Locale.US, "Random: %.1f%%", random.successRate() * 100));
        reward.setText(String.format(Locale.US, "%+.2f", learned.reward()));
        rewardSub.setText((last == null ? "" : String.format(Locale.US, "Last trip: %+.0f  |  ", last.reward()))
                + String.format(Locale.US, "Random: %.1f", random.reward()));
        steps.setText(String.format(Locale.US, "%.2f", learned.steps()));
        stepsSub.setText((last == null ? "" : String.format(Locale.US, "Last trip: %.0f  |  ", last.steps()))
                + String.format(Locale.US, "Random: %.1f", random.steps()));
        profit.setText(String.format(Locale.US, "%+,.2f", learned.profit()));
        profitSub.setText(last == null ? "Fares minus running cost"
                : String.format(Locale.US, "Last trip: %+.2f  |  %d trips", last.profit(), trips));
    }

    private static VBox tile(String title, Label value, Label sub, String colour) {
        Label t = new Label(title);
        t.getStyleClass().add("section-label");
        value.getStyleClass().add("kpi-value");
        value.setStyle("-fx-text-fill: " + colour + "; -fx-effect: dropshadow(gaussian, " + colour + ", 14, 0.15, 0, 0);");
        sub.getStyleClass().add("muted");
        VBox box = new VBox(0, t, value, sub);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(6, 14, 6, 14));
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }
}
