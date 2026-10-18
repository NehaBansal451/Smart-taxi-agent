package com.smarttaxi;

import com.smarttaxi.ui.DashboardView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Starts the JavaFX window. */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Smart Taxi Agent");
        stage.setScene(new Scene(new DashboardView(), 1000, 700));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
