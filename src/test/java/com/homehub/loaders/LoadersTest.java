package com.homehub.loaders;

import com.homehub.automation.*;
import com.homehub.devices.*;
import com.homehub.exceptions.*;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LoadersTest {

    @TempDir
    Path folder;

    private Path write(String name, String... lines) throws IOException {
        return Files.write(folder.resolve(name), List.of(lines));
    }

    @Test
    void devicesLoaderBuildsTheRightTypes() throws IOException {
        Path file = write("devices.csv",
                "type,id,name,brand,model,room,watts,online",
                "BULB,b1,Hall Light,Philips,LB-100,Hallway,12,false",
                "TV,t1,TV,Samsung,QN90,Lounge,120,true");

        List<Devices> devices = new DevicesLoader().load(file);

        assertEquals(2, devices.size());
        assertInstanceOf(SmartBulb.class, devices.get(0));
        assertInstanceOf(SmartTv.class, devices.get(1));
        assertEquals(120, devices.get(1).getRatedWatts());
        assertEquals("Samsung", devices.get(1).getBrand());
    }

    @Test
    void eventsLoaderReadsTimestampTypeSourceAndValue() throws IOException {
        Path file = write("events.csv",
                "timestamp,type,source,value",
                "2026-10-27T23:30,MOTION,m1,1");

        Event event = new EventsLoader().load(file).get(0);

        assertEquals(LocalDateTime.of(2026, 10, 27, 23, 30), event.timestamp());
        assertEquals(EventType.MOTION, event.type());
        assertEquals("m1", event.sourceId());
    }

    @Test
    void rulesLoaderBuildsAConditionThatRespectsTheTimeWindow() throws IOException {
        Path file = write("rules.csv",
                "eventType,source,minValue,windowStart,windowEnd,deviceId,action,priority",
                "MOTION,m1,1,22:00,06:00,b1,TURN_ON,1");

        Rule rule = new RulesLoader().load(file).get(0);

        assertTrue(rule.matches(new Event(EventType.MOTION, LocalDateTime.of(2026, 10, 27, 23, 30), "m1", 1)));
        assertFalse(rule.matches(new Event(EventType.MOTION, LocalDateTime.of(2026, 10, 27, 12, 0), "m1", 1)));
    }

    @Test
    void blankLinesAreSkipped() throws IOException {
        Path file = write("events.csv",
                "timestamp,type,source,value",
                "",
                "2026-10-27T23:30,MOTION,m1,1");

        assertEquals(1, new EventsLoader().load(file).size());
    }

    @Test
    void corruptedCellNamesTheFileAndLine() throws IOException {
        Path file = write("devices.csv",
                "type,id,name,brand,model,room,watts,online",
                "BULB,b1,Hall Light,Philips,LB-100,Hallway,12,false",
                "BULB,b2,Lounge Light,Philips,LB-100,Living Room,forty,false");

        DataLoadException ex = assertThrows(DataLoadException.class, () -> new DevicesLoader().load(file));

        assertTrue(ex.getMessage().contains("devices.csv"), ex.getMessage());
        assertTrue(ex.getMessage().contains("line 3"), ex.getMessage());
    }

    @Test
    void wrongNumberOfColumnsIsReported() throws IOException {
        Path file = write("events.csv", "timestamp,type,source,value", "2026-10-27T23:30,MOTION,m1");

        DataLoadException ex = assertThrows(DataLoadException.class, () -> new EventsLoader().load(file));

        assertTrue(ex.getMessage().contains("line 2"), ex.getMessage());
    }

    @Test
    void unknownDeviceTypeIsReported() throws IOException {
        Path file = write("devices.csv", "type,id,name,brand,model,room,watts,online", "TOASTER,x1,Toaster,Acme,T-1,Kitchen,800,false");

        assertThrows(DataLoadException.class, () -> new DevicesLoader().load(file));
    }

    @Test
    void missingFileIsReported() {
        assertThrows(DataLoadException.class, () -> new DevicesLoader().load(folder.resolve("nope.csv")));
    }
}
