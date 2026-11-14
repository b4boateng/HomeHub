package com.homehub.service;

import java.time.LocalDateTime;
import java.util.function.Consumer;

import com.homehub.automation.Event;
import com.homehub.automation.EventBus;
import com.homehub.devices.DeviceRegistry;
import com.homehub.devices.Devices;
import com.homehub.devices.Sensing;

// Takes readings from the sensors and publishes them as events, so any matching rule can act on them.
public class SensorService {

    private final DeviceRegistry registry;
    private final EventBus bus;

    public SensorService(DeviceRegistry registry, EventBus bus) {
        this.registry = registry;
        this.bus = bus;
    }

    // Reads every online sensor once and tells the caller about each; returns how many were read.
    public int readAll(Consumer<Devices> onRead) {
        int read = 0;
        for (Devices device : registry.all()) {
            Sensing sensor = device.asSensor().orElse(null);
            if (sensor == null || !device.isOnline()) {
                continue;
            }
            read++;
            sensor.readValue();
            onRead.accept(device);
            // "No motion" is not an event, so it is not published.
            if (sensor.hasNewEvent()) {
                bus.publish(new Event(sensor.eventType(), LocalDateTime.now(), device.getId(), sensor.getReading()));
            }
        }
        return read;
    }
}
