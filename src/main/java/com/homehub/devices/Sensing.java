package com.homehub.devices;

import com.homehub.automation.EventType;

// devices with the ability to detect environmental changes
public interface Sensing {

    // Takes a new reading; getReading() then returns it.
    void readValue();

    // The latest reading as a whole number: 1 or 0 for motion, degrees for temperature, percent for humidity.
    int getReading();

    // The kind of event this sensor's readings produce.
    EventType eventType();

    // Whether the latest reading is worth publishing. Most are; "no motion" is not.
    default boolean hasNewEvent() {
        return true;
    }
}
