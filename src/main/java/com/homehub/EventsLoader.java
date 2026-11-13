package com.homehub;

import java.time.LocalDateTime;

// events.csv columns: timestamp,type,source,value
// timestamp looks like 2026-10-27T23:30
public class EventsLoader extends CsvLoader<Event> {

    public EventsLoader() {
        super(4);
    }

    @Override
    protected Event parseRow(String[] c) {
        return new Event(EventType.valueOf(c[1]), LocalDateTime.parse(c[0]), c[2], Integer.parseInt(c[3]));
    }
}
