package com.homehub.automation;

public enum EventType {
    MOTION,
    DOOR_LOCKED,
    DOOR_UNLOCKED,
    TEMPERATURE,
    HUMIDITY;

    // Door events only say that they happened; the others carry a reading.
    public boolean hasReading() {
        return this != DOOR_LOCKED && this != DOOR_UNLOCKED;
    }
}
