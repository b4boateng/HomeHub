package com.homehub;

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
    void offlineDeviceThrowsDeviceOfflineException() {
        SmartTv tv = new SmartTv("t1", "TV", "Samsung", false, "Lounge", "QN", 120, 20, false, "Netflix", 5);
        assertThrows(DeviceOfflineException.class, tv::play);
    }

    @Test
    void unknownIdThrowsDeviceNotFoundException() {
        assertThrows(DeviceNotFoundException.class, () -> new DeviceRegistry().get("nope"));
    }

    @Test
    void overTheLimitThrowsPowerLimitExceededException() {
        PowerManager manager = new PowerManager(100);
        assertThrows(PowerLimitExceededException.class, () -> manager.require(90, 20));
    }

    @Test
    void sameDeviceSamePriorityDifferentActionThrowsRuleConflictException() {
        RuleEngine engine = new RuleEngine(new DeviceRegistry(), new EventBus(), new PowerManager(100), s -> { });
        engine.addRule(new Rule(ALWAYS, "b1", new Command(Action.TURN_ON), 1));
        assertThrows(RuleConflictException.class,
                () -> engine.addRule(new Rule(ALWAYS, "b1", new Command(Action.TURN_OFF), 1)));
    }

    @Test
    void dataLoadExceptionNamesFileAndLine() {
        DataLoadException ex = assertThrows(DataLoadException.class, () -> {
            throw new DataLoadException("devices.csv", 3, "bad number");
        });
        assertEquals("devices.csv line 3: bad number", ex.getMessage());
    }

    @Test
    void everyCustomExceptionIsAHomeHubException() {
        assertThrows(HomeHubException.class, () -> new DeviceRegistry().get("nope"));
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
