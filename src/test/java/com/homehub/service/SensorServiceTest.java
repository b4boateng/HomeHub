package com.homehub.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.homehub.automation.Event;
import com.homehub.automation.EventBus;
import com.homehub.automation.EventType;
import com.homehub.devices.DeviceRegistry;
import com.homehub.devices.Devices;
import com.homehub.devices.HumiditySensor;
import com.homehub.devices.SmartBulb;
import com.homehub.devices.TemperatureSensor;

class SensorServiceTest {

    private DeviceRegistry registry;
    private List<Event> published;
    private SensorService service;

    @BeforeEach
    void setUp() {
        registry = new DeviceRegistry();
        EventBus bus = new EventBus();
        published = new ArrayList<>();
        bus.subscribe(published::add);
        service = new SensorService(registry, bus);
    }

    @Test
    void everyOnlineSensorIsReadAndReported() {
        registry.register(new TemperatureSensor("tp1", "Thermo", "A", true, "Lounge", "M", 1));
        registry.register(new HumiditySensor("h1", "Hygro", "A", true, "Bath", "M", 1));
        List<Devices> shown = new ArrayList<>();

        int read = service.readAll(shown::add);

        assertEquals(2, read);
        assertEquals(2, shown.size());
    }

    @Test
    void aReadingIsPublishedAsAnEventFromThatSensor() {
        registry.register(new TemperatureSensor("tp1", "Thermo", "A", true, "Lounge", "M", 1));

        service.readAll(device -> { });

        assertEquals(1, published.size());
        Event event = published.get(0);
        assertEquals(EventType.TEMPERATURE, event.type());
        assertEquals("tp1", event.sourceId());
        assertTrue(event.value() >= TemperatureSensor.MIN_CELSIUS && event.value() <= TemperatureSensor.MAX_CELSIUS);
    }

    @Test
    void offlineSensorsAndOtherDevicesAreSkipped() {
        registry.register(new TemperatureSensor("tp1", "Thermo", "A", false, "Lounge", "M", 1));
        registry.register(new SmartBulb(50, 128, "b1", "M", "Lamp", "B", "Hall", true, 10, ""));

        int read = service.readAll(device -> fail("nothing should be read"));

        assertEquals(0, read);
        assertTrue(published.isEmpty());
    }
}
