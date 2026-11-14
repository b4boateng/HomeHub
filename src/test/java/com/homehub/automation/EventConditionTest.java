package com.homehub.automation;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class EventConditionTest {

    private static Event event(EventType type, String source, int value, String time) {
        return new Event(type, LocalDateTime.parse("2026-10-27T" + time), source, value);
    }

    @Test
    void matchesOnTypeSourceAndMinimumValue() {
        EventCondition c = new EventCondition(EventType.HUMIDITY, "h1", 80, null);

        assertTrue(c.test(event(EventType.HUMIDITY, "h1", 80, "12:00")));    // exactly the minimum counts
        assertTrue(c.test(event(EventType.HUMIDITY, "h1", 95, "12:00")));
        assertFalse(c.test(event(EventType.HUMIDITY, "h1", 79, "12:00")));
        assertFalse(c.test(event(EventType.HUMIDITY, "h2", 90, "12:00")));
        assertFalse(c.test(event(EventType.TEMPERATURE, "h1", 90, "12:00")));
    }

    @Test
    void withoutAWindowAnyTimeOfDayMatches() {
        EventCondition c = new EventCondition(EventType.MOTION, "m1", 1, null);
        assertTrue(c.test(event(EventType.MOTION, "m1", 1, "03:00")));
        assertTrue(c.test(event(EventType.MOTION, "m1", 1, "15:00")));
    }

    @Test
    void aWindowThatCrossesMidnightIsRespected() {
        TimeWindow night = new TimeWindow(LocalTime.of(22, 0), LocalTime.of(6, 0));
        EventCondition c = new EventCondition(EventType.MOTION, "m1", 1, night);

        assertTrue(c.test(event(EventType.MOTION, "m1", 1, "23:30")));
        assertTrue(c.test(event(EventType.MOTION, "m1", 1, "05:59")));
        assertFalse(c.test(event(EventType.MOTION, "m1", 1, "12:00")));
    }

    @Test
    void describeShowsTypeSourceValueAndWindow() {
        TimeWindow night = new TimeWindow(LocalTime.of(22, 0), LocalTime.of(6, 0));
        assertEquals("MOTION from m1, value >= 1, between 22:00 and 06:00",
                new EventCondition(EventType.MOTION, "m1", 1, night).describe());
        assertEquals("HUMIDITY from h1, value >= 80",
                new EventCondition(EventType.HUMIDITY, "h1", 80, null).describe());
    }

    @Test
    void describeLeavesOutTheValueForDoorEvents() {
        assertEquals("DOOR_UNLOCKED from l1", new EventCondition(EventType.DOOR_UNLOCKED, "l1", 0, null).describe());
        assertEquals("DOOR_LOCKED from l1", new EventCondition(EventType.DOOR_LOCKED, "l1", 0, null).describe());
    }

    @Test
    void aConditionWrittenAsALambdaHasADefaultDescription() {
        Condition any = e -> true;
        assertEquals("custom condition", any.describe());
    }

    @Test
    void timeWindowDescribesItself() {
        assertEquals("between 22:00 and 06:00", new TimeWindow(LocalTime.of(22, 0), LocalTime.of(6, 0)).describe());
    }

    @Test
    void aMissingTypeOrSourceIsRefused() {
        assertThrows(NullPointerException.class, () -> new EventCondition(null, "m1", 1, null));
        assertThrows(NullPointerException.class, () -> new EventCondition(EventType.MOTION, null, 1, null));
    }

    @Test
    void aBlankSourceIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new EventCondition(EventType.MOTION, "", 1, null));
        assertThrows(IllegalArgumentException.class, () -> new EventCondition(EventType.MOTION, "   ", 1, null));
    }
}
