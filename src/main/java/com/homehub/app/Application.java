package com.homehub.app;

import com.homehub.automation.*;
import com.homehub.energy.*;
import com.homehub.loaders.*;
import com.homehub.devices.*;
import com.homehub.exceptions.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Application {

    private static final double POWER_LIMIT_WATTS = 3000;

    public static void main(String[] args) {
        boolean batch = Arrays.asList(args).contains("--batch");
        Path folder = Path.of(args.length > 0 && !args[0].equals("--batch") ? args[0] : "src/main/resources");

        try {
            DeviceRegistry registry = new DeviceRegistry();
            for (Devices device : new DevicesLoader().load(folder.resolve("devices.csv"))) {
                registry.register(device);
            }

            PowerManager powerManager = new PowerManager(POWER_LIMIT_WATTS);
            EventBus bus = new EventBus();
            RuleEngine engine = new RuleEngine(registry, bus, powerManager, System.out::println);
            for (Rule rule : new RulesLoader().load(folder.resolve("rules.csv"))) {
                engine.addRule(rule);
            }

            List<Event> events = new EventsLoader().load(folder.resolve("events.csv"));

            if (batch) {
                runBatch(registry, bus, events);
            } else {
                runConsole(registry, bus, powerManager, events, folder);
            }

        } catch (HomeHubException e) {
            System.out.println("Could not start: " + e.getMessage());
        }
    }

    private static void runConsole(DeviceRegistry registry, EventBus bus, PowerManager powerManager, List<Event> events, Path folder) {
        LocalDateTime start = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        EnergyAccountant accountant = new EnergyAccountant(registry, start);
        Path energyFile = folder.resolve("energy.csv");
        if (Files.exists(energyFile)) {
            accountant.addHistory(new EnergyLoader().load(energyFile));
        }
        DeviceControl control = new DeviceControl(registry, powerManager);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        new Console(registry, bus, control, powerManager, accountant, events,
                folder.resolve("devices.csv"), energyFile, in, System.out).run();
    }

    private static void runBatch(DeviceRegistry registry, EventBus bus, List<Event> events) {
        if (events.isEmpty()) {
            System.out.println("No events to run.");
            return;
        }

        LocalDateTime first = events.stream().map(Event::timestamp).min(Comparator.naturalOrder()).get();
        LocalDateTime last = events.stream().map(Event::timestamp).max(Comparator.naturalOrder()).get();

        EnergyAccountant accountant = new EnergyAccountant(registry, first);
        Simulation simulation = new Simulation(bus, accountant, System.out::println);
        events.forEach(simulation::addEvent);
        simulation.run(last.plusHours(1));

        System.out.println("\n=== energy used (kWh) ===");
        accountant.getKwhByDevice().forEach((id, kwh) -> System.out.printf("%-4s %.4f%n", id, kwh));
    }
}
