package com.homehub.app;

import com.homehub.automation.*;
import com.homehub.energy.*;
import com.homehub.loaders.*;
import com.homehub.devices.*;
import com.homehub.exceptions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

public class Console {

    private static final List<String> MAIN_MENU = List.of(
            "View devices",
            "Control a device",
            "Read a sensor",
            "Trigger an event",
            "Advance the clock",
            "View current power consumption",
            "View energy consumption to date",
            "Add a device");

    private static final List<String> DEVICE_TYPES = List.of(
            "Light bulb",
            "Door lock",
            "Television",
            "Speaker",
            "Humidity sensor",
            "Air quality monitor");

    private static final List<String> TIME_MENU = List.of(
            "30 minutes",
            "1 hour",
            "6 hours",
            "Until 22:00",
            "Until 06:00");

    private static final List<String> AMOUNT_MENU = List.of(
            "Small (10)",
            "Medium (25)",
            "Large (50)");
    private static final int[] AMOUNTS = {10, 25, 50};
    private static final String[] CSV_TYPES = {"BULB", "LOCK", "TV", "SPEAKER", "HUMIDITY", "AIR"};

    private final DeviceRegistry registry;
    private final EventBus bus;
    private final DeviceControl control;
    private final PowerManager powerManager;
    private final EnergyAccountant accountant;
    private final List<Event> events;
    private final BufferedReader in;
    private final PrintStream out;

    private final Path devicesFile;
    private final Path energyFile;

    private LocalDateTime now;

    public Console(DeviceRegistry registry, EventBus bus, DeviceControl control, PowerManager powerManager,
                   EnergyAccountant accountant, List<Event> events, Path devicesFile, Path energyFile,
                   BufferedReader in, PrintStream out) {
        this.devicesFile = devicesFile;
        this.energyFile = energyFile;
        this.registry = registry;
        this.bus = bus;
        this.control = control;
        this.powerManager = powerManager;
        this.accountant = accountant;
        this.events = events;
        this.in = in;
        this.out = out;
        this.now = accountant.getLastBilled();
    }

    public void run() {
        out.println("Welcome to HomeHub. Enter the number of your selection and press Enter.");
        while (true) {
            int choice = choose("Main menu (current time: " + prettyTime(now) + ")", MAIN_MENU, "Exit");
            if (choice == 0) {
                out.println("The HomeHub session has ended.");
                return;
            }
            try {
                switch (choice) {
                    case 1 -> listDevices();
                    case 2 -> controlDevice();
                    case 3 -> readSensor();
                    case 4 -> fireEvent();
                    case 5 -> letTimePass();
                    case 6 -> showPower();
                    case 7 -> showEnergy();
                    case 8 -> addDevice();
                }
            } catch (HomeHubException | IllegalStateException | IllegalArgumentException e) {
                out.println("The request could not be completed: " + e.getMessage());
            }
        }
    }

    private void listDevices() {
        out.println();
        out.println("Registered devices:");
        for (Devices d : registry.all()) {
            out.println("  - " + describe(d));
        }
    }

    private String describe(Devices d) {
        String text = d.getName() + " (" + d.getRoom() + "): " + (d.isOnline() ? "on" : "off");
        if (d instanceof EnergyReporting meter && d.isOnline()) {
            text += ", consuming " + watts(meter.currentPowerWatts());
        }
        return text;
    }

    private void controlDevice() {
        List<Devices> devices = registry.all().stream().filter(d -> d instanceof Controllable).toList();
        int deviceChoice = choose("Step 1 of 3: Select a device", devices.stream().map(this::describe).toList(), "Return");
        if (deviceChoice == 0) {
            return;
        }
        Devices device = devices.get(deviceChoice - 1);

        List<Action> actions = Arrays.stream(Action.values())
                .filter(a -> ((Controllable) device).supports(a)).toList();
        int actionChoice = choose("Step 2 of 3: Select an action for " + device.getName(),
                actions.stream().map(this::friendly).toList(), "Return");
        if (actionChoice == 0) {
            return;
        }
        Action action = actions.get(actionChoice - 1);

        int amount = 0;
        if (action == Action.BRIGHTEN || action == Action.DIM) {
            int amountChoice = choose("Step 3 of 3: Select an amount", AMOUNT_MENU, "Return");
            if (amountChoice == 0) {
                return;
            }
            amount = AMOUNTS[amountChoice - 1];
        }

        control.send(device.getId(), new Command(action, amount));
        out.println("Completed: " + friendly(action) + " - " + device.getName() + ".");
    }

    private String friendly(Action a) {
        return switch (a) {
            case TURN_ON -> "Turn on";
            case TURN_OFF -> "Turn off";
            case LOCK -> "Lock";
            case UNLOCK -> "Unlock";
            case NOTIFY -> "Send a notification";
            case SOUND_ALARM -> "Sound the alarm";
            case BRIGHTEN -> "Increase brightness";
            case DIM -> "Decrease brightness";
            case PLAY -> "Play";
            case STOP -> "Stop";
            case PAUSE -> "Pause";
            case RESUME -> "Resume";
        };
    }

    private void readSensor() {
        List<Devices> sensors = registry.all().stream().filter(d -> d instanceof Sensing).toList();
        int choice = choose("Select a sensor", sensors.stream().map(Devices::getName).toList(), "Return");
        if (choice == 0) {
            return;
        }
        Devices sensor = sensors.get(choice - 1);
        ((Sensing) sensor).readValue();

        if (sensor instanceof HumiditySensor h) {
            out.println(h.getName() + " reports a humidity of " + h.getHumidity() + "%.");
        } else if (sensor instanceof AirQualityMonitor a) {
            out.println(a.getName() + " reports: carbon dioxide " + a.getCarbonDioxideLevel()
                    + ", carbon monoxide " + a.getCarbonMonoxideLevel()
                    + ", oxygen " + a.getOxygenLevel() + ", temperature " + a.getTemperature()
                    + ", PM2.5 " + a.getPm25() + ".");
        } else {
            out.println("A new reading was taken from " + sensor.getName() + ".");
        }
    }

    private void fireEvent() {
        List<String> labels = events.stream().map(this::describeEvent).toList();
        int choice = choose("Select the event that occurred", labels, "Return");
        if (choice == 0) {
            return;
        }
        Event chosen = events.get(choice - 1);
        out.println("The event has been submitted. Any matching rules will now run.");
        bus.publish(new Event(chosen.type(), now, chosen.sourceId(), chosen.value()));
        out.println("Event processed.");
    }

    private String describeEvent(Event e) {
        return switch (e.type()) {
            case MOTION -> "Motion detected (" + e.sourceId() + ")";
            case DOOR_LOCKED -> "Door locked (" + e.sourceId() + ")";
            case DOOR_UNLOCKED -> "Door unlocked (" + e.sourceId() + ")";
            case TEMPERATURE -> "Temperature changed to " + e.value() + " (" + e.sourceId() + ")";
            case HUMIDITY -> "Humidity changed to " + e.value() + "% (" + e.sourceId() + ")";
        };
    }

    private void letTimePass() {
        switch (choose("Select the duration to advance", TIME_MENU, "Return")) {
            case 1 -> moveClockTo(now.plusMinutes(30));
            case 2 -> moveClockTo(now.plusHours(1));
            case 3 -> moveClockTo(now.plusHours(6));
            case 4 -> moveClockTo(nextTime(LocalTime.of(22, 0)));
            case 5 -> moveClockTo(nextTime(LocalTime.of(6, 0)));
            default -> { }
        }
    }

    private LocalDateTime nextTime(LocalTime time) {
        LocalDateTime target = now.toLocalDate().atTime(time);
        return target.isAfter(now) ? target : target.plusDays(1);
    }

    private void moveClockTo(LocalDateTime target) {
        long minutes = Duration.between(now, target).toMinutes();
        accountant.advanceTo(target);
        if (energyFile != null) {
            new EnergyLoader().save(energyFile, accountant.getHistory());
        }
        now = target;
        out.println("The clock was advanced by " + minutes / 60 + "h " + minutes % 60 + "m. The current time is "
                + prettyTime(now) + ".");
    }

    private void showPower() {
        out.println("Current consumption is " + watts(control.currentLoad()) + ". "
                + "The configured limit is " + watts(powerManager.getLimitWatts()) + ".");
    }

    private void showEnergy() {
        if (accountant.getKwhByDevice().isEmpty()) {
            out.println("No energy consumption has been recorded. Advance the clock to record usage.");
            return;
        }
        out.println("Energy consumption to date:");
        double total = 0;
        for (var entry : accountant.getKwhByDevice().entrySet()) {
            String name = registry.contains(entry.getKey()) ? registry.get(entry.getKey()).getName() : entry.getKey();
            out.printf("  - %-20s %.3f kWh%n", name, entry.getValue());
            total += entry.getValue();
        }
        out.printf("Total: %.3f kWh%n", total);
    }

