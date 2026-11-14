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
        DeviceControl control = new DeviceControl(registry, powerManager);
        ByteArrayOutputStream captured = new ByteArrayOutputStream();

        new Console(registry, bus, control, powerManager, accountant, events, devicesFile, energyFile,
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
}
