package com.homehub.devices;

import java.util.Random;

public class MotionSensor extends Devices implements Sensing {
    private static final Random RANDOM = new Random();

    private boolean motionDetected;

    public MotionSensor(String id, String name, String brand, boolean onlineStatus, String room, String modelNumber, double ratedWatts) {
        super(id, name, brand, onlineStatus, room, modelNumber, ratedWatts);
    }

    public boolean isMotionDetected() {
        return motionDetected;
    }

    public void setMotionDetected(boolean motionDetected) {
        this.motionDetected = motionDetected;
    }

    // Simulates a new reading where someone could be in a room
    @Override
    public void readValue() {
        motionDetected = RANDOM.nextBoolean();
    }
}
