package com.energyapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class EnergyStatsTestApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // IMPORTANT: path is from /resources
        Parent root = FXMLLoader.load(
                getClass().getResource("/com/energyapp/view/EnergyStatistics.fxml")
//                getClass().getResource("/com/energyapp/view/energy_statistics_screen_without_sidebar.fxml")
        );

        Scene scene = new Scene(root, 1200, 800);
        stage.setTitle("Energy Stats - Test");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}