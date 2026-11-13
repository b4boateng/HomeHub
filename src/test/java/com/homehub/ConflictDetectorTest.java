package com.homehub;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ConflictDetectorTest {

    private static final Condition ANY = e -> true;

    private Rule rule(String device, Action action, int priority) {
        return new Rule(ANY, device, new Command(action), priority);
    }

    @Test
    void noExistingRulesMeansNoConflict() {
        assertDoesNotThrow(() -> ConflictDetector.check(rule("b1", Action.TURN_ON, 1), List.of()));
    }

    @Test
    void sameDeviceSamePriorityDifferentActionConflicts() {
        assertThrows(RuleConflictException.class,
                () -> ConflictDetector.check(rule("b1", Action.TURN_OFF, 1), List.of(rule("b1", Action.TURN_ON, 1))));
    }

    @Test
    void theMessageNamesTheDevice() {
        RuleConflictException ex = assertThrows(RuleConflictException.class,
                () -> ConflictDetector.check(rule("b1", Action.TURN_OFF, 1), List.of(rule("b1", Action.TURN_ON, 1))));
        assertTrue(ex.getMessage().contains("b1"), ex.getMessage());
    }

    @Test
    void findsTheConflictingRuleAmongSeveral() {
        List<Rule> existing = List.of(
                rule("b2", Action.TURN_ON, 1),
                rule("b1", Action.TURN_ON, 2),
                rule("b1", Action.TURN_ON, 1));
        assertThrows(RuleConflictException.class,
                () -> ConflictDetector.check(rule("b1", Action.TURN_OFF, 1), existing));
    }
}
