package com.homehub;

import java.util.Objects;

public class Command {

    private final Action action;
    private final int value;

    public Command(Action action, int value) {
        this.action = Objects.requireNonNull(action, "action");
        this.value = value;
    }

    public Command(Action action) {
        this(action, 0);
    }

    public Action action() {
        return action;
    }

    public int value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Command other)) return false;
        return action == other.action && value == other.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, value);
    }

    @Override
    public String toString() {
        return "Command[action=" + action + ", value=" + value + "]";
    }
}
