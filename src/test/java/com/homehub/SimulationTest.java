package com.homehub;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimulationTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 27, 20, 0);

    private final EventBus bus = new EventBus();
    private final EnergyAccountant accountant = new EnergyAccountant(new DeviceRegistry(), START);
    private final List<String> log = new ArrayList<>();
    private final Simulation sim = new Simulation(bus, accountant, log::add);

    @Test
    void keepsTheServicesItWasGiven() {
        assertSame(bus, sim.getBus());
        assertSame(accountant, sim.getAccountant());
    }

    @Test
    void addedEventsCanBeListed() {
        Event e = new Event(EventType.MOTION, START.plusHours(1), "m1", 1);
        sim.addEvent(e);
        assertEquals(List.of(e), sim.getEvents());
    }

    @Test
    void theListedEventsAreACopy() {
        sim.addEvent(new Event(EventType.MOTION, START.plusHours(1), "m1", 1));
        assertThrows(UnsupportedOperationException.class, () -> sim.getEvents().clear());
    }

    @Test
    void runLogsOneLinePerEvent() {
        sim.addEvent(new Event(EventType.MOTION, START.plusHours(1), "m1", 1));
        sim.addEvent(new Event(EventType.HUMIDITY, START.plusHours(2), "h1", 90));

        sim.run(START.plusHours(3));

        assertEquals(2, log.size());
        assertTrue(log.get(0).contains("MOTION from m1"), log.get(0));
        assertTrue(log.get(1).contains("HUMIDITY from h1"), log.get(1));
    }

    @Test
    void runMovesTheClockToTheEnd() {
        sim.run(START.plusHours(4));
        assertEquals(START.plusHours(4), accountant.getLastBilled());
    }

    @Test
    void anEventBeforeTheStartTimeIsRejected() {
        sim.addEvent(new Event(EventType.MOTION, START.minusHours(1), "m1", 1));
        assertThrows(IllegalArgumentException.class, () -> sim.run(START.plusHours(1)));
    }
}
