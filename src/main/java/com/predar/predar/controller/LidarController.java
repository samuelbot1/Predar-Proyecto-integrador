package com.predar.predar.controller;
import com.predar.predar.model.TunnelRiskModel;

import com.predar.predar.app.LidarState;
import com.predar.predar.app.StageManager;
import com.predar.predar.lidar.LidarLoader;
import com.predar.predar.model.AlertLevel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LidarController {

    @FXML private Pane viewerPane;
    @FXML private Label coordsLabel;
    @FXML private StackPane viewerStack;
    @FXML private VBox loadingOverlay;
    @FXML private ProgressBar loadingBar;
    @FXML private Label loadingDetail;
    @FXML private Label statusLabel;
    @FXML private Label pointCountLabel;
    @FXML private Label deformationLabel;
    private final com.predar.predar.model.TunnelRiskModel riskModel = new com.predar.predar.model.TunnelRiskModel();

    private final LidarLoader loader = new LidarLoader();
    private List<LidarLoader.Point3D> points;
    private double rotateX = 15, rotateY = 0, scale = 1.0;
    private double anchorX, anchorY, anchorRotX, anchorRotY;
    private boolean isLoading = false;

    private List<LidarLoader.DeformationZone> deformationZones = new ArrayList<>();
    private boolean showDeformations = false;
    private double expectedRadius = 3.0;

    @FXML
    public void initialize() {
        viewerPane.widthProperty().addListener((obs, o, n) -> redraw());
        viewerPane.heightProperty().addListener((obs, o, n) -> redraw());

        if (LidarState.hasData()) {
            points = LidarState.getPoints();
            rotateX = LidarState.getRotateX();
            rotateY = LidarState.getRotateY();
            scale = LidarState.getScale();
            statusLabel.setText(LidarState.getFileName());
            pointCountLabel.setText("Puntos: " + points.size());
            setupMouseControls();
            Platform.runLater(this::redraw);
        } else {
            statusLabel.setText("Sin archivo cargado");
            pointCountLabel.setText("Puntos: 0");
        }
    }

    @FXML
    public void handleLoadCSV() {
        if (isLoading) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar CSV de nube de puntos");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showOpenDialog(viewerPane.getScene().getWindow());
        if (file == null) return;
        showLoading(true, "Leyendo archivo: " + file.getName());
        Thread thread = new Thread(() -> {
            Platform.runLater(() -> loadingDetail.setText("Parseando coordenadas X, Y, Z..."));
            List<LidarLoader.Point3D> loaded = loader.loadFromCSV(file.getAbsolutePath());
            Platform.runLater(() -> loadingDetail.setText("Renderizando " + loaded.size() + " puntos..."));
            try { Thread.sleep(400); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                points = loaded;
                deformationZones.clear();
                showDeformations = false;
                LidarState.save(points, file.getName(), rotateX, rotateY, scale);
                setupMouseControls();
                redraw();
                showLoading(false, "");
                statusLabel.setText(file.getName());
                pointCountLabel.setText("Puntos: " + points.size());
                if (deformationLabel != null)
                    deformationLabel.setText("Sin analizar");
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void handleAnalyzeDeformations() {
        if (points == null || points.isEmpty()) {
            statusLabel.setText("Carga un archivo CSV primero.");
            return;
        }

        // Mostrar overlay de carga
        loadingOverlay.setVisible(true);
        viewerPane.setDisable(true);
        loadingDetail.setText("Analizando estructura del tunel...");
        statusLabel.setText("Analizando...");

        Thread thread = new Thread(() -> {
            Platform.runLater(() -> loadingDetail.setText("Calculando radios por seccion..."));
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}

            List<LidarLoader.DeformationZone> zones =
                    loader.detectDeformations(points, expectedRadius);

            Platform.runLater(() -> loadingDetail.setText("Clasificando zonas de riesgo..."));
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}

            Platform.runLater(() -> {
                deformationZones = zones;
                showDeformations = true;
                loadingOverlay.setVisible(false);
                viewerPane.setDisable(false);
                redraw();

                if (zones.isEmpty()) {
                    statusLabel.setText("Sin deformaciones detectadas");
                    if (deformationLabel != null) {
                        deformationLabel.setText("Estructura: NORMAL");
                        deformationLabel.setStyle("-fx-text-fill: #3fb950; -fx-font-weight: bold;");
                    }
                } else {
                    long critical = zones.stream().filter(z -> z.level == AlertLevel.CRITICAL).count();
                    long warning  = zones.stream().filter(z -> z.level == AlertLevel.WARNING).count();
                    statusLabel.setText("Analisis completo — " + zones.size() + " zonas");
                    if (deformationLabel != null) {
                        deformationLabel.setText("Criticas: " + critical + "  Advertencias: " + warning);
                        deformationLabel.setStyle(critical > 0
                                ? "-fx-text-fill: #f85149; -fx-font-weight: bold;"
                                : "-fx-text-fill: #d29922; -fx-font-weight: bold;");
                    }
                }
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void handleToggleDeformations() {
        showDeformations = !showDeformations;
        redraw();
    }

    @FXML
    public void handleRiskAnalysis() {
        if (points == null || points.isEmpty()) {
            statusLabel.setText("Carga un archivo CSV primero.");
            return;
        }

        loadingOverlay.setVisible(true);
        viewerPane.setDisable(true);
        loadingDetail.setText("Calculando Factor de Seguridad...");
        statusLabel.setText("Analizando riesgo...");

        Thread thread = new Thread(() -> {
            Platform.runLater(() -> loadingDetail.setText("Analizando convergencia por secciones..."));
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}

            TunnelRiskModel.TunnelRiskReport report =
                    riskModel.analyze(points, expectedRadius);

            Platform.runLater(() -> loadingDetail.setText("Calculando asimetria y Factor de Seguridad..."));
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}

            Platform.runLater(() -> {
                loadingOverlay.setVisible(false);
                viewerPane.setDisable(false);

                if (report == null) return;

                // Mostrar resultado en deformationLabel
                String color;
                switch (report.nivelGlobal) {
                    case CRITICO   -> color = "#f85149";
                    case ALTO      -> color = "#f97316";
                    case PRECAUCION -> color = "#d29922";
                    default        -> color = "#3fb950";
                }

                deformationLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                deformationLabel.setText(
                        "FS Global: " + String.format("%.2f", report.fsGlobal) + "\n" +
                                "Nivel: " + report.nivelGlobal + "\n" +
                                "Criticas: " + report.seccionesCriticas + "\n" +
                                "Conv. max: " + String.format("%.1f", report.convergenciaMaxima) + "%\n" +
                                report.recomendacion
                );

                statusLabel.setText("Riesgo: " + report.nivelGlobal +
                        " — FS=" + String.format("%.2f", report.fsGlobal));

                // Colorear secciones en el canvas
                drawRiskSections(report);
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void drawRiskSections(TunnelRiskModel.TunnelRiskReport report) {
        if (points == null || points.isEmpty()) return;

        double w = viewerPane.getWidth() > 0 ? viewerPane.getWidth() : viewerPane.getPrefWidth();
        double h = viewerPane.getHeight() > 0 ? viewerPane.getHeight() : viewerPane.getPrefHeight();
        if (w <= 0 || h <= 0) return;

        Canvas canvas = loader.renderToCanvas(points, w, h, rotateX, rotateY, scale);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double minZ = points.stream().mapToDouble(p -> p.z).min().orElse(0);
        double maxZ = points.stream().mapToDouble(p -> p.z).max().orElse(1);
        double rangeZ = maxZ - minZ == 0 ? 1 : maxZ - minZ;
        double radX = Math.toRadians(rotateX);
        double radY = Math.toRadians(rotateY);

        for (TunnelRiskModel.SectionRisk section : report.sections) {
            double nz = ((section.z - minZ) / rangeZ - 0.5) * 200 * scale;
            double x1 = -nz * Math.sin(radY);
            double z1 =  nz * Math.cos(radY);
            double y1 = -z1 * Math.sin(radX);
            double screenX = w / 2 + x1;
            double screenY = h / 2 + y1;

            Color strokeColor;
            Color fillColor;
            switch (section.nivel) {
                case CRITICO -> {
                    strokeColor = Color.web("#f85149");
                    fillColor   = Color.web("#f8514950");
                }
                case ALTO -> {
                    strokeColor = Color.web("#f97316");
                    fillColor   = Color.web("#f9731640");
                }
                case PRECAUCION -> {
                    strokeColor = Color.web("#d29922");
                    fillColor   = Color.web("#d2992230");
                }
                default -> {
                    strokeColor = Color.web("#3fb950");
                    fillColor   = Color.web("#3fb95020");
                }
            }

            gc.setStroke(strokeColor);
            gc.setFill(fillColor);
            gc.setLineWidth(1.5);
            double size = 30 * scale;
            gc.strokeOval(screenX - size/2, screenY - size/2, size, size);
            gc.fillOval(screenX - size/2, screenY - size/2, size, size);

            // FS encima de cada seccion
            gc.setFill(strokeColor);
            gc.setFont(Font.font(9));
            gc.fillText(String.format("%.1f", section.factorSeguridad),
                    screenX - 8, screenY + 3);
        }

        viewerPane.getChildren().clear();
        viewerPane.getChildren().add(canvas);
    }

    private void showLoading(boolean show, String detail) {
        isLoading = show;
        loadingOverlay.setVisible(show);
        viewerPane.setDisable(show);
        if (show) { statusLabel.setText("Cargando..."); loadingDetail.setText(detail); }
    }

    private void redraw() {
        if (points == null || points.isEmpty()) return;
        double w = viewerPane.getWidth() > 0 ? viewerPane.getWidth() : viewerPane.getPrefWidth();
        double h = viewerPane.getHeight() > 0 ? viewerPane.getHeight() : viewerPane.getPrefHeight();
        if (w <= 0 || h <= 0) return;

        Canvas canvas = loader.renderToCanvas(points, w, h, rotateX, rotateY, scale);

        if (showDeformations && !deformationZones.isEmpty()) {
            GraphicsContext gc = canvas.getGraphicsContext2D();
            double minZ = points.stream().mapToDouble(p -> p.z).min().orElse(0);
            double maxZ = points.stream().mapToDouble(p -> p.z).max().orElse(1);
            double rangeZ = maxZ - minZ == 0 ? 1 : maxZ - minZ;
            double radX = Math.toRadians(rotateX);
            double radY = Math.toRadians(rotateY);

            for (LidarLoader.DeformationZone zone : deformationZones) {
                double nz = ((zone.z - minZ) / rangeZ - 0.5) * 200 * scale;
                double x1 = -nz * Math.sin(radY);
                double z1 =  nz * Math.cos(radY);
                double y1 = -z1 * Math.sin(radX);
                double screenX = w / 2 + x1;
                double screenY = h / 2 + y1;

                if (zone.level == AlertLevel.CRITICAL) {
                    gc.setStroke(Color.web("#f85149"));
                    gc.setFill(Color.web("#f8514940"));
                } else {
                    gc.setStroke(Color.web("#d29922"));
                    gc.setFill(Color.web("#d2992240"));
                }
                gc.setLineWidth(2);
                gc.strokeOval(screenX - 20, screenY - 20, 40, 40);
                gc.fillOval(screenX - 20, screenY - 20, 40, 40);
                gc.setFill(zone.level == AlertLevel.CRITICAL
                        ? Color.web("#f85149") : Color.web("#d29922"));
                gc.setFont(Font.font(10));
                gc.fillText(String.format("%.0f%%", zone.deviation * 100), screenX - 10, screenY + 4);
            }
        }

        viewerPane.getChildren().clear();
        viewerPane.getChildren().add(canvas);

        if (LidarState.hasData()) {
            LidarState.save(points, LidarState.getFileName(), rotateX, rotateY, scale);
        }
    }

    private void setupMouseControls() {
        viewerPane.setOnMousePressed(e -> {
            if (isLoading) return;
            anchorX = e.getSceneX();
            anchorY = e.getSceneY();
            anchorRotX = rotateX;
            anchorRotY = rotateY;
            viewerPane.setCursor(javafx.scene.Cursor.CLOSED_HAND);
        });

        viewerPane.setOnMouseReleased(e -> {
            viewerPane.setCursor(javafx.scene.Cursor.OPEN_HAND);
        });

        viewerPane.setOnMouseDragged(e -> {
            if (isLoading) return;
            double dx = (e.getSceneX() - anchorX) * 0.25;
            double dy = (e.getSceneY() - anchorY) * 0.25;
            rotateY = anchorRotY + dx;
            rotateX = Math.max(-89, Math.min(89, anchorRotX - dy));
            redraw();
        });

        viewerPane.setOnScroll(e -> {
            if (isLoading) return;
            double factor = e.getDeltaY() > 0 ? 1.08 : 0.92;
            scale = Math.max(0.1, Math.min(8.0, scale * factor));
            redraw();
        });

        viewerPane.setOnMouseMoved(e -> {
            if (points == null || points.isEmpty()) return;
            double w = viewerPane.getWidth();
            double h = viewerPane.getHeight();
            double relX = e.getX() - w / 2;
            double relY = e.getY() - h / 2;
            if (coordsLabel != null) {
                coordsLabel.setText(String.format("X: %.1f  Y: %.1f", relX / scale, relY / scale));
            }
        });

        viewerPane.setCursor(javafx.scene.Cursor.OPEN_HAND);
    }

    @FXML
    public void handleBackToDashboard() {
        if (isLoading) return;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("/com/predar/predar/dashboard.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/com/predar/predar/styles.css").toExternalForm());
            StageManager.applyScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleResetView() {
        rotateX = 15;
        rotateY = 0;
        scale = 1.0;
        redraw();
    }

}