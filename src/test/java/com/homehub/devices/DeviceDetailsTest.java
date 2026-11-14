package com.homehub.devices;

import static org.junit.jupiter.api.Assertions.*;

import com.homehub.exceptions.HomeHubException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeviceDetailsTest {

    private SmartBulb bulb;
    private SmartLock lock;
    private MediaPlayer player;

    @BeforeEach
    void setUp() {
        bulb = new SmartBulb(50, 128, "b1", "LB", "Bulb", "Philips", "Hall", false, 60, "");
        lock = new SmartLock("l1", "Door", "Yale", true, "Hall", "YL", 2, true, "1234", 90);
        player = new MediaPlayer("t1", "TV", "Samsung", true, "Lounge", "QN", 120, 20, false);
    }

    @Test
    void baseDeviceFieldsCanBeReadAndChanged() {
        assertEquals("Bulb", bulb.getName());
        assertEquals("Philips", bulb.getBrand());
        assertEquals("Hall", bulb.getRoom());
        assertEquals("LB", bulb.getModelNumber());
        assertEquals(60, bulb.getRatedWatts());

        bulb.setName("Lamp");
        bulb.setRoom("Study");
        bulb.setBrand("Ikea");
        bulb.setModelNumber("X1");

        assertEquals("Lamp", bulb.getName());
        assertEquals("Study", bulb.getRoom());
        assertEquals("Ikea", bulb.getBrand());
        assertEquals("X1", bulb.getModelNumber());
    }

    @Test
    void bulbTurnOnAndOffSwitchItAndColourTemperatureIsClamped() {
        bulb.execute(new Command(Action.TURN_ON));
        assertTrue(bulb.isOnline());
        bulb.execute(new Command(Action.TURN_OFF));
        assertFalse(bulb.isOnline());

        bulb.setColorTemperature(300);
        assertEquals(255, bulb.getColorTemperature());
        bulb.setColorTemperature(-1);
        assertEquals(0, bulb.getColorTemperature());
    }

    @Test
    void warmerAndCoolerMoveTheColourTemperatureAndStopAtTheEnds() {
        bulb.execute(new Command(Action.WARMER, 28));
        assertEquals(100, bulb.getColorTemperature());
        bulb.execute(new Command(Action.COOLER, 50));
        assertEquals(150, bulb.getColorTemperature());
        bulb.execute(new Command(Action.COOLER, Integer.MAX_VALUE));
        assertEquals(255, bulb.getColorTemperature());
        bulb.execute(new Command(Action.WARMER, Integer.MAX_VALUE));
        assertEquals(0, bulb.getColorTemperature());
        assertTrue(bulb.supports(Action.WARMER) && bulb.supports(Action.COOLER));
    }

    @Test
    void theBulbRemembersTheLastCommandAsItsMessage() {
        assertEquals("", bulb.getMessage());
        bulb.execute(new Command(Action.TURN_ON));
        assertEquals("last command: TURN_ON", bulb.getMessage());
        bulb.execute(new Command(Action.BRIGHTEN, 30));
        assertEquals("last command: BRIGHTEN 30", bulb.getMessage());
    }

    @Test
    void lockChecksTheCodeWithoutGivingItOut() {
        assertTrue(lock.codeMatches("1234"));
        assertFalse(lock.codeMatches("0000"));
        assertFalse(lock.codeMatches(""));
    }

    @Test
    void everyLockOrUnlockUsesOnePercentOfBattery() {
        assertEquals(90, lock.getBatteryLevel());
        lock.execute(new Command(Action.UNLOCK));
        lock.execute(new Command(Action.LOCK));
        assertEquals(88, lock.getBatteryLevel());
    }

    @Test
    void aLockWithAFlatBatteryCannotMoveAndStaysAsItWas() {
        SmartLock flat = new SmartLock("l2", "Back", "Yale", true, "Hall", "YL", 2, true, "1234", 0);
        HomeHubException e = assertThrows(HomeHubException.class, () -> flat.execute(new Command(Action.UNLOCK)));
        assertEquals("Back battery is flat", e.getMessage());
        assertTrue(flat.getLockStatus());
        assertEquals(0, flat.getBatteryLevel());
    }

}
