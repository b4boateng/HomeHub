package com.homehub.devices;

import com.homehub.exceptions.*;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeviceRegistryTest {

    private DeviceRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DeviceRegistry();
    }

    private SmartBulb bulb(String id) {
        return new SmartBulb(50, 128, id, "LB", "Bulb " + id, "Philips", "Hall", false, 60, "");
    }

    @Test
    void allKeepsRegistrationOrder() {
        registry.register(bulb("c"));
        registry.register(bulb("a"));
        registry.register(bulb("b"));

        List<String> ids = new ArrayList<>();
        registry.all().forEach(d -> ids.add(d.getId()));

        assertEquals(List.of("c", "a", "b"), ids);
    }

    @Test
    void allCannotBeUsedToChangeTheRegistry() {
        registry.register(bulb("a"));
        assertThrows(UnsupportedOperationException.class, () -> registry.all().clear());
    }

    @Test
    void unknownIdThrowsAndNamesTheId() {
        HomeHubException ex = assertThrows(HomeHubException.class, () -> registry.get("ghost"));
        assertEquals("No device with id: ghost", ex.getMessage());
    }

    @Test
    void duplicateIdIsRejectedAndTheFirstDeviceIsKept() {
        SmartBulb first = bulb("a");
        registry.register(first);
        assertThrows(IllegalArgumentException.class, () -> registry.register(bulb("a")));
        assertSame(first, registry.get("a"));
    }
}
