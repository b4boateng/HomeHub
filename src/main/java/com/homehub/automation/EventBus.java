package com.homehub.automation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class EventBus {
    private final List<Consumer<Event>> subscribers = new ArrayList<>();

    public void subscribe(Consumer<Event> subscriber) {
        subscribers.add(Objects.requireNonNull(subscriber));
    }

    // Publishers do not need to know who listens.
    public void publish(Event event) {
        Objects.requireNonNull(event);
        // Iterate a copy, so a subscriber that subscribes another one mid-publish cannot break this loop.
        for (Consumer<Event> s : List.copyOf(subscribers)) {
            s.accept(event);
        }
    }
}
