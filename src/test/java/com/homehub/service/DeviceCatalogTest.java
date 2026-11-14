package com.homehub.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.homehub.devices.DeviceRegistry;
import com.homehub.devices.Devices;
import com.homehub.exceptions.HomeHubException;

class DeviceCatalogTest {

    private static final String START = "type,id,name,brand,model,room,watts,online\n"
            + "BULB,b1,Hall Light,Philips,LB-100,Hallway,12,false\n";

    @TempDir
    Path folder;

    private Path devicesFile;
    private DeviceRegistry registry;
    private DeviceCatalog catalog;

    @BeforeEach
    void setUp() throws IOException {
        devicesFile = folder.resolve("devices.csv");
        Files.writeString(devicesFile, START);
        registry = new DeviceRegistry();
        catalog = new DeviceCatalog(registry, devicesFile);
    }

    private static String[] lamp(String id) {
        return new String[] {"BULB", id, "Test Lamp", "Acme", "T-1", "Lab", "20", "false"};
    }

    @Test
    void anAddedDeviceIsInTheRegistryAndTheFile() throws IOException {
        Devices added = catalog.add(lamp("z1"));

        assertSame(added, registry.get("z1"));
        assertTrue(Files.readString(devicesFile).contains("BULB,z1,Test Lamp,Acme,T-1,Lab,20,false"));
    }

    @Test
    void addingThenRemovingLeavesTheFileAsItWas() throws IOException {
        catalog.add(lamp("z1"));
        Devices removed = catalog.remove("z1");

        assertEquals("z1", removed.getId());
        assertFalse(registry.contains("z1"));
        assertEquals(START, Files.readString(devicesFile));
    }

    @Test
    void aDuplicateIdIsRejectedAndTheFileIsNotTouched() throws IOException {
        catalog.add(lamp("z1"));
        String before = Files.readString(devicesFile);

        assertThrows(IllegalArgumentException.class, () -> catalog.add(lamp("z1")));
        assertEquals(before, Files.readString(devicesFile));
    }

    @Test
    void aDeviceThatCannotBeSavedIsNotAddedToTheRegistry() {
        // A folder where a file should be: reading it fails, so the append fails.
        DeviceCatalog broken = new DeviceCatalog(registry, folder);

        assertThrows(HomeHubException.class, () -> broken.add(lamp("z1")));
        assertFalse(registry.contains("z1"));
    }

    @Test
    void aDeviceWhoseRowCannotBeRemovedStaysInTheRegistry() throws IOException {
        catalog.add(lamp("z1"));
        DeviceCatalog broken = new DeviceCatalog(registry, folder);

        assertThrows(HomeHubException.class, () -> broken.remove("z1"));
        assertTrue(registry.contains("z1"));
    }

    @Test
    void removingAnUnknownIdIsReported() {
        assertThrows(HomeHubException.class, () -> catalog.remove("nope"));
    }

    @Test
    void anInvalidRowIsRejectedBeforeAnythingChanges() throws IOException {
        String[] bad = lamp("z1");
        bad[6] = "lots";

        assertThrows(IllegalArgumentException.class, () -> catalog.add(bad));
        assertFalse(registry.contains("z1"));
        assertEquals(START, Files.readString(devicesFile));
    }
}
