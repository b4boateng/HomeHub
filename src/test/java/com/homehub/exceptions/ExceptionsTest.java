package com.homehub.exceptions;

import com.homehub.automation.*;
import com.homehub.devices.*;
import com.homehub.energy.*;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExceptionsTest {

    private static final Condition ALWAYS = e -> true;

    private SmartBulb bulb(String id, boolean on, double watts) {
        return new SmartBulb(50, 128, id, "LB", "Bulb " + id, "Philips", "Hall", on, watts, "");
    }

    @Test
    void ruleEngineRefusesTurnOnWhenOverLimitAndKeepsRunning() {
        DeviceRegistry registry = new DeviceRegistry();
        registry.register(bulb("b1", true, 60));
        registry.register(bulb("b2", false, 60));
        EventBus bus = new EventBus();
        List<String> log = new ArrayList<>();
        RuleEngine engine = new RuleEngine(registry, bus, new PowerManager(100), log::add);
        engine.addRule(new Rule(ALWAYS, "b2", new Command(Action.TURN_ON), 1));

        bus.publish(new Event(EventType.MOTION, LocalDateTime.now(), "m1", 1));

        assertFalse(registry.get("b2").isOnline());
        assertTrue(log.stream().anyMatch(line -> line.startsWith("Failed")));
    }
}
