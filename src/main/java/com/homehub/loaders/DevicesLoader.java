package com.homehub.loaders;

import com.homehub.devices.*;
import com.homehub.exceptions.DataLoadException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

// Reads devices.csv and creates the right device type for each row.
public class DevicesLoader extends CsvLoader<Devices> {

    private static final int START_BRIGHTNESS = 50;
    private static final int START_COLOR_TEMPERATURE = 128;
    private static final int START_VOLUME = 20;
    private static final int START_CHANNEL = 1;
    private static final String START_LOCK_CODE = "0000";
    private static final int START_BATTERY = 100;
    private static final int START_HUMIDITY = 50;
    private static final int START_OXYGEN = 21;
    private static final int START_CARBON_MONOXIDE = 0;
    private static final int START_CARBON_DIOXIDE = 400;
    private static final int START_AIR_TEMPERATURE = 21;
    private static final int START_PM25 = 5;

    public DevicesLoader() {
        super(8);
    }

    // Builds one device from the same 8 cells a devices.csv row has (type,id,name,brand,model,room,watts,online).
    public Devices create(String[] cells) {
        return parseRow(cells);
    }

    // Adds one row to the end of devices.csv, in the same 8-column format that load() reads.
    public void append(Path path, String[] cells) {
        for (String cell : cells) {
            if (cell.contains(",")) {
                throw new IllegalArgumentException("commas are not allowed in '" + cell + "'");
            }
        }
        try {
            String existing = Files.readString(path);
            String prefix = existing.isEmpty() || existing.endsWith("\n") ? "" : "\n";
            Files.writeString(path, prefix + String.join(",", cells) + "\n",
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new DataLoadException(path.getFileName().toString(), 0, "cannot write file");
        }
    }

    // Deletes the row with this device id from devices.csv and keeps every other line as it was.
    public void removeRow(Path path, String id) {
        String file = path.getFileName().toString();
        try {
            List<String> kept = new ArrayList<>();
            boolean found = false;
            for (String line : Files.readAllLines(path)) {
                String[] cells = line.split(",", -1);
                if (!found && cells.length > 1 && cells[1].trim().equals(id) && !cells[0].trim().equals("type")) {
                    found = true;
                    continue;
                }
                kept.add(line);
            }
            if (!found) {
                throw new DataLoadException(file, 0, "no row for device '" + id + "'");
            }
            // "\n" like the existing file, not the platform separator, so lines are not all rewritten.
            Files.writeString(path, String.join("\n", kept) + "\n");
        } catch (IOException e) {
            throw new DataLoadException(file, 0, "cannot write file");
        }
    }

    @Override
    protected Devices parseRow(String[] c) {
        String id = c[1];
        String name = c[2];
        String brand = c[3];
        String model = c[4];
        String room = c[5];
        double watts;
        try {
            watts = Double.parseDouble(c[6]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("watts must be a number but found '" + c[6] + "'");
        }
        if (id.isEmpty() || name.isEmpty()) {
            throw new IllegalArgumentException("a device needs an id and a name");
        }
        // NaN, infinity and negative watts would break the power limit and the energy totals.
        if (Double.isNaN(watts) || Double.isInfinite(watts) || watts < 0) {
            throw new IllegalArgumentException("watts must be a number of 0 or more but found '" + c[6] + "'");
        }
        boolean online = parseBoolean(c[7]);

        return switch (c[0]) {
            case "BULB"     -> new SmartBulb(START_BRIGHTNESS, START_COLOR_TEMPERATURE, id, model, name, brand, room, online, watts, "");
            case "TV"       -> new SmartTv(id, name, brand, online, room, model, watts, START_VOLUME, false, "", START_CHANNEL);
            case "SPEAKER"  -> new SmartSpeaker(id, name, brand, online, room, model, watts, START_VOLUME, false, "");
            case "LOCK"     -> new SmartLock(id, name, brand, online, room, model, watts, true, START_LOCK_CODE, START_BATTERY);
            case "HUMIDITY" -> new HumiditySensor(id, model, name, brand, online, room, watts, START_HUMIDITY);
            case "AIR"      -> new AirQualityMonitor(id, name, brand, online, room, model, START_OXYGEN, START_CARBON_MONOXIDE,
                                                     START_CARBON_DIOXIDE, START_AIR_TEMPERATURE, START_PM25, watts);
            case "MOTION"   -> new MotionSensor(id, name, brand, online, room, model, watts);
            default -> throw new IllegalArgumentException("unknown device type '" + c[0] + "'");
        };
    }

    // Boolean.parseBoolean would quietly turn a typo into false.
    private static boolean parseBoolean(String text) {
        if (text.equals("true")) return true;
        if (text.equals("false")) return false;
        throw new IllegalArgumentException("expected true or false but found '" + text + "'");
    }
}
