package com.homehub.automation;
import java.time.LocalTime;
import java.util.Objects;

// A range of times of day. It only answers whether a time falls inside it; matching events is EventCondition's job.
public class TimeWindow {

    private final LocalTime start;
    private final LocalTime end;

    public TimeWindow(LocalTime start, LocalTime end) {
        this.start = Objects.requireNonNull(start);
        this.end = Objects.requireNonNull(end);
    }

    public LocalTime getStart() {
        return start;
    }

    public LocalTime getEnd() {
        return end;
    }

    // Start is inclusive, end is exclusive; start after end means it crosses midnight.
    public boolean contains(LocalTime time) {
        if (start.isBefore(end)) {
            return !time.isBefore(start) && time.isBefore(end);
        }
        return !time.isBefore(start) || time.isBefore(end);
    }

    public String describe() {
        return "between " + start + " and " + end;
    }
}
