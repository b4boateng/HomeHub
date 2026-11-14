package com.homehub.automation;

@FunctionalInterface 
public interface Condition {
    public boolean test(Event e);
}
