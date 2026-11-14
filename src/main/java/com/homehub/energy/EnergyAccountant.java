package com.homehub.energy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.homehub.devices.DeviceRegistry;
import com.homehub.devices.Devices;
import com.homehub.devices.EnergyReporting;

public class EnergyAccountant {

    private final DeviceRegistry registry;
    private final Map<String, Double> kwhByDevice = new LinkedHashMap<>();
    private final Map<LocalDateTime, Map<String, Double>> kwhByHour = new TreeMap<>();
    private final LocalDateTime start;
    private LocalDateTime lastBilled;

    public EnergyAccountant(DeviceRegistry registry, LocalDateTime start) {
        this.registry = registry;
        this.start = start;
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

    //Saves this run’s figures over the hours it covered and keeps every other hour, so replaying the same period twice doesn’t double-count it
    public List<EnergyRecord> mergedWith(List<EnergyRecord> saved) {
        LocalDateTime firstHour = start.truncatedTo(ChronoUnit.HOURS);
        boolean billedAnyTime = lastBilled.isAfter(start);
        List<EnergyRecord> merged = new ArrayList<>();
        for (EnergyRecord r : saved) {
            boolean coveredByThisRun = billedAnyTime && !r.hour().isBefore(firstHour) && r.hour().isBefore(lastBilled);
            if (!coveredByThisRun) {
                merged.add(r);
            }
        }
        merged.addAll(getHistory());
        merged.sort(Comparator.comparing(EnergyRecord::hour).thenComparing(EnergyRecord::deviceId));
        return merged;
    }

    // Returns a read-only copy in device order, so callers can't change the totals.
    public Map<String, Double> getKwhByDevice() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(kwhByDevice));
    }

    public LocalDateTime getLastBilled() {
        return this.lastBilled;
    }
}
