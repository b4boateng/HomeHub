package com.homehub.app;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import com.homehub.automation.DeviceControl;
import com.homehub.automation.Event;
import com.homehub.automation.EventBus;
import com.homehub.automation.EventType;
import com.homehub.automation.RuleEngine;
import com.homehub.devices.Action;
import com.homehub.devices.Command;
import com.homehub.devices.DeviceRegistry;
import com.homehub.devices.Devices;
import com.homehub.exceptions.HomeHubException;
import com.homehub.loaders.DevicesLoader;
import com.homehub.service.DeviceCatalog;
import com.homehub.service.SensorService;
import com.homehub.service.SimulationRunner;

// Menu only: asks the questions, calls the right service and prints the answer. Every decision is made elsewhere.
public class Console {

    private final DeviceRegistry registry;
    private final RuleEngine engine;
    private final DeviceControl control;
    private final EventBus bus;
    private final List<Event> events;
    private final DeviceCatalog catalog;
    private final SimulationRunner runner;
    private final SensorService sensors;
    private final Prompter input;
    private final PrintStream out;
    private final Display display;

    public Console(DeviceRegistry registry, RuleEngine engine, DeviceControl control, EventBus bus,
                   List<Event> events, Path devicesFile, Path energyFile, BufferedReader in, PrintStream out) {
        this.registry = registry;
        this.engine = engine;
        this.control = control;
        this.bus = bus;
        this.events = events;
        this.catalog = new DeviceCatalog(registry, devicesFile);
        this.runner = new SimulationRunner(registry, bus, energyFile);
        this.sensors = new SensorService(registry, bus);
        this.input = new Prompter(in, out);
        this.out = out;
        this.display = new Display(out);
    }

    public void run() {
        try {
            while (true) {
                printMenu();
                String choice = input.ask("Choose: ");
                if (choice.equals("0")) {
                    break;
                }
                try {
                    handle(choice);
                } catch (HomeHubException | IllegalArgumentException e) {
                    // A wrong id, action or number should not end the session.
                    out.println("Could not do that: " + e.getMessage());
                }
            }
        } catch (Prompter.InputEnded e) {
            // Input closed (Ctrl+D / Ctrl+Z or a pipe ended), even in the middle of a question: leave cleanly.
            out.println();
        }
        out.println("Goodbye.");
    }

    private void printMenu() {
        out.println();
        out.println("1) List devices   2) List rules   3) Send a command");
        out.println("4) Trigger an event   5) Run the simulation");
        out.println("6) Add a device   7) Remove a device   8) Read sensors   0) Quit");
    }

    private void handle(String choice) {
        switch (choice) {
            case "1" -> display.devices(registry);
            case "2" -> display.rules(engine.getRules(), registry);
            case "3" -> sendCommand();
            case "4" -> triggerEvent();
            case "5" -> runSimulation();
            case "6" -> addDevice();
            case "7" -> removeDevice();
            case "8" -> readSensors();
            default -> out.println("Please choose a number from the menu.");
        }
    }

    private void sendCommand() {
        String id = input.ask("Device id: ");
        Devices target = registry.get(id);                  // an unknown id is reported before anything else is asked
        Action action = input.askChoice("Action", Action.class);
        control.requireSupports(id, action);                // so is an action the device cannot do, such as a lock dimming

        // Unlocking from the console needs the lock's code (rules unlock without one: they are the owner's own automation).
        if (target.requiresCodeFor(action) && !target.codeMatches(input.ask("Lock code: "))) {
            throw new HomeHubException("Wrong code for " + target.getName() + ".");
        }
        boolean needsAmount = action == Action.BRIGHTEN || action == Action.DIM
                || action == Action.WARMER || action == Action.COOLER;
        control.send(id, new Command(action, needsAmount ? input.askAmount() : 0));
        out.print("OK - ");
        display.state(target);
    }

    private void triggerEvent() {
        EventType type = input.askChoice("Event type", EventType.class);
        String source = input.ask("Source id: ");
        // A door event only says it happened, so it has no reading to ask for.
        int value = type.hasReading() ? input.askNumber("Value: ") : 0;
        bus.publish(new Event(type, LocalDateTime.now(), source, value));
    }

    private void readSensors() {
        if (sensors.readAll(display::state) == 0) {
            out.println("No sensors are online.");
        }
    }

    private void runSimulation() {
        if (events.isEmpty()) {
            out.println("No events to run.");
            return;
        }
        SimulationRunner.Result result = runner.run(events, out::println);
        display.energy(result.kwhByDevice());
        out.println("Energy history saved (" + result.savedRecords() + " hourly records).");
    }

    private void addDevice() {
        String[] cells = {
            input.ask("Type " + DevicesLoader.TYPES + ": ").toUpperCase(),
            input.ask("Id: "), input.ask("Name: "), input.ask("Brand: "), input.ask("Model: "), input.ask("Room: "),
            input.ask("Rated watts: "), input.ask("Online (true/false): ").toLowerCase()
        };
        Devices device = catalog.add(cells);
        out.print("Added and saved - ");
        display.state(device);
    }

    private void removeDevice() {
        Devices removed = catalog.remove(input.ask("Device id: "));
        out.println("Removed and saved - " + removed.getName() + ".");
    }
}
