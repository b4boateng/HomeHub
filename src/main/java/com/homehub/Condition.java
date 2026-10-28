package com.homehub;

@FunctionalInterface 
public interface Condition {
    public boolean test(Event e);
}
