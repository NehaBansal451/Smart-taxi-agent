package com.smarttaxi.ui;

import com.smarttaxi.db.DatabaseManager;
import com.smarttaxi.db.TrainingRunDao;
import com.smarttaxi.db.TripDao;
import com.smarttaxi.exception.DataAccessException;
import com.smarttaxi.map.MapFileHandler;
import com.smarttaxi.service.HistoryService;

import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * Main window content: Simulation, Training Dashboard and History tabs.
 * It only connects the screens; training, storage and game logic live in other packages.
 */
public class MainView extends TabPane {

    private static final String DB_URL = "jdbc:sqlite:data/taxi.db";
    private static final int SAMPLE_EVERY = 50;
    private static final double DEFAULT_ALPHA = 0.1;
    private static final double DEFAULT_GAMMA = 0.99;
    private static final double EPSILON_START = 1.0;

    /** Opens the database and builds all tabs. */
    public MainView() throws DataAccessException {
        DatabaseManager db = new DatabaseManager(DB_URL);
        HistoryService history = new HistoryService(new TrainingRunDao(db), new TripDao(db), SAMPLE_EVERY);

        DashboardView dashboard = new DashboardView();
        SimulationView simulation = new SimulationView(dashboard.getMap(), dashboard::getAgent);
        HistoryView historyView = new HistoryView(history, MapFileHandler::parse);

        dashboard.setRunSaver(results -> {
            try {
                int id = history.saveRun("Q-learning", results, DEFAULT_ALPHA, DEFAULT_GAMMA, EPSILON_START,
                        dashboard.getAgent().getEpsilon(), dashboard.getMap(), "");
                historyView.refresh();
                new Alert(Alert.AlertType.INFORMATION, "Saved as run " + id).showAndWait();
            } catch (DataAccessException e) {
                new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
            }
        });

        Tab simulationTab = new Tab("Simulation", simulation);
        Tab dashboardTab = new Tab("Training Dashboard", dashboard);
        Tab historyTab = new Tab("History", historyView);
        Tab mapTab = new Tab("Map Editor", new MapEditorView(dashboard.getMap(), null)); getTabs().addAll(simulationTab, dashboardTab, historyTab, mapTab);
        setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
        getSelectionModel().selectedItemProperty().addListener((o, oldTab, newTab) -> {
            if (newTab == historyTab) {
                historyView.refresh();
            }
        });
    }
}