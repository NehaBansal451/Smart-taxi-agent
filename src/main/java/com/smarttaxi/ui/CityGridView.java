package com.smarttaxi.ui;

import java.util.function.Consumer;

import com.smarttaxi.map.CityMap;
import com.smarttaxi.model.Position;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Draws a {@link CityMap} as a glowing night-city grid and shows the taxi, passenger and destination.
 * The taxi is a separate node that glides from cell to cell and turns to face its direction.
 * Reused by the simulation tab, the replay screen and the map editor.
 * It only displays state; it contains no game or learning logic.
 */
public class CityGridView extends GridPane {

    private static final double GAP = 1;
    private static final double CORNER = 14;

    private static final Color WALL_COLOR = Color.web("#27358F");
    private static final Color STOP_COLOR = Color.web("#5A2A0A");
    private static final Color ROAD_COLOR = Color.web("#18246E");
    private static final Color STOP_LINE = Color.web("#FF9F1C");
    private static final Color WINDOW_LIT = Color.web("#FFD166");
    private static final Color WINDOW_COOL = Color.web("#37D4FF");
    private static final Color CAR_COLOR = Color.web("#FF9F1C");
    private static final Color PASSENGER_COLOR = Color.web("#37D4FF");
    private static final Color DEST_COLOR = Color.web("#FF2E97");

    private final CityMap map;
    private final double cellSize;
    private final double carLength;
    private final double carWidth;
    private final Cell[][] cells;
    private final DoubleProperty pulse = new SimpleDoubleProperty(0);
    private final Timeline pulseAnimation;
    private final Group car = new Group();
    private Timeline moveAnimation;
    private double moveMillis = 300;
    private double heading;
    private boolean laidOut;
    private Position taxi;
    private Position passenger;
    private Position destination;
    private Consumer<Position> cellClickHandler;

