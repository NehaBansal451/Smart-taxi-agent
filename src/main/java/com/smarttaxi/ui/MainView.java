package com.smarttaxi.ui;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/** Main window content: the simulation and the training dashboard share one agent. */
public class MainView extends TabPane {

    public MainView() {
        DashboardView dashboard = new DashboardView();
        SimulationView simulation = new SimulationView(dashboard.getMap(), dashboard::getAgent);
        Tab simulationTab = new Tab("Simulation", simulation);
        Tab dashboardTab = new Tab("Training dashboard", dashboard);
        simulationTab.setClosable(false);
        dashboardTab.setClosable(false);
        getTabs().addAll(simulationTab, dashboardTab);
    }
}
