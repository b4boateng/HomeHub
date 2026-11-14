package com.homehub.loaders;

import com.homehub.automation.*;

import java.time.LocalDateTime;

// Reads events.csv and turns each row into an Event.
public class EventsLoader extends CsvLoader<Event> {

    public EventsLoader() {
        super(4);
    }

    @Override
    protected Event parseRow(String[] c) {
        return new Event(EventType.valueOf(c[1]), LocalDateTime.parse(c[0]), c[2], Integer.parseInt(c[3]));
    }
}
