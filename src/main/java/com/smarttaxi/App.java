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
        Scene scene = new Scene(new DashboardView(), 1000, 700);
        scene.getStylesheets().add(App.class.getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
