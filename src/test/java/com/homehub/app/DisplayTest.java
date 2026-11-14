package com.homehub.app;

import com.homehub.automation.*;
import com.homehub.devices.*;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class DisplayTest {

    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final Display display = new Display(new PrintStream(bytes));

    private String shown() {
        return bytes.toString().replace("\r\n", "\n").trim();
    }

    @Test
    void stateOfABulbShowsPowerAndBrightness() {
        display.state(new SmartBulb(70, 128, "b1", "M", "Hall Light", "B", "Hall", true, 10, ""));
        assertEquals("Hall Light: ON, brightness 70, colour temperature 128", shown());
    }

    @Test
    void stateOfAnOfflineDeviceSaysOff() {
        display.state(new SmartBulb(70, 128, "b1", "M", "Hall Light", "B", "Hall", false, 10, ""));
        assertEquals("Hall Light: OFF, brightness 70, colour temperature 128", shown());
    }

    @Test
    void stateOfALockShowsLockedOrUnlocked() {
        SmartLock lock = new SmartLock("l1", "Front Door", "Yale", true, "Hall", "Y1", 2, true, "0000", 100);
        display.state(lock);
        lock.setLockStatus(false);
        display.state(lock);
        assertEquals("Front Door: ON, locked, battery 100%\nFront Door: ON, unlocked, battery 100%", shown());
    }

    @Test
    void stateOfABulbShowsItsLastCommandWhenItHasOne() {
        SmartBulb bulb = new SmartBulb(70, 128, "b1", "M", "Hall Light", "B", "Hall", true, 10, "");
        bulb.execute(new Command(Action.COOLER, 40));
        display.state(bulb);
        assertEquals("Hall Light: ON, brightness 70, colour temperature 168 (last command: COOLER 40)", shown());
    }

    @Test
    void stateOfATemperatureAndAHumiditySensorShowsTheirReading() {
        display.state(new TemperatureSensor("tp1", "Thermometer", "Aqara", true, "Hall", "TH-1", 1));
        display.state(new HumiditySensor("h1", "Hygrometer", "Aqara", true, "Bath", "TH-1", 1));
        assertEquals("Thermometer: ON, reading 20\nHygrometer: ON, reading 50", shown());
    }

    @Test
    void stateOfAMediaPlayerShowsVolumeAndPlayback() {
        MediaPlayer p = new MediaPlayer("s1", "Kitchen Speaker", "Sonos", true, "Kitchen", "One", 15, 20, false);
        p.play();
        display.state(p);
        assertEquals("Kitchen Speaker: ON, volume 20, PLAYING", shown());
    }

    @Test
    void aStoppedMediaPlayerShowsStop() {
        display.state(new MediaPlayer("t1", "Living Room TV", "Samsung", true, "Living", "QN90", 120, 20, false));
        assertEquals("Living Room TV: ON, volume 20, STOP", shown());
    }

    @Test
    void stateOfAMutedMediaPlayerSaysSo() {
        display.state(new MediaPlayer("s1", "Kitchen Speaker", "Sonos", true, "Kitchen", "One", 15, 20, true));
        assertTrue(shown().contains("(muted)"), shown());
    }

    @Test
    void stateOfAMotionSensorShowsItsReading() {
        display.state(new MotionSensor("m1", "Hall Motion", "Aqara", true, "Hall", "MS-1", 1));
        assertEquals("Hall Motion: ON, reading 0", shown());
    }

    @Test
    void devicesListsEveryDeviceWithItsPowerState() {
        DeviceRegistry registry = new DeviceRegistry();
        registry.register(new SmartBulb(50, 128, "b1", "M", "Hall Light", "B", "Hallway", false, 12, ""));
        registry.register(new MotionSensor("m1", "Hall Motion", "Aqara", true, "Hallway", "MS-1", 1));

        display.devices(registry);

        String out = shown();
        assertTrue(out.contains("b1") && out.contains("Hall Light") && out.contains("OFF"), out);
        assertTrue(out.contains("m1") && out.contains("Hall Motion") && out.contains("ON"), out);
    }

    @Test
    void rulesShowTheTriggerTheActionAndTheDeviceName() {
        DeviceRegistry registry = new DeviceRegistry();
        registry.register(new SmartBulb(50, 128, "b1", "M", "Hall Light", "B", "Hallway", false, 12, ""));
        Rule rule = new Rule(new EventCondition(EventType.MOTION, "m1", 1, null), "b1", new Command(Action.TURN_ON), 2);

        display.rules(List.of(rule), registry);

        assertEquals("priority 2: when MOTION from m1, value >= 1 -> TURN_ON b1 (Hall Light)", shown());
    }

    @Test
    void rulesFlagADeviceThatNoLongerExists() {
        Rule rule = new Rule(new EventCondition(EventType.DOOR_LOCKED, "l1", 0, null), "gone", new Command(Action.TURN_OFF), 3);

        display.rules(List.of(rule), new DeviceRegistry());

        assertEquals("priority 3: when DOOR_LOCKED from l1 -> TURN_OFF gone (no such device)", shown());
    }

    @Test
    void rulesWrittenAsLambdasStillListWithoutFailing() {
        Rule rule = new Rule(e -> true, "gone", new Command(Action.TURN_OFF), 1);
        display.rules(List.of(rule), new DeviceRegistry());
        assertTrue(shown().contains("custom condition"), shown());
    }

    @Test
    void energyListsKwhPerDevice() {
        display.energy(java.util.Map.of("b1", 0.5));
        String out = shown();
        assertTrue(out.contains("energy used") && out.contains("b1") && out.contains("0.5000"), out);
    }
}
