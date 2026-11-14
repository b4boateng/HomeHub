package com.homehub;

import com.homehub.automation.*;
import com.homehub.devices.*;
import com.homehub.energy.*;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicesTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 27, 20, 0);

    private DeviceRegistry registry;
    private EventBus bus;
    private EnergyAccountant accountant;

    @BeforeEach
    void setUp() {
        registry = new DeviceRegistry();
        bus = new EventBus();
        accountant = new EnergyAccountant(registry, START);
    }

    private SmartBulb bulb(String id, boolean on) {
        return new SmartBulb(50, 128, id, "LB", "Bulb " + id, "Philips", "Hall", on, 60, "");
    }

    @Test
    void registryFindsARegisteredDevice() {
        SmartBulb b = bulb("b1", false);
        registry.register(b);
        assertSame(b, registry.get("b1"));
        assertTrue(registry.contains("b1"));
        assertFalse(registry.contains("b2"));
    }

    @Test
    void registryRejectsADuplicateId() {
        registry.register(bulb("b1", false));
        assertThrows(IllegalArgumentException.class, () -> registry.register(bulb("b1", true)));
    }

    @Test
    void powerManagerAllowsUpToTheLimitAndRefusesJustOver() {
        PowerManager manager = new PowerManager(100);
        assertTrue(manager.allows(50, 50));
        assertFalse(manager.allows(50, 50.01));
    }

    @Test
    void powerManagerWithZeroLimitAllowsNothingExceptZero() {
        PowerManager manager = new PowerManager(0);
        assertTrue(manager.allows(0, 0));
        assertFalse(manager.allows(0, 1));
    }

    @Test
    void powerManagerRejectsANegativeLimit() {
        assertThrows(IllegalArgumentException.class, () -> new PowerManager(-1));
    }

    @Test
    void zeroElapsedTimeBillsNothing() {
        registry.register(bulb("b1", true));
        accountant.advanceTo(START);
        assertEquals(0.0, accountant.getKwhByDevice().get("b1"), 1e-9);
    }

    @Test
    void timeIsBilledAtTheStateBeforeTheChange() {
        SmartBulb b = bulb("b1", true);
        registry.register(b);

        accountant.advanceTo(START.plusHours(1));
        b.setOnline(false);
        accountant.advanceTo(START.plusHours(2));

        assertEquals(0.06, accountant.getKwhByDevice().get("b1"), 1e-9);
    }

    @Test
    void accountantRejectsTimeGoingBackwards() {
        assertThrows(IllegalArgumentException.class, () -> accountant.advanceTo(START.minusMinutes(1)));
    }

    @Test
    void simulationProcessesEventsInTimestampOrderNotInsertionOrder() {
        List<LocalDateTime> seen = new ArrayList<>();
        bus.subscribe(e -> seen.add(e.timestamp()));
        Simulation sim = new Simulation(bus, accountant, s -> { });

        sim.addEvent(new Event(EventType.MOTION, START.plusHours(3), "m1", 1));
        sim.addEvent(new Event(EventType.MOTION, START.plusHours(1), "m1", 1));
        sim.addEvent(new Event(EventType.MOTION, START.plusHours(2), "m1", 1));
        sim.run(START.plusHours(4));

        assertEquals(List.of(START.plusHours(1), START.plusHours(2), START.plusHours(3)), seen);
    }

    @Test
    void simulationBillsTheOldStateBeforeAnEventChangesIt() {
        registry.register(bulb("b1", true));
        RuleEngine engine = new RuleEngine(registry, bus, new PowerManager(1000), s -> { });
        engine.addRule(new Rule(e -> true, "b1", new Command(Action.TURN_OFF), 1));
        Simulation sim = new Simulation(bus, accountant, s -> { });

        sim.addEvent(new Event(EventType.MOTION, START.plusHours(1), "m1", 1));
        sim.run(START.plusHours(3));

        assertEquals(0.06, accountant.getKwhByDevice().get("b1"), 1e-9);
    }

    @Test
    void simulationWithNoEventsStillBillsUpToTheEnd() {
        registry.register(bulb("b1", true));
        new Simulation(bus, accountant, s -> { }).run(START.plusHours(2));
        assertEquals(0.12, accountant.getKwhByDevice().get("b1"), 1e-9);
    }

    @Test
    void rulesDifferingInDeviceOrPriorityOrSameActionDoNotConflict() {
        Condition any = e -> true;
        Rule existing = new Rule(any, "b1", new Command(Action.TURN_ON), 1);

        assertDoesNotThrow(() -> ConflictDetector.check(
                new Rule(any, "b2", new Command(Action.TURN_OFF), 1), List.of(existing)));
        assertDoesNotThrow(() -> ConflictDetector.check(
                new Rule(any, "b1", new Command(Action.TURN_OFF), 2), List.of(existing)));
        assertDoesNotThrow(() -> ConflictDetector.check(
                new Rule(any, "b1", new Command(Action.TURN_ON), 1), List.of(existing)));
    }

    @Test
    void eventAndCommandRejectNullParts() {
        assertThrows(NullPointerException.class, () -> new Event(null, START, "m1", 1));
        assertThrows(NullPointerException.class, () -> new Event(EventType.MOTION, null, "m1", 1));
        assertThrows(NullPointerException.class, () -> new Event(EventType.MOTION, START, null, 1));
        assertThrows(NullPointerException.class, () -> new Command(null));
    }
}