    /** Creates a view for the given map. */
    public CityGridView(CityMap map) {
        this.map = map;
        int size = map.getSize();
        this.cellSize = size <= 7 ? 50 : size <= 10 ? 44 : 36;
        this.carLength = cellSize * 0.62;
        this.carWidth = cellSize * 0.37;
        this.cells = new Cell[size][size];
        setHgap(GAP);
        setVgap(GAP);
        setAlignment(Pos.CENTER);
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Position p = new Position(r, c);
                Cell cell = new Cell(pulse, cellSize);
                cell.setOnMouseClicked(e -> {
                    if (cellClickHandler != null) {
                        cellClickHandler.accept(p);
                    }
                });
                cells[r][c] = cell;
                add(cell, c, r);
            }
        }
        buildCar();
        getChildren().add(car);
        pulseAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(pulse, 0.0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(900), new KeyValue(pulse, 1.0, Interpolator.EASE_BOTH)));
        pulseAnimation.setAutoReverse(true);
        pulseAnimation.setCycleCount(Animation.INDEFINITE);
        sceneProperty().addListener((o, oldScene, newScene) -> {
            if (newScene == null) {
                pulseAnimation.pause();
            } else {
                pulseAnimation.play();
            }
        });
        refresh();
    }

    /** Moves the taxi marker with a short glide. Pass null to hide it. */
    public void setTaxi(Position taxi) {
        Position old = this.taxi;
        this.taxi = taxi;
        car.setVisible(taxi != null);
        if (taxi != null && laidOut) {
            boolean oneStep = old != null
                    && Math.abs(old.getRow() - taxi.getRow()) + Math.abs(old.getCol() - taxi.getCol()) == 1;
            moveCarTo(old, taxi, oneStep);
        }
    }

    /** Stops any glide and puts the taxi exactly on its cell. Called when a trip ends so it never stops short. */
    public void settle() {
        place();
        javafx.animation.PauseTransition later = new javafx.animation.PauseTransition(Duration.millis(120));
        later.setOnFinished(e -> place());
        later.play();
    }

    private void place() {
        if (taxi != null && laidOut) {
            if (moveAnimation != null) {
                moveAnimation.stop();
            }
            moveCarTo(taxi, taxi, false);
            car.setVisible(false);
            car.setVisible(true);
        }
    }

    /** The cell the taxi marker is currently set to, for checking. */
    public Position getTaxi() {
        return taxi;
    }

    /** Sets how long the taxi takes to glide from one cell to the next. Use 0 to jump. */
    public void setMoveMillis(double millis) {
        this.moveMillis = Math.max(0, millis);
    }

    /** Sets where the passenger waits. Pass null while the passenger is inside the taxi. */
    public void setPassenger(Position passenger) {
        if (java.util.Objects.equals(this.passenger, passenger)) {
            return;
        }
        this.passenger = passenger;
        refresh();
    }

    /** Sets the destination marker. Pass null to hide it. */
    public void setDestination(Position destination) {
        if (java.util.Objects.equals(this.destination, destination)) {
            return;
        }
        this.destination = destination;
        refresh();
    }

    /** Registers a handler that receives the clicked cell position. */
    public void setCellClickHandler(Consumer<Position> handler) {
        this.cellClickHandler = handler;
    }

    /** Overlay hook for the heatmap: tints one cell and shows a short text such as an arrow. */
    public void setCellOverlay(Position p, Color tint, String text) {
        if (map.isInside(p)) {
            Cell cell = cells[p.getRow()][p.getCol()];
            cell.heat.setFill(tint == null ? Color.TRANSPARENT : tint);
            String shown = text == null ? "" : text;
            if (!shown.equals(cell.hint.getText())) {
                cell.hint.setText(shown);
            }
        }
    }

    /** Shows a small name (such as Airport) at the bottom of a stop cell. Pass null to remove it. */
    public void setStopName(Position p, String name) {
        if (map.isInside(p)) {
            cells[p.getRow()][p.getCol()].stopLabel.setText(name == null ? "" : name);
        }
    }

    /** Removes every overlay tint and text. */
    public void clearOverlay() {
        for (Cell[] row : cells) {
            for (Cell cell : row) {
                cell.heat.setFill(Color.TRANSPARENT);
                cell.hint.setText("");
            }
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        laidOut = true;
        if (taxi != null && (moveAnimation == null || moveAnimation.getStatus() != Animation.Status.RUNNING)) {
            moveCarTo(taxi, taxi, false);
        }
    }

    private void refresh() {
        for (int r = 0; r < map.getSize(); r++) {
            for (int c = 0; c < map.getSize(); c++) {
                Position p = new Position(r, c);
                Cell cell = cells[r][c];
                boolean wall = map.isWall(p);
                boolean stop = map.getStops().contains(p);
                cell.setKind(wall, stop);
                cell.passengerRing.setVisible(p.equals(passenger));
                cell.passengerBadge.setVisible(p.equals(passenger));
                cell.destRing.setVisible(p.equals(destination));
                cell.destBadge.setVisible(p.equals(destination));
            }
        }
    }

    /**
     * Draws a top-down yellow cab once, turns it into a picture and moves that picture around.
     * A bitmap is much cheaper to animate than live shapes with blur effects.
     */
    private void buildCar() {
        double k = carLength / 40.0;
        double pad = 8 * k;
        double l = carLength;
        double w = carWidth;
        Rectangle bounds = new Rectangle(-pad, -pad, l + 2 * pad, w + 2 * pad);
        bounds.setFill(Color.TRANSPARENT);
        Group g = new Group(bounds);
        // wheels
        for (double wx : new double[] {0.14 * l, 0.68 * l}) {
            for (double wy : new double[] {-1.5 * k, w - 4.5 * k}) {
                Rectangle wheel = new Rectangle(wx, wy, 0.18 * l, 6 * k);
                wheel.setArcWidth(4 * k);
                wheel.setArcHeight(4 * k);
                wheel.setFill(Color.web("#0B0B12"));
                g.getChildren().add(wheel);
            }
        }
        // mirrors
        for (double my : new double[] {-3 * k, w - 1 * k}) {
            Rectangle mirror = new Rectangle(0.62 * l, my, 4 * k, 4 * k);
            mirror.setArcWidth(2 * k);
            mirror.setArcHeight(2 * k);
            mirror.setFill(Color.web("#E0A100"));
            g.getChildren().add(mirror);
        }
        Rectangle body = new Rectangle(l, w);
        body.setArcWidth(16 * k);
        body.setArcHeight(16 * k);
        body.setFill(new javafx.scene.paint.LinearGradient(0, 0, 0, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#FFE14D")),
                new javafx.scene.paint.Stop(0.5, Color.web("#FFC400")),
                new javafx.scene.paint.Stop(1, Color.web("#D99A00"))));
        body.setStroke(Color.web("#5A3D00"));
        body.setStrokeWidth(1.2);
        // bonnet and boot lines
        Rectangle bonnet = new Rectangle(0.74 * l, 3 * k, 0.2 * l, w - 6 * k);
        bonnet.setArcWidth(8 * k);
        bonnet.setArcHeight(8 * k);
        bonnet.setFill(Color.web("#FFFFFF", 0.14));
        // glass: windscreen, roof cabin, rear window
        Rectangle windscreen = new Rectangle(0.56 * l, 3 * k, 0.17 * l, w - 6 * k);
        windscreen.setArcWidth(6 * k);
        windscreen.setArcHeight(6 * k);
        windscreen.setFill(Color.web("#1B2A4A"));
        windscreen.setStroke(Color.web("#7FB2FF", 0.7));
        windscreen.setStrokeWidth(0.8);
        Rectangle roof = new Rectangle(0.28 * l, 2.5 * k, 0.28 * l, w - 5 * k);
        roof.setArcWidth(8 * k);
        roof.setArcHeight(8 * k);
        roof.setFill(Color.web("#F2B705"));
        roof.setStroke(Color.web("#8A6200"));
        roof.setStrokeWidth(0.8);
        Rectangle rearGlass = new Rectangle(0.14 * l, 3.5 * k, 0.13 * l, w - 7 * k);
        rearGlass.setArcWidth(6 * k);
        rearGlass.setArcHeight(6 * k);
        rearGlass.setFill(Color.web("#16213D"));
        // roof sign
        Rectangle sign = new Rectangle(0.34 * l, w / 2 - 4 * k, 0.16 * l, 8 * k);
        sign.setArcWidth(3 * k);
        sign.setArcHeight(3 * k);
        sign.setFill(Color.web("#FFFDF0"));
        sign.setStroke(Color.web("#222222"));
        sign.setStrokeWidth(0.8);
        javafx.scene.text.Text taxiText = new javafx.scene.text.Text("TAXI");
        taxiText.setFont(Font.font("System", FontWeight.BOLD, Math.max(5, 5.2 * k)));
        taxiText.setFill(Color.web("#111111"));
        taxiText.setX(0.34 * l + (0.16 * l - taxiText.getLayoutBounds().getWidth()) / 2);
        taxiText.setY(w / 2 + taxiText.getLayoutBounds().getHeight() / 4);
        // lights
        Group lights = new Group();
        for (double ly : new double[] {1.5 * k, w - 4.5 * k}) {
            Rectangle head = new Rectangle(l - 3.5 * k, ly, 3.5 * k, 3 * k);
            head.setArcWidth(2 * k);
            head.setArcHeight(2 * k);
            head.setFill(Color.web("#FFFBD0"));
            Rectangle tail = new Rectangle(0, ly, 2.5 * k, 3 * k);
            tail.setFill(Color.web("#E5233A"));
            lights.getChildren().addAll(head, tail);
        }
        g.getChildren().addAll(body, bonnet, rearGlass, roof, windscreen, sign, taxiText, lights);
        g.setEffect(new DropShadow(6 * k, 0, 2 * k, Color.web("#000000", 0.7)));

        double scale = 2.0;
        javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setTransform(javafx.scene.transform.Transform.scale(scale, scale));
        javafx.scene.image.Image image = g.snapshot(params, null);
        javafx.scene.image.ImageView view = new javafx.scene.image.ImageView(image);
        view.setFitWidth(l + 2 * pad);
        view.setFitHeight(w + 2 * pad);
        view.setSmooth(true);
        view.setPreserveRatio(false);
        view.setLayoutX(-pad);
        view.setLayoutY(-pad);
        car.getChildren().add(view);
        car.setManaged(false);
        car.setMouseTransparent(true);
        car.setVisible(false);
    }

    /** Places the car on the cell of {@code to}; glides there when {@code animate} is true. */
    private void moveCarTo(Position from, Position to, boolean animate) {
        Cell cell = cells[to.getRow()][to.getCol()];
        double x = cell.getLayoutX() + (cellSize - carLength) / 2.0;
        double y = cell.getLayoutY() + (cellSize - carWidth) / 2.0;
        double target = heading;
        if (from != null && !from.equals(to)) {
            target = headingFor(from, to);
        }
        double turn = (((target - heading + 540.0) % 360.0) + 360.0) % 360.0 - 180.0;
        double newHeading = heading + turn;
        heading = newHeading;
        if (moveAnimation != null) {
            moveAnimation.stop();
        }
        car.setLayoutX(0);
        car.setLayoutY(0);
        if (!animate || moveMillis <= 0 || !car.isVisible()) {
            car.setTranslateX(x);
            car.setTranslateY(y);
            car.setRotate(newHeading);
            return;
        }
        Duration d = Duration.millis(moveMillis);
        moveAnimation = new Timeline(new KeyFrame(d,
                new KeyValue(car.translateXProperty(), x, Interpolator.LINEAR),
                new KeyValue(car.translateYProperty(), y, Interpolator.LINEAR),
                new KeyValue(car.rotateProperty(), newHeading, Interpolator.EASE_BOTH)));
        moveAnimation.play();
    }

    private static double headingFor(Position from, Position to) {
        if (to.getCol() > from.getCol()) {
            return 0;
        }
        if (to.getRow() > from.getRow()) {
            return 90;
        }
        if (to.getCol() < from.getCol()) {
            return 180;
        }
        return 270;
    }

    /** One square of the grid: base, building windows, heat tint, arrow text, rings and badges. */
    private static final class Cell extends StackPane {
        private final Rectangle base;
        private final Group windows = new Group();
        private final Rectangle heat;
        private final Label hint = new Label();
        private final Label stopLabel = new Label();
        private final Circle passengerRing;
        private final Rectangle destRing;
        private final Label passengerBadge = badge("P", PASSENGER_COLOR, 12);
        private final Label destBadge = badge("D", DEST_COLOR, 6);

        Cell(DoubleProperty pulse, double size) {
            base = new Rectangle(size, size);
            heat = new Rectangle(size, size, Color.TRANSPARENT);
            passengerRing = new Circle(size / 2.0 - 8);
            destRing = new Rectangle(size - 14, size - 14);
            base.setArcWidth(CORNER);
            base.setArcHeight(CORNER);
            heat.setArcWidth(CORNER);
            heat.setArcHeight(CORNER);
            double w = size * 0.22;
            for (int i = 0; i < 4; i++) {
                Rectangle win = new Rectangle(w, w);
                win.setArcWidth(4);
                win.setArcHeight(4);
                win.setFill(i == 1 ? WINDOW_COOL : WINDOW_LIT);
                win.setLayoutX(i % 2 == 0 ? size * 0.19 : size * 0.59);
                win.setLayoutY(i < 2 ? size * 0.19 : size * 0.59);
                windows.getChildren().add(win);
            }
            windows.setMouseTransparent(true);
            stopLabel.setFont(Font.font("System", FontWeight.BOLD, Math.max(9, size * 0.15)));
            stopLabel.setTextFill(STOP_LINE);
            stopLabel.setMouseTransparent(true);
            StackPane.setAlignment(stopLabel, Pos.BOTTOM_CENTER);
            StackPane.setMargin(stopLabel, new javafx.geometry.Insets(0, 0, 3, 0));
            hint.setFont(Font.font("System", FontWeight.BOLD, size * 0.42));
            hint.setTextFill(Color.WHITE);
            hint.setEffect(new DropShadow(6, Color.BLACK));
            hint.setMouseTransparent(true);
            passengerRing.setFill(Color.TRANSPARENT);
            passengerRing.setStroke(PASSENGER_COLOR);
            passengerRing.setStrokeWidth(4);
            passengerRing.opacityProperty().bind(pulse.multiply(0.6).add(0.4));
            passengerRing.setMouseTransparent(true);
            destRing.setArcWidth(12);
            destRing.setArcHeight(12);
            destRing.setFill(Color.TRANSPARENT);
            destRing.setStroke(DEST_COLOR);
            destRing.setStrokeWidth(4);
            destRing.opacityProperty().bind(pulse.multiply(0.6).add(0.4));
            destRing.setMouseTransparent(true);
            passengerRing.setVisible(false);
            destRing.setVisible(false);
            passengerBadge.setVisible(false);
            destBadge.setVisible(false);
            StackPane.setAlignment(passengerBadge, Pos.TOP_LEFT);
            StackPane.setAlignment(destBadge, Pos.TOP_RIGHT);
            setMinSize(size, size);
            setMaxSize(size, size);
            getChildren().addAll(base, windows, heat, passengerRing, destRing, hint, stopLabel, passengerBadge, destBadge);
            windows.setManaged(false);
            windows.setVisible(false);
        }

        void setKind(boolean wall, boolean stop) {
            windows.setVisible(wall);
            if (wall) {
                base.setFill(WALL_COLOR);
                base.setStroke(null);
            } else if (stop) {
                base.setFill(STOP_COLOR);
                base.setStroke(STOP_LINE);
                base.setStrokeWidth(2);
            } else {
                base.setFill(ROAD_COLOR);
                base.setStroke(null);
            }
        }

        private static Label badge(String text, Color colour, double radius) {
            Label l = new Label(text);
            l.setFont(Font.font("System", FontWeight.BOLD, 12));
            l.setTextFill(Color.web("#05081C"));
            l.setMinSize(22, 22);
            l.setMaxSize(22, 22);
            l.setAlignment(Pos.CENTER);
            l.setStyle("-fx-background-color: " + toHex(colour) + "; -fx-background-radius: " + radius + ";");
            l.setEffect(new DropShadow(8, colour));
            l.setMouseTransparent(true);
            StackPane.setMargin(l, new javafx.geometry.Insets(3));
            return l;
        }

        private static String toHex(Color c) {
            return String.format("#%02X%02X%02X", (int) Math.round(c.getRed() * 255),
                    (int) Math.round(c.getGreen() * 255), (int) Math.round(c.getBlue() * 255));
        }
    }
}
