package com.homehub.devices;

import com.homehub.automation.EventType;

import java.util.Optional;
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

    @Override
    public int getReading() {
        return motionDetected ? 1 : 0;
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
        return EventType.MOTION;
    }

    @Override
    public boolean hasNewEvent() {
        return motionDetected;
    }
}
