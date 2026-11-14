package com.homehub.automation;

import java.util.Objects;

// The condition a rules.csv row describes: an event type from one source, at least minValue, optionally inside a time window.
public class EventCondition implements Condition {

    private final EventType type;
    private final String sourceId;
    private final int minValue;
    private final TimeWindow window;   // null means any time of day

    public EventCondition(EventType type, String sourceId, int minValue, TimeWindow window) {
        this.type = Objects.requireNonNull(type);
        this.sourceId = Objects.requireNonNull(sourceId);
        if (sourceId.isBlank()) {
            throw new IllegalArgumentException("Source ID cannot be blank");
        }
        this.minValue = minValue;
        this.window = window;
    }

    @Override
    public boolean test(Event event) {
        return event.type() == type
                && event.sourceId().equals(sourceId)
                && event.value() >= minValue
                && (window == null || window.contains(event.timestamp().toLocalTime()));
    }

    @Override
    public String describe() {
        StringBuilder sb = new StringBuilder(type.toString()).append(" from ").append(sourceId);
        // Door events carry no reading, so a minimum value would only be noise.
        if (type.hasReading()) {
            sb.append(", value >= ").append(minValue);
        }
        if (window != null) {
            sb.append(", ").append(window.describe());
        }
        return sb.toString();
    }
}
