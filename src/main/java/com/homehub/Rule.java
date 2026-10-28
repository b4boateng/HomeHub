package com.homehub;
import java.util.Objects;

public class Rule{

        private Condition condition;
        private String targetDeviceId;
        private Command command;
        private int priority;

    public Rule (Condition condition, String targetDeviceId, Command command, int priority) {
        this.condition = Objects.requireNonNull(condition);
        this.targetDeviceId = Objects.requireNonNull(targetDeviceId);
        this.command = Objects.requireNonNull(command);
        this.priority = Objects.requireNonNull(priority);
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
