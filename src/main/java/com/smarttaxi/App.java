package com.smarttaxi;

import com.smarttaxi.ui.MainView;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/** Starts the JavaFX window. */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        try {
            stage.setTitle("Smart Taxi Agent");
            stage.setScene(new Scene(new MainView(), 1100, 760));
            stage.show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Could not start the application: " + e.getMessage()).showAndWait();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}