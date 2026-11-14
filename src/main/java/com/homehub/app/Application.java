package com.homehub.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

import com.homehub.automation.DeviceControl;
import com.homehub.automation.Event;
import com.homehub.automation.EventBus;
import com.homehub.automation.Rule;
import com.homehub.automation.RuleEngine;
import com.homehub.devices.DeviceRegistry;
import com.homehub.devices.Devices;
import com.homehub.energy.PowerManager;
import com.homehub.exceptions.HomeHubException;
import com.homehub.loaders.DevicesLoader;
import com.homehub.loaders.EventsLoader;
import com.homehub.loaders.RulesLoader;

// Only wires things together: load the files, build the objects, hand over to the console.
public class Application {

    private static final double POWER_LIMIT_WATTS = 3000;

    public static void main(String[] args) {
        Path folder = Path.of(args.length > 0 ? args[0] : "src/main/resources");

        try {
            DeviceRegistry registry = new DeviceRegistry();
            Path devicesFile = folder.resolve("devices.csv");
            for (Devices device : new DevicesLoader().load(devicesFile)) {
                registry.register(device);
            }

            PowerManager powerManager = new PowerManager(POWER_LIMIT_WATTS);
            EventBus bus = new EventBus();
            RuleEngine engine = new RuleEngine(registry, bus, powerManager, System.out::println);
            for (Rule rule : new RulesLoader().load(folder.resolve("rules.csv"))) {
                engine.addRule(rule);
            }
            List<Event> events = new EventsLoader().load(folder.resolve("events.csv"));

            DeviceControl control = new DeviceControl(registry, powerManager);
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            new Console(registry, engine, control, bus, events, devicesFile, in, System.out).run();

        } catch (HomeHubException | IllegalArgumentException e) {
            // A bad or missing data file ends up here, so say which one instead of crashing.
            System.out.println("Could not start: " + e.getMessage());
        }
    }
}
