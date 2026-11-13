package com.homehub;

import java.time.LocalTime;

// rules.csv columns: eventType,source,minValue,windowStart,windowEnd,deviceId,action,priority
// windowStart and windowEnd (HH:mm) may both be left empty, meaning "any time of day".
public class RulesLoader extends CsvLoader<Rule> {

    public RulesLoader() {
        super(8);
    }

    @Override
    protected Rule parseRow(String[] c) {
        EventType type = EventType.valueOf(c[0]);
        String source = c[1];
        int minValue = Integer.parseInt(c[2]);
        Action action = Action.valueOf(c[6]);
        int priority = Integer.parseInt(c[7]);

        // Only build a TimeWindow if the window cells are filled in.
        TimeWindow window = c[3].isEmpty() && c[4].isEmpty()
                ? null
                : new TimeWindow(LocalTime.parse(c[3]), LocalTime.parse(c[4]));

        Condition condition = event -> event.type() == type
                && event.sourceId().equals(source)
                && event.value() >= minValue
                && (window == null || window.test(event));

        return new Rule(condition, c[5], new Command(action), priority);
    }
}
