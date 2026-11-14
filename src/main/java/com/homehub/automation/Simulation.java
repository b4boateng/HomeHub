package com.homehub.automation;

import com.homehub.energy.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class Simulation {

    private final List<Event> events = new ArrayList<>();
    private final EventBus bus;
    private final EnergyAccountant accountant;
    private final Consumer<String> log;

    public Simulation(EventBus bus, EnergyAccountant accountant, Consumer<String> log) {
        this.bus = Objects.requireNonNull(bus);
        this.accountant = Objects.requireNonNull(accountant);
        this.log = Objects.requireNonNull(log);
    }

    public List<Event> getEvents() {
        return List.copyOf(events);
    }

    public void run(LocalDateTime end){
        // Sort so time only moves forward.
        List<Event> ordered = events.stream().sorted(Comparator.comparing(Event::timestamp)).toList();

        for (Event event : ordered)  {
            // Bill the  time passed before the event changes anything.
            accountant.advanceTo(event.timestamp());
            log.accept("t=" + event.timestamp() + " " + event.type() + " from " + event.sourceId());
            bus.publish(event);
        }
        accountant.advanceTo(end);
    }

      public void addEvent(Event event){
        events.add(event);
    }

}
