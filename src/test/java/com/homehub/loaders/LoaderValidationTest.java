package com.homehub.loaders;

import com.homehub.energy.EnergyRecord;
import com.homehub.exceptions.*;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// Input checks added to the loaders: bad rows must stop with a message that names the file and line.
class LoaderValidationTest {

    private static final String RULE_HEADER = "eventType,source,minValue,windowStart,windowEnd,deviceId,action,priority";
    private static final String DEVICE_HEADER = "type,id,name,brand,model,room,watts,online";

    @TempDir
    Path folder;

    private Path write(String name, String... lines) throws IOException {
        return Files.write(folder.resolve(name), List.of(lines));
    }

    private static void assertRejectedAtLine2(Runnable load, String expectedPart) {
        DataLoadException e = assertThrows(DataLoadException.class, load::run);
        assertTrue(e.getMessage().contains("line 2"), e.getMessage());
        assertTrue(e.getMessage().contains(expectedPart), e.getMessage());
    }

    // ---- CsvLoader ----

    @Test
    void aLoaderNeedsAtLeastOneColumn() {
        assertThrows(IllegalArgumentException.class, () -> new CsvLoader<String>(0) {
            @Override
            protected String parseRow(String[] cells) {
                return cells[0];
            }
        });
        assertThrows(IllegalArgumentException.class, () -> new CsvLoader<String>(-3) {
            @Override
            protected String parseRow(String[] cells) {
                return cells[0];
            }
        });
    }

    @Test
    void aOneColumnLoaderIsFine() throws IOException {
        CsvLoader<String> loader = new CsvLoader<>(1) {
            @Override
            protected String parseRow(String[] cells) {
                return cells[0];
            }
        };
        assertEquals(List.of("a", "b"), loader.load(write("one.csv", "header", "a", "b")));
    }

    // ---- DevicesLoader ----

    @Test
    void createNeedsExactlyEightFields() {
        DevicesLoader loader = new DevicesLoader();
        assertThrows(IllegalArgumentException.class, () -> loader.create(null));
        assertThrows(IllegalArgumentException.class, () -> loader.create(new String[0]));
        assertThrows(IllegalArgumentException.class,
                () -> loader.create(new String[] {"BULB", "b1", "Lamp", "B", "M", "R", "5"}));            // 7
        assertThrows(IllegalArgumentException.class,
                () -> loader.create(new String[] {"BULB", "b1", "Lamp", "B", "M", "R", "5", "true", "x"})); // 9
    }

    @Test
    void theFieldCountMessageSaysHowManyAreNeeded() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new DevicesLoader().create(new String[] {"BULB"}));
        assertTrue(e.getMessage().contains("8"), e.getMessage());
    }

    @Test
    void appendRefusesTheWrongNumberOfFieldsAndLeavesTheFileAlone() throws IOException {
        Path file = write("devices.csv", DEVICE_HEADER);
        String before = Files.readString(file);

        assertThrows(IllegalArgumentException.class, () -> new DevicesLoader().append(file, new String[] {"BULB", "b1"}));
        assertThrows(IllegalArgumentException.class, () -> new DevicesLoader().append(file, null));
        assertEquals(before, Files.readString(file));
    }

    @Test
    void aFullRowStillWorks() {
        assertEquals("b1", new DevicesLoader().create(
                new String[] {"BULB", "b1", "Lamp", "B", "M", "R", "5", "true"}).getId());
    }

    // ---- EventsLoader ----

    @Test
    void anEventNeedsASource() throws IOException {
        Path blank = write("events1.csv", "timestamp,type,source,value", "2026-10-27T12:00,MOTION,,1");
        Path spaces = write("events2.csv", "timestamp,type,source,value", "2026-10-27T12:00,MOTION,   ,1");

        assertRejectedAtLine2(() -> new EventsLoader().load(blank), "source");
        assertRejectedAtLine2(() -> new EventsLoader().load(spaces), "source");
    }

    @Test
    void anEventWithASourceStillLoads() throws IOException {
        Path ok = write("events.csv", "timestamp,type,source,value", "2026-10-27T12:00,MOTION,m1,1");
        assertEquals("m1", new EventsLoader().load(ok).get(0).sourceId());
    }

    // ---- RulesLoader: the time window ----

    @Test
    void aRuleWithOnlyAWindowStartIsRefused() throws IOException {
        Path file = write("r1.csv", RULE_HEADER, "MOTION,m1,1,22:00,,b1,TURN_ON,1");
        assertRejectedAtLine2(() -> new RulesLoader().load(file), "Both window start and end must be provided");
    }

    @Test
    void aRuleWithOnlyAWindowEndIsRefused() throws IOException {
        Path file = write("r2.csv", RULE_HEADER, "MOTION,m1,1,,06:00,b1,TURN_ON,1");
        assertRejectedAtLine2(() -> new RulesLoader().load(file), "Both window start and end must be provided");
    }

    @Test
    void aRuleWithNoWindowOrAFullWindowStillLoads() throws IOException {
        Path file = write("r3.csv", RULE_HEADER,
                "MOTION,m1,1,,,b1,TURN_ON,1",
                "MOTION,m1,1,22:00,06:00,b2,TURN_ON,1");
        assertEquals(2, new RulesLoader().load(file).size());
    }

    // ---- EnergyLoader ----

    @Test
    void energyThatIsNotAFiniteNonNegativeNumberIsRefused() throws IOException {
        for (String bad : new String[] {"NaN", "Infinity", "-Infinity", "-0.5", "1e999"}) {
            Path file = write("energy.csv", "hour,deviceId,kwh", "2026-10-27T12:00,b1," + bad);
            assertRejectedAtLine2(() -> new EnergyLoader().load(file), "kwh");
        }
    }

    @Test
    void zeroAndOrdinaryEnergyStillLoad() throws IOException {
        Path file = write("energy.csv", "hour,deviceId,kwh",
                "2026-10-27T12:00,b1,0",
                "2026-10-27T13:00,b1,0.012345");
        List<EnergyRecord> records = new EnergyLoader().load(file);
        assertEquals(0, records.get(0).kwh());
        assertEquals(0.012345, records.get(1).kwh(), 1e-9);
    }
}
