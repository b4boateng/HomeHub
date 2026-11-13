package com.homehub;

import static org.junit.jupiter.api.Assertions.*;

import com.homehub.Playable.PlaybackState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DevicesTest {

    // A fresh, switched-on device of each kind before every test.
    private SmartBulb bulb;
    private SmartTv tv;
    private SmartSpeaker speaker;
    private SmartLock lock;
    private AirQualityMonitor air;

    @BeforeEach
    void setUp() {
        bulb = new SmartBulb(50, 128, "b1", "LB", "Bulb", "Philips", "Hall", true, 60, "");
        tv = new SmartTv("t1", "TV", "Samsung", true, "Lounge", "QN", 120, 20, false, "Netflix", 5);
        speaker = new SmartSpeaker("s1", "Speaker", "Sonos", true, "Kitchen", "One", 15, 30, false, "Radio");
        lock = new SmartLock("l1", "Door", "Yale", true, "Hall", "YL", 2, true, "1234", 90);
        air = new AirQualityMonitor("a1", "Air", "Awair", true, "Bedroom", "AW-2", 21, 0, 400, 21, 5, 3);
    }

    // ----- bulb -----

    @Test
    void bulbBrightenAndDimChangeBrightness() {                     // normal
        bulb.execute(new Command(Action.BRIGHTEN, 30));
        assertEquals(80, bulb.getBrightness());
        bulb.execute(new Command(Action.DIM, 10));
        assertEquals(70, bulb.getBrightness());
    }

    @Test
    void bulbBrightnessIsClampedBetween0And100() {                  // edge
        bulb.execute(new Command(Action.BRIGHTEN, 500));
        assertEquals(100, bulb.getBrightness());
        bulb.execute(new Command(Action.DIM, 500));
        assertEquals(0, bulb.getBrightness());
    }

    @Test
    void bulbDrawsRatedWattsOnlyWhenOn() {                          // normal + edge
        assertEquals(60, bulb.currentPowerWatts());
        bulb.setOnline(false);
        assertEquals(0, bulb.currentPowerWatts());
    }

    @Test
    void bulbRejectsActionsItDoesNotSupport() {                     // invalid
        assertFalse(bulb.supports(Action.LOCK));
        assertThrows(IllegalStateException.class, () -> bulb.execute(new Command(Action.LOCK)));
    }

    // ----- lock -----

    @Test
    void lockLocksAndUnlocks() {                                    // normal
        lock.execute(new Command(Action.UNLOCK));
        assertFalse(lock.getLockStatus());
        lock.execute(new Command(Action.LOCK));
        assertTrue(lock.getLockStatus());
    }

    @Test
    void lockRejectsActionsItDoesNotSupport() {                     // invalid
        assertFalse(lock.supports(Action.TURN_ON));
        assertThrows(IllegalStateException.class, () -> lock.execute(new Command(Action.TURN_ON)));
    }

    // ----- tv -----

    @Test
    void tvPlayPauseResumeAndTurnOffMoveThroughStates() {           // normal
        tv.execute(new Command(Action.PLAY));
        assertEquals(PlaybackState.PLAYING, tv.getPlaybackState());
        tv.execute(new Command(Action.PAUSE));
        assertEquals(PlaybackState.PAUSED, tv.getPlaybackState());
        tv.execute(new Command(Action.RESUME));
        assertEquals(PlaybackState.PLAYING, tv.getPlaybackState());
        tv.execute(new Command(Action.TURN_OFF));
        assertEquals(PlaybackState.STOP, tv.getPlaybackState());
        assertFalse(tv.isOnline());
    }

    @Test
    void tvVolumeAndChannelAreClamped() {                           // edge
        tv.setVolume(150);
        assertEquals(100, tv.getVolume());
        tv.setVolume(-5);
        assertEquals(0, tv.getVolume());
        tv.setChannel(-3);
        assertEquals(0, tv.getChannel());
    }

    @Test
    void tvPauseWhileStoppedStaysStopped() {                        // edge
        tv.pause();
        assertEquals(PlaybackState.STOP, tv.getPlaybackState());
    }

    @Test
    void tvCannotPlayWhenOffline() {                                // invalid
        tv.setOnline(false);
        assertThrows(DeviceOfflineException.class, tv::play);
    }

    // ----- speaker -----

    @Test
    void speakerPlaysAndStops() {                                   // normal
        speaker.execute(new Command(Action.PLAY));
        assertEquals(PlaybackState.PLAYING, speaker.getPlaybackState());
        speaker.execute(new Command(Action.STOP));
        assertEquals(PlaybackState.STOP, speaker.getPlaybackState());
    }

    @Test
    void speakerVolumeIsClamped() {                                 // edge
        speaker.setVolume(-10);
        assertEquals(0, speaker.getVolume());
        speaker.setVolume(250);
        assertEquals(100, speaker.getVolume());
    }

    @Test
    void speakerCannotPlayWhenOffline() {                           // invalid
        speaker.setOnline(false);
        assertThrows(DeviceOfflineException.class, speaker::play);
    }

    // ----- humidity sensor -----

    @Test
    void humiditySensorReadingStaysWithin10And100Inclusive() {      // normal + edge
        HumiditySensor h = new HumiditySensor("h1", "HS", "Humidity", "Aqara", true, "Bath", 1, 40);
        for (int i = 0; i < 1000; i++) {
            h.readValue();
            assertTrue(h.getHumidity() >= 10 && h.getHumidity() <= 100);
        }
    }

    // ----- air quality monitor -----

    @Test
    void airMonitorStartsWithTheValuesItWasGiven() {                // normal
        assertEquals(21, air.getOxygenLevel());
        assertEquals(0, air.getCarbonMonoxideLevel());
        assertEquals(400, air.getCarbonDioxideLevel());
        assertEquals(21, air.getTemperature());
        assertEquals(5, air.getPm25());
    }

    @Test
    void airMonitorReadingsStayWithinMinAndMaxInclusive() {         // normal + edge
        for (int i = 0; i < 1000; i++) {      // random, so repeat to catch an off-by-one at the edges
            air.readValue();
            assertTrue(air.getCarbonDioxideLevel() >= 1000 && air.getCarbonDioxideLevel() <= 2000);
            assertTrue(air.getCarbonMonoxideLevel() >= 9 && air.getCarbonMonoxideLevel() <= 34);
            assertTrue(air.getTemperature() >= 10 && air.getTemperature() <= 29);
        }
    }

    @Test
    void airMonitorReadingLeavesOxygenAndPm25Alone() {              // edge: these two have no range yet
        air.readValue();
        assertEquals(21, air.getOxygenLevel());
        assertEquals(5, air.getPm25());
    }
}
