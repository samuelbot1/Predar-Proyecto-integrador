package com.predar.predar.simulation;

import com.predar.predar.model.GasReading;
import java.util.Random;

public class SensorSimulator {

    private final Random random = new Random();

    private double methane = 0.5;
    private double carbonMonoxide = 10.0;
    private double oxygen = 20.9;

    public GasReading getNextReading() {
        methane = clamp(methane + randomDelta(0.3), 0.0, 3.0);
        carbonMonoxide = clamp(carbonMonoxide + randomDelta(8.0), 0.0, 100.0);
        oxygen = clamp(oxygen + randomDelta(0.4), 15.0, 21.0);

        return new GasReading(methane, carbonMonoxide, oxygen);
    }

    private double randomDelta(double range) {
        return (random.nextDouble() * 2 - 1) * range;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
} 