package com.homehub.energy;

import java.time.LocalDateTime;

// kwh used by a device in one clock hour
public class EnergyRecord {

    private final LocalDateTime hour;
    private final String deviceId;
    private final double kwh;

    public EnergyRecord(LocalDateTime hour, String deviceId, double kwh) {
        if (hour == null) {
            throw new IllegalArgumentException("Hour cannot be null");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("Device ID cannot be empty");
        }
        if (!Double.isFinite(kwh) || kwh < 0) {
            throw new IllegalArgumentException("kWh must be finite and non-negative");
        }
        this.hour = hour;
        this.deviceId = deviceId;
        this.kwh = kwh;
    }

    public LocalDateTime hour() {
        return hour;
    }

    public String deviceId() {
        return deviceId;
    }

    public double kwh() {
        return kwh;
    }
}
