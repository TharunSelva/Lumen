package com.energyapp;
import com.energyapp.db.DbBootstrap;

import com.energyapp.controller.DashboardController;
import com.energyapp.controller.MainLayoutController;
import com.energyapp.service.ApiRefreshService;
import com.energyapp.service.SimulationService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.energyapp.controller.ThemeController;

public class App extends Application {

    private final SimulationService simulationService = new SimulationService();
    private final ApiRefreshService apiRefreshService = new ApiRefreshService();

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/energyapp/view/MainLayout.fxml"));
        Parent root = loader.load();

        DbBootstrap.init();  // DB initialisation
        apiRefreshService.start();
        simulationService.loadData();
        simulationService.startSimulation();

        MainLayoutController mainController = loader.getController();
        mainController.setSimService(simulationService);
        mainController.handleShowDashboard();

        primaryStage.setTitle("Home Energy Manager");
        Scene scene = new Scene(root, 1280, 720);
        ThemeController.applyTheme(scene);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    // overridden stop() method to kill simulation when app is closed via Close Window.
    @Override
    public void stop() {
        apiRefreshService.stop();
        simulationService.shutdown(); // if you already have this
    }
    //    @Override
//    public void stop() throws Exception {
//        if (simulationService != null) {
//            simulationService.shutdown();
//        }
//        super.stop();
//    }

    public static void main(String[] args) {
        launch();
    }
}
