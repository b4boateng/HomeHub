package com.homehub.app;

import com.homehub.automation.*;
import com.homehub.devices.*;
import com.homehub.energy.PowerManager;
import com.homehub.loaders.*;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

// Drives the console with typed input, exactly as a person would, against small data files in a temp folder.
// The timeout turns a console that never stops asking (for example at end of input) into a failure, not a hang.
@Timeout(10)
class ConsoleTest {

    private static final String HEADER = "type,id,name,brand,model,room,watts,online\n";
    private static final String DEVICES = HEADER
            + "BULB,b1,Hall Light,Philips,LB-100,Hallway,12,false\n"
            + "SPEAKER,s1,Kitchen Speaker,Sonos,One,Kitchen,15,true\n"
            + "LOCK,l1,Front Door,Yale,YL-5,Hallway,2,true\n"
            + "MOTION,m1,Hall Motion,Aqara,MS-1,Hallway,1,true\n";
    private static final String RULES = "eventType,source,minValue,windowStart,windowEnd,deviceId,action,priority\n"
            + "MOTION,m1,1,,,b1,TURN_ON,1\n"
            + "DOOR_UNLOCKED,l1,0,,,b1,TURN_ON,2\n"
            + "HUMIDITY,h1,80,,,s1,PLAY,1\n";
    private static final String EVENTS = "timestamp,type,source,value\n"
            + "2026-10-27T12:00,MOTION,m1,1\n"
            + "2026-10-27T14:00,DOOR_UNLOCKED,l1,0\n";

    @TempDir
    Path folder;

    private DeviceRegistry registry;
    private Path devicesFile;
    private Path fileConsoleWritesTo;   // normally devicesFile; a test can point it somewhere that fails

    @BeforeEach
    void setUp() throws IOException {
        devicesFile = folder.resolve("devices.csv");
        fileConsoleWritesTo = devicesFile;
        Files.writeString(devicesFile, DEVICES);
        Files.writeString(folder.resolve("rules.csv"), RULES);
        Files.writeString(folder.resolve("events.csv"), EVENTS);
    }

    // Runs one console session and returns everything it printed.
    private String session(String... lines) throws IOException {
        registry = new DeviceRegistry();
        for (Devices d : new DevicesLoader().load(devicesFile)) {
            registry.register(d);
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true);
        PowerManager power = new PowerManager(3000);
        EventBus bus = new EventBus();
        RuleEngine engine = new RuleEngine(registry, bus, power, out::println);
        for (Rule r : new RulesLoader().load(folder.resolve("rules.csv"))) {
            engine.addRule(r);
        }
        List<Event> events = new EventsLoader().load(folder.resolve("events.csv"));
        BufferedReader in = new BufferedReader(new StringReader(String.join("\n", lines) + (lines.length > 0 ? "\n" : "")));

        new Console(registry, engine, new DeviceControl(registry, power), bus, events, fileConsoleWritesTo, in, out).run();
        return bytes.toString().replace("\r\n", "\n");
    }

    private String fileText() throws IOException {
        return Files.readString(devicesFile);
    }

    // ---- menu and quitting ----

    @Test
    void zeroQuits() throws IOException {
        assertTrue(session("0").contains("Goodbye."));
    }

    @Test
    void theMenuListsEveryOption() throws IOException {
        String out = session("0");
        for (String item : new String[] {"1) List devices", "2) List rules", "3) Send a command",
                "4) Trigger an event", "5) Run the simulation", "6) Add a device", "7) Remove a device", "0) Quit"}) {
            assertTrue(out.contains(item), item);
        }
    }

    @Test
    void anythingThatIsNotAMenuNumberIsRefusedAndTheSessionContinues() throws IOException {
        String out = session("abc", "", "9", "-1", "0");
        assertEquals(4, out.split("Please choose a number from the menu.", -1).length - 1);
        assertTrue(out.contains("Goodbye."));
    }

    @Test
    void endOfInputQuitsCleanly() throws IOException {
        assertTrue(session().contains("Goodbye."));
    }

