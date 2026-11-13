package com.homehub;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PowerAndEnergyTest {

    @Test
    void sixtyWattLightOnForTwoHoursUsesPointOneTwoKwh() {
        DeviceRegistry registry = new DeviceRegistry();
        registry.register(new SmartBulb(50, 128, "b1", "LB", "Bulb", "Philips", "Hall", true, 60, ""));
        LocalDateTime start = LocalDateTime.of(2026, 10, 27, 20, 0);
        EnergyAccountant accountant = new EnergyAccountant(registry, start);

        accountant.advanceTo(start.plusHours(2));

        assertEquals(0.12, accountant.getKwhByDevice().get("b1"), 1e-9);
    }

    @Test
    void loadExactlyAtTheLimitIsAllowed() {
        assertTrue(new PowerManager(100).allows(60, 40));
    }
}
