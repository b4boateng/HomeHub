package com.homehub.automation;

import com.homehub.devices.*;
import com.homehub.energy.*;
import com.homehub.exceptions.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Two rules on one device with the same priority but different actions are refused by the engine when added.
class RuleConflictTest {

    private static final Condition ANY = e -> true;

    private RuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RuleEngine(new DeviceRegistry(), new EventBus(), new PowerManager(1000), s -> { });
    }

    private static Rule rule(String device, Action action, int priority) {
        return new Rule(ANY, device, new Command(action), priority);
    }

    @Test
    void theFirstRuleNeverConflicts() {
        assertDoesNotThrow(() -> engine.addRule(rule("b1", Action.TURN_ON, 1)));
    }

    @Test
    void sameDeviceSamePriorityDifferentActionConflicts() {
        engine.addRule(rule("b1", Action.TURN_ON, 1));
        assertThrows(HomeHubException.class, () -> engine.addRule(rule("b1", Action.TURN_OFF, 1)));
    }

    @Test
    void theMessageNamesTheDeviceThePriorityAndBothActions() {
        engine.addRule(rule("b1", Action.TURN_ON, 1));
        HomeHubException ex = assertThrows(HomeHubException.class,
                () -> engine.addRule(rule("b1", Action.TURN_OFF, 1)));
        String message = ex.getMessage();
        assertTrue(message.contains("b1") && message.contains("priority 1")
                && message.contains("TURN_ON") && message.contains("TURN_OFF"), message);
    }

    @Test
    void aRefusedRuleIsNotAdded() {
        engine.addRule(rule("b1", Action.TURN_ON, 1));
        assertThrows(HomeHubException.class, () -> engine.addRule(rule("b1", Action.TURN_OFF, 1)));

        assertEquals(1, engine.getRules().size());
        assertEquals(Action.TURN_ON, engine.getRules().get(0).getCommand().action());
    }

    @Test
    void findsTheConflictingRuleAmongSeveral() {
        engine.addRule(rule("b2", Action.TURN_ON, 1));
        engine.addRule(rule("b1", Action.TURN_ON, 2));
        engine.addRule(rule("b1", Action.TURN_ON, 1));

        assertThrows(HomeHubException.class, () -> engine.addRule(rule("b1", Action.TURN_OFF, 1)));
    }

    @Test
    void rulesDifferingInDeviceOrPriorityOrSameActionDoNotConflict() {
        engine.addRule(rule("b1", Action.TURN_ON, 1));

        assertDoesNotThrow(() -> engine.addRule(rule("b2", Action.TURN_OFF, 1)));   // other device
        assertDoesNotThrow(() -> engine.addRule(rule("b1", Action.TURN_OFF, 2)));   // other priority
        assertDoesNotThrow(() -> engine.addRule(rule("b1", Action.TURN_ON, 1)));    // same action
        assertEquals(4, engine.getRules().size());
    }
}
