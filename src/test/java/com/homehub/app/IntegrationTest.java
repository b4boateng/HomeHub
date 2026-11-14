package com.homehub.app;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// Starts the real Application on a copy of the real data files, the way a user would.
class IntegrationTest {

    private static final Path RESOURCES = Path.of("src/main/resources");

    @TempDir
    Path folder;

    private void copyData() throws IOException {
        for (String name : new String[] {"devices.csv", "rules.csv", "events.csv"}) {
            Files.copy(RESOURCES.resolve(name), folder.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String runApplication(String... lines) {
        InputStream realIn = System.in;
        PrintStream realOut = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream((String.join("\n", lines) + "\n").getBytes()));
            System.setOut(new PrintStream(bytes, true));
            Application.main(new String[] {folder.toString()});
        } finally {
            System.setIn(realIn);
            System.setOut(realOut);
        }
        return bytes.toString().replace("\r\n", "\n");
    }

    @Test
    void theRealDataFilesLoadAndTheMenuAppears() throws IOException {
        copyData();
        String out = runApplication("0");
        assertTrue(out.contains("1) List devices"), out);
        assertTrue(out.contains("Goodbye."), out);
        assertFalse(out.contains("Could not start"), out);
    }

    @Test
    void aFullSessionAddsCommandsTriggersSimulatesAndRemoves() throws IOException {
        copyData();
        String before = Files.readString(folder.resolve("devices.csv"));

        String out = runApplication(
                "6", "BULB", "z1", "Test Lamp", "Acme", "T-1", "Lab", "20", "false",   // add
                "3", "z1", "TURN_ON",                                                  // command it
                "3", "z1", "BRIGHTEN", "25",
                "4", "DOOR_UNLOCKED", "l1",                                            // event, no value asked
                "5",                                                                   // simulation
                "2",                                                                   // rules
                "7", "z1",                                                             // remove it again
                "0");

        assertTrue(out.contains("Added and saved - Test Lamp: OFF"), out);
        assertTrue(out.contains("OK - Test Lamp: ON, brightness 50"), out);
        assertTrue(out.contains("OK - Test Lamp: ON, brightness 75"), out);
        assertTrue(out.contains("triggered by DOOR_UNLOCKED"), out);
        assertTrue(out.contains("=== energy used (kWh) ==="), out);
        assertTrue(out.contains("when MOTION from m1"), out);
        assertTrue(out.contains("Removed and saved - Test Lamp."), out);
        assertFalse(out.contains("Could not do that"), out);

        // Added then removed: the file is exactly as it started.
        assertEquals(before, Files.readString(folder.resolve("devices.csv")));
    }

    @Test
    void addedDevicesSurviveARestart() throws IOException {
        copyData();
        runApplication("6", "MOTION", "z9", "Porch Motion", "Aqara", "MS-1", "Porch", "1", "true", "0");

        String second = runApplication("1", "0");
        assertTrue(second.contains("z9") && second.contains("Porch Motion"), second);
    }

    @Test
    void removedDevicesStayRemovedAfterARestart() throws IOException {
        copyData();
        runApplication("7", "b1", "0");

        String second = runApplication("1", "0");
        assertFalse(second.contains("Hall Light"), second);
    }

    @Test
    void aMissingDataFileStopsStartupWithAMessage() throws IOException {
        copyData();
        Files.delete(folder.resolve("rules.csv"));

        String out = runApplication("0");
        assertTrue(out.contains("Could not start:"), out);
    }

    @Test
    void aRulesRowUsingARemovedActionStopsStartupWithAMessage() throws IOException {
        copyData();
        Files.writeString(folder.resolve("rules.csv"),
                "eventType,source,minValue,windowStart,windowEnd,deviceId,action,priority\n"
                + "MOTION,m1,1,,,b1,NOTIFY,1\n");

        String out = runApplication("0");
        assertTrue(out.contains("Could not start:") && out.contains("line 2"), out);
    }

    @Test
    void theBundledDataFilesAreValid() {
        // Guards the shipped data: bad watts, empty ids or unknown actions would stop the app from starting.
        assertDoesNotThrow(() -> {
            copyData();
            String out = runApplication("0");
            assertFalse(out.contains("Could not start"), out);
        });
    }
}
