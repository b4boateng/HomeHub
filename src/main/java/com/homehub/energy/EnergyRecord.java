package com.homehub.energy;

import java.time.LocalDateTime;

// The energy one device used during one clock hour. "hour" is always on the hour, for example 13:00.
public class EnergyRecord {

    private final LocalDateTime hour;
    private final String deviceId;
    private final double kwh;

    public EnergyRecord(LocalDateTime hour, String deviceId, double kwh) {
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
