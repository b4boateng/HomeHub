package com.homehub;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class RuleEngine {

    private final DeviceRegistry registry;
    private final List<Rule> rules = new ArrayList<>();
    private final Consumer<String> log;

    public RuleEngine(DeviceRegistry registry, EventBus bus, Consumer<String> log) {
        this.registry = registry;
        this.log = log;
        bus.subscribe(this::handle);
    }

    public void addRule(Rule rule) {
        rules.add(rule);
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

        Devices device = registry.get(targetId);

        if (!(device instanceof Controllable controllable) || !controllable.supports(command.action())) {
            log.accept("Skipped: " + device.getName() + " cannot " + command.action());
            return;
        }

        try {
            controllable.execute(command);
            log.accept("Applied " + command.action() + " to " + device.getName()
                    + " (priority " + rule.getPriority() + ", triggered by " + event.type() + ")");
        } catch (IllegalStateException ex) {
            log.accept("Failed: " + command.action() + " on " + device.getName() + ": " + ex.getMessage());
        }
    }

      

}

