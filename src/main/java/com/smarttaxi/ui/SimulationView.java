package com.smarttaxi.ui;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.smarttaxi.agent.Agent;
import com.smarttaxi.agent.EvaluationSummary;
import com.smarttaxi.agent.Evaluator;
import com.smarttaxi.agent.QLearningAgent;
import com.smarttaxi.agent.RandomAgent;
import com.smarttaxi.env.GridWorld;
import com.smarttaxi.env.StepResult;
import com.smarttaxi.exception.InvalidMoveException;
import com.smarttaxi.fare.PeakHourFare;
import com.smarttaxi.fare.StandardFare;
import com.smarttaxi.map.CityMap;
import com.smarttaxi.model.Action;
import com.smarttaxi.model.Position;
import com.smarttaxi.model.State;
import com.smarttaxi.service.EpisodeResult;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Simulation tab: book a ride between two stops, then watch the Random or the Q-learning agent drive it.
 * Shows the live trip (steps, reward, epsilon), the fare breakdown and an event log.
 * It only displays the environment; all game and learning logic stays in env and agent.
 */
public class SimulationView extends BorderPane {

    private static final String RANDOM = "Random";
    private static final String LEARNED = "Q-learning";
    private static final String[] STOP_NAMES = {"Airport", "Mall", "Station", "Hospital", "Park", "School", "Stadium", "Market"};
    private static final String GOOD = "#2DFFD0";
    private static final String BAD = "#FF2E97";
    private static final String INFO = "#37D4FF";
    private static final String WARM = "#FF9F1C";
    private static final String MUTED = "#A9B6F0";
    private static final double MOVE_SHARE = 1.0;
    private static final int LOG_LINES = 6;

    private final StandardFare standard = new StandardFare();
    private final PeakHourFare peak = new PeakHourFare(standard, 1.5);
    private final Supplier<Agent> learnedAgent;
    private final Agent randomAgent = new RandomAgent(new Random());

    private GridWorld env;
    private CityGridView grid;
    private final StackPane gridHolder = new StackPane();
    private List<Position> stops = new ArrayList<>();

    private final ToggleGroup pickupGroup = new ToggleGroup();
    private final ToggleGroup destGroup = new ToggleGroup();
    private final FlowPane pickupChips = new FlowPane(6, 6);
    private final FlowPane destChips = new FlowPane(6, 6);
    private final Label bubble = new Label();

    private final ToggleGroup agentGroup = new ToggleGroup();
    private final ToggleButton randomButton = new ToggleButton(RANDOM);
    private final ToggleButton learnedButton = new ToggleButton(LEARNED);
    private final ToggleButton rushButton = new ToggleButton("Rush hour x1.5");
    private final ToggleButton heatmapButton = new ToggleButton("Show heatmap");
    private final Button bookButton = new Button("Book ride");
    private final Button runButton = new Button("Run trip");
    private final Button stepButton = new Button("Step");
    private final Button newTripButton = new Button("New trip");
    private final Button trainButton = new Button("Retrain");
    private Consumer<Runnable> trainer;
    private boolean training;
    private final Slider delay = new Slider(80, 800, 320);
    private final Label delayLabel = new Label();

    private final Label phaseLabel = new Label("WAITING");
    private final Label stepValue = new Label("0 / " + GridWorld.DEFAULT_MAX_STEPS);
    private final Label rewardValue = new Label("0");
    private final Label epsilonValue = new Label("0.00");
    private final ProgressBar progress = new ProgressBar(0);
    private final Label baseValue = new Label();
    private final Label cellsKey = new Label();
    private final Label cellsValue = new Label();
    private final Label multValue = new Label();
    private final Label fareValue = new Label();
    private final Label costKey = new Label();
    private final Label costValue = new Label();
    private final Label profitValue = new Label();
    private final Label fareTitle = new Label();
    private final VBox log = new VBox(6);

