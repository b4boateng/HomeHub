package com.homehub.loaders;

import com.homehub.devices.*;
import com.homehub.exceptions.*;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// The parts of DevicesLoader the console uses to add and remove devices, and the checks on the data.
class DevicesLoaderEditTest {

    private static final String HEADER = "type,id,name,brand,model,room,watts,online";

    @TempDir
    Path folder;

    private final DevicesLoader loader = new DevicesLoader();

    private static String[] row(String type, String id, String name, String watts) {
        return new String[] {type, id, name, "Brand", "Model", "Room", watts, "true"};
    }

    private Path file(String content) throws IOException {
        return Files.writeString(folder.resolve("devices.csv"), content);
    }

    // ---- create ----

    @Test
    void createBuildsTheRightTypeFromCells() {
        Devices bulb = loader.create(row("BULB", "b1", "Lamp", "9"));
        assertInstanceOf(SmartBulb.class, bulb);
        assertEquals("b1", bulb.getId());
        assertEquals(9, bulb.getRatedWatts());
        assertTrue(bulb.isOnline());
    }

    @Test
    void createRejectsAnUnknownType() {
        assertThrows(IllegalArgumentException.class, () -> loader.create(row("TOASTER", "x", "N", "9")));
    }

    @Test
    void createRejectsAnEmptyIdOrName() {
        assertThrows(IllegalArgumentException.class, () -> loader.create(row("BULB", "", "N", "9")));
        assertThrows(IllegalArgumentException.class, () -> loader.create(row("BULB", "x", "", "9")));
    }

    @Test
    void createRejectsWattsThatBreakThePowerLimit() {
        for (String bad : new String[] {"-5", "NaN", "Infinity", "1e400"}) {
            assertThrows(IllegalArgumentException.class, () -> loader.create(row("BULB", "x", "N", bad)), bad);
        }
    }

    @Test
    void createExplainsWattsThatAreNotANumber() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> loader.create(row("BULB", "x", "N", "abc")));
        assertTrue(e.getMessage().contains("'abc'"), e.getMessage());
        assertFalse(e.getMessage().contains("For input string"), e.getMessage());
    }

    @Test
    void zeroWattsIsAllowed() {
        assertEquals(0, loader.create(row("BULB", "x", "N", "0")).getRatedWatts());
    }

    @Test
    void loadAlsoRejectsBadWattsAndNamesTheLine() throws IOException {
        Path f = file(HEADER + "\nBULB,b1,Lamp,B,M,R,-5,true\n");
        DataLoadException e = assertThrows(DataLoadException.class, () -> loader.load(f));
        assertTrue(e.getMessage().contains("line 2"), e.getMessage());
    }

    @Test
    void loadRejectsAnEmptyId() throws IOException {
        Path f = file(HEADER + "\nBULB,,Lamp,B,M,R,5,true\n");
        assertThrows(DataLoadException.class, () -> loader.load(f));
    }

    // ---- append ----

    @Test
    void appendAddsOneRowAtTheEndAndTheFileStillLoads() throws IOException {
        Path f = file(HEADER + "\nBULB,b1,Lamp,B,M,R,5,true\n");
        loader.append(f, row("MOTION", "m1", "Sensor", "1"));

        assertEquals(HEADER + "\nBULB,b1,Lamp,B,M,R,5,true\nMOTION,m1,Sensor,Brand,Model,Room,1,true\n",
                Files.readString(f));
        assertEquals(2, loader.load(f).size());
    }

    @Test
    void appendStartsANewLineWhenTheFileDoesNotEndWithOne() throws IOException {
        Path f = file(HEADER + "\nBULB,b1,Lamp,B,M,R,5,true");   // no trailing newline
        loader.append(f, row("MOTION", "m1", "Sensor", "1"));
        assertEquals(2, loader.load(f).size());
    }

    @Test
    void appendUsesPlainNewlinesNotThePlatformSeparator() throws IOException {
        Path f = file(HEADER + "\n");
        loader.append(f, row("MOTION", "m1", "Sensor", "1"));
        assertFalse(Files.readString(f).contains("\r"));
    }

    @Test
    void appendRefusesACommaBecauseItWouldBreakTheRow() throws IOException {
        Path f = file(HEADER + "\n");
        assertThrows(IllegalArgumentException.class, () -> loader.append(f, row("BULB", "b1", "A,B", "5")));
        assertEquals(HEADER + "\n", Files.readString(f));
    }

    @Test
    void appendToAMissingFileFailsWithADataLoadException() {
        assertThrows(DataLoadException.class, () -> loader.append(folder.resolve("nope.csv"), row("BULB", "b1", "N", "5")));
    }

    // ---- removeRow ----

    @Test
    void removeRowDeletesOnlyThatDevicesRow() throws IOException {
        Path f = file(HEADER + "\nBULB,b1,One,B,M,R,5,true\nBULB,b2,Two,B,M,R,5,true\nBULB,b3,Three,B,M,R,5,true\n");
        loader.removeRow(f, "b2");

        assertEquals(HEADER + "\nBULB,b1,One,B,M,R,5,true\nBULB,b3,Three,B,M,R,5,true\n", Files.readString(f));
    }

    @Test
    void removeRowMatchesTheIdColumnOnlyNotOtherCells() throws IOException {
        // "b1" also appears as a name here; only the row whose id is b1 may go.
        Path f = file(HEADER + "\nBULB,b2,b1,B,M,R,5,true\nBULB,b1,Real,B,M,R,5,true\n");
        loader.removeRow(f, "b1");

        assertEquals(HEADER + "\nBULB,b2,b1,B,M,R,5,true\n", Files.readString(f));
    }

    @Test
    void removeRowKeepsTheHeaderEvenForAnIdCalledType() throws IOException {
        Path f = file(HEADER + "\nBULB,id,Odd,B,M,R,5,true\n");
        loader.removeRow(f, "id");   // the header's second cell is also "id"

        assertEquals(HEADER + "\n", Files.readString(f));
    }

    @Test
    void removeRowDoesNotIntroduceWindowsLineEndings() throws IOException {
        Path f = file(HEADER + "\nBULB,b1,One,B,M,R,5,true\nBULB,b2,Two,B,M,R,5,true\n");
        loader.removeRow(f, "b1");
        assertFalse(Files.readString(f).contains("\r"));
    }

    @Test
    void removeRowFailsAndLeavesTheFileAloneForAnUnknownId() throws IOException {
        String content = HEADER + "\nBULB,b1,One,B,M,R,5,true\n";
        Path f = file(content);

        assertThrows(DataLoadException.class, () -> loader.removeRow(f, "zzz"));
        assertEquals(content, Files.readString(f));
    }

    @Test
    void removeRowOnAMissingFileFailsWithADataLoadException() {
        assertThrows(DataLoadException.class, () -> loader.removeRow(folder.resolve("nope.csv"), "b1"));
    }
}
