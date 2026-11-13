package com.homehub;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.Duration;

public class EnergyAccountant {

    
    private final DeviceRegistry registry;
    private final Map<String, Double> kwhByDevice = new LinkedHashMap<>();
    private LocalDateTime lastBilled;

    public EnergyAccountant(DeviceRegistry registry, LocalDateTime start) {
        this.registry = registry;
        this.lastBilled = start;
    }

    public void advanceTo(LocalDateTime now) {
        if (now.isBefore(lastBilled)) {
            throw new IllegalArgumentException("Time went backwards: " + now);
        }

       double hours = Duration.between(lastBilled, now).toMillis() / 3_600_000.0;
        for (Devices device :registry.all()) {
            if (device instanceof EnergyReporting meter) {
            double kwh = meter.currentPowerWatts() / 1000.0 * hours;
            kwhByDevice.merge(device.getId(), kwh, Double::sum);
        }
    }
      lastBilled = now;
    }



    public DeviceRegistry getRegistry() {
        return this.registry;
    }


    public Map<String,Double> getKwhByDevice() {
        return this.kwhByDevice;
    }


    public LocalDateTime getLastBilled() {
        return this.lastBilled;
    }

    public void setLastBilled(LocalDateTime lastBilled) {
        this.lastBilled = lastBilled;
    }
    
}
