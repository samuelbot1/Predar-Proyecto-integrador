package com.predar.predar.app;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class StageManager {
    private static Stage primaryStage;

    public static void setStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getStage() {
        return primaryStage;
    }

    public static void applyScene(Scene scene) {
        boolean maximized = primaryStage.isMaximized();
        double w = primaryStage.getWidth();
        double h = primaryStage.getHeight();
        double x = primaryStage.getX();
        double y = primaryStage.getY();

        if (maximized) {
            primaryStage.setMaximized(false);
        }

        primaryStage.setScene(scene);

        if (maximized) {
            // Esperar a que JavaFX procese el cambio de escena antes de maximizar
            Platform.runLater(() -> primaryStage.setMaximized(true));
        } else {
            primaryStage.setX(x);
            primaryStage.setY(y);
            primaryStage.setWidth(w);
            primaryStage.setHeight(h);
        }
    }
}