package com.homehub.automation;

import com.homehub.devices.*;
import com.homehub.energy.PowerManager;
import com.homehub.exceptions.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// The one place that decides whether a device may be given a command.
class DeviceControlTest {

    private DeviceRegistry registry;
    private DeviceControl control;

    @BeforeEach
    void setUp() {
        registry = new DeviceRegistry();
        registry.register(new SmartBulb(50, 128, "b1", "LB", "Hall Light", "Philips", "Hall", false, 60, ""));
        registry.register(new SmartLock("l1", "Front Door", "Yale", true, "Hall", "Y1", 2, true, "0000", 100));
        registry.register(new MotionSensor("m1", "Hall Motion", "Aqara", true, "Hall", "MS-1", 1));
        registry.register(new MediaPlayer("t1", "Living TV", "Samsung", false, "Living", "QN90", 120, 20, false));
        control = new DeviceControl(registry, new PowerManager(100));
    }

    // ---- requireSupports: asked before the console wastes a question on an impossible command ----

    @Test
    void requireSupportsAcceptsWhatTheDeviceCanDo() {
        assertDoesNotThrow(() -> control.requireSupports("b1", Action.DIM));
        assertDoesNotThrow(() -> control.requireSupports("l1", Action.UNLOCK));
        assertDoesNotThrow(() -> control.requireSupports("t1", Action.PLAY));
    }

    @Test
    void requireSupportsRefusesAnActionTheDeviceCannotDo() {
        HomeHubException e = assertThrows(HomeHubException.class, () -> control.requireSupports("l1", Action.DIM));
        assertEquals("Front Door cannot DIM", e.getMessage());
    }

    @Test
    void requireSupportsRefusesAnythingForASensor() {
        HomeHubException e = assertThrows(HomeHubException.class, () -> control.requireSupports("m1", Action.TURN_ON));
        assertEquals("Hall Motion cannot TURN_ON", e.getMessage());
    }

    @Test
    void requireSupportsReportsAnUnknownDevice() {
        HomeHubException e = assertThrows(HomeHubException.class, () -> control.requireSupports("zzz", Action.TURN_ON));
        assertEquals("No device with id: zzz", e.getMessage());
    }

    @Test
    void requireSupportsDoesNotChangeAnything() {
        control.requireSupports("b1", Action.TURN_ON);
        assertFalse(registry.get("b1").isOnline());
    }

    // ---- send ----

    @Test
    void sendRunsASupportedCommand() {
        control.send("b1", new Command(Action.TURN_ON));
        assertTrue(registry.get("b1").isOnline());
    }

    @Test
    void sendRefusesTheSameCasesRequireSupportsDoes() {
        assertThrows(HomeHubException.class, () -> control.send("l1", new Command(Action.DIM, 5)));
        assertThrows(HomeHubException.class, () -> control.send("m1", new Command(Action.TURN_ON)));
        assertThrows(HomeHubException.class, () -> control.send("zzz", new Command(Action.TURN_ON)));
    }

    @Test
    void anOfflineDeviceRefusesEverythingExceptSwitchingOnOrOff() {
        HomeHubException e = assertThrows(HomeHubException.class,
                () -> control.send("b1", new Command(Action.BRIGHTEN, 10)));
        assertEquals("Hall Light is offline", e.getMessage());

        HomeHubException tv = assertThrows(HomeHubException.class, () -> control.send("t1", new Command(Action.PLAY)));
        assertEquals("Living TV is offline", tv.getMessage());

        assertDoesNotThrow(() -> control.send("b1", new Command(Action.TURN_ON)));
        assertDoesNotThrow(() -> control.send("b1", new Command(Action.TURN_OFF)));
    }

    @Test
    void switchingOnIsRefusedWhenItWouldExceedThePowerLimit() {
        registry.register(new SmartBulb(50, 128, "big", "X", "Floodlight", "X", "Yard", false, 500, ""));

        HomeHubException e = assertThrows(HomeHubException.class, () -> control.send("big", new Command(Action.TURN_ON)));
        assertTrue(e.getMessage().contains("would exceed the limit"), e.getMessage());
        assertFalse(registry.get("big").isOnline());
    }

    @Test
    void currentLoadAddsUpOnlyTheDevicesThatAreOn() {
        assertEquals(0, control.currentLoad());
        control.send("b1", new Command(Action.TURN_ON));
        assertEquals(60, control.currentLoad());
    }
}
