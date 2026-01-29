package com.energyapp;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        // 1. Create a simple "Node" (A label)
        String javaVersion = System.getProperty("java.version");
        Button buton = new Button("Click Me");
        Button login = new Button("Login");
        String javafxVersion = System.getProperty("javafx.version");
        Label label = new Label("Hello, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");

        // 2. Put the Node in a Layout (StackPane centers it)
        StackPane root = new StackPane();
        root.getChildren().add(label);
        Scene Dashboard = new Scene(buton, 200, 100);
        Scene Login = new Scene(login, 200,100);

        // 3. Create the Scene
        Scene w = new Scene(root, 640, 480);

        // 4. Set the Stage
        stage.setScene(Dashboard);
        stage.setTitle("My JavaFX Project");
        stage.show();

        buton.setOnAction(e->{
            stage.setScene(Login);
            stage.setTitle("Login");
            stage.show();
                }
        );

        login.setOnAction(e->{
            stage.setScene(Dashboard);
            stage.setTitle("My JavaFX Project");
        });
    }

    public static void main(String[] args) {
        launch();
    }
}