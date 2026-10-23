package com.smarttaxi;

import java.util.logging.Logger;

import com.smarttaxi.config.AppConfig;
import com.smarttaxi.config.LoggerUtil;
import com.smarttaxi.exception.InvalidConfigException;
import com.smarttaxi.ui.MainView;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/** Starts the JavaFX window. */
public class App extends Application {

    private static final Logger LOG = LoggerUtil.getLogger(App.class);

    @Override
    public void start(Stage stage) {
        startLogging();
        try {
            stage.setTitle("Smart Taxi Agent");
            stage.setScene(new Scene(new MainView(), 1400, 800));
            stage.setMaximized(true);
            stage.show();
            LOG.info("Application started");
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Could not start the application: " + e.getMessage()).showAndWait();
        }
    }

    @Override
    public void stop() {
        LOG.info("Application closed");
        LoggerUtil.reset();
    }

    /** Starts writing the log file named in config.properties. The app still runs if the config is broken. */
    private static void startLogging() {
        try {
            AppConfig config = AppConfig.load();
            LoggerUtil.configure(config.getLogFile(), config.getLogLevel());
        } catch (InvalidConfigException e) {
            System.err.println("Logging not started: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}