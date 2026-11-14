package com.homehub.energy;

import com.homehub.devices.*;
import com.homehub.exceptions.*;
import com.homehub.loaders.*;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnergyHistoryTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 27, 12, 30);

    @TempDir
    Path tempDir;

    private DeviceRegistry registry;
    private EnergyAccountant accountant;

    @BeforeEach
    void setUp() {
        registry = new DeviceRegistry();
        registry.register(new SmartBulb(50, 128, "b1", "LB", "Hall", "Philips", "Hall", true, 60, ""));
        accountant = new EnergyAccountant(registry, START);
    }

    @Test
    void aLongAdvanceIsSplitAtEveryHourBoundary() {
        accountant.advanceTo(LocalDateTime.of(2026, 10, 27, 15, 10));
        List<EnergyRecord> history = accountant.getHistory();
        assertEquals(4, history.size());
        assertEquals(LocalDateTime.of(2026, 10, 27, 12, 0), history.get(0).hour());
        assertEquals(0.03, history.get(0).kwh(), 1e-9);
        assertEquals(0.06, history.get(1).kwh(), 1e-9);
        assertEquals(0.06, history.get(2).kwh(), 1e-9);
        assertEquals(0.01, history.get(3).kwh(), 1e-9);
    }

    @Test
    void theTotalStillEqualsTheSumOfTheHours() {
        accountant.advanceTo(LocalDateTime.of(2026, 10, 27, 15, 10));
        assertEquals(0.16, accountant.getKwhByDevice().get("b1"), 1e-9);
    }

    @Test
    void twoAdvancesInTheSameHourMergeIntoOneRow() {
        accountant.advanceTo(LocalDateTime.of(2026, 10, 27, 12, 40));
        accountant.advanceTo(LocalDateTime.of(2026, 10, 27, 12, 50));
        assertEquals(1, accountant.getHistory().size());
        assertEquals(0.02, accountant.getHistory().get(0).kwh(), 1e-9);
    }

    @Test
    void aDeviceThatIsOffLeavesNoRows() {
        registry.get("b1").setOnline(false);
        accountant.advanceTo(START.plusHours(3));
        assertTrue(accountant.getHistory().isEmpty());
    }

    @Test
    void loadedHistoryIsAddedToTheTotalsAndKept() {
        LocalDateTime hour = LocalDateTime.of(2026, 10, 26, 9, 0);
        accountant.addHistory(List.of(new EnergyRecord(hour, "b1", 0.5)));
        accountant.advanceTo(START.plusHours(1));
        assertEquals(0.56, accountant.getKwhByDevice().get("b1"), 1e-9);
        assertEquals(hour, accountant.getHistory().get(0).hour());
    }

    @Test
    void savedHistoryCanBeLoadedBack() {
        accountant.advanceTo(START.plusHours(2));
        Path file = tempDir.resolve("energy.csv");
        new EnergyLoader().save(file, accountant.getHistory());

        List<EnergyRecord> loaded = new EnergyLoader().load(file);
        assertEquals(accountant.getHistory().size(), loaded.size());
        assertEquals("b1", loaded.get(0).deviceId());
        assertEquals(accountant.getHistory().get(0).kwh(), loaded.get(0).kwh(), 1e-6);
    }

    @Test
    void aNegativeKwhIsRejectedWithTheLineNumber() throws IOException {
        Path file = tempDir.resolve("energy.csv");
        Files.writeString(file, "hour,deviceId,kwh\n2026-10-27T12:00,b1,0.5\n2026-10-27T13:00,b1,-1\n");
        DataLoadException e = assertThrows(DataLoadException.class, () -> new EnergyLoader().load(file));
        assertTrue(e.getMessage().contains("line 3"), e.getMessage());
    }

    @Test
    void aBadHourIsRejected() throws IOException {
        Path file = tempDir.resolve("energy.csv");
        Files.writeString(file, "hour,deviceId,kwh\nnoon,b1,0.5\n");
        assertThrows(DataLoadException.class, () -> new EnergyLoader().load(file));
    }

    @Test
    void aHeaderOnlyFileLoadsAsEmpty() throws IOException {
        Path file = tempDir.resolve("energy.csv");
        Files.writeString(file, "hour,deviceId,kwh\n");
        assertTrue(new EnergyLoader().load(file).isEmpty());
    }
}
