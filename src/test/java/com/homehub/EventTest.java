package com.homehub;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class EventTest {

    private static final LocalDateTime T = LocalDateTime.of(2026, 10, 27, 23, 30);

    @Test
    void keepsAllFourParts() {
        Event e = new Event(EventType.MOTION, T, "m1", 7);
        assertEquals(EventType.MOTION, e.type());
        assertEquals(T, e.timestamp());
        assertEquals("m1", e.sourceId());
        assertEquals(7, e.value());
    }

    @Test
    void sameFieldsAreEqualWithTheSameHash() {
        Event a = new Event(EventType.MOTION, T, "m1", 7);
        Event b = new Event(EventType.MOTION, T, "m1", 7);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void anyDifferentFieldMakesEventsUnequal() {
        Event base = new Event(EventType.MOTION, T, "m1", 7);
        assertNotEquals(base, new Event(EventType.HUMIDITY, T, "m1", 7));
        assertNotEquals(base, new Event(EventType.MOTION, T.plusMinutes(1), "m1", 7));
        assertNotEquals(base, new Event(EventType.MOTION, T, "m2", 7));
        assertNotEquals(base, new Event(EventType.MOTION, T, "m1", 8));
    }

    @Test
    void toStringMentionsTheSource() {
        assertTrue(new Event(EventType.MOTION, T, "m1", 7).toString().contains("m1"));
    }

    @Test
    void nullPartsAreRejected() {
        assertThrows(NullPointerException.class, () -> new Event(null, T, "m1", 1));
        assertThrows(NullPointerException.class, () -> new Event(EventType.MOTION, null, "m1", 1));
        assertThrows(NullPointerException.class, () -> new Event(EventType.MOTION, T, null, 1));
    }
}
