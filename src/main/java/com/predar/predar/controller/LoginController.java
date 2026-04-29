package com.predar.predar.controller;

import com.predar.predar.database.DatabaseManager;
import com.predar.predar.database.PreferencesManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private CheckBox rememberCheckbox;

    @FXML
    public void initialize() {
        if (PreferencesManager.isRemembered()) {
            usernameField.setText(PreferencesManager.getSavedUsername());
            rememberCheckbox.setSelected(true);
        }
    }

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Por favor ingresa usuario y contraseña.");
            return;
        }

        if (DatabaseManager.validateUser(username, password)) {
            PreferencesManager.saveUser(username, rememberCheckbox.isSelected());
            openDashboard();
        } else {
            showError("Usuario o contraseña incorrectos.");
        }
    }

    private void openDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/predar/predar/dashboard.fxml")
            );
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/com/predar/predar/styles.css").toExternalForm()
            );
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle("PREDAR — Sistema de Monitoreo Minero");
            stage.setScene(scene);
            stage.setMinWidth(1000);
            stage.setMinHeight(650);
            stage.setResizable(true);
        } catch (Exception e) {
            showError("Error al cargar el dashboard.");
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}