package com.homehub.energy;

import java.time.LocalDateTime;

// kwh used by a device in one clock hour
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
