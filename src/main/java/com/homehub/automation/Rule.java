package com.homehub.automation;

import com.homehub.devices.*;
import java.util.Objects;
// Rules check when a  condition matches an event, if it does it  sends a  command to a device.
public class Rule{

        private final Condition condition;
        private final String targetDeviceId;
        private final Command command;
        private final int priority;

    public Rule (Condition condition, String targetDeviceId, Command command, int priority) {
        // Fail early on missing values.
        this.condition = Objects.requireNonNull(condition);
        this.targetDeviceId = Objects.requireNonNull(targetDeviceId);
        if (targetDeviceId.isBlank()) {
            throw new IllegalArgumentException("Target device ID cannot be blank");
        }
        this.command = Objects.requireNonNull(command);
        this.priority = priority;
    }

    public boolean matches(Event event) {
        return condition.test(event);
    }

     public Condition getCondition() {
        return condition;
    }

    

    public String getTargetDeviceId() {
        return targetDeviceId;
    }

    public Command getCommand() {
        return command;
    }

  

    public int getPriority() {
        return priority;
    }

    
}
