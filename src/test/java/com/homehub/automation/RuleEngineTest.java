package com.homehub.automation;

import com.homehub.devices.*;
import com.homehub.energy.*;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RuleEngineTest {

    // Motion from m1 between 22:00 and 06:00, the same trigger the shipped rules use.
    private static final Condition NIGHT = new EventCondition(EventType.MOTION, "m1", 1,
            new TimeWindow(LocalTime.of(22, 0), LocalTime.of(6, 0)));

    private DeviceRegistry registry;
    private EventBus bus;
    private RuleEngine engine;
    private List<String> log;
    private SmartBulb light;

    @BeforeEach
    void setUp() {
        registry = new DeviceRegistry();
        light = new SmartBulb(50, 128, "b1", "LB", "Hall Light", "Philips", "Hall", false, 60, "");
        registry.register(light);
        bus = new EventBus();
        log = new ArrayList<>();
        engine = new RuleEngine(registry, bus, new PowerManager(1000), log::add);
    }

    private void motionAt(int hour, int minute) {
        bus.publish(new Event(EventType.MOTION, LocalDateTime.of(2026, 10, 27, hour, minute), "m1", 1));
    }

    private boolean logHas(String text) {
        return log.stream().anyMatch(line -> line.contains(text));
    }

    @Test
    void nightTimeMotionTurnsTheLightOn() {
        engine.addRule(new Rule(NIGHT, "b1", new Command(Action.TURN_ON), 1));
        motionAt(23, 30);
        assertTrue(light.isOnline());
    }

    @Test
    void theSameMotionAtNoonDoesNotTurnTheLightOn() {
        engine.addRule(new Rule(NIGHT, "b1", new Command(Action.TURN_ON), 1));
        motionAt(12, 0);
        assertFalse(light.isOnline());
    }

    @Test
    void motionExactlyAtTheWindowStartCountsButAtTheEndDoesNot() {
        engine.addRule(new Rule(NIGHT, "b1", new Command(Action.TURN_ON), 1));
        motionAt(6, 0);
        assertFalse(light.isOnline());
        motionAt(22, 0);
        assertTrue(light.isOnline());
    }

    @Test
    void whenRulesClashTheHighestPriorityNumberWins() {
        engine.addRule(new Rule(e -> true, "b1", new Command(Action.TURN_ON), 1));
        engine.addRule(new Rule(e -> true, "b1", new Command(Action.TURN_OFF), 2));
        light.setOnline(true);
        motionAt(12, 0);
        assertFalse(light.isOnline());
    }

    @Test
    void eventWithNoMatchingRuleIsLogged() {
        motionAt(12, 0);
        assertTrue(logHas("No rule matched"));
    }

    @Test
    void ruleForAnUnknownDeviceIsSkipped() {
        engine.addRule(new Rule(e -> true, "ghost", new Command(Action.TURN_ON), 1));
        motionAt(12, 0);
        assertTrue(logHas("Skipped: no device 'ghost'"));
    }

    @Test
    void ruleWithAnActionTheDeviceCannotDoIsSkipped() {
        engine.addRule(new Rule(e -> true, "b1", new Command(Action.LOCK), 1));
        motionAt(12, 0);
        assertTrue(logHas("cannot LOCK"));
    }

    @Test
    void aFailingRuleDoesNotStopTheRulesAfterIt() {
        registry.register(new MediaPlayer("t1", "TV", "Samsung", false, "Lounge", "QN", 120, 20, false));
        engine.addRule(new Rule(e -> true, "t1", new Command(Action.PLAY), 1));
        engine.addRule(new Rule(e -> true, "b1", new Command(Action.TURN_ON), 2));
        motionAt(12, 0);
        assertTrue(logHas("Failed"));
        assertTrue(light.isOnline());
    }

    @Test
    void nullPartsOfARuleAreRejected() {
        assertThrows(NullPointerException.class, () -> new Rule(null, "b1", new Command(Action.TURN_ON), 1));
        assertThrows(NullPointerException.class, () -> new Rule(e -> true, null, new Command(Action.TURN_ON), 1));
        assertThrows(NullPointerException.class, () -> new Rule(e -> true, "b1", null, 1));
    }
}
