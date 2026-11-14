package com.homehub.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.homehub.automation.Event;
import com.homehub.automation.EventBus;
import com.homehub.automation.EventType;
import com.homehub.devices.DeviceRegistry;
import com.homehub.devices.SmartBulb;
import com.homehub.exceptions.HomeHubException;

class SimulationRunnerTest {

    @TempDir
    Path folder;

    private DeviceRegistry registry;
    private EventBus bus;
    private Path energyFile;
    private SimulationRunner runner;

    @BeforeEach
    void setUp() {
        registry = new DeviceRegistry();
        // A 1000 W bulb that is already on, so one hour of simulated time is exactly 1 kWh.
        registry.register(new SmartBulb(50, 128, "b1", "M", "Lamp", "B", "Hall", true, 1000, ""));
        bus = new EventBus();
        energyFile = folder.resolve("energy.csv");
        runner = new SimulationRunner(registry, bus, energyFile);
    }

    private static Event motionAt(String time) {
        return new Event(EventType.MOTION, LocalDateTime.parse(time), "m1", 1);
    }

    @Test
    void billsFromTheFirstEventToOneHourAfterTheLast() {
        SimulationRunner.Result result = runner.run(List.of(motionAt("2026-10-27T12:00")), new ArrayList<String>()::add);

        assertEquals(1.0, result.kwhByDevice().get("b1"), 1e-9);
        assertEquals(1, result.savedRecords());
    }

    @Test
    void writesTheHistoryToTheEnergyFile() throws IOException {
        runner.run(List.of(motionAt("2026-10-27T12:00")), msg -> { });

        String text = Files.readString(energyFile);
        assertTrue(text.startsWith("hour,deviceId,kwh"), text);
        assertTrue(text.contains("2026-10-27T12:00,b1,1.000000"), text);
    }

    @Test
    void runningTheSamePeriodTwiceDoesNotCountItTwice() {
        runner.run(List.of(motionAt("2026-10-27T12:00")), msg -> { });
        SimulationRunner.Result second = runner.run(List.of(motionAt("2026-10-27T12:00")), msg -> { });

        assertEquals(1, second.savedRecords());
    }

    @Test
    void keepsHoursFromEarlierRunsThatThisRunDidNotCover() {
        runner.run(List.of(motionAt("2026-10-27T12:00")), msg -> { });
        SimulationRunner.Result later = runner.run(List.of(motionAt("2026-10-28T12:00")), msg -> { });

        assertEquals(2, later.savedRecords());
    }

    @Test
    void logsEachEventItReplays() {
        List<String> log = new ArrayList<>();
        runner.run(List.of(motionAt("2026-10-27T12:00")), log::add);

        assertTrue(log.stream().anyMatch(line -> line.contains("MOTION from m1")), log.toString());
    }

    @Test
    void noEventsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> runner.run(List.of(), msg -> { }));
    }

    @Test
    void anUnreadableEnergyFileStopsTheRunBeforeAnyEventIsReplayed() throws IOException {
        Files.writeString(energyFile, "hour,deviceId,kwh\nnot-a-time,b1,1\n");
        List<String> log = new ArrayList<>();

        assertThrows(HomeHubException.class, () -> runner.run(List.of(motionAt("2026-10-27T12:00")), log::add));
        assertTrue(log.isEmpty(), log.toString());
    }
}
