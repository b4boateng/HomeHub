package com.homehub;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;


public class Application {

    private static final double POWER_LIMIT_WATTS = 3000;

    public static void main(String[] args) {
        Path folder = Path.of(args.length > 0 ? args[0] : "src/main/resources");

        try {
            DeviceRegistry registry = new DeviceRegistry();
            for (Devices device : new DevicesLoader().load(folder.resolve("devices.csv"))) {
                registry.register(device);
            }

            EventBus bus = new EventBus();
            RuleEngine engine = new RuleEngine(registry, bus, new PowerManager(POWER_LIMIT_WATTS), System.out::println);
            for (Rule rule : new RulesLoader().load(folder.resolve("rules.csv"))) {
                engine.addRule(rule);
            }

            List<Event> events = new EventsLoader().load(folder.resolve("events.csv"));
            if (events.isEmpty()) {
                System.out.println("No events to run.");
                return;
            }

            // Bill from the first event until an hour after the last one.
            LocalDateTime first = events.stream().map(Event::timestamp).min(Comparator.naturalOrder()).get();
            LocalDateTime last = events.stream().map(Event::timestamp).max(Comparator.naturalOrder()).get();

            EnergyAccountant accountant = new EnergyAccountant(registry, first);
            Simulation simulation = new Simulation(bus, accountant, System.out::println);
            events.forEach(simulation::addEvent);
            simulation.run(last.plusHours(1));

            System.out.println("\n=== energy used (kWh) ===");
            accountant.getKwhByDevice().forEach((id, kwh) -> System.out.printf("%-4s %.4f%n", id, kwh));

        } catch (HomeHubException e) {
            System.out.println("Could not start: " + e.getMessage());
        }
    }
}
