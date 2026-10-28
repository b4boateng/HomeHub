package com.homehub;

import java.time.LocalDateTime;
import java.util.Objects;

public class Event {

    private final EventType type;
    private final LocalDateTime timestamp;
    private final String sourceId;
    private final int value;

    public Event(EventType type, LocalDateTime timestamp, String sourceId, int value) {
        this.type = Objects.requireNonNull(type, "type");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId");
        this.value = value;
    }

    public EventType type() {
        return type;
    }

    public LocalDateTime timestamp() {
        return timestamp;
    }

    public String sourceId() {
        return sourceId;
    }

    public int value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event other)) return false;
        return type == other.type
                && timestamp.equals(other.timestamp)
                && sourceId.equals(other.sourceId)
                && value == other.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, timestamp, sourceId, value);
    }

    @Override
    public String toString() {
        return "Event[type=" + type + ", timestamp=" + timestamp
                + ", sourceId=" + sourceId + ", value=" + value + "]";
    }
}