    @Test
    void endOfInputInTheMiddleOfAQuestionQuitsCleanlyInsteadOfCrashing() throws IOException {
        for (String[] partial : new String[][] {
                {"3", "b1"}, {"4"}, {"4", "HUMIDITY"}, {"6", "BULB", "x1"}, {"7"}}) {
            assertTrue(session(partial).contains("Goodbye."), String.join("|", partial));
        }
    }

    // ---- 1 and 2: listing ----

    @Test
    void optionOneListsTheDevices() throws IOException {
        String out = session("1", "0");
        assertTrue(out.contains("Hall Light") && out.contains("Kitchen Speaker") && out.contains("Front Door"), out);
    }

    @Test
    void optionTwoListsRulesWithTheirTriggerAndTargetName() throws IOException {
        String out = session("2", "0");
        assertTrue(out.contains("when MOTION from m1, value >= 1 -> TURN_ON b1 (Hall Light)"), out);
        assertTrue(out.contains("when DOOR_UNLOCKED from l1 -> TURN_ON b1 (Hall Light)"), out);
        assertTrue(out.contains("when HUMIDITY from h1, value >= 80 -> PLAY s1 (Kitchen Speaker)"), out);
    }

    // ---- 3: send a command ----

    @Test
    void aCommandShowsTheDevicesNewState() throws IOException {
        String out = session("3", "b1", "TURN_ON", "0");
        assertTrue(out.contains("OK - Hall Light: ON, brightness 50"), out);
        assertTrue(registry.get("b1").isOnline());
    }

    @Test
    void brightenAndDimShowTheNewBrightness() throws IOException {
        String out = session("3", "b1", "TURN_ON", "3", "b1", "BRIGHTEN", "30", "3", "b1", "DIM", "20", "0");
        assertTrue(out.contains("brightness 80"), out);
        assertTrue(out.contains("brightness 60"), out);
    }

    @Test
    void actionNamesAreNotCaseSensitive() throws IOException {
        assertTrue(session("3", "b1", "turn_on", "0").contains("OK - Hall Light: ON"));
    }

    @Test
    void aLockShowsLockedAndUnlocked() throws IOException {
        String out = session("3", "l1", "UNLOCK", "3", "l1", "LOCK", "0");
        assertTrue(out.contains("OK - Front Door: ON, unlocked"), out);
        assertTrue(out.contains("OK - Front Door: ON, locked"), out);
    }

    @Test
    void speakerPlaybackChangesAreShown() throws IOException {
        String out = session("3", "s1", "PLAY", "3", "s1", "PAUSE", "3", "s1", "RESUME", "3", "s1", "STOP", "0");
        assertTrue(out.contains("PLAYING") && out.contains("PAUSED") && out.contains("STOP"), out);
    }

    @Test
    void aCommandForAnUnknownDeviceIsRefusedWithAMessage() throws IOException {
        assertTrue(session("3", "zzz", "TURN_ON", "0").contains("Could not do that: No device with id: zzz"));
    }

    @Test
    void idsAreCaseSensitive() throws IOException {
        assertTrue(session("3", "B1", "TURN_ON", "0").contains("No device with id: B1"));
    }

    @Test
    void aSensorCannotBeCommanded() throws IOException {
        assertTrue(session("3", "m1", "TURN_ON", "0").contains("Hall Motion cannot TURN_ON"));
    }

    @Test
    void aDeviceCannotDoAnActionItDoesNotSupport() throws IOException {
        assertTrue(session("3", "l1", "DIM", "0").contains("Front Door cannot DIM"));
    }

    @Test
    void anOfflineDeviceRefusesEverythingExceptSwitchingOnOrOff() throws IOException {
        assertTrue(session("3", "b1", "BRIGHTEN", "10", "0").contains("Hall Light is offline"));
    }

    @Test
    void anUnknownActionListsTheValidOnesWithoutJavaInternals() throws IOException {
        String out = session("3", "b1", "FLY", "0");
        assertTrue(out.contains("'FLY' is not a valid action. Choose from ["), out);
        assertFalse(out.contains("No enum constant"), out);
    }

