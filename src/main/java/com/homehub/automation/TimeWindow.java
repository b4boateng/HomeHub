package com.homehub.automation;
import java.time.LocalTime;
import java.util.Objects;

public class TimeWindow implements Condition {

    
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

    @Override
    public String describe() {
        return "between " + start + " and " + end;
    }

    @Override
    public boolean test(Event event) {
        LocalTime t = event.timestamp().toLocalTime();
        // Start is inclusive, end is exclusive; start after end means it crosses midnight.
        if(start.isBefore(end)) {
            return !t.isBefore(start) && t.isBefore(end);
        }
        return !t.isBefore(start) || t.isBefore(end);
    }
    
}
