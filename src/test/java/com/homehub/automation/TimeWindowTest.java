package com.homehub.automation;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class TimeWindowTest {

    private boolean inside(TimeWindow window, int hour, int minute) {
        return window.contains(LocalTime.of(hour, minute));
    }

    @Test
    void timeInsideADaytimeWindowMatches() {
        TimeWindow day = new TimeWindow(LocalTime.of(9, 0), LocalTime.of(17, 0));
        assertTrue(inside(day, 12, 0));
    }

    @Test
    void startIsInclusiveAndEndIsExclusive() {
        TimeWindow day = new TimeWindow(LocalTime.of(9, 0), LocalTime.of(17, 0));
        assertTrue(inside(day, 9, 0));
        assertFalse(inside(day, 17, 0));
        assertTrue(inside(day, 16, 59));
        assertFalse(inside(day, 8, 59));
    }

    @Test
    void wrappingWindowMatchesLateEveningAndEarlyMorning() {
        TimeWindow night = new TimeWindow(LocalTime.of(22, 0), LocalTime.of(6, 0));
        assertTrue(inside(night, 23, 30));
        assertTrue(inside(night, 0, 0));
        assertTrue(inside(night, 3, 0));
    }

    @Test
    void wrappingWindowEdgesFollowTheSameRule() {
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

    @Test
    void nullStartOrEndIsRejected() {
        assertThrows(NullPointerException.class, () -> new TimeWindow(null, LocalTime.of(6, 0)));
        assertThrows(NullPointerException.class, () -> new TimeWindow(LocalTime.of(22, 0), null));
    }

    @Test
    void midnightBelongsToAWrappingWindowButNotToADaytimeOne() {
        assertTrue(new TimeWindow(LocalTime.of(22, 0), LocalTime.of(6, 0)).contains(LocalTime.MIDNIGHT));
        assertFalse(new TimeWindow(LocalTime.of(9, 0), LocalTime.of(17, 0)).contains(LocalTime.MIDNIGHT));
    }

}
