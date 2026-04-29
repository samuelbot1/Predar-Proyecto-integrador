package com.predar.predar.app;

import com.predar.predar.lidar.LidarLoader;
import java.util.List;

public class LidarState {
    private static List<LidarLoader.Point3D> savedPoints = null;
    private static String savedFileName = null;
    private static double savedRotateX = 10;
    private static double savedRotateY = 90;
    private static double savedScale = 1.0;

    public static void save(List<LidarLoader.Point3D> points, String fileName,
                            double rotX, double rotY, double scale) {
        savedPoints = points;
        savedFileName = fileName;
        savedRotateX = rotX;
        savedRotateY = rotY;
        scale = 1.0;
        savedScale = scale;
    }

    public static List<LidarLoader.Point3D> getPoints() { return savedPoints; }
    public static String getFileName() { return savedFileName; }
    public static double getRotateX() { return savedRotateX; }
    public static double getRotateY() { return savedRotateY; }
    public static double getScale() { return savedScale; }
    public static boolean hasData() { return savedPoints != null && !savedPoints.isEmpty(); }
    public static void clear() { savedPoints = null; savedFileName = null; }
}