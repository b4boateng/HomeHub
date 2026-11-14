package com.homehub.devices;

import com.homehub.automation.EventType;

import java.util.Optional;
import java.util.Random;

public class TemperatureSensor extends Devices implements Sensing {
    public static final int MIN_CELSIUS = 10;
    public static final int MAX_CELSIUS = 35;

    private static final Random RANDOM = new Random();

    private int temperature = 20;

    public TemperatureSensor(String id, String name, String brand, boolean onlineStatus, String room, String modelNumber, double ratedWatts) {
        super(id, name, brand, onlineStatus, room, modelNumber, ratedWatts);
    }

    public int getTemperature() {
        return temperature;
    }

    // Simulates a new reading between MIN_CELSIUS and MAX_CELSIUS.
    @Override
    public void readValue() {
        temperature = MIN_CELSIUS + RANDOM.nextInt(MAX_CELSIUS - MIN_CELSIUS + 1);
    }

    @Override
    public int getReading() {
        return temperature;
    }

    @Override
    public String describeDetails() {
        return ", reading " + getReading();
    }

    @Override
    public Optional<Sensing> asSensor() {
        return Optional.of(this);
    }

    @Override
    public EventType eventType() {
        return EventType.TEMPERATURE;
    }
}
