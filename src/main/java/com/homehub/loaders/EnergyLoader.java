package com.homehub.loaders;

import com.homehub.energy.*;
import com.homehub.exceptions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// energy.csv columns: hour,deviceId,kwh
// One row per device per hour, for example: 2026-11-14T13:00,b1,0.060000
public class EnergyLoader extends CsvLoader<EnergyRecord> {

    private static final String HEADER = "hour,deviceId,kwh";

    public EnergyLoader() {
        super(3);
    }

    @Override
    protected EnergyRecord parseRow(String[] c) {
        LocalDateTime hour = LocalDateTime.parse(c[0]);
        if (c[1].isEmpty()) {
            throw new IllegalArgumentException("the device id is empty");
        }
        double kwh = Double.parseDouble(c[2]);
        if (kwh < 0) {
            throw new IllegalArgumentException("kwh cannot be negative but found " + c[2]);
        }
        return new EnergyRecord(hour, c[1], kwh);
    }

    public void save(Path file, List<EnergyRecord> records) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (EnergyRecord r : records) {
            lines.add(r.hour() + "," + r.deviceId() + "," + String.format(Locale.ROOT, "%.6f", r.kwh()));
        }
        try {
            Files.write(file, lines);
        } catch (IOException e) {
            throw new HomeHubException("The energy history could not be saved to " + file + ".");
        }
    }
}
