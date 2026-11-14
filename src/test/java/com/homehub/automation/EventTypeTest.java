package com.homehub.automation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EventTypeTest {

    @Test
    void doorEventsCarryNoReading() {
        assertFalse(EventType.DOOR_LOCKED.hasReading());
        assertFalse(EventType.DOOR_UNLOCKED.hasReading());
    }

    @Test
    void sensorEventsCarryAReading() {
        assertTrue(EventType.MOTION.hasReading());
        assertTrue(EventType.TEMPERATURE.hasReading());
        assertTrue(EventType.HUMIDITY.hasReading());
    }
}
