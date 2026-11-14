package com.homehub.automation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EventBus {
    private final List<Consumer<Event>> subscribers = new ArrayList<>();

    public void subscribe(Consumer<Event> subscriber) {
        subscribers.add(subscriber);
    }

    public void publish(Event event) {
        for (Consumer<Event> s : subscribers) {
            s.accept(event);
        }
    }
}

