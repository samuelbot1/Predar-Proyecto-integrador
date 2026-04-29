package com.predar.predar.database;

import java.sql.*;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:predar.db";

    public static void initialize() {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "username TEXT NOT NULL UNIQUE," +
                            "password TEXT NOT NULL)"
            );

            // Siempre verificar que admin existe
            PreparedStatement check = conn.prepareStatement(
                    "SELECT id FROM users WHERE username = 'admin'"
            );
            ResultSet rs = check.executeQuery();
            if (!rs.next()) {
                stmt.execute("INSERT INTO users (username, password) VALUES ('admin', 'predar123')");
                System.out.println("Usuario admin creado.");
            } else {
                System.out.println("Usuario admin ya existe.");
            }

        } catch (SQLException e) {
            System.err.println("Error inicializando DB: " + e.getMessage());
        }
    }

    public static boolean validateUser(String username, String password) {
        String sql = "SELECT id FROM users WHERE username = ? AND password = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            System.err.println("Error validando usuario: " + e.getMessage());
            return false;
        }
    }

    public static Connection connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver SQLite no encontrado: " + e.getMessage());
        }
        return DriverManager.getConnection(DB_URL);
    }
}