package com.homehub.energy;

import com.homehub.devices.*;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnergyAccountantTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 27, 20, 0);

    private DeviceRegistry registry;
    private EnergyAccountant accountant;

    @BeforeEach
    void setUp() {
        registry = new DeviceRegistry();
        accountant = new EnergyAccountant(registry, START);
    }

    private SmartBulb bulb(String id, double watts, boolean on) {
        return new SmartBulb(50, 128, id, "LB", "Bulb " + id, "Philips", "Hall", on, watts, "");
    }

    @Test
    void startsWithNothingBilledAndRemembersTheStartTime() {
        assertTrue(accountant.getKwhByDevice().isEmpty());
        assertEquals(START, accountant.getLastBilled());
    }

    @Test
    void advancingMovesLastBilledForward() {
        accountant.advanceTo(START.plusMinutes(30));
        assertEquals(START.plusMinutes(30), accountant.getLastBilled());
    }

    @Test
    void usageAddsUpAcrossSeveralAdvances() {
        registry.register(bulb("b1", 60, true));

        accountant.advanceTo(START.plusHours(1));
        accountant.advanceTo(START.plusHours(2));

        assertEquals(0.12, accountant.getKwhByDevice().get("b1"), 1e-9);
    }

    @Test
    void eachDeviceIsBilledAtItsOwnWattage() {
        registry.register(bulb("small", 60, true));
        registry.register(bulb("big", 120, true));

        accountant.advanceTo(START.plusHours(1));

        assertEquals(0.06, accountant.getKwhByDevice().get("small"), 1e-9);
        assertEquals(0.12, accountant.getKwhByDevice().get("big"), 1e-9);
    }

    @Test
    void deviceThatIsOffUsesNothing() {
        registry.register(bulb("b1", 60, false));

        accountant.advanceTo(START.plusHours(5));

        assertEquals(0.0, accountant.getKwhByDevice().get("b1"), 1e-9);
    }

    @Test
    void devicesThatDoNotReportEnergyAreNotBilled() {
        registry.register(new SmartLock("l1", "Door", "Yale", true, "Hall", "YL", 2, true, "1234", 90));
        registry.register(new MotionSensor("m1", "Motion", "Aqara", true, "Bath", "MS-1", 1));

        accountant.advanceTo(START.plusHours(1));

        assertTrue(accountant.getKwhByDevice().isEmpty());
    }

    @Test
    void goingBackwardsIsRejectedAndLeavesTheClockAlone() {
        assertThrows(IllegalArgumentException.class, () -> accountant.advanceTo(START.minusSeconds(1)));
        assertEquals(START, accountant.getLastBilled());
    }
}
