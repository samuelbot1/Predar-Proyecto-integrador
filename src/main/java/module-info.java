module com.predar.predar {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.prefs;

    opens com.predar.predar.controller to javafx.fxml;

    exports com.predar.predar.app;
    exports com.predar.predar.controller;
    exports com.predar.predar.database;
    exports com.predar.predar.lidar;
    exports com.predar.predar.model;
    exports com.predar.predar.simulation;
}