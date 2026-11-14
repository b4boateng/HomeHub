package com.homehub.automation;

import com.homehub.energy.*;
import com.homehub.devices.*;
import com.homehub.exceptions.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

// Listens for events and runs the commands of every rule that matches, highest priority first.
public class RuleEngine {

    private final DeviceRegistry registry;
    private final List<Rule> rules = new ArrayList<>();
    private final Consumer<String> log;
    private final DeviceControl control;

    public RuleEngine(DeviceRegistry registry, EventBus bus, PowerPolicy powerPolicy, Consumer<String> log) {
        this.registry = registry;
        this.control = new DeviceControl(registry, powerPolicy);
        this.log = log;
        // Subscribe so every published event reaches handle().
        bus.subscribe(this::handle);
    }

    public void addRule(Rule rule) {
        // Reject the rule before adding it if it conflicts with one already there.
        rejectIfConflicting(rule);
        rules.add(rule);
    }

    // A conflict is two rules on one device with the same priority but different actions: the engine could not tell which should win.
    private void rejectIfConflicting(Rule newRule) {
        for (Rule other : rules) {
            boolean sameDevice = other.getTargetDeviceId().equals(newRule.getTargetDeviceId());
            boolean samePriority = other.getPriority() == newRule.getPriority();
            boolean differentAction = other.getCommand().action() != newRule.getCommand().action();

            if (sameDevice && samePriority && differentAction) {
                throw new HomeHubException("Rules for '" + newRule.getTargetDeviceId()
                        + "' both have priority " + newRule.getPriority() + " but ask for "
                        + other.getCommand().action() + " and " + newRule.getCommand().action());
            }
        }
    }

    public List<Rule> getRules() {
        // A copy, so callers cannot change the engine's rules.
        return List.copyOf(rules);
    }

    private void handle(Event event) {
        List<Rule> matching = rules.stream()
                .filter(r -> r.matches(event))
                // Lower number runs first.
                .sorted(Comparator.comparingInt(Rule::getPriority))
                .toList();

        if (matching.isEmpty()) {
            log.accept("No rule matched " + event);
            return;
        }

        for (Rule rule : matching) {
            apply(rule, event);
        }
    }

    private void apply(Rule rule, Event event) {
        String targetId = rule.getTargetDeviceId();
        Command command = rule.getCommand();

        // A rule can point to a device that is not in devices.csv, it is  skipped instead of crashing.
        if (!registry.contains(targetId)) {
            log.accept("Skipped: no device '" + targetId + "'");
            return;
        }

        String name = registry.get(targetId).getName();

        try {
            control.send(targetId, command);
            log.accept("Applied " + command.action() + " to " + name
                    + " (priority " + rule.getPriority() + ", triggered by " + event.type() + ")");
        // Log the failure and keep going with the other rules.
        } catch (IllegalStateException | HomeHubException ex) {
            log.accept("Failed: " + command.action() + " on " + name + ": " + ex.getMessage());
        }
    }

      

}

