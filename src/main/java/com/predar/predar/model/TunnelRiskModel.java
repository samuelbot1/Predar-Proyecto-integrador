package com.predar.predar.model;

import com.predar.predar.lidar.LidarLoader;
import java.util.List;
import java.util.stream.Collectors;

public class TunnelRiskModel {

    public enum RiskLevel {
        ESTABLE, PRECAUCION, ALTO, CRITICO
    }

    public static class SectionRisk {
        public final double z;
        public final double convergencia;
        public final double asimetria;
        public final double factorSeguridad;
        public final RiskLevel nivel;
        public final String descripcion;

        public SectionRisk(double z, double convergencia, double asimetria,
                           double factorSeguridad, RiskLevel nivel, String descripcion) {
            this.z = z;
            this.convergencia = convergencia;
            this.asimetria = asimetria;
            this.factorSeguridad = factorSeguridad;
            this.nivel = nivel;
            this.descripcion = descripcion;
        }
    }

    public static class TunnelRiskReport {
        public final List<SectionRisk> sections;
        public final double fsGlobal;
        public final RiskLevel nivelGlobal;
        public final int seccionesCriticas;
        public final int seccionesPrecaucion;
        public final double convergenciaMaxima;
        public final String recomendacion;

        public TunnelRiskReport(List<SectionRisk> sections, double fsGlobal,
                                RiskLevel nivelGlobal, int seccionesCriticas,
                                int seccionesPrecaucion, double convergenciaMaxima,
                                String recomendacion) {
            this.sections = sections;
            this.fsGlobal = fsGlobal;
            this.nivelGlobal = nivelGlobal;
            this.seccionesCriticas = seccionesCriticas;
            this.seccionesPrecaucion = seccionesPrecaucion;
            this.convergenciaMaxima = convergenciaMaxima;
            this.recomendacion = recomendacion;
        }
    }

    public TunnelRiskReport analyze(List<LidarLoader.Point3D> points, double radioEsperado) {
        if (points == null || points.isEmpty()) return null;

        // Centro del tunel
        double centerX = points.stream().mapToDouble(p -> p.x).average().orElse(0);
        double centerY = points.stream().mapToDouble(p -> p.y).average().orElse(0);

        // Dividir en secciones por Z
        double minZ = points.stream().mapToDouble(p -> p.z).min().orElse(0);
        double maxZ = points.stream().mapToDouble(p -> p.z).max().orElse(1);
        int numSecciones = 20;
        double sectionSize = (maxZ - minZ) / numSecciones;

        List<SectionRisk> sections = new java.util.ArrayList<>();

        for (int i = 0; i < numSecciones; i++) {
            final double zMin = minZ + i * sectionSize;
            final double zMax = zMin + sectionSize;
            final double zCenter = (zMin + zMax) / 2;

            List<LidarLoader.Point3D> secPoints = points.stream()
                    .filter(p -> p.z >= zMin && p.z < zMax)
                    .collect(Collectors.toList());

            if (secPoints.size() < 5) continue;

            // Radio promedio de la seccion
            double radioPromedio = secPoints.stream()
                    .mapToDouble(p -> Math.sqrt(
                            Math.pow(p.x - centerX, 2) +
                                    Math.pow(p.y - centerY, 2)))
                    .average().orElse(radioEsperado);

            // Convergencia: cuanto se cerro el tunel (%)
            double convergencia = (radioEsperado - radioPromedio) / radioEsperado * 100;

            // Asimetria: diferencia entre lado izquierdo y derecho
            double radioIzq = secPoints.stream()
                    .filter(p -> p.x < centerX)
                    .mapToDouble(p -> Math.sqrt(Math.pow(p.x - centerX, 2) + Math.pow(p.y - centerY, 2)))
                    .average().orElse(radioEsperado);

            double radioDer = secPoints.stream()
                    .filter(p -> p.x >= centerX)
                    .mapToDouble(p -> Math.sqrt(Math.pow(p.x - centerX, 2) + Math.pow(p.y - centerY, 2)))
                    .average().orElse(radioEsperado);

            double asimetria = Math.abs(radioIzq - radioDer) / radioEsperado * 100;

            // Factor de Seguridad simplificado
            // FS = 1 + (radio_actual / radio_esperado) * factor_asimetria
            double ratioRadio = radioPromedio / radioEsperado;
            double factorAsimetria = 1.0 - (asimetria / 200.0);
            double fs = ratioRadio * factorAsimetria * 1.5;
            fs = Math.max(0.1, Math.min(3.0, fs));

            // Clasificar nivel
            RiskLevel nivel;
            String descripcion;

            if (fs >= 1.5) {
                nivel = RiskLevel.ESTABLE;
                descripcion = "Seccion estable";
            } else if (fs >= 1.2) {
                nivel = RiskLevel.PRECAUCION;
                descripcion = String.format("Convergencia %.1f%% — monitorear", convergencia);
            } else if (fs >= 1.0) {
                nivel = RiskLevel.ALTO;
                descripcion = String.format("Convergencia %.1f%% — intervencion recomendada", convergencia);
            } else {
                nivel = RiskLevel.CRITICO;
                descripcion = String.format("Convergencia %.1f%% — riesgo de colapso", convergencia);
            }

            sections.add(new SectionRisk(zCenter, convergencia, asimetria, fs, nivel, descripcion));
        }

        // Calcular reporte global
        double fsGlobal = sections.stream()
                .mapToDouble(s -> s.factorSeguridad)
                .min().orElse(1.5);

        int criticas   = (int) sections.stream().filter(s -> s.nivel == RiskLevel.CRITICO).count();
        int precaucion = (int) sections.stream().filter(s -> s.nivel == RiskLevel.PRECAUCION || s.nivel == RiskLevel.ALTO).count();

        double convergenciaMax = sections.stream()
                .mapToDouble(s -> s.convergencia)
                .max().orElse(0);

        RiskLevel nivelGlobal;
        String recomendacion;

        if (criticas > 0) {
            nivelGlobal = RiskLevel.CRITICO;
            recomendacion = "EVACUACION INMEDIATA. " + criticas + " secciones en riesgo de colapso.";
        } else if (precaucion > 3) {
            nivelGlobal = RiskLevel.ALTO;
            recomendacion = "Instalar soportes adicionales. Monitoreo continuo requerido.";
        } else if (precaucion > 0) {
            nivelGlobal = RiskLevel.PRECAUCION;
            recomendacion = "Aumentar frecuencia de monitoreo. Revisar zonas marcadas.";
        } else {
            nivelGlobal = RiskLevel.ESTABLE;
            recomendacion = "Tunel estable. Continuar monitoreo rutinario.";
        }

        return new TunnelRiskReport(sections, fsGlobal, nivelGlobal,
                criticas, precaucion, convergenciaMax, recomendacion);
    }
}