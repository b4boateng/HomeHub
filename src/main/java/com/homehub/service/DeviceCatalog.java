package com.homehub.service;

import java.nio.file.Path;

import com.homehub.devices.DeviceRegistry;
import com.homehub.devices.Devices;
import com.homehub.loaders.DevicesLoader;

// Adds and removes devices so that memory and devices.csv always change together.
public class DeviceCatalog {

    private final DeviceRegistry registry;
    private final Path devicesFile;
    private final DevicesLoader loader = new DevicesLoader();

    public DeviceCatalog(DeviceRegistry registry, Path devicesFile) {
        this.registry = registry;
        this.devicesFile = devicesFile;
    }

    // Takes the same 8 cells a devices.csv row has.
    public Devices add(String[] cells) {
        Devices device = loader.create(cells);
        registry.register(device);
        try {
            loader.append(devicesFile, cells);
        } catch (RuntimeException e) {
            registry.remove(device.getId());               // it could not be saved, so it is not added
            throw e;
        }
        return device;
    }

    public Devices remove(String id) {
        Devices removed = registry.remove(id);
        try {
            loader.removeRow(devicesFile, removed.getId());
        } catch (RuntimeException e) {
            registry.register(removed);                    // the file could not be updated, so the device stays
            throw e;
        }
        return removed;
    }
}
