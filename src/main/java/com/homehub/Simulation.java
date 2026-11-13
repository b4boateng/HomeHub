package com.homehub;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class Simulation {

    private final List<Event> events = new ArrayList<>();
    private final EventBus bus;
    private final EnergyAccountant accountant;
    private final Consumer<String> log;

    public Simulation(EventBus bus, EnergyAccountant accountant, Consumer<String> log) {
        this.bus = bus;
        this.accountant = accountant;
        this.log = log;
    }



    public List<Event> getEvents() {
        return List.copyOf(events);
    }


  

    public EventBus getBus() {
        return this.bus;
    }


    public EnergyAccountant getAccountant() {
        return this.accountant;
    }


    public Consumer<String> getLog() {
        return this.log;
    }


    public void run(LocalDateTime end){
        List<Event> ordered = events.stream().sorted(Comparator.comparing(Event::timestamp)).toList();

        for (Event event : ordered)  {
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
