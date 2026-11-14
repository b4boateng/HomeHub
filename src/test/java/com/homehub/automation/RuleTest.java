package com.homehub.automation;

import com.homehub.devices.*;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RuleTest {

    private final Event motion = new Event(EventType.MOTION, LocalDateTime.of(2026, 10, 27, 12, 0), "m1", 1);

    @Test
    void keepsItsTargetCommandAndPriority() {
        Command command = new Command(Action.TURN_ON);
        Rule rule = new Rule(e -> true, "b1", command, 3);
        assertEquals("b1", rule.getTargetDeviceId());
        assertEquals(command, rule.getCommand());
        assertEquals(3, rule.getPriority());
    }

    @Test
    void matchesWhenItsConditionIsTrue() {
        assertTrue(new Rule(e -> true, "b1", new Command(Action.TURN_ON), 1).matches(motion));
    }

    @Test
    void doesNotMatchWhenItsConditionIsFalse() {
        assertFalse(new Rule(e -> false, "b1", new Command(Action.TURN_ON), 1).matches(motion));
    }

    @Test
    void passesTheEventToItsCondition() {
        Rule rule = new Rule(e -> e.sourceId().equals("m1"), "b1", new Command(Action.TURN_ON), 1);
        assertTrue(rule.matches(motion));
        assertFalse(rule.matches(new Event(EventType.MOTION, motion.timestamp(), "m2", 1)));
    }

    @Test
    void aBlankTargetDeviceIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Rule(e -> true, "", new Command(Action.TURN_ON), 1));
        assertThrows(IllegalArgumentException.class, () -> new Rule(e -> true, "  ", new Command(Action.TURN_ON), 1));
    }

}
