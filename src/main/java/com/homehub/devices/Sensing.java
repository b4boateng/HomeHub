package com.homehub.devices;
// devices with the ability to detect environmental changes
public interface Sensing {

    // Takes a new reading; getReading() then returns it.
    void readValue();

    // The latest reading as a whole number: 1 or 0 for motion, degrees for temperature, percent for humidity.
    int getReading();
}
