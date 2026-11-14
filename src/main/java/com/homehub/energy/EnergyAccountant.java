package com.homehub.energy;

import com.homehub.devices.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class EnergyAccountant {

    private final DeviceRegistry registry;
    private final Map<String, Double> kwhByDevice = new LinkedHashMap<>();
    private final Map<LocalDateTime, Map<String, Double>> kwhByHour = new TreeMap<>();
    private LocalDateTime lastBilled;

    public EnergyAccountant(DeviceRegistry registry, LocalDateTime start) {
        this.registry = registry;
        this.lastBilled = start;
    }

    //bills all devices from the last time they were billed up to now
    public void advanceTo(LocalDateTime now) {
        // Going back in time would count energy twice.
        if (now.isBefore(lastBilled)) {
            throw new IllegalArgumentException("Time went backwards: " + now);
        }

        LocalDateTime from = lastBilled;
        do {
            LocalDateTime hour = from.truncatedTo(ChronoUnit.HOURS);
            LocalDateTime next = hour.plusHours(1);
            LocalDateTime to = next.isBefore(now) ? next : now;
            // dividing milliseconds by 3,600,000 to get the hourly equivalent
            double hours = Duration.between(from, to).toMillis() / 3_600_000.0;
            bill(hour, hours);
            from = to;
        } while (from.isBefore(now));

        lastBilled = now;
    }

    //adds kWH to each device's total and the hour it was recorded
    private void bill(LocalDateTime hour, double hours) {
        for (Devices device : registry.all()) {
            if (device instanceof EnergyReporting meter) {
                //power in kilowatts x time in hours
                double kwh = meter.currentPowerWatts() / 1000.0 * hours;
                kwhByDevice.merge(device.getId(), kwh, Double::sum);
                if (kwh > 0) {
                    kwhByHour.computeIfAbsent(hour, h -> new LinkedHashMap<>()).merge(device.getId(), kwh, Double::sum);
                }
            }
        }
    }

    public void addHistory(List<EnergyRecord> records) {
        for (EnergyRecord r : records) {
            kwhByDevice.merge(r.deviceId(), r.kwh(), Double::sum);
            kwhByHour.computeIfAbsent(r.hour(), h -> new LinkedHashMap<>()).merge(r.deviceId(), r.kwh(), Double::sum);
        }
    }

    public List<EnergyRecord> getHistory() {
        List<EnergyRecord> history = new ArrayList<>();
        kwhByHour.forEach((hour, devices) ->
                devices.forEach((id, kwh) -> history.add(new EnergyRecord(hour, id, kwh))));
        return history;
    }

    public DeviceRegistry getRegistry() {
        return this.registry;
    }

    public Map<String, Double> getKwhByDevice() {
        return this.kwhByDevice;
    }

    public LocalDateTime getLastBilled() {
        return this.lastBilled;
    }

    public void setLastBilled(LocalDateTime lastBilled) {
        this.lastBilled = lastBilled;
    }
}
