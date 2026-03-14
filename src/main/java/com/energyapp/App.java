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
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;

public class App extends Application {

    // 1. THIS IS THE NEW LINE THAT LETS THE AD TALK TO THE MUSIC
    public static MediaPlayer bgMusicPlayer;

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

        // --- WII SHOP MUSIC INJECTION ---
        try {
            URL musicUrl = getClass().getResource("/com/energyapp/wii-shop.mp3");
            if (musicUrl != null) {
                Media media = new Media(musicUrl.toString());

                // 2. NOTICE WE ARE USING bgMusicPlayer NOW!
                bgMusicPlayer = new MediaPlayer(media);
                bgMusicPlayer.setVolume(0.5); // 50% volume
                bgMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Loop forever
                bgMusicPlayer.play();
            } else {
                System.out.println("Wii music file not found in resources!");
            }
        } catch (Exception e) {
            System.out.println("Media player error: " + e.getMessage());
        }
        // --------------------------------

        primaryStage.show();
    }

    @Override
    public void stop() {
        apiRefreshService.stop();
        simulationService.shutdown();
    }

    public static void main(String[] args) {
        launch();
    }
}