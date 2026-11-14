package com.homehub.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import com.homehub.automation.DeviceControl;
import com.homehub.automation.Event;
import com.homehub.automation.EventBus;
import com.homehub.automation.EventType;
import com.homehub.automation.RuleEngine;
import com.homehub.automation.Simulation;
import com.homehub.devices.Action;
import com.homehub.devices.Command;
import com.homehub.devices.DeviceRegistry;
import com.homehub.devices.Devices;
import com.homehub.loaders.DevicesLoader;
import com.homehub.energy.EnergyAccountant;
import com.homehub.exceptions.HomeHubException;

// Menu and input only. Every decision is made by the engine, DeviceControl or the simulation.
public class Console {

    private final DeviceRegistry registry;
    private final RuleEngine engine;
    private final DeviceControl control;
    private final EventBus bus;
    private final List<Event> events;
    private final Path devicesFile;
    private final BufferedReader in;
    private final PrintStream out;
    private final Display display;

    public Console(DeviceRegistry registry, RuleEngine engine, DeviceControl control, EventBus bus,
                   List<Event> events, Path devicesFile, BufferedReader in, PrintStream out) {
        this.devicesFile = devicesFile;
        this.registry = registry;
        this.engine = engine;
        this.control = control;
        this.bus = bus;
        this.events = events;
        this.in = in;
        this.out = out;
        this.display = new Display(out);
    }

    public void run() {
        try {
            while (true) {
                printMenu();
                String choice = ask("Choose: ");
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
        } catch (InputEnded e) {
            // Input closed (Ctrl+D / Ctrl+Z or a pipe ended), even in the middle of a question: leave cleanly.
            out.println();
        }
        out.println("Goodbye.");
    }

    private static class InputEnded extends RuntimeException {
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
            default -> out.println("Please choose a number from the menu.");
        }
    }

    private void sendCommand() {
        String id = ask("Device id: ");
        Action action = askChoice("Action", Action.class);
        int value = 0;
        if (action == Action.BRIGHTEN || action == Action.DIM) {
            value = askNumber("Amount (0-100): ");
            if (value < 0 || value > 100) {
                throw new HomeHubException("The amount must be between 0 and 100.");
            }
        }
        control.send(id, new Command(action, value));
        out.print("OK - ");
        display.state(registry.get(id));
    }

    private void addDevice() {
        String[] cells = {
            ask("Type (BULB, TV, SPEAKER, LOCK, HUMIDITY, AIR, MOTION): ").toUpperCase(),
            ask("Id: "), ask("Name: "), ask("Brand: "), ask("Model: "), ask("Room: "),
            ask("Rated watts: "), ask("Online (true/false): ").toLowerCase()
        };
        DevicesLoader loader = new DevicesLoader();
        Devices device = loader.create(cells);
        registry.register(device);
        try {
            loader.append(devicesFile, cells);
        } catch (RuntimeException e) {
            // Keep memory and file in step: if it could not be saved, it is not added.
            registry.remove(device.getId());
            throw e;
        }
        out.print("Added and saved - ");
        display.state(device);
    }

    private void removeDevice() {
        Devices removed = registry.remove(ask("Device id: "));
        try {
            new DevicesLoader().removeRow(devicesFile, removed.getId());
        } catch (RuntimeException e) {
            // Keep memory and file in step: if the file could not be updated, the device stays.
            registry.register(removed);
            throw e;
        }
        out.println("Removed and saved - " + removed.getName() + ".");
    }

    private void triggerEvent() {
        EventType type = askChoice("Event type", EventType.class);
        String source = ask("Source id: ");
        // A door event only says it happened, so it has no reading to ask for.
        boolean hasReading = type != EventType.DOOR_LOCKED && type != EventType.DOOR_UNLOCKED;
        int value = hasReading ? askNumber("Value: ") : 0;
        bus.publish(new Event(type, LocalDateTime.now(), source, value));
    }

    private void runSimulation() {
        if (events.isEmpty()) {
            out.println("No events to run.");
            return;
        }
        // Start billing at the first event, otherwise the idle time before it would count as usage.
        LocalDateTime first = events.stream().map(Event::timestamp).min(Comparator.naturalOrder()).get();
        LocalDateTime last = events.stream().map(Event::timestamp).max(Comparator.naturalOrder()).get();

        EnergyAccountant accountant = new EnergyAccountant(registry, first.truncatedTo(ChronoUnit.MINUTES));
        Simulation simulation = new Simulation(bus, accountant, out::println);
        events.forEach(simulation::addEvent);
        simulation.run(last.plusHours(1));
        display.energy(accountant.getKwhByDevice());
    }

    private void printMenu() {
        out.println();
        out.println("1) List devices   2) List rules   3) Send a command");
        out.println("4) Trigger an event   5) Run the simulation");
        out.println("6) Add a device   7) Remove a device   0) Quit");
    }

    // Asks for one of an enum's values and explains the options if the answer is not one of them.
    private <E extends Enum<E>> E askChoice(String label, Class<E> type) {
        List<E> options = List.of(type.getEnumConstants());
        String text = ask(label + " " + options + ": ");
        try {
            return Enum.valueOf(type, text.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new HomeHubException("'" + text + "' is not a valid " + label.toLowerCase() + ". Choose from " + options + ".");
        }
    }

    private int askNumber(String prompt) {
        String text = ask(prompt);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new HomeHubException("'" + text + "' is not a whole number.");
        }
    }

    private String ask(String prompt) {
        out.print(prompt);
        try {
            String line = in.readLine();
            if (line == null) {
                throw new InputEnded();
            }
            return line.trim();
        } catch (IOException e) {
            throw new HomeHubException("Could not read your input.");
        }
    }
}
