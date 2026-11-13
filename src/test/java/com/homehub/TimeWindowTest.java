package com.homehub;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

// Edge decisions (written before the tests, as Lab 2 asks):
//   start is INCLUSIVE, end is EXCLUSIVE, and a window with start after end wraps past midnight.
class TimeWindowTest {

    private boolean inside(TimeWindow window, int hour, int minute) {
        Event e = new Event(EventType.MOTION, LocalDateTime.of(2026, 10, 27, hour, minute), "m1", 1);
        return window.test(e);
    }

    // ----- normal window: 09:00 to 17:00 -----

    @Test
    void timeInsideADaytimeWindowMatches() {
        TimeWindow day = new TimeWindow(LocalTime.of(9, 0), LocalTime.of(17, 0));
        assertTrue(inside(day, 12, 0));
    }

    @Test
    void startIsInclusiveAndEndIsExclusive() {                       // edge
        TimeWindow day = new TimeWindow(LocalTime.of(9, 0), LocalTime.of(17, 0));
        assertTrue(inside(day, 9, 0));
        assertFalse(inside(day, 17, 0));
        assertTrue(inside(day, 16, 59));
        assertFalse(inside(day, 8, 59));
    }

    // ----- window that wraps midnight: 22:00 to 06:00 -----

    @Test
    void wrappingWindowMatchesLateEveningAndEarlyMorning() {
        TimeWindow night = new TimeWindow(LocalTime.of(22, 0), LocalTime.of(6, 0));
        assertTrue(inside(night, 23, 30));
        assertTrue(inside(night, 0, 0));
        assertTrue(inside(night, 3, 0));
    }

    @Test
    void wrappingWindowEdgesFollowTheSameRule() {                    // edge
        TimeWindow night = new TimeWindow(LocalTime.of(22, 0), LocalTime.of(6, 0));
        assertTrue(inside(night, 22, 0));
        assertFalse(inside(night, 21, 59));
        assertTrue(inside(night, 5, 59));
        assertFalse(inside(night, 6, 0));
    }

    @Test
    void wrappingWindowDoesNotMatchMidday() {
        TimeWindow night = new TimeWindow(LocalTime.of(22, 0), LocalTime.of(6, 0));
        assertFalse(inside(night, 12, 0));
    }

    // ----- invalid -----

    @Test
    void nullStartOrEndIsRejected() {
        assertThrows(NullPointerException.class, () -> new TimeWindow(null, LocalTime.of(6, 0)));
        assertThrows(NullPointerException.class, () -> new TimeWindow(LocalTime.of(22, 0), null));
    }
}