    @Test
    void anEmptyActionIsRefused() throws IOException {
        assertTrue(session("3", "b1", "", "0").contains("is not a valid action"));
    }

    @Test
    void theAmountMustBeAWholeNumber() throws IOException {
        String out = session("3", "b1", "TURN_ON", "3", "b1", "BRIGHTEN", "abc", "0");
        assertTrue(out.contains("'abc' is not a whole number."), out);
        assertFalse(out.contains("For input string"), out);
    }

    @Test
    void theAmountMustBeBetweenZeroAndOneHundred() throws IOException {
        for (String bad : new String[] {"-30", "101", "2147483647"}) {
            String out = session("3", "b1", "TURN_ON", "3", "b1", "BRIGHTEN", bad, "0");
            assertTrue(out.contains("The amount must be between 0 and 100."), bad + ": " + out);
            assertEquals(50, ((SmartBulb) registry.get("b1")).getBrightness(), bad);
        }
    }

    @Test
    void anAmountIsOnlyAskedForBrightenAndDim() throws IOException {
        assertFalse(session("3", "b1", "TURN_ON", "0").contains("Amount"));
    }

    @Test
    void turningOnADeviceThatWouldExceedThePowerLimitIsRefused() throws IOException {
        Files.writeString(devicesFile, DEVICES + "BULB,big,Floodlight,X,Y,Yard,99999,false\n");
        String out = session("3", "big", "TURN_ON", "0");
        assertTrue(out.contains("would exceed the limit"), out);
        assertFalse(registry.get("big").isOnline());
    }

    // ---- 4: trigger an event ----

    @Test
    void anEventThatMatchesARuleRunsItsCommand() throws IOException {
        String out = session("4", "MOTION", "m1", "1", "0");
        assertTrue(out.contains("Applied TURN_ON to Hall Light (priority 1, triggered by MOTION)"), out);
        assertTrue(registry.get("b1").isOnline());
    }

    @Test
    void doorEventsDoNotAskForAValue() throws IOException {
        String out = session("4", "DOOR_UNLOCKED", "l1", "0");
        assertFalse(out.contains("Value:"), out);
        assertTrue(out.contains("Applied TURN_ON to Hall Light (priority 2, triggered by DOOR_UNLOCKED)"), out);
    }

    @Test
    void doorLockedAlsoSkipsTheValue() throws IOException {
        assertFalse(session("4", "DOOR_LOCKED", "l1", "0").contains("Value:"));
    }

    @Test
    void otherEventsStillAskForAValue() throws IOException {
        for (String type : new String[] {"MOTION", "HUMIDITY", "TEMPERATURE"}) {
            assertTrue(session("4", type, "x1", "1", "0").contains("Value:"), type);
        }
    }

    @Test
    void aReadingBelowTheRulesMinimumDoesNotTrigger() throws IOException {
        String out = session("4", "HUMIDITY", "h1", "50", "0");
        assertTrue(out.contains("No rule matched"), out);
        assertFalse(out.contains("Applied PLAY"), out);
    }

    @Test
    void aReadingAtOrAboveTheMinimumTriggers() throws IOException {
        assertTrue(session("4", "HUMIDITY", "h1", "85", "0").contains("Applied PLAY to Kitchen Speaker"));
    }

    @Test
    void eventTypesAreNotCaseSensitive() throws IOException {
        assertTrue(session("4", "motion", "m1", "1", "0").contains("Applied TURN_ON"));
    }

    @Test
    void anEventNoRuleWantsIsReportedNotIgnored() throws IOException {
        assertTrue(session("4", "TEMPERATURE", "h1", "30", "0").contains("No rule matched"));
    }

    @Test
    void anUnknownEventTypeListsTheValidOnes() throws IOException {
        String out = session("4", "FOO", "0");
        assertTrue(out.contains("'FOO' is not a valid event type. Choose from ["), out);
        assertFalse(out.contains("No enum constant"), out);
    }