    private Timeline timeline;
    private boolean tripActive;
    private boolean tripEnded;
    private boolean tripSuccess;
    private boolean pickedUp;
    private int tripDistance;
    private int startDistance;
    private double totalReward;
    private boolean rebuilding;
    private int rides;
    private KpiRow.Values lastTrip;
    private final Stats learnedStats = new Stats();
    private final Stats randomStats = new Stats();
    private KpiRow.Values baseLearned = new KpiRow.Values(0, 0, 0, 0);
    private KpiRow.Values baseRandom = new KpiRow.Values(0, 0, 0, 0);
    private Object heatKey;
    private int trainedVersion;
    private double earnings;
    private Supplier<List<EpisodeResult>> results;
    private final KpiRow kpis = new KpiRow();
    private final LearningCurveCard curve = new LearningCurveCard();
    private final ComparisonCard comparison = new ComparisonCard();
    private final Label ridesLabel = new Label("0");
    private final Label earningsLabel = new Label("0.00");

    /**
     * Creates the tab.
     *
     * @param map city to show
     * @param learnedAgent supplies the Q-learning agent each time it is needed
     */
    public SimulationView(CityMap map, Supplier<Agent> learnedAgent) {
        this.learnedAgent = learnedAgent;
        this.env = new GridWorld(map, new Random());
        this.env.reset();
        this.grid = new CityGridView(map);
        buildUi();
        loadMap(map);
    }

    /** Shows another city: builds a new environment and grid, then waits for a new booking. */
    public void setMap(CityMap map) {
        stopTimeline();
        learnedStats.clear();
        randomStats.clear();
        lastTrip = null;
        this.env = new GridWorld(map, new Random());
        this.env.reset();
        this.grid = new CityGridView(map);
        loadMap(map);
    }

    // ---------------------------------------------------------------- building the screen

