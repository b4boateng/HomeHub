package com.homehub.automation;

// The condition a rules.csv row describes: an event type from one source, at least minValue, optionally inside a time window.
public class EventCondition implements Condition {

    private final EventType type;
    private final String sourceId;
    private final int minValue;
    private final TimeWindow window;   // null means any time of day

    public EventCondition(EventType type, String sourceId, int minValue, TimeWindow window) {
        this.type = type;
        this.sourceId = sourceId;
        this.minValue = minValue;
        this.window = window;
    }

    @Override
    public boolean test(Event event) {
        return event.type() == type
                && event.sourceId().equals(sourceId)
                && event.value() >= minValue
                && (window == null || window.test(event));
    }

    @Override
    public String describe() {
        // Door events carry no reading, so a minimum value would only be noise.
        boolean door = type == EventType.DOOR_LOCKED || type == EventType.DOOR_UNLOCKED;
        StringBuilder sb = new StringBuilder(type.toString()).append(" from ").append(sourceId);
        if (!door) {
            sb.append(", value >= ").append(minValue);
        }
        if (window != null) {
            sb.append(", ").append(window.describe());
        }
        return sb.toString();
    }
}
