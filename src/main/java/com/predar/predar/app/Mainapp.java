package com.predar.predar.app;

import com.predar.predar.database.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Mainapp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        DatabaseManager.initialize();
        StageManager.setStage(stage); // ← AGREGAR ESTA LÍNEA
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/predar/predar/login.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(
                getClass().getResource("/com/predar/predar/styles.css").toExternalForm());
        stage.setTitle("PREDAR - Iniciar Sesion");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}