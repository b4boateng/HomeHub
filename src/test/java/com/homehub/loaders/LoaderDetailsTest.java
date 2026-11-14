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

class LoaderDetailsTest {

    @TempDir
    Path folder;

    private Path write(String name, String... lines) throws IOException {
        return Files.write(folder.resolve(name), List.of(lines));
    }

    private static final String DEVICE_HEADER = "type,id,name,brand,model,room,watts,online";
    private static final String RULE_HEADER = "eventType,source,minValue,windowStart,windowEnd,deviceId,action,priority";

    private Event motion(String source, int value, int hour) {
        return new Event(EventType.MOTION, LocalDateTime.of(2026, 10, 27, hour, 0), source, value);
    }

    @Test
    void everyDeviceTypeBecomesTheRightClass() throws IOException {
        Path file = write("devices.csv", DEVICE_HEADER,
                "BULB,1,n,b,m,r,10,true",
                "TV,2,n,b,m,r,10,true",
                "SPEAKER,3,n,b,m,r,10,true",
                "LOCK,4,n,b,m,r,10,true",
                "HUMIDITY,5,n,b,m,r,10,true",
                "AIR,6,n,b,m,r,10,true",
                "MOTION,7,n,b,m,r,1,true");

        List<Devices> d = new DevicesLoader().load(file);
        assertInstanceOf(MotionSensor.class, d.get(6));

        assertInstanceOf(SmartBulb.class, d.get(0));
        assertInstanceOf(SmartTv.class, d.get(1));
        assertInstanceOf(SmartSpeaker.class, d.get(2));
        assertInstanceOf(SmartLock.class, d.get(3));
        assertInstanceOf(HumiditySensor.class, d.get(4));
        assertInstanceOf(AirQualityMonitor.class, d.get(5));
    }

    @Test
    void identityFieldsComeFromTheFile() throws IOException {
        Path file = write("devices.csv", DEVICE_HEADER, "LOCK,l1,Front Door,Yale,YL-5,Hallway,2,true");
        Devices lock = new DevicesLoader().load(file).get(0);
        assertEquals("l1", lock.getId());
        assertEquals("Front Door", lock.getName());
        assertEquals("Yale", lock.getBrand());
        assertEquals("YL-5", lock.getModelNumber());
        assertEquals("Hallway", lock.getRoom());
        assertEquals(2, lock.getRatedWatts());
        assertTrue(lock.isOnline());
    }

    @Test
    void spacesAroundCellsAreIgnored() throws IOException {
        Path file = write("devices.csv", DEVICE_HEADER, "BULB , b1 , Hall , Philips , LB , Hall , 12 , false");
        assertEquals("b1", new DevicesLoader().load(file).get(0).getId());
    }

    @Test
    void aFileWithOnlyAHeaderLoadsNothing() throws IOException {
        assertTrue(new DevicesLoader().load(write("devices.csv", DEVICE_HEADER)).isEmpty());
    }

    @Test
    void onlineMustBeExactlyTrueOrFalse() throws IOException {
        Path file = write("devices.csv", DEVICE_HEADER, "BULB,b1,Hall,Philips,LB,Hall,12,yes");
        DataLoadException ex = assertThrows(DataLoadException.class, () -> new DevicesLoader().load(file));
        assertTrue(ex.getMessage().contains("line 2"), ex.getMessage());
    }

    @Test
    void ruleWithNoWindowMatchesAtAnyHour() throws IOException {
        Rule rule = new RulesLoader().load(write("rules.csv", RULE_HEADER, "MOTION,m1,1,,,b1,TURN_ON,1")).get(0);
        assertTrue(rule.matches(motion("m1", 1, 3)));
        assertTrue(rule.matches(motion("m1", 1, 15)));
    }

    @Test
    void ruleNeedsTheValueToReachMinValue() throws IOException {
        Rule rule = new RulesLoader().load(write("rules.csv", RULE_HEADER, "MOTION,m1,5,,,b1,TURN_ON,1")).get(0);
        assertFalse(rule.matches(motion("m1", 4, 12)));
        assertTrue(rule.matches(motion("m1", 5, 12)));
    }

    @Test
    void ruleOnlyMatchesItsOwnSourceAndEventType() throws IOException {
        Rule rule = new RulesLoader().load(write("rules.csv", RULE_HEADER, "MOTION,m1,1,,,b1,TURN_ON,1")).get(0);
        assertFalse(rule.matches(motion("m2", 1, 12)));
        assertFalse(rule.matches(new Event(EventType.HUMIDITY, LocalDateTime.of(2026, 10, 27, 12, 0), "m1", 1)));
    }

    @Test
    void ruleKeepsTargetActionAndPriority() throws IOException {
        Rule rule = new RulesLoader().load(write("rules.csv", RULE_HEADER, "MOTION,m1,1,,,b1,DIM,4")).get(0);
        assertEquals("b1", rule.getTargetDeviceId());
        assertEquals(Action.DIM, rule.getCommand().action());
        assertEquals(4, rule.getPriority());
    }

    @Test
    void unknownActionBadTimeAndHalfAWindowAreAllReported() throws IOException {
        Path badAction = write("a.csv", RULE_HEADER, "MOTION,m1,1,,,b1,EXPLODE,1");
        Path badTime = write("b.csv", RULE_HEADER, "MOTION,m1,1,25:99,06:00,b1,TURN_ON,1");
        Path halfWindow = write("c.csv", RULE_HEADER, "MOTION,m1,1,22:00,,b1,TURN_ON,1");

        assertThrows(DataLoadException.class, () -> new RulesLoader().load(badAction));
        assertThrows(DataLoadException.class, () -> new RulesLoader().load(badTime));
        assertThrows(DataLoadException.class, () -> new RulesLoader().load(halfWindow));
    }

    @Test
    void eventsKeepTheOrderOfTheFile() throws IOException {
        Path file = write("events.csv", "timestamp,type,source,value",
                "2026-10-27T23:00,MOTION,m1,1",
                "2026-10-27T12:00,HUMIDITY,h1,80");
        List<Event> events = new EventsLoader().load(file);
        assertEquals(EventType.MOTION, events.get(0).type());
        assertEquals(EventType.HUMIDITY, events.get(1).type());
    }

    @Test
    void badTimestampOrUnknownTypeIsReportedWithItsLine() throws IOException {
        Path badTime = write("a.csv", "timestamp,type,source,value", "yesterday,MOTION,m1,1");
        Path badType = write("b.csv", "timestamp,type,source,value", "2026-10-27T23:00,EARTHQUAKE,m1,1");

        assertTrue(assertThrows(DataLoadException.class, () -> new EventsLoader().load(badTime)).getMessage().contains("a.csv line 2"));
        assertThrows(DataLoadException.class, () -> new EventsLoader().load(badType));
    }
}
