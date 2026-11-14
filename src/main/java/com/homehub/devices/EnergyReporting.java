package com.homehub.devices;

// Lets energy code read power without knowing the device type.
public interface EnergyReporting {
    public double currentPowerWatts();
}