    private void addDevice() {
        int type = choose("Step 1 of 4: Select the device type", DEVICE_TYPES, "Return");
        if (type == 0) {
            return;
        }

        out.println();
        out.println("== Step 2 of 4: Enter the device details ==");
        String name = askText("Device name (for example, Bedside Lamp):");
        String room = name == null ? null : askText("Room (for example, Bedroom):");
        String brand = room == null ? null : askText("Manufacturer (for example, Philips):");
        String model = brand == null ? null : askText("Model number (printed on the packaging):");
        Double watts = model == null ? null : askNumber("Rated power in watts (for example, 9):");
        if (watts == null) {
            out.println("The operation was cancelled. No device was added.");
            return;
        }

        out.println();
        out.println("== Step 3 of 4: Review the details ==");
        out.println("  " + DEVICE_TYPES.get(type - 1) + ": " + name + " (" + room + ")");
        out.println("  Manufacturer " + brand + ", model " + model + ", " + watts + " W");
        out.println("  Default settings will be applied (for example, volume 20 or brightness 50).");
        if (choose("Step 4 of 4: Confirm", List.of("Add the device"), "Cancel") == 0) {
            out.println("No device was added.");
            return;
        }

        String id = nextId(type);
        saveToCsv(CSV_TYPES[type - 1], id, name, brand, model, room, watts);
        registry.register(build(type, id, name, brand, room, model, watts));
        out.println("The device has been added: " + name + " (identifier: " + id + ").");
    }

    private void saveToCsv(String type, String id, String name, String brand, String model, String room, double watts) {
        if (devicesFile == null) {
            return;
        }
        String line = String.join(",", type, id, clean(name), clean(brand), clean(model), clean(room), String.valueOf(watts), "true");
        try {
            String existing = Files.exists(devicesFile) ? Files.readString(devicesFile) : "";
            String separator = existing.isEmpty() || existing.endsWith("\n") ? "" : System.lineSeparator();
            Files.writeString(devicesFile, separator + line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new HomeHubException("The device could not be saved to " + devicesFile + ".");
        }
    }

    private static String clean(String text) {
        return text.replace(',', ' ').trim();
    }

    private Devices build(int type, String id, String name, String brand, String room, String model, double watts) {
        return switch (type) {
            case 1 -> new SmartBulb(50, 128, id, model, name, brand, room, true, watts, "");
            case 2 -> new SmartLock(id, name, brand, true, room, model, watts, true, "0000", 100);
            case 3 -> new SmartTv(id, name, brand, true, room, model, watts, 20, false, "None", 1);
            case 4 -> new SmartSpeaker(id, name, brand, true, room, model, watts, 20, false, "None");
            case 5 -> new HumiditySensor(id, model, name, brand, true, room, watts, 40);
            default -> new AirQualityMonitor(id, name, brand, true, room, model, 21, 10, 1200, 22, 8, watts);
        };
    }

    private String nextId(int type) {
        String prefix = "bltsha".substring(type - 1, type);
        int n = 1;
        while (registry.contains(prefix + n)) {
            n++;
        }
        return prefix + n;
    }

    private String askText(String question) {
        while (true) {
            out.print(question + " ");
            out.flush();
            String answer = readLine();
            if (answer == null) {
                return null;
            }
            if (!answer.isBlank()) {
                return answer;
            }
            out.println("An entry is required. Press Ctrl+D to cancel.");
        }
    }

    private Double askNumber(String question) {
        while (true) {
            String answer = askText(question);
            if (answer == null) {
                return null;
            }
            try {
                double value = Double.parseDouble(answer);
                if (value >= 0) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
            }
            out.println("Please enter a non-negative number, for example 9 or 12.5.");
        }
    }

    private String watts(double w) {
        return (w == Math.rint(w) ? String.valueOf((long) w) : String.format("%.1f", w)) + " W";
    }

    private String prettyTime(LocalDateTime t) {
        return t.toLocalDate() + " " + t.toLocalTime();
    }

    private int choose(String title, List<String> options, String zeroLabel) {
        while (true) {
            out.println();
            out.println("== " + title + " ==");
            for (int i = 0; i < options.size(); i++) {
                out.println("  " + (i + 1) + ". " + options.get(i));
            }
            out.println("  0. " + zeroLabel);
            out.print("Selection: ");
            out.flush();

            String answer = readLine();
            if (answer == null) {
                return 0;
            }
            if (answer.matches("\\d{1,6}") && Integer.parseInt(answer) <= options.size()) {
                return Integer.parseInt(answer);
            }
            out.println("Invalid selection. Please enter a number between 0 and " + options.size() + ".");
        }
    }

    private String readLine() {
        try {
            String line = in.readLine();
            return line == null ? null : line.trim();
        } catch (IOException e) {
            return null;
        }
    }
}