    @Test
    void theEventValueMustBeAWholeNumber() throws IOException {
        String out = session("4", "HUMIDITY", "h1", "abc", "0");
        assertTrue(out.contains("'abc' is not a whole number."), out);
    }

    // ---- 5: simulation ----

    @Test
    void theSimulationReplaysTheEventsAndReportsEnergy() throws IOException {
        String out = session("5", "0");
        assertTrue(out.contains("t=2026-10-27T12:00 MOTION from m1"), out);
        assertTrue(out.contains("t=2026-10-27T14:00 DOOR_UNLOCKED from l1"), out);
        assertTrue(out.contains("Applied TURN_ON to Hall Light"), out);
        assertTrue(out.contains("=== energy used (kWh) ==="), out);
    }

    @Test
    void theSimulationBillsOnlyWhileDevicesAreOn() throws IOException {
        // b1 (12 W) comes on at 12:00; the run ends one hour after the last event (15:00): 3 hours = 0.036 kWh.
        String out = session("5", "0");
        assertTrue(out.contains("b1   0.0360"), out);
    }

    @Test
    void withNoEventsTheSimulationSaysSo() throws IOException {
        Files.writeString(folder.resolve("events.csv"), "timestamp,type,source,value\n");
        assertTrue(session("5", "0").contains("No events to run."));
    }

    // ---- 6: add a device ----

    @Test
    void addingADeviceRegistersItSavesItAndShowsItsState() throws IOException {
        String out = session("6", "BULB", "b9", "Desk Lamp", "Ikea", "L-1", "Office", "9", "true", "0");

        assertTrue(out.contains("Added and saved - Desk Lamp: ON, brightness 50"), out);
        assertTrue(registry.contains("b9"));
        assertTrue(fileText().endsWith("BULB,b9,Desk Lamp,Ikea,L-1,Office,9,true\n"), fileText());
    }

    @Test
    void aSavedDeviceIsStillThereAfterARestart() throws IOException {
        session("6", "BULB", "b9", "Desk Lamp", "Ikea", "L-1", "Office", "9", "true", "0");
        session("0");   // a fresh session reloads the file
        assertTrue(registry.contains("b9"));
    }

    @Test
    void aNewDeviceCanBeCommandedStraightAway() throws IOException {
        String out = session("6", "BULB", "b9", "Desk Lamp", "Ikea", "L-1", "Office", "9", "false",
                "3", "b9", "TURN_ON", "0");
        assertTrue(out.contains("OK - Desk Lamp: ON"), out);
    }

    @Test
    void everyDeviceTypeCanBeAdded() throws IOException {
        String[][] rows = {
            {"TV", "t9"}, {"SPEAKER", "s9"}, {"LOCK", "l9"}, {"HUMIDITY", "h9"}, {"AIR", "a9"}, {"MOTION", "m9"}, {"BULB", "b9"}};
        for (String[] r : rows) {
            session("6", r[0], r[1], "Name", "Brand", "Model", "Room", "5", "true", "0");
            assertTrue(registry.contains(r[1]), r[0]);
        }
    }

    @Test
    void typeAndOnlineAreNotCaseSensitive() throws IOException {
        session("6", "bulb", "b9", "Lamp", "I", "M", "R", "9", "TRUE", "0");
        assertTrue(registry.contains("b9"));
        assertTrue(fileText().contains("BULB,b9,Lamp,I,M,R,9,true"), fileText());
    }

    @Test
    void aBadDeviceIsRefusedAndNeitherMemoryNorFileChange() throws IOException {
        String[][] bad = {
            {"BULB", "b1", "Lamp", "9", "true"},        // duplicate id
            {"TOASTER", "x1", "Lamp", "9", "true"},     // unknown type
            {"BULB", "x1", "Lamp", "-5", "true"},       // negative watts
            {"BULB", "x1", "Lamp", "NaN", "true"},      // not a real number
            {"BULB", "x1", "Lamp", "abc", "true"},      // not a number
            {"BULB", "", "Lamp", "9", "true"},          // empty id
            {"BULB", "x1", "", "9", "true"},            // empty name
            {"BULB", "x1", "A,B", "9", "true"},         // comma would break the row
            {"BULB", "x1", "Lamp", "9", "yes"},         // not true/false
        };
        for (String[] b : bad) {
            String out = session("6", b[0], b[1], b[2], "Brand", "Model", "Room", b[3], b[4], "0");
            assertTrue(out.contains("Could not do that:"), String.join("|", b) + "\n" + out);
            assertEquals(DEVICES, fileText(), String.join("|", b));
            assertFalse(registry.contains("x1"), String.join("|", b));
        }
    }

