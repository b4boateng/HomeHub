package com.homehub.devices;

import java.util.Random;

public class HumiditySensor extends Devices implements Sensing {
    public static final int MIN_PERCENT = 20;
    public static final int MAX_PERCENT = 90;

    private static final Random RANDOM = new Random();

    private int humidity = 50;

    public HumiditySensor(String id, String name, String brand, boolean onlineStatus, String room, String modelNumber, double ratedWatts) {
        super(id, name, brand, onlineStatus, room, modelNumber, ratedWatts);
    }

    public int getHumidity() {
        return humidity;
    }

    // Simulates a new reading between MIN_PERCENT and MAX_PERCENT.
    @Override
    public void readValue() {
        humidity = MIN_PERCENT + RANDOM.nextInt(MAX_PERCENT - MIN_PERCENT + 1);
    }

    @Override
    public int getReading() {
        return humidity;
    }
}
