package com.homehub.app;

import com.homehub.automation.*;
import com.homehub.devices.*;
import com.homehub.energy.*;
import com.homehub.loaders.*;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsoleTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 27, 12, 0);

    private DeviceRegistry registry;
    private EventBus bus;
    private SmartBulb light;
    private EnergyAccountant accountant;
    private List<Event> events;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        devicesFile = tempDir.resolve("devices.csv");
        energyFile = tempDir.resolve("energy.csv");
        Files.writeString(devicesFile, "type,id,name,brand,model,room,watts,online\n");
        registry = new DeviceRegistry();
        bus = new EventBus();
        light = new SmartBulb(50, 128, "b1", "LB", "Hall Light", "Philips", "Hallway", false, 60, "");
        registry.register(light);
        accountant = new EnergyAccountant(registry, START);
        events = List.of(new Event(EventType.MOTION, START, "m1", 1));
    }

    private Path devicesFile;
    private Path energyFile;

    private String play(double limitWatts, String script) {
        PowerManager powerManager = new PowerManager(limitWatts);
        RuleEngine engine = new RuleEngine(registry, bus, powerManager, s -> { });
        engine.addRule(new Rule(e -> e.type() == EventType.MOTION, "b1", new Command(Action.TURN_ON), 1));
        engine.addRule(new Rule(e -> e.type() == EventType.DOOR_UNLOCKED, "b1", new Command(Action.TURN_ON), 2));
        DeviceControl control = new DeviceControl(registry, powerManager);
        ByteArrayOutputStream captured = new ByteArrayOutputStream();

        new Console(registry, bus, engine, control, powerManager, accountant, events, devicesFile, energyFile,
                new BufferedReader(new StringReader(script)),
                new PrintStream(captured, true, StandardCharsets.UTF_8)).run();

        return captured.toString(StandardCharsets.UTF_8);
    }

    @Test
    void zeroQuitsTheProgram() {
        assertTrue(play(1000, "0\n").contains("The HomeHub session has ended"));
    }

    @Test
    void endOfInputQuitsInsteadOfLoopingForever() {
        assertTrue(play(1000, "").contains("The HomeHub session has ended"));
    }

    @Test
    void anInvalidChoiceIsRefusedAndAskedAgain() {
        String output = play(1000, "abc\n99\n0\n");
        assertTrue(output.contains("Invalid selection. Please enter a number between 0 and 8"), output);
        assertTrue(output.contains("The HomeHub session has ended"));
    }

    @Test
    void seeMyDevicesListsEachDeviceInPlainWords() {
        assertTrue(play(1000, "1\n0\n").contains("Hall Light (Hallway): off"));
    }

    @Test
    void controllingADeviceTakesThreePicksAndWorks() {
        String output = play(1000, "2\n1\n1\n0\n");
        assertTrue(light.isOnline());
        assertTrue(output.contains("Completed: Turn on - Hall Light."), output);
    }

    @Test
    void goingBackFromTheDeviceListChangesNothing() {
        play(1000, "2\n0\n0\n");
        assertFalse(light.isOnline());
    }

    @Test
    void theOverLimitMessageIsShownAndTheDeviceStaysOff() {
        String output = play(50, "2\n1\n1\n0\n");
        assertFalse(light.isOnline());
        assertTrue(output.contains("The request could not be completed"), output);
    }

    @Test
    void firingAnEventLetsTheRulesReact() {
        play(1000, "4\n1\n0\n");
        assertTrue(light.isOnline());
    }

    @Test
    void letTimePassMovesTheClockAndBillsEnergy() {
        light.setOnline(true);
        String output = play(1000, "5\n2\n0\n");
        assertEquals(0.06, accountant.getKwhByDevice().get("b1"), 1e-9);
        assertTrue(output.contains("The clock was advanced by 1h 0m"), output);
    }

    @Test
    void advancingTheClockSavesTheHourlyHistory() throws IOException {
        light.setOnline(true);
        play(1000, "5\n3\n0\n");
        List<EnergyRecord> saved = new EnergyLoader().load(energyFile);
        assertEquals(6, saved.size());
        assertEquals(LocalDateTime.of(2026, 10, 27, 12, 0), saved.get(0).hour());
        assertEquals("b1", saved.get(0).deviceId());
        assertEquals(0.06, saved.get(0).kwh(), 1e-9);
    }

    @Test
    void nothingIsSavedWhenNoTimePasses() {
        play(1000, "4\n1\n0\n");
        assertFalse(Files.exists(energyFile));
    }

    @Test
    void untilTonightSkipsToTenOClock() {
        play(1000, "5\n4\n0\n");
        assertEquals(LocalDateTime.of(2026, 10, 27, 22, 0), accountant.getLastBilled());
    }

    @Test
    void showPowerReportsTheLoadAndTheLimit() {
        light.setOnline(true);
        String output = play(1000, "6\n0\n");
        assertTrue(output.contains("Current consumption is 60 W") && output.contains("limit is 1000 W"), output);
    }

    @Test
    void energyBeforeAnyTimePassesSaysSo() {
        assertTrue(play(1000, "7\n0\n").contains("No energy consumption has been recorded"));
    }

    @Test
    void addingADeviceRegistersItWithTheNextFreeId() {
        play(1000, "8\n1\nDesk Lamp\nStudy\nIkea\nL1\n9\n1\n0\n");
        assertEquals("Desk Lamp", registry.get("b2").getName());
        assertEquals(9, registry.get("b2").getRatedWatts());
    }

    @Test
    void cancellingAtTheLastStepAddsNothing() throws IOException {
        play(1000, "8\n1\nDesk Lamp\nStudy\nIkea\nL1\n9\n0\n0\n");
        assertFalse(registry.contains("b2"));
        assertEquals(1, Files.readAllLines(devicesFile).size());
    }

    @Test
    void anAddedDeviceIsAppendedToTheCsvAndCanBeLoadedBack() throws IOException {
        play(1000, "8\n1\nDesk, Lamp\nStudy\nIkea\nL1\n9\n1\n0\n");
        assertEquals("BULB,b2,Desk  Lamp,Ikea,L1,Study,9.0,true", Files.readAllLines(devicesFile).get(1));
        Devices loaded = new DevicesLoader().load(devicesFile).get(0);
        assertEquals("b2", loaded.getId());
        assertEquals(9, loaded.getRatedWatts());
    }

    @Test
    void aMissingTrailingNewlineDoesNotMergeTwoRows() throws IOException {
        Files.writeString(devicesFile, "type,id,name,brand,model,room,watts,online\nBULB,b9,X,Y,Z,R,5,false");
        play(1000, "8\n1\nDesk Lamp\nStudy\nIkea\nL1\n9\n1\n0\n");
        assertEquals(2, new DevicesLoader().load(devicesFile).size());
    }

    @Test
    void aBadWattsAnswerIsAskedAgain() {
        String output = play(1000, "8\n1\nDesk Lamp\nStudy\nIkea\nL1\nlots\n9\n1\n0\n");
        assertTrue(output.contains("Please enter a non-negative number"), output);
        assertTrue(registry.contains("b2"));
    }

    @Test
    void controllingAnOfflineDeviceOtherThanTurnOnIsRefused() {
        String output = play(1000, "2\n1\n3\n1\n0\n");
        assertTrue(output.contains("Hall Light is offline") || output.contains("Hall Light cannot"), output);
        assertEquals(50, light.getBrightness());
    }

    @Test
    void eventsAreOfferedPerDeviceAndLockEventsMoveTheLock() {
        SmartLock lock = new SmartLock("l1", "Front Door", "Yale", true, "Hallway", "YL", 2, true, "0000", 100);
        registry.register(lock);
        String output = play(1000, "4\n1\n2\n0\n");
        assertTrue(output.contains("The door was unlocked"), output);
        assertFalse(lock.getLockStatus());
    }

    /** Humidity sensor whose readings are scripted, so the tests are not random. */
    private static class ScriptedHumidity extends HumiditySensor {
        private int value;
        private final int next;

        ScriptedHumidity(int start, int next) {
            super("h1", "HS", "Bathroom Humidity", "Aqara", true, "Bathroom", 1, start);
            this.value = start;
            this.next = next;
        }

        @Override
        public int getHumidity() {
            return value;
        }

        @Override
        public void readValue() {
            value = next;
        }
    }

    private static class ScriptedMotion extends MotionSensor {
        private final boolean detects;

        ScriptedMotion(boolean detects) {
            super("m1", "Hall Motion Sensor", "Aqara", true, "Hallway", "MS", 1);
            this.detects = detects;
        }

        @Override
        public void readValue() {
            setMotionDetected(detects);
        }
    }

    private List<Event> record() {
        List<Event> seen = new java.util.ArrayList<>();
        bus.subscribe(seen::add);
        return seen;
    }

    @Test
    void lockingAndUnlockingFromTheControlMenuPublishDoorEvents() {
        registry.register(new SmartLock("l1", "Front Door", "Yale", true, "Hallway", "YL", 2, false, "0000", 100));
        List<Event> seen = record();

        play(1000, "2\n2\n1\n0\n");   // lock
        play(1000, "2\n2\n2\n0\n");   // unlock

        assertEquals(List.of(EventType.DOOR_LOCKED, EventType.DOOR_UNLOCKED), seen.stream().map(Event::type).toList());
        assertEquals("l1", seen.get(0).sourceId());
    }

    @Test
    void controllingALightPublishesNoEvent() {
        List<Event> seen = record();
        play(1000, "2\n1\n1\n0\n");
        assertTrue(seen.isEmpty());
    }

    @Test
    void aHumidityReadingThatCrossesTheThresholdPublishesAnEvent() {
        registry.register(new ScriptedHumidity(40, 85));
        List<Event> seen = record();
        PowerManager pm = new PowerManager(1000);
        RuleEngine engine = new RuleEngine(registry, bus, pm, s -> { });
        engine.addRule(new Rule(e -> e.type() == EventType.HUMIDITY && e.value() >= 70, "b1", new Command(Action.TURN_ON), 1));

        new Console(registry, bus, engine, new DeviceControl(registry, pm), pm, accountant, events, devicesFile, energyFile,
                new BufferedReader(new StringReader("3\n1\n0\n")),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8)).run();

        assertEquals(1, seen.size());
        assertEquals(EventType.HUMIDITY, seen.get(0).type());
        assertEquals(85, seen.get(0).value());
        assertTrue(light.isOnline());
    }

    @Test
    void aHumidityReadingThatStaysOverTheThresholdDoesNotRepublish() {
        registry.register(new ScriptedHumidity(75, 90));
        List<Event> seen = record();
        PowerManager pm = new PowerManager(1000);
        RuleEngine engine = new RuleEngine(registry, bus, pm, s -> { });
        engine.addRule(new Rule(e -> e.type() == EventType.HUMIDITY && e.value() >= 70, "b1", new Command(Action.TURN_ON), 1));

        new Console(registry, bus, engine, new DeviceControl(registry, pm), pm, accountant, events, devicesFile, energyFile,
                new BufferedReader(new StringReader("3\n1\n0\n")),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8)).run();

        assertTrue(seen.isEmpty());
        assertFalse(light.isOnline());
    }

    @Test
    void aHumidityReadingBelowTheThresholdPublishesNothing() {
        registry.register(new ScriptedHumidity(40, 50));
        List<Event> seen = record();
        play(1000, "3\n1\n0\n");
        assertTrue(seen.isEmpty());
    }

    @Test
    void humiditySensorsAreNotOfferedInTheEventMenu() {
        registry.register(new ScriptedHumidity(40, 40));
        PowerManager pm = new PowerManager(1000);
        RuleEngine engine = new RuleEngine(registry, bus, pm, s -> { });
        engine.addRule(new Rule(e -> e.type() == EventType.HUMIDITY && e.value() >= 70, "b1", new Command(Action.TURN_ON), 1));

        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        new Console(registry, bus, engine, new DeviceControl(registry, pm), pm, accountant, events, devicesFile, energyFile,
                new BufferedReader(new StringReader("4\n0\n")),
                new PrintStream(captured, true, StandardCharsets.UTF_8)).run();

        assertFalse(captured.toString(StandardCharsets.UTF_8).contains("Bathroom Humidity"));
    }

    @Test
    void readingAMotionSensorThatDetectsSomeonePublishesMotionAndRulesReact() {
        registry.register(new ScriptedMotion(true));
        List<Event> seen = record();

        String output = play(1000, "3\n1\n0\n");

        assertEquals(1, seen.size());
        assertEquals(EventType.MOTION, seen.get(0).type());
        assertEquals("m1", seen.get(0).sourceId());
        assertEquals(1, seen.get(0).value());
        assertTrue(light.isOnline());
        assertTrue(output.contains("detected motion in the Hallway"), output);
    }

    @Test
    void readingAMotionSensorThatSeesNobodyPublishesNothing() {
        registry.register(new ScriptedMotion(false));
        List<Event> seen = record();

        String output = play(1000, "3\n1\n0\n");

        assertTrue(seen.isEmpty());
        assertFalse(light.isOnline());
        assertTrue(output.contains("detected no motion"), output);
    }

    @Test
    void aRegisteredMotionSensorCanBeFiredFromTheEventMenu() {
        registry.register(new ScriptedMotion(false));
        List<Event> seen = record();

        // events list also holds m1, which is now a registered device, so it must be offered only once
        String output = play(1000, "4\n1\n0\n");

        assertEquals(1, seen.size());
        assertEquals("m1", seen.get(0).sourceId());
        assertFalse(output.contains("Motion sensor m1"), output);
    }
}
