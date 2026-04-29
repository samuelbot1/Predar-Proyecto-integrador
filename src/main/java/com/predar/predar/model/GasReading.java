package com.predar.predar.model;

public class GasReading {

    private double methane;
    private double carbonMonoxide;
    private double oxygen;

    public GasReading(double methane, double carbonMonoxide, double oxygen) {
        this.methane = methane;
        this.carbonMonoxide = carbonMonoxide;
        this.oxygen = oxygen;
    }

    public double getMethane() { return methane; }
    public double getCarbonMonoxide() { return carbonMonoxide; }
    public double getOxygen() { return oxygen; }

    public AlertLevel getMethaneAlert() {
        if (methane > 1.5) return AlertLevel.CRITICAL;
        if (methane > 1.0) return AlertLevel.WARNING;
        return AlertLevel.NORMAL;
    }

    public AlertLevel getCOAlert() {
        if (carbonMonoxide > 50) return AlertLevel.CRITICAL;
        if (carbonMonoxide > 25) return AlertLevel.WARNING;
        return AlertLevel.NORMAL;
    }

    public AlertLevel getOxygenAlert() {
        if (oxygen < 17) return AlertLevel.CRITICAL;
        if (oxygen < 19.5) return AlertLevel.WARNING;
        return AlertLevel.NORMAL;
    }
}