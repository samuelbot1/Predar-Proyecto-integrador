package com.predar.predar.controller;

import com.predar.predar.app.StageManager;
import com.predar.predar.model.AlertLevel;
import com.predar.predar.model.GasReading;
import com.predar.predar.simulation.SensorSimulator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DashboardController {

    // --- Gases
    @FXML private Label methaneValue;
    @FXML private Label coValue;
    @FXML private Label oxygenValue;

    // --- Alertas
    @FXML private Label methaneAlert;
    @FXML private Label coAlert;
    @FXML private Label oxygenAlert;

    // --- Tarjetas de riesgo
    @FXML private HBox methaneCard;
    @FXML private HBox coCard;
    @FXML private HBox oxygenCard;

    // --- Estado sistema
    @FXML private Label systemStatus;
    @FXML private Label signalLabel;
    @FXML private Label batteryLabel;
    @FXML private Circle statusDot;

    // --- Panel de alertas
    @FXML private VBox alertsPanel;

    private final SensorSimulator simulator = new SensorSimulator();
    private Timeline timeline;

    @FXML
    public void initialize() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> updateReadings()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void updateReadings() {
        GasReading reading = simulator.getNextReading();

        // Actualizar valores
        methaneValue.setText(String.format("%.2f%%", reading.getMethane()));
        coValue.setText(String.format("%.0f ppm", reading.getCarbonMonoxide()));
        oxygenValue.setText(String.format("%.1f%%", reading.getOxygen()));

        // Actualizar alertas individuales
        applyAlert(methaneAlert, methaneCard, reading.getMethaneAlert(), "CH₄");
        applyAlert(coAlert, coCard, reading.getCOAlert(), "CO");
        applyAlert(oxygenAlert, oxygenCard, reading.getOxygenAlert(), "O₂");

        // Estado general del sistema
        AlertLevel worst = getWorstLevel(
                reading.getMethaneAlert(),
                reading.getCOAlert(),
                reading.getOxygenAlert()
        );
        updateSystemStatus(worst);
    }

    private void applyAlert(Label alertLabel, HBox card, AlertLevel level, String gas) {
        card.getStyleClass().removeAll("card-normal", "card-warning", "card-critical");
        alertLabel.getStyleClass().removeAll("alert-normal", "alert-warning", "alert-critical");

        switch (level) {
            case NORMAL -> {
                card.getStyleClass().add("card-normal");
                alertLabel.getStyleClass().add("alert-normal");
                alertLabel.setText(gas + ": NORMAL");
            }
            case WARNING -> {
                card.getStyleClass().add("card-warning");
                alertLabel.getStyleClass().add("alert-warning");
                alertLabel.setText(gas + ": ⚠ ADVERTENCIA");
            }
            case CRITICAL -> {
                card.getStyleClass().add("card-critical");
                alertLabel.getStyleClass().add("alert-critical");
                alertLabel.setText(gas + ": ✖ CRÍTICO");
            }
        }
    }

    private AlertLevel getWorstLevel(AlertLevel... levels) {
        AlertLevel worst = AlertLevel.NORMAL;
        for (AlertLevel l : levels) {
            if (l == AlertLevel.CRITICAL) return AlertLevel.CRITICAL;
            if (l == AlertLevel.WARNING) worst = AlertLevel.WARNING;
        }
        return worst;
    }

    private void updateSystemStatus(AlertLevel level) {
        statusDot.getStyleClass().removeAll("dot-normal", "dot-warning", "dot-critical");
        systemStatus.getStyleClass().removeAll("alert-normal", "alert-warning", "alert-critical");

        switch (level) {
            case NORMAL -> {
                statusDot.getStyleClass().add("dot-normal");
                systemStatus.getStyleClass().add("alert-normal");
                systemStatus.setText("Sistema: OPERATIVO");
            }
            case WARNING -> {
                statusDot.getStyleClass().add("dot-warning");
                systemStatus.getStyleClass().add("alert-warning");
                systemStatus.setText("Sistema: PRECAUCIÓN");
            }
            case CRITICAL -> {
                statusDot.getStyleClass().add("dot-critical");
                systemStatus.getStyleClass().add("alert-critical");
                systemStatus.setText("Sistema: ALERTA CRÍTICA");
            }
        }
    }
    @FXML

    public void handleLidarView() {
        try {
            if (timeline != null) timeline.stop(); // detener simulación al salir
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/predar/predar/lidar.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/com/predar/predar/styles.css").toExternalForm());
            StageManager.applyScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}