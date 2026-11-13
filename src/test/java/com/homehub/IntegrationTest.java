package com.homehub;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// Real classes working together, nothing faked:
// loaders + registry + rules + time windows + bus + engine + power limit + simulation + accountant.
class IntegrationTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 27, 20, 0);
    private static final Condition ALWAYS = e -> true;

    @TempDir
    Path folder;

    // A fresh house wiring before every test; each test adds its own devices and rules.
    private DeviceRegistry registry;
    private EventBus bus;
    private List<String> log;

    @BeforeEach
    void setUp() {
        registry = new DeviceRegistry();
        bus = new EventBus();
        log = new ArrayList<>();
    }

    private RuleEngine engineWithLimit(double limitWatts) {
        return new RuleEngine(registry, bus, new PowerManager(limitWatts), log::add);
    }

    private void write(String name, String... lines) throws IOException {
        Files.write(folder.resolve(name), List.of(lines));
    }

    private SmartBulb bulb(String id, double watts, boolean on) {
        return new SmartBulb(50, 128, id, "LB", "Bulb " + id, "Philips", "Hall", on, watts, "");
    }

    private Event motionAt(LocalDateTime when) {
        return new Event(EventType.MOTION, when, "m1", 1);
    }

    // ===== files -> loaders -> rules -> time window -> simulation -> energy =====

    @Test
    void csvFilesDriveTheWholeSimulationAndTheEnergyIsBilled() throws IOException {
        write("devices.csv", "type,id,name,brand,model,room,watts,online",
                "BULB,b1,Hall Light,Philips,LB-100,Hallway,60,false");
        write("rules.csv", "eventType,source,minValue,windowStart,windowEnd,deviceId,action,priority",
                "MOTION,m1,1,22:00,06:00,b1,TURN_ON,1");
        write("events.csv", "timestamp,type,source,value",
                "2026-10-27T23:00,MOTION,m1,1",     // night: light turns on (listed first on purpose)
                "2026-10-27T12:00,MOTION,m1,1");    // noon: rule must not fire

        new DevicesLoader().load(folder.resolve("devices.csv")).forEach(registry::register);
        RuleEngine engine = engineWithLimit(1000);
        new RulesLoader().load(folder.resolve("rules.csv")).forEach(engine::addRule);
        EnergyAccountant accountant = new EnergyAccountant(registry, LocalDateTime.of(2026, 10, 27, 12, 0));
        Simulation sim = new Simulation(bus, accountant, log::add);
        new EventsLoader().load(folder.resolve("events.csv")).forEach(sim::addEvent);

        sim.run(LocalDateTime.of(2026, 10, 28, 1, 0));

        assertTrue(registry.get("b1").isOnline());
        // off from 12:00 to 23:00 (nothing), on from 23:00 to 01:00 (2 h x 60 W)
        assertEquals(0.12, accountant.getKwhByDevice().get("b1"), 1e-9);
    }

    @Test
    void applicationRunsFromTheFilesAndPrintsALogAndAReport() throws IOException {
        write("devices.csv", "type,id,name,brand,model,room,watts,online",
                "BULB,b1,Hall Light,Philips,LB-100,Hallway,60,false");
        write("rules.csv", "eventType,source,minValue,windowStart,windowEnd,deviceId,action,priority",
                "MOTION,m1,1,,,b1,TURN_ON,1");
        write("events.csv", "timestamp,type,source,value", "2026-10-27T23:00,MOTION,m1,1");

        String output = runApplication();

        assertTrue(output.contains("Applied TURN_ON to Hall Light"), output);
        assertTrue(output.contains("energy used"), output);
        assertTrue(output.contains("b1"), output);
    }

    @Test
    void applicationReportsABadFileWithItsNameAndLineInsteadOfCrashing() throws IOException {
        write("devices.csv", "type,id,name,brand,model,room,watts,online",
                "BULB,b1,Hall Light,Philips,LB-100,Hallway,sixty,false");
        write("rules.csv", "eventType,source,minValue,windowStart,windowEnd,deviceId,action,priority");
        write("events.csv", "timestamp,type,source,value");

        String output = runApplication();

        assertTrue(output.contains("Could not start"), output);
        assertTrue(output.contains("devices.csv line 2"), output);
    }

    // ===== power limit + rules + registry =====

    @Test
    void powerLimitRefusesTheLightThatWouldGoOverButAllowsOneExactlyAtTheLimit() {
        SmartBulb first = bulb("b1", 60, false);
        SmartBulb second = bulb("b2", 60, false);
        SmartBulb third = bulb("b3", 40, false);
        registry.register(first);
        registry.register(second);
        registry.register(third);
        RuleEngine engine = engineWithLimit(100);
        engine.addRule(new Rule(ALWAYS, "b1", new Command(Action.TURN_ON), 1));   // 0 + 60  = 60   ok
        engine.addRule(new Rule(ALWAYS, "b2", new Command(Action.TURN_ON), 2));   // 60 + 60 = 120  refused
        engine.addRule(new Rule(ALWAYS, "b3", new Command(Action.TURN_ON), 3));   // 60 + 40 = 100  exactly at limit

        bus.publish(motionAt(START));

        assertTrue(first.isOnline());
        assertFalse(second.isOnline());
        assertTrue(third.isOnline());
        assertTrue(log.stream().anyMatch(l -> l.startsWith("Failed") && l.contains("Bulb b2")), log.toString());
    }

    @Test
    void aRefusedTurnOnIsNeverBilled() {
        registry.register(bulb("b1", 60, true));
        registry.register(bulb("b2", 60, false));
        engineWithLimit(100).addRule(new Rule(ALWAYS, "b2", new Command(Action.TURN_ON), 1));
        EnergyAccountant accountant = new EnergyAccountant(registry, START);
        Simulation sim = new Simulation(bus, accountant, log::add);
        sim.addEvent(motionAt(START.plusHours(1)));

        sim.run(START.plusHours(2));

        assertEquals(0.12, accountant.getKwhByDevice().get("b1"), 1e-9);
        assertEquals(0.0, accountant.getKwhByDevice().get("b2"), 1e-9);
    }

    @Test
    void turnOffThenOnAgainStaysUnderTheLimitBecauseTheLoadIsRecalculatedEachTime() {
        SmartBulb a = bulb("a", 60, true);
        SmartBulb b = bulb("b", 60, false);
        registry.register(a);
        registry.register(b);
        RuleEngine engine = engineWithLimit(100);
        engine.addRule(new Rule(ALWAYS, "a", new Command(Action.TURN_OFF), 1));   // frees 60 W first
        engine.addRule(new Rule(ALWAYS, "b", new Command(Action.TURN_ON), 2));    // then 0 + 60 fits

        bus.publish(motionAt(START));

        assertFalse(a.isOnline());
        assertTrue(b.isOnline());
    }

    // ===== rules + conflict detector + engine =====

    @Test
    void aConflictingRuleIsRejectedAndTheExistingRuleKeepsWorking() {
        SmartBulb light = bulb("b1", 60, false);
        registry.register(light);
        RuleEngine engine = engineWithLimit(1000);
        engine.addRule(new Rule(ALWAYS, "b1", new Command(Action.TURN_ON), 1));

        assertThrows(RuleConflictException.class,
                () -> engine.addRule(new Rule(ALWAYS, "b1", new Command(Action.TURN_OFF), 1)));
        bus.publish(motionAt(START));

        assertTrue(light.isOnline());
    }

    @Test
    void theSameRuleLoadedFromCsvAlsoTriggersTheConflictCheck() throws IOException {
        write("rules.csv", "eventType,source,minValue,windowStart,windowEnd,deviceId,action,priority",
                "MOTION,m1,1,,,b1,TURN_ON,1",
                "MOTION,m1,1,,,b1,TURN_OFF,1");
        RuleEngine engine = engineWithLimit(1000);
        List<Rule> rules = new RulesLoader().load(folder.resolve("rules.csv"));

        engine.addRule(rules.get(0));
        assertThrows(RuleConflictException.class, () -> engine.addRule(rules.get(1)));
    }

    // ===== different device types reacting to events =====

    @Test
    void oneEventCanLockTheDoorAndTurnOnALight() {
        SmartLock door = new SmartLock("l1", "Door", "Yale", true, "Hall", "YL", 2, false, "1234", 90);
        SmartBulb light = bulb("b1", 60, false);
        registry.register(door);
        registry.register(light);
        RuleEngine engine = engineWithLimit(1000);
        engine.addRule(new Rule(ALWAYS, "l1", new Command(Action.LOCK), 1));
        engine.addRule(new Rule(ALWAYS, "b1", new Command(Action.TURN_ON), 2));

        bus.publish(motionAt(START));

        assertTrue(door.getLockStatus());
        assertTrue(light.isOnline());
    }

    @Test
    void anOfflineTvFailsWithItsNameInTheLogAndTheNextRuleStillRuns() {
        registry.register(new SmartTv("t1", "Living TV", "Samsung", false, "Lounge", "QN", 120, 20, false, "", 1));
        SmartBulb light = bulb("b1", 60, false);
        registry.register(light);
        RuleEngine engine = engineWithLimit(1000);
        engine.addRule(new Rule(ALWAYS, "t1", new Command(Action.PLAY), 1));
        engine.addRule(new Rule(ALWAYS, "b1", new Command(Action.TURN_ON), 2));

        bus.publish(motionAt(START));

        assertTrue(log.stream().anyMatch(l -> l.contains("Living TV is offline")), log.toString());
        assertTrue(light.isOnline());
    }

    @Test
    void ruleEngineLogsAreAlsoSeenInTheSimulationLogInOrder() {
        registry.register(bulb("b1", 60, false));
        engineWithLimit(1000).addRule(new Rule(ALWAYS, "b1", new Command(Action.TURN_ON), 1));
        Simulation sim = new Simulation(bus, new EnergyAccountant(registry, START), log::add);
        sim.addEvent(motionAt(START.plusHours(1)));

        sim.run(START.plusHours(2));

        assertTrue(log.get(0).contains("MOTION from m1"), log.toString());   // simulation line first
        assertTrue(log.get(1).startsWith("Applied TURN_ON"), log.toString()); // then the engine's reaction
    }

    // ----- helper -----

    private String runApplication() {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            Application.main(new String[] { folder.toString() });
        } finally {
            System.setOut(original);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }
}
