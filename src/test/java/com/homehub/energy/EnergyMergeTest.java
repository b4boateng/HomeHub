package com.homehub.energy;

import com.homehub.devices.*;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

// How a simulation run is folded into the saved history.
class EnergyMergeTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 27, 12, 30);

    private static LocalDateTime at(int hour) {
        return LocalDateTime.of(2026, 10, 27, hour, 0);
    }

    private static EnergyRecord rec(int hour, String id, double kwh) {
        return new EnergyRecord(at(hour), id, kwh);
    }

    private static DeviceRegistry registryWith(boolean online) {
        DeviceRegistry registry = new DeviceRegistry();
        registry.register(new SmartBulb(50, 128, "b1", "LB", "Hall", "Philips", "Hall", online, 60, ""));
        return registry;
    }

    // hour, device and kwh as text, so two lists can be compared
    private static List<String> show(List<EnergyRecord> records) {
        return records.stream().map(r -> r.hour() + " " + r.deviceId() + " " + String.format("%.6f", r.kwh())).toList();
    }

    @Test
    void hoursThisRunCoveredAreReplacedNotAddedTo() {
        EnergyAccountant accountant = new EnergyAccountant(registryWith(true), START);
        accountant.advanceTo(LocalDateTime.of(2026, 10, 27, 14, 10));     // covers the 12:00, 13:00 and 14:00 hours

        List<EnergyRecord> merged = accountant.mergedWith(List.of(rec(13, "b1", 9.0)));

        assertEquals(List.of(
                "2026-10-27T12:00 b1 0.030000",
                "2026-10-27T13:00 b1 0.060000",
                "2026-10-27T14:00 b1 0.010000"), show(merged));
    }

    @Test
    void hoursOutsideThisRunAreKept() {
        EnergyAccountant accountant = new EnergyAccountant(registryWith(true), START);
        accountant.advanceTo(LocalDateTime.of(2026, 10, 27, 14, 10));

        List<EnergyRecord> merged = accountant.mergedWith(List.of(rec(11, "b1", 1.0), rec(15, "b1", 2.0)));

        assertEquals(5, merged.size());
        assertTrue(show(merged).contains("2026-10-27T11:00 b1 1.000000"));
        assertTrue(show(merged).contains("2026-10-27T15:00 b1 2.000000"));
    }

    @Test
    void anHourThisRunBilledAsZeroRemovesTheOldFigure() {
        EnergyAccountant accountant = new EnergyAccountant(registryWith(false), START);   // device off: no usage
        accountant.advanceTo(LocalDateTime.of(2026, 10, 27, 14, 0));

        List<EnergyRecord> merged = accountant.mergedWith(List.of(rec(13, "b1", 5.0), rec(9, "b1", 1.0)));

        assertEquals(List.of("2026-10-27T09:00 b1 1.000000"), show(merged));
    }

    @Test
    void theHourAfterTheRunEndsExactlyOnTheHourIsKept() {
        EnergyAccountant accountant = new EnergyAccountant(registryWith(true),
                LocalDateTime.of(2026, 10, 27, 12, 0));
        accountant.advanceTo(at(14));                                      // 12:00-14:00 billed; the 14:00 hour never started

        List<EnergyRecord> merged = accountant.mergedWith(List.of(rec(14, "b1", 7.0)));

        assertTrue(show(merged).contains("2026-10-27T14:00 b1 7.000000"));
        assertEquals(3, merged.size());
    }

    @Test
    void mergingTheSameRunAgainGivesTheSameHistory() {
        EnergyAccountant accountant = new EnergyAccountant(registryWith(true), START);
        accountant.advanceTo(LocalDateTime.of(2026, 10, 27, 14, 10));
        List<EnergyRecord> saved = List.of(rec(9, "b1", 1.0), rec(13, "b1", 9.0), rec(20, "b1", 2.0));

        List<EnergyRecord> once = accountant.mergedWith(saved);
        List<EnergyRecord> twice = accountant.mergedWith(once);

        assertEquals(show(once), show(twice));
    }

    @Test
    void theResultIsSortedByHourThenDevice() {
        EnergyAccountant accountant = new EnergyAccountant(registryWith(true), START);
        accountant.advanceTo(LocalDateTime.of(2026, 10, 27, 13, 0));

        List<EnergyRecord> merged = accountant.mergedWith(List.of(rec(11, "z9", 1.0), rec(11, "a1", 1.0), rec(8, "m5", 1.0)));

        assertEquals(List.of("m5", "a1", "z9", "b1"), merged.stream().map(EnergyRecord::deviceId).toList());
    }

    @Test
    void aRunThatBilledNothingChangesNothing() {
        EnergyAccountant accountant = new EnergyAccountant(registryWith(true), START);
        List<EnergyRecord> saved = List.of(rec(12, "b1", 1.0), rec(13, "b1", 2.0));

        assertEquals(show(saved), show(accountant.mergedWith(saved)));
    }

    @Test
    void mergingDoesNotChangeTheListItWasGivenOrTheAccountantsTotals() {
        EnergyAccountant accountant = new EnergyAccountant(registryWith(true), START);
        accountant.advanceTo(at(14));
        List<EnergyRecord> saved = List.of(rec(13, "b1", 9.0));

        accountant.mergedWith(saved);

        assertEquals(9.0, saved.get(0).kwh());
        assertEquals(0.09, accountant.getKwhByDevice().get("b1"), 1e-9);   // 12:30 to 14:00 is 1.5 hours at 60 W
    }
}
