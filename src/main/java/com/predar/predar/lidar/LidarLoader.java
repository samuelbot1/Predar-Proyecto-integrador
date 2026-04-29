package com.predar.predar.lidar;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import com.predar.predar.model.AlertLevel;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class LidarLoader {

    public static class Point3D {
        public final double x, y, z;
        public Point3D(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public List<Point3D> loadFromCSV(String filePath) {
        List<Point3D> points = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            br.readLine(); // saltar encabezado
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    double x = Double.parseDouble(parts[0].trim());
                    double y = Double.parseDouble(parts[1].trim());
                    double z = Double.parseDouble(parts[2].trim());
                    points.add(new Point3D(x, y, z));
                }
            }
        } catch (Exception e) {
            System.err.println("Error leyendo CSV: " + e.getMessage());
        }
        System.out.println("Puntos cargados: " + points.size());
        return points;
    }

    public Canvas renderToCanvas(List<Point3D> points,
                                 double width, double height,
                                 double rotX, double rotY,
                                 double scale) {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.web("#010409"));
        gc.fillRect(0, 0, width, height);

        if (points.isEmpty()) return canvas;

        // Normalizar puntos
        double minX = points.stream().mapToDouble(p -> p.x).min().orElse(0);
        double maxX = points.stream().mapToDouble(p -> p.x).max().orElse(1);
        double minY = points.stream().mapToDouble(p -> p.y).min().orElse(0);
        double maxY = points.stream().mapToDouble(p -> p.y).max().orElse(1);
        double minZ = points.stream().mapToDouble(p -> p.z).min().orElse(0);
        double maxZ = points.stream().mapToDouble(p -> p.z).max().orElse(1);

        double rangeX = maxX - minX == 0 ? 1 : maxX - minX;
        double rangeY = maxY - minY == 0 ? 1 : maxY - minY;
        double rangeZ = maxZ - minZ == 0 ? 1 : maxZ - minZ;

        double cx = width / 2;
        double cy = height / 2;

        double radX = Math.toRadians(rotX);
        double radY = Math.toRadians(rotY);

        for (Point3D p : points) {
            double nx = ((p.x - minX) / rangeX - 0.5) * 600 * scale;
            double ny = ((p.y - minY) / rangeY - 0.5) * 600 * scale;
            double nz = ((p.z - minZ) / rangeZ - 0.5) * 200 * scale;
            double t  = (p.z - minZ) / rangeZ;

            // Rotación Y
            double x1 = nx * Math.cos(radY) - nz * Math.sin(radY);
            double z1 = nx * Math.sin(radY) + nz * Math.cos(radY);

            // Rotación X
            double y1 = ny * Math.cos(radX) - z1 * Math.sin(radX);
            double z2 = ny * Math.sin(radX) + z1 * Math.cos(radX);

            double screenX = cx + x1;
            double screenY = cy + y1;

            gc.setFill(heightColor(t));
            gc.fillRect(screenX, screenY, 1.5, 1.5);
        }

        return canvas;
    }

    private Color heightColor(double t) {
        if (t < 0.25) return interpolate(Color.web("#0d47a1"), Color.web("#1976d2"), t / 0.25);
        if (t < 0.50) return interpolate(Color.web("#1976d2"), Color.web("#43a047"), (t - 0.25) / 0.25);
        if (t < 0.75) return interpolate(Color.web("#43a047"), Color.web("#ffb300"), (t - 0.50) / 0.25);
        return interpolate(Color.web("#ffb300"), Color.web("#e53935"), (t - 0.75) / 0.25);
    }

    private Color interpolate(Color a, Color b, double t) {
        return new Color(
                clamp(a.getRed()   + (b.getRed()   - a.getRed())   * t),
                clamp(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                clamp(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t),
                1.0
        );
    }

    private double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    public List<DeformationZone> detectDeformations(List<Point3D> points, double expectedRadius) {
        List<DeformationZone> zones = new ArrayList<>();
        if (points.isEmpty()) return zones;

        // Calcular centro del tunel
        double centerX = points.stream().mapToDouble(p -> p.x).average().orElse(0);
        double centerY = points.stream().mapToDouble(p -> p.y).average().orElse(0);

        // Dividir el tunel en secciones por Z
        double minZ = points.stream().mapToDouble(p -> p.z).min().orElse(0);
        double maxZ = points.stream().mapToDouble(p -> p.z).max().orElse(1);
        int sections = 20;
        double sectionSize = (maxZ - minZ) / sections;

        for (int i = 0; i < sections; i++) {
            final double zMin = minZ + i * sectionSize;
            final double zMax = zMin + sectionSize;
            final double zCenter = (zMin + zMax) / 2;

            List<Point3D> sectionPoints = points.stream()
                    .filter(p -> p.z >= zMin && p.z < zMax)
                    .collect(java.util.stream.Collectors.toList());

            if (sectionPoints.size() < 10) continue;

            // Calcular radio promedio en esta seccion
            double avgRadius = sectionPoints.stream()
                    .mapToDouble(p -> Math.sqrt(
                            Math.pow(p.x - centerX, 2) +
                                    Math.pow(p.y - centerY, 2)))
                    .average().orElse(expectedRadius);

            double deviation = Math.abs(avgRadius - expectedRadius) / expectedRadius;

            if (deviation > 0.15) {
                DeformationZone zone = new DeformationZone(
                        centerX, centerY, zCenter,
                        avgRadius, deviation,
                        deviation > 0.30 ? AlertLevel.CRITICAL : AlertLevel.WARNING
                );
                zones.add(zone);
            }
        }
        return zones;
    }

    public static class DeformationZone {
        public final double x, y, z;
        public final double radius;
        public final double deviation;
        public final AlertLevel level;

        public DeformationZone(double x, double y, double z,
                               double radius, double deviation,
                               AlertLevel level) {
            this.x = x; this.y = y; this.z = z;
            this.radius = radius;
            this.deviation = deviation;
            this.level = level;
        }
    }

}