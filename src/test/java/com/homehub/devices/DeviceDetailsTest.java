package com.homehub.devices;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeviceDetailsTest {

    private SmartBulb bulb;
    private SmartLock lock;
    private MediaPlayer player;

    @BeforeEach
    void setUp() {
        bulb = new SmartBulb(50, 128, "b1", "LB", "Bulb", "Philips", "Hall", false, 60, "hi");
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

        assertEquals("hi", bulb.getMessage());
        bulb.setMessage("bye");
        assertEquals("bye", bulb.getMessage());
    }

    @Test
    void lockKeepsItsCodeAndBattery() {
        assertEquals("1234", lock.getCode());
        assertEquals(90, lock.getBatteryLevel());
        lock.setCode("9999");
        lock.setBatteryLevel(10);
        assertEquals("9999", lock.getCode());
        assertEquals(10, lock.getBatteryLevel());
    }

}