    @Test
    void aDuplicateIdKeepsTheOriginalDevice() throws IOException {
        session("6", "BULB", "b1", "Impostor", "I", "M", "R", "9", "true", "0");
        assertEquals("Hall Light", registry.get("b1").getName());
    }

    @Test
    void ifTheFileCannotBeWrittenTheDeviceIsNotAdded() throws IOException {
        fileConsoleWritesTo = folder;   // a folder cannot be appended to
        String out = session("6", "BULB", "b9", "Lamp", "I", "M", "R", "9", "true", "0");

        assertTrue(out.contains("Could not do that:"), out);
        assertFalse(registry.contains("b9"));
    }

    // ---- 7: remove a device ----

    @Test
    void removingADeviceForgetsItAndDeletesItsRow() throws IOException {
        String out = session("7", "b1", "0");

        assertTrue(out.contains("Removed and saved - Hall Light."), out);
        assertFalse(registry.contains("b1"));
        assertFalse(fileText().contains("b1,Hall Light"));
        session("0");   // a fresh session reloads from the file, so the row really is gone
        assertFalse(registry.contains("b1"));
        assertTrue(fileText().contains("s1,Kitchen Speaker"));
    }

    @Test
    void aRemovedDeviceCanNoLongerBeCommandedOrListed() throws IOException {
        String out = session("7", "b1", "3", "b1", "TURN_ON", "1", "0");
        assertTrue(out.contains("No device with id: b1"), out);
        assertFalse(out.contains("Hall Light         "), out);
    }

    @Test
    void removingAnUnknownDeviceChangesNothing() throws IOException {
        for (String id : new String[] {"zzz", "", "B1"}) {
            assertTrue(session("7", id, "0").contains("No device with id: " + id), id);
            assertEquals(DEVICES, fileText(), id);
        }
    }

    @Test
    void removingTheSameDeviceTwiceFailsTheSecondTime() throws IOException {
        String out = session("7", "b1", "7", "b1", "0");
        assertTrue(out.contains("Removed and saved - Hall Light."), out);
        assertTrue(out.contains("No device with id: b1"), out);
    }

    @Test
    void spacesAroundTheIdAreIgnored() throws IOException {
        assertTrue(session("7", "  b1  ", "0").contains("Removed and saved - Hall Light."));
    }

    @Test
    void aRemovedIdCanBeAddedAgain() throws IOException {
        session("7", "b1", "6", "BULB", "b1", "Hall Light", "Philips", "LB-100", "Hallway", "12", "false", "0");
        session("0");
        assertTrue(registry.contains("b1"));
    }

    @Test
    void rulesThatTargetARemovedDeviceAreFlaggedInTheRulesList() throws IOException {
        String out = session("7", "b1", "2", "0");
        assertTrue(out.contains("TURN_ON b1 (no such device)"), out);
        assertTrue(out.contains("PLAY s1 (Kitchen Speaker)"), out);
    }

    @Test
    void aRuleForARemovedDeviceIsSkippedNotCrashed() throws IOException {
        String out = session("7", "b1", "4", "MOTION", "m1", "1", "0");
        assertTrue(out.contains("Skipped: no device 'b1'"), out);
    }

    @Test
    void ifTheFileCannotBeUpdatedTheDeviceStays() throws IOException {
        fileConsoleWritesTo = folder;   // a folder cannot be rewritten as a file
        String out = session("7", "b1", "0");

        assertTrue(out.contains("Could not do that:"), out);
        assertTrue(registry.contains("b1"));
        assertEquals(DEVICES, fileText());
    }
}
