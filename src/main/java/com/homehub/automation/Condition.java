package com.homehub.automation;

@FunctionalInterface 
// Interface so any kind of check can be plugged into a rule.
public interface Condition {
    public boolean test(Event e);

    // Plain-words summary for listings; conditions written as lambdas fall back to this.
    default String describe() {
        return "custom condition";
    }
}
