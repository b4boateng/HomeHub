package com.homehub.automation;

import com.homehub.energy.*;
import com.homehub.devices.*;
import com.homehub.exceptions.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class RuleEngine {

    private final DeviceRegistry registry;
    private final List<Rule> rules = new ArrayList<>();
    private final Consumer<String> log;
    private final DeviceControl control;

    public RuleEngine(DeviceRegistry registry, EventBus bus, PowerPolicy powerPolicy, Consumer<String> log) {
        this.registry = registry;
        this.control = new DeviceControl(registry, powerPolicy);
        this.log = log;
        bus.subscribe(this::handle);
    }

    public void addRule(Rule rule) {
        ConflictDetector.check(rule, rules);
        rules.add(rule);
    }

    public List<Rule> getRules() {
        return List.copyOf(rules);
    }

    private void handle(Event event) {
        List<Rule> matching = rules.stream()
                .filter(r -> r.matches(event))
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

        if (!registry.contains(targetId)) {
            log.accept("Skipped: no device '" + targetId + "'");
            return;
        }

        String name = registry.get(targetId).getName();

        try {
            control.send(targetId, command);
            log.accept("Applied " + command.action() + " to " + name
                    + " (priority " + rule.getPriority() + ", triggered by " + event.type() + ")");
        } catch (IllegalStateException | HomeHubException ex) {
            log.accept("Failed: " + command.action() + " on " + name + ": " + ex.getMessage());
        }
    }

      

}