    private void buildUi() {
        randomButton.setToggleGroup(agentGroup);
        learnedButton.setToggleGroup(agentGroup);
        randomButton.getStyleClass().add("segment");
        learnedButton.getStyleClass().add("segment");
        randomButton.setMaxWidth(Double.MAX_VALUE);
        learnedButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(randomButton, Priority.ALWAYS);
        HBox.setHgrow(learnedButton, Priority.ALWAYS);
        learnedButton.setSelected(true);
        agentGroup.selectedToggleProperty().addListener((o, oldT, newT) -> {
            if (newT == null) {
                oldT.setSelected(true);
            } else {
                refreshAll();
            }
        });
        pickupGroup.selectedToggleProperty().addListener((o, oldT, newT) -> onSelectionChanged(oldT, newT));
        destGroup.selectedToggleProperty().addListener((o, oldT, newT) -> onSelectionChanged(oldT, newT));
        rushButton.setSelected(true);
        heatmapButton.setSelected(true);
        rushButton.setOnAction(e -> updateFare());
        heatmapButton.setOnAction(e -> updateHeatmap(context()));
        trainButton.setOnAction(e -> trainAgent());

        bookButton.getStyleClass().add("primary");
        runButton.getStyleClass().add("primary");
        bookButton.setOnAction(e -> book(true));
        runButton.setOnAction(e -> onRun());
        stepButton.setOnAction(e -> onStep());
        newTripButton.setOnAction(e -> onNewTrip());

        delay.valueProperty().addListener((o, a, b) -> onSpeedChanged());
        progress.setMaxWidth(Double.MAX_VALUE);
        bubble.setWrapText(true);
        bubble.getStyleClass().add("bubble");
        bubble.setMaxWidth(Double.MAX_VALUE);
        bubble.setMinHeight(Region.USE_PREF_SIZE);

        // left: the city
        Label caption = new Label("Heatmap: dark = low value, bright = high value. Arrows = preferred move.");
        caption.getStyleClass().add("muted");
        caption.setWrapText(true);
        caption.setMaxWidth(440);
        VBox cityCard = new VBox(8, gridHolder, caption);
        cityCard.getStyleClass().add("card");
        cityCard.setAlignment(Pos.CENTER);
        cityCard.setMinWidth(Region.USE_PREF_SIZE);

        // middle column: booking and controls
        VBox controls = controlsCard();
        VBox.setVgrow(controls, Priority.ALWAYS);
        VBox middle = new VBox(10, bookingCard(), controls);
        middle.setPrefWidth(340);
        middle.setMinWidth(270);

        // right column: live trip and log
        VBox right = new VBox(10, liveTripCard(), logCard());
        right.setPrefWidth(340);
        right.setMinWidth(270);

        HBox main = new HBox(12, cityCard, middle, right);
        HBox.setHgrow(middle, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        middle.setMaxWidth(Double.MAX_VALUE);
        right.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(main, Priority.ALWAYS);

        comparison.setMinWidth(300);
        comparison.setPrefWidth(400);
        HBox.setHgrow(curve, Priority.ALWAYS);
        curve.setMaxWidth(Double.MAX_VALUE);
        HBox lower = new HBox(12, curve, comparison);

        VBox all = new VBox(10, header(), kpis, main, lower);
        all.setPadding(new Insets(8, 12, 10, 12));
        ScrollPane scroll = new ScrollPane(all);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPannable(true);
        setCenter(scroll);
    }

    private HBox header() {
        Rectangle badge = new Rectangle(30, 30);
        badge.setArcWidth(10);
        badge.setArcHeight(10);
        badge.setFill(Color.web("#FF9F1C"));
        Rectangle car = new Rectangle(16, 9);
        car.setArcWidth(5);
        car.setArcHeight(5);
        car.setFill(Color.web("#05081C"));
        StackPane logo = new StackPane(badge, car);
        Label name = new Label("SMART TAXI AGENT");
        name.getStyleClass().add("card-title");
        Label tag = new Label("Book a ride, watch the agent drive it, then see what it learned");
        tag.getStyleClass().add("muted");
        VBox titles = new VBox(2, name, tag);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label ridesKey = new Label("RIDES TODAY");
        ridesKey.getStyleClass().add("section-label");
        Label earnKey = new Label("EARNINGS");
        earnKey.getStyleClass().add("section-label");
        ridesLabel.getStyleClass().add("stat-value");
        earningsLabel.getStyleClass().add("stat-value");
        earningsLabel.setStyle("-fx-text-fill: " + GOOD + ";");
        HBox counters = new HBox(22, new HBox(8, ridesKey, ridesLabel), new HBox(8, earnKey, earningsLabel));
        counters.setAlignment(Pos.CENTER_RIGHT);
        HBox header = new HBox(12, logo, titles, spacer, counters);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private VBox bookingCard() {
        Label title = new Label("New ride request");
        title.getStyleClass().add("card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bookButton.getStyleClass().add("small-button");
        HBox top = new HBox(title, spacer, bookButton);
        top.setAlignment(Pos.CENTER_LEFT);
        Label pickupKey = sectionLabel("PICKUP");
        pickupKey.setMinWidth(78);
        Label destKey = sectionLabel("DESTINATION");
        destKey.setMinWidth(78);
        HBox pickupRow = new HBox(6, pickupKey, pickupChips);
        HBox destRow = new HBox(6, destKey, destChips);
        HBox.setHgrow(pickupChips, Priority.ALWAYS);
        HBox.setHgrow(destChips, Priority.ALWAYS);
        pickupRow.setAlignment(Pos.CENTER_LEFT);
        destRow.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(6, top, bubble, pickupRow, destRow);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox controlsCard() {
        Label title = new Label("Controls");
        title.getStyleClass().add("card-title");
        HBox segmented = new HBox(6, learnedButton, randomButton);
        for (javafx.scene.control.ButtonBase b : new javafx.scene.control.ButtonBase[] {runButton, stepButton,
                newTripButton, trainButton, rushButton, heatmapButton}) {
            b.getStyleClass().add("small-button");
        }
        HBox buttons = new HBox(6, runButton, stepButton, newTripButton, trainButton);
        HBox options = new HBox(6, rushButton, heatmapButton);
        HBox speed = new HBox(10, new Label("Delay"), delay, delayLabel);
        speed.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(delay, Priority.ALWAYS);
        delayLabel.getStyleClass().add("mono");
        delayLabel.setMinWidth(54);
        VBox card = new VBox(6, title, segmented, buttons, options, speed);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox liveTripCard() {
        Label title = new Label("Live trip");
        title.getStyleClass().add("card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        phaseLabel.getStyleClass().add("mono");
        HBox header = new HBox(title, spacer, phaseLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox tiles = new HBox(8, tile("STEP", stepValue), tile("REWARD", rewardValue), tile("EPSILON", epsilonValue));
        for (javafx.scene.Node n : tiles.getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
        }
        rewardValue.setStyle("-fx-text-fill: " + BAD + ";");

        fareTitle.getStyleClass().add("section-label");
        GridPane rows = new GridPane();
        rows.setHgap(8);
        rows.setVgap(4);
        ColumnConstraints keyCol = new ColumnConstraints();
        keyCol.setHgrow(Priority.ALWAYS);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setMinWidth(Region.USE_PREF_SIZE);
        valueCol.setHalignment(HPos.RIGHT);
        rows.getColumnConstraints().addAll(keyCol, valueCol);
        addRow(rows, 0, new Label("Base fare"), baseValue, true);
        addRow(rows, 1, cellsKey, cellsValue, true);
        addRow(rows, 2, new Label("Peak multiplier"), multValue, true);
        addRow(rows, 3, new Label("Fare"), fareValue, false);
        addRow(rows, 4, costKey, costValue, true);
        addRow(rows, 5, new Label("Profit"), profitValue, false);
        fareValue.setStyle("-fx-text-fill: " + WARM + ";");
        profitValue.getStyleClass().add("profit-value");

        VBox card = new VBox(8, header, tiles, progress, fareTitle, rows);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox logCard() {
        VBox card = new VBox(8, sectionLabel("EVENT LOG"), log);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private static void addRow(GridPane rows, int row, Label key, Label value, boolean muted) {
        key.getStyleClass().add("mono");
        value.getStyleClass().add("mono");
        if (muted) {
            key.setStyle("-fx-text-fill: " + MUTED + ";");
        }
        GridPane.setHalignment(value, HPos.RIGHT);
        value.setMinWidth(Region.USE_PREF_SIZE);
        rows.add(key, 0, row);
        rows.add(value, 1, row);
    }

    private static VBox tile(String name, Label value) {
        Label l = new Label(name);
        l.getStyleClass().add("stat-label");
        value.getStyleClass().add("stat-value");
        VBox box = new VBox(2, l, value);
        box.getStyleClass().add("stat-tile");
        return box;
    }

    private static Label sectionLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-label");
        return l;
    }

    // ---------------------------------------------------------------- training

    /** Supplies the results of the last training run, used for the learning curve and the headline numbers. */
    public void setResultsSupplier(Supplier<List<EpisodeResult>> results) {
        this.results = results;
    }

    /** Recomputes the headline numbers, the learning curve and the comparison bars after training. */
    private void refreshInsights() {
        if (results == null) {
            return;
        }
        List<EpisodeResult> run = results.get();
        if (run == null || run.isEmpty()) {
            return;
        }
        try {
            EvaluationSummary learned = Evaluator.evaluate(new GridWorld(env.getMap(), new Random()),
                    learnedAgent.get(), 200);
            EvaluationSummary random = Evaluator.evaluate(new GridWorld(env.getMap(), new Random()),
                    randomAgent, 200);
            int from = run.size() - Math.max(1, run.size() / 5);
            double profitSum = 0;
            for (int i = from; i < run.size(); i++) {
                profitSum += run.get(i).profit();
            }
            baseLearned = new KpiRow.Values(learned.successRate(), learned.averageReward(), learned.averageSteps(),
                    profitSum / (run.size() - from));
            baseRandom = new KpiRow.Values(random.successRate(), random.averageReward(), random.averageSteps(), 0);
            learnedStats.clear();
            showKpis();
            curve.update(run);
            comparison.update(learned, random);
        } catch (Exception e) {
            addLog("Could not evaluate the agent: " + e.getMessage(), BAD);
        }
    }

    /** Running totals of the trips driven in this session by one kind of driver. */
    private static final class Stats {
        private int trips;
        private int successes;
        private double reward;
        private double steps;
        private double profit;

        void add(boolean success, double tripReward, int tripSteps, double tripProfit) {
            trips++;
            if (success) {
                successes++;
            }
            reward += tripReward;
            steps += tripSteps;
            profit += tripProfit;
        }

        void clear() {
            trips = 0;
            successes = 0;
            reward = 0;
            steps = 0;
            profit = 0;
        }

        KpiRow.Values values() {
            return new KpiRow.Values((double) successes / trips, reward / trips, steps / trips, profit / trips);
        }
    }

    /** Shows the numbers of the trips driven so far; before the first trip it shows the evaluation of the agent. */
    private void showKpis() {
        KpiRow.Values learned = learnedStats.trips > 0 ? learnedStats.values() : baseLearned;
        KpiRow.Values random = randomStats.trips > 0 ? randomStats.values() : baseRandom;
        kpis.update(learned, random, learnedStats.trips + randomStats.trips, lastTrip);
    }

    /** Sets how the tab asks for training; the given runnable must be run when training has finished. */
    public void setTrainer(Consumer<Runnable> trainer) {
        this.trainer = trainer;
        trainButton.setDisable(trainer == null);
    }

    /** Trains the Q-learning agent (through the dashboard) so that the taxi can drive and the heatmap has data. */
    public void trainAgent() {
        if (trainer == null || training) {
            return;
        }
        training = true;
        stopTimeline();
        trainButton.setDisable(true);
        trainButton.setText("Training");
        addLog("Training the agent, please wait", WARM);
        trainer.accept(() -> {
            training = false;
            trainButton.setDisable(false);
            trainButton.setText("Retrain");
            trainedVersion++;
            addLog("Agent trained, ready to drive", GOOD);
            refreshInsights();
            refreshAll();
        });
    }

    // ---------------------------------------------------------------- map and booking

    private void loadMap(CityMap map) {
        heatKey = null;
        stops = new ArrayList<>(map.getStops());
        gridHolder.getChildren().setAll(grid);
        for (int i = 0; i < stops.size(); i++) {
            grid.setStopName(stops.get(i), stopName(i));
        }
        rebuilding = true;
        pickupChips.getChildren().clear();
        destChips.getChildren().clear();
        pickupGroup.getToggles().clear();
        destGroup.getToggles().clear();
        for (int i = 0; i < stops.size(); i++) {
            pickupChips.getChildren().add(chip(i, pickupGroup, "chip-pickup"));
            destChips.getChildren().add(chip(i, destGroup, "chip-dest"));
        }
        pickupGroup.getToggles().get(0).setSelected(true);
        destGroup.getToggles().get(Math.min(1, stops.size() - 1)).setSelected(true);
        rebuilding = false;
        tripActive = false;
        tripEnded = false;
        totalReward = 0;
        setDelay();
        show(context());
        refreshAll();
    }

    private ToggleButton chip(int index, ToggleGroup group, String style) {
        ToggleButton b = new ToggleButton(stopName(index));
        b.setToggleGroup(group);
        b.setUserData(stops.get(index));
        b.getStyleClass().add(style);
        return b;
    }

    /** A chip was clicked: keep one chip selected, and prepare the next booking after a finished trip. */
    private void onSelectionChanged(Toggle oldT, Toggle newT) {
        if (rebuilding) {
            return;
        }
        if (newT == null) {
            if (oldT != null) {
                oldT.setSelected(true);
            }
            return;
        }
        if (tripEnded) {
            tripActive = false;
            tripEnded = false;
            totalReward = 0;
            Position pickup = selected(pickupGroup);
            Position dest = selected(destGroup);
            if (pickup != null && dest != null) {
                env.reset(new State(env.getState().getTaxi(), Optional.of(pickup), dest));
                show(env.getState());
            }
        }
        if (!tripActive) {
            refreshAll();
        }
    }

    private String stopName(int index) {
        return index < STOP_NAMES.length ? STOP_NAMES[index] : "Stop " + (index + 1);
    }

    private String nameOf(Position p) {
        int i = stops.indexOf(p);
        return i < 0 ? p.toString() : stopName(i);
    }

    private Position selected(ToggleGroup group) {
        Toggle t = group.getSelectedToggle();
        return t == null ? null : (Position) t.getUserData();
    }

    /** Starts a trip between the chosen stops. The taxi starts where the last trip ended. */
    private boolean book(boolean runNow) {
        stopTimeline();
        Position pickup = selected(pickupGroup);
        Position dest = selected(destGroup);
        if (pickup == null || dest == null || pickup.equals(dest)) {
            return false;
        }
        Position start = env.getState().getTaxi();
        env.reset(new State(start, Optional.of(pickup), dest));
        tripActive = true;
        tripEnded = false;
        tripSuccess = false;
        pickedUp = false;
        totalReward = 0;
        tripDistance = manhattan(pickup, dest);
        startDistance = manhattan(start, pickup) + tripDistance;
        log.getChildren().clear();
        addLog("Ride booked: heading to " + nameOf(pickup), WARM);
        if (learnedButton.isSelected() && learnedAgent.get().getEpsilon() > 0.9) {
            addLog("The Q-learning agent is untrained. Press Retrain first", BAD);
        }
        show(env.getState());
        refreshAll();
        if (runNow) {
            startTimeline();
        }
        return true;
    }

    private void onNewTrip() {
        stopTimeline();
        State s = env.reset();
        tripActive = false;
        tripEnded = false;
        pickupGroup.getToggles().get(Math.max(0, stops.indexOf(s.getPassenger().orElse(null)))).setSelected(true);
        destGroup.getToggles().get(Math.max(0, stops.indexOf(s.getDestination()))).setSelected(true);
        addLog("New trip. Waiting for a ride request", MUTED);
        show(s);
        refreshAll();
    }

    // ---------------------------------------------------------------- running a trip

    private void onRun() {
        if (!tripActive || tripEnded) {
            if (!book(false)) {
                return;
            }
        }
        startTimeline();
    }

    private void onStep() {
        if (!tripActive || tripEnded) {
            book(false);
            return;
        }
        stopTimeline();
        advance();
    }

    private void startTimeline() {
        stopTimeline();
        timeline = new Timeline(new KeyFrame(Duration.millis(delay.getValue()), e -> advance()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        refreshButtons();
    }

    private void stopTimeline() {
        if (timeline != null) {
            timeline.stop();
        }
        refreshButtons();
    }

    private boolean running() {
        return timeline != null && timeline.getStatus() == javafx.animation.Animation.Status.RUNNING;
    }

    private void onSpeedChanged() {
        setDelay();
        if (running()) {
            startTimeline();
        }
    }

    private void setDelay() {
        double ms = Math.round(delay.getValue());
        delayLabel.setText((int) ms + " ms");
        grid.setMoveMillis(ms * MOVE_SHARE);
    }

    private void advance() {
        if (env.isDone()) {
            stopTimeline();
            return;
        }
        try {
            Agent agent = currentAgent();
            Action action = agent.chooseAction(env.getState());
            StepResult result = env.step(action);
            totalReward += result.getReward();
            State state = result.getNextState();
            if (!pickedUp && state.isPassengerInTaxi()) {
                pickedUp = true;
                addLog("Passenger on board. Heading to " + nameOf(state.getDestination()), INFO);
            }
            if (result.getReward() <= GridWorld.ILLEGAL_REWARD) {
                addLog("Illegal " + action + " (" + (int) result.getReward() + ")", BAD);
            }
            show(state);
            if (result.isDone()) {
                grid.settle();
                addLog("Taxi cell: env " + state.getTaxi() + ", map " + grid.getTaxi(), MUTED);
                tripEnded = true;
                tripSuccess = result.isSuccess();
                stopTimeline();
                double profit = (tripSuccess ? fare() : 0) - cost();
                (learnedButton.isSelected() ? learnedStats : randomStats)
                        .add(tripSuccess, totalReward, env.getStepCount(), profit);
                lastTrip = new KpiRow.Values(tripSuccess ? 1 : 0, totalReward, env.getStepCount(), profit);
                showKpis();
                if (tripSuccess) {
                    rides++;
                    earnings += profit;
                    ridesLabel.setText(String.valueOf(rides));
                    earningsLabel.setText(money(earnings));
                    addLog("Delivered at " + nameOf(state.getDestination()) + ". Profit " + signed(profit), GOOD);
                } else {
                    addLog("Gave up after " + env.getStepCount() + " steps. No fare", BAD);
                }
            }
            refreshAll();
        } catch (InvalidMoveException ex) {
            stopTimeline();
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    private Agent currentAgent() {
        return learnedButton.isSelected() ? learnedAgent.get() : randomAgent;
    }

    // ---------------------------------------------------------------- showing things

    private void show(State state) {
        grid.setTaxi(state.getTaxi());
        grid.setPassenger(state.getPassenger().orElse(null));
        grid.setDestination(state.getDestination());
        updateHeatmap(state);
    }

    /** The situation the heatmap describes: the chosen stops while idle, the real state during a trip. */
    private State context() {
        State s = env.getState();
        Position pickup = selected(pickupGroup);
        Position dest = selected(destGroup);
        if (!tripActive && pickup != null && dest != null) {
            return new State(s.getTaxi(), Optional.of(pickup), dest);
        }
        return s;
    }

    /**
     * Paints the heatmap. The values only depend on the passenger and the destination, not on where the taxi is,
     * so the colours are recalculated only when those change. Repainting every cell at each step made the taxi stutter.
     */
    private void updateHeatmap(State state) {
        Agent agent = learnedAgent.get();
        if (!heatmapButton.isSelected() || !(agent instanceof QLearningAgent q)) {
            if (heatKey != null) {
                grid.clearOverlay();
                heatKey = null;
            }
            return;
        }
        Object key = List.of(String.valueOf(state.getPassenger().orElse(null)), state.getDestination().toString(),
                System.identityHashCode(agent), trainedVersion);
        if (key.equals(heatKey)) {
            return;
        }
        heatKey = key;
        HeatmapOverlay.apply(grid, env.getMap(), q.getQTable(), state);
    }

    private void refreshAll() {
        Position pickup = selected(pickupGroup);
        Position dest = selected(destGroup);
        if (!tripActive && pickup != null && dest != null) {
            grid.setPassenger(pickup);
            grid.setDestination(dest);
        }
        bubble.setText(pickup == null || dest == null ? "" : "Hi! Please pick me up at " + nameOf(pickup)
                + " and take me to " + nameOf(dest) + ".");
        double eps = learnedButton.isSelected() ? learnedAgent.get().getEpsilon() : 1.0;
        epsilonValue.setText(String.format(Locale.US, "%.2f", eps));
        stepValue.setText(env.getStepCount() + " / " + GridWorld.DEFAULT_MAX_STEPS);
        rewardValue.setText(String.valueOf((int) totalReward));
        if (!tripActive) {
            updateHeatmap(context());
        }
        updatePhase();
        updateProgress();
        updateFare();
        refreshButtons();
    }

    private void refreshButtons() {
        boolean busy = tripActive && !tripEnded;
        boolean same = selected(pickupGroup) != null && selected(pickupGroup).equals(selected(destGroup));
        pickupChips.setDisable(busy);
        destChips.setDisable(busy);
        randomButton.setDisable(busy);
        learnedButton.setDisable(busy);
        bookButton.setDisable(busy || same);
        runButton.setDisable(running() || same && !busy);
        runButton.setText(busy && !running() ? "Resume" : tripEnded ? "Run again" : "Run trip");
        stepButton.setDisable(running() || same && !busy);
    }

    private void updatePhase() {
        String text;
        String colour;
        if (!tripActive) {
            text = "WAITING";
            colour = MUTED;
        } else if (tripEnded) {
            text = tripSuccess ? "RIDE COMPLETE" : "GAVE UP";
            colour = tripSuccess ? GOOD : BAD;
        } else if (!pickedUp) {
            text = "PICKING UP: " + nameOf(selected(pickupGroup)).toUpperCase(Locale.ROOT);
            colour = GOOD;
        } else {
            text = "DROPPING AT: " + nameOf(selected(destGroup)).toUpperCase(Locale.ROOT);
            colour = INFO;
        }
        phaseLabel.setText(text);
        phaseLabel.setStyle("-fx-text-fill: " + colour + ";");
    }

    private void updateProgress() {
        if (!tripActive) {
            progress.setProgress(0);
        } else if (tripEnded) {
            progress.setProgress(tripSuccess ? 1 : 0);
        } else {
            State s = env.getState();
            int remaining = pickedUp ? manhattan(s.getTaxi(), s.getDestination())
                    : manhattan(s.getTaxi(), s.getPassenger().orElse(s.getTaxi())) + tripDistance;
            progress.setProgress(Math.max(0, Math.min(1, 1.0 - remaining / (double) Math.max(1, startDistance))));
        }
    }

    // ---------------------------------------------------------------- fare breakdown

    private LocalTime pickupTime() {
        return rushButton.isSelected() ? LocalTime.of(18, 0) : LocalTime.of(12, 0);
    }

    private int previewDistance() {
        Position p = selected(pickupGroup);
        Position d = selected(destGroup);
        return tripActive || p == null || d == null ? tripDistance : manhattan(p, d);
    }

    private double fare() {
        return peak.calculateFare(previewDistance(), pickupTime());
    }

    private double cost() {
        return peak.calculateCost(env.getStepCount());
    }

    private void updateFare() {
        boolean rush = rushButton.isSelected();
        LocalTime t = pickupTime();
        int d = previewDistance();
        double base = standard.calculateFare(0, t);
        double perCell = standard.calculateFare(1, t) - base;
        double shownFare = tripEnded && !tripSuccess ? 0 : fare();
        double profit = shownFare - cost();
        fareTitle.setText(rush ? "FARE BREAKDOWN (evening peak x1.5)" : "FARE BREAKDOWN (off peak)");
        baseValue.setText(money(base));
        cellsKey.setText(d + " cells x " + money(perCell));
        cellsValue.setText(money(perCell * d));
        multValue.setText("x " + (rush ? "1.5" : "1.0"));
        fareValue.setText(money(shownFare));
        costKey.setText("Cost: " + env.getStepCount() + " steps x " + money(peak.calculateCost(1)));
        costValue.setText("-" + money(cost()));
        profitValue.setText(signed(profit));
        profitValue.setStyle("-fx-text-fill: " + (profit >= 0 ? GOOD : BAD) + ";");
    }

    // ---------------------------------------------------------------- helpers

    private void addLog(String text, String colour) {
        Label l = new Label(text);
        l.getStyleClass().add("mono");
        l.setWrapText(true);
        l.setStyle("-fx-text-fill: " + colour + ";");
        log.getChildren().add(0, l);
        while (log.getChildren().size() > LOG_LINES) {
            log.getChildren().remove(log.getChildren().size() - 1);
        }
    }

    private static int manhattan(Position a, Position b) {
        return Math.abs(a.getRow() - b.getRow()) + Math.abs(a.getCol() - b.getCol());
    }

    private static String money(double v) {
        return String.format(Locale.US, "%.2f", v);
    }

    private static String signed(double v) {
        return (v >= 0 ? "+" : "-") + money(Math.abs(v));
    }
}
