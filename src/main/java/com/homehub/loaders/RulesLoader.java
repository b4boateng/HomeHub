package com.homehub.loaders;

import com.homehub.automation.*;
import com.homehub.devices.*;

import java.time.LocalTime;

// Reads rules.csv and turns each row into a Rule.
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

        // No window cells means "any time of day"; a half-filled window is a mistake, not a default.
        TimeWindow window = null;
        if (!c[3].isEmpty() && !c[4].isEmpty()) {
            window = new TimeWindow(LocalTime.parse(c[3]), LocalTime.parse(c[4]));
        } else if (!c[3].isEmpty() || !c[4].isEmpty()) {
            throw new IllegalArgumentException("Both window start and end must be provided");
        }

        Condition condition = new EventCondition(type, source, minValue, window);

        return new Rule(condition, c[5], new Command(action), priority);
    }
}
