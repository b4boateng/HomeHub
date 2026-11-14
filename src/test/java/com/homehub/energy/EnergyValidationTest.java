package com.homehub.energy;

import com.homehub.devices.*;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

// The checks and protections added to the energy package.
class EnergyValidationTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 27, 20, 0);

    private static SmartBulb bulb(String id, double watts) {
        return new SmartBulb(50, 128, id, "LB", "Bulb " + id, "Philips", "Hall", true, watts, "");
    }

    private static EnergyAccountant accountantWith(String... ids) {
        DeviceRegistry registry = new DeviceRegistry();
        for (String id : ids) {
            registry.register(bulb(id, 1000));
        }
        return new EnergyAccountant(registry, START);
    }

    // ---- EnergyAccountant: results are read-only snapshots ----

    @Test
    void theReturnedTotalsCannotBeChanged() {
        EnergyAccountant accountant = accountantWith("b1");
        accountant.advanceTo(START.plusHours(1));

        assertThrows(UnsupportedOperationException.class, () -> accountant.getKwhByDevice().put("b1", 99.0));
        assertThrows(UnsupportedOperationException.class, () -> accountant.getKwhByDevice().clear());
        assertEquals(1.0, accountant.getKwhByDevice().get("b1"), 1e-9);
    }

    @Test
    void aSnapshotDoesNotChangeWhenMoreTimeIsBilled() {
        EnergyAccountant accountant = accountantWith("b1");
        accountant.advanceTo(START.plusHours(1));
        var snapshot = accountant.getKwhByDevice();

        accountant.advanceTo(START.plusHours(3));

        assertEquals(1.0, snapshot.get("b1"), 1e-9);
        assertEquals(3.0, accountant.getKwhByDevice().get("b1"), 1e-9);
    }

    @Test
    void theTotalsStayInDeviceOrder() {
        // The console prints them as returned, so the order devices were registered in must survive.
        EnergyAccountant accountant = accountantWith("b3", "b1", "b2", "a9", "z0");
        accountant.advanceTo(START.plusHours(1));

        assertEquals(List.of("b3", "b1", "b2", "a9", "z0"), List.copyOf(accountant.getKwhByDevice().keySet()));
    }

    // ---- EnergyRecord ----

    @Test
    void zeroKwhIsAllowed() {
        assertEquals(0, new EnergyRecord(START, "b1", 0).kwh());
    }

    @Test
    void aRecordNeedsAnHour() {
        assertThrows(IllegalArgumentException.class, () -> new EnergyRecord(null, "b1", 1));
    }

    @Test
    void aRecordNeedsADeviceId() {
        assertThrows(IllegalArgumentException.class, () -> new EnergyRecord(START, null, 1));
        assertThrows(IllegalArgumentException.class, () -> new EnergyRecord(START, "", 1));
        assertThrows(IllegalArgumentException.class, () -> new EnergyRecord(START, "   ", 1));
    }

    @Test
    void aRecordRefusesEnergyThatIsNegativeOrNotARealNumber() {
        for (double bad : new double[] {-0.001, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new EnergyRecord(START, "b1", bad), "kwh " + bad);
        }
    }

    // ---- PowerManager and PowerPolicy ----

    @Test
    void thePowerLimitMustBeFiniteAndNotNegative() {
        for (double bad : new double[] {-1, -0.001, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new PowerManager(bad), "limit " + bad);
        }
    }

    @Test
    void zeroAndOrdinaryLimitsAreFine() {
        assertEquals(0, new PowerManager(0).getLimitWatts());
        assertEquals(3000, new PowerManager(3000).getLimitWatts());
        assertEquals(1e9, new PowerManager(1e9).getLimitWatts());
    }

    @Test
    void exactlyAtTheLimitIsAllowedAndAboveIsNot() {
        PowerManager manager = new PowerManager(100);
        assertTrue(manager.allows(60, 40));
        assertFalse(manager.allows(60, 40.001));
    }

}
