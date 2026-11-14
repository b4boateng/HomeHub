package com.homehub.devices;

import com.homehub.exceptions.*;

import static org.junit.jupiter.api.Assertions.*;

import com.homehub.devices.Playable.PlaybackState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DevicesTest {

    private SmartBulb bulb;
    private MediaPlayer player;
    private SmartLock lock;
    private MotionSensor sensor;

    @BeforeEach
    void setUp() {
        bulb = new SmartBulb(50, 128, "b1", "LB", "Bulb", "Philips", "Hall", true, 60, "");
        player = new MediaPlayer("t1", "TV", "Samsung", true, "Lounge", "QN", 120, 20, false);
        lock = new SmartLock("l1", "Door", "Yale", true, "Hall", "YL", 2, true, "1234", 90);
        sensor = new MotionSensor("m1", "Hall Motion", "Aqara", true, "Hall", "MS-1", 1);
    }

    @Test
    void bulbBrightenAndDimChangeBrightness() {
        bulb.execute(new Command(Action.BRIGHTEN, 30));
        assertEquals(80, bulb.getBrightness());
        bulb.execute(new Command(Action.DIM, 10));
        assertEquals(70, bulb.getBrightness());
    }

    @Test
    void bulbDrawsRatedWattsOnlyWhenOn() {
        assertEquals(60, bulb.currentPowerWatts());
        bulb.setOnline(false);
        assertEquals(0, bulb.currentPowerWatts());
    }

    @Test
    void bulbRejectsActionsItDoesNotSupport() {
        assertFalse(bulb.supports(Action.LOCK));
        assertThrows(IllegalStateException.class, () -> bulb.execute(new Command(Action.LOCK)));
    }

    @Test
    void lockLocksAndUnlocks() {
        lock.execute(new Command(Action.UNLOCK));
        assertFalse(lock.getLockStatus());
        lock.execute(new Command(Action.LOCK));
        assertTrue(lock.getLockStatus());
    }

    @Test
    void lockRejectsActionsItDoesNotSupport() {
        assertFalse(lock.supports(Action.TURN_ON));
        assertThrows(IllegalStateException.class, () -> lock.execute(new Command(Action.TURN_ON)));
    }

    @Test
    void playerPlayPauseResumeAndTurnOffMoveThroughStates() {
        player.execute(new Command(Action.PLAY));
        assertEquals(PlaybackState.PLAYING, player.getPlaybackState());
        player.execute(new Command(Action.PAUSE));
        assertEquals(PlaybackState.PAUSED, player.getPlaybackState());
        player.execute(new Command(Action.RESUME));
        assertEquals(PlaybackState.PLAYING, player.getPlaybackState());
        player.execute(new Command(Action.TURN_OFF));
        assertEquals(PlaybackState.STOP, player.getPlaybackState());
        assertFalse(player.isOnline());
    }

    @Test
    void playerPlaysAndStops() {
        player.execute(new Command(Action.PLAY));
        assertEquals(PlaybackState.PLAYING, player.getPlaybackState());
        player.execute(new Command(Action.STOP));
        assertEquals(PlaybackState.STOP, player.getPlaybackState());
    }

    @Test
    void playerRejectsActionsItDoesNotSupport() {
        assertFalse(player.supports(Action.BRIGHTEN));
        assertThrows(IllegalStateException.class, () -> player.execute(new Command(Action.BRIGHTEN, 5)));
    }

    @Test
    void sensorStartsWithNoMotion() {
        assertFalse(sensor.isMotionDetected());
    }

    @Test
    void sensorReadingsSometimesDetectMotionAndSometimesNot() {
        boolean sawMotion = false;
        boolean sawNone = false;
        for (int i = 0; i < 1000; i++) {
            sensor.readValue();
            sawMotion |= sensor.isMotionDetected();
            sawNone |= !sensor.isMotionDetected();
        }
        assertTrue(sawMotion && sawNone);
    }

    @Test
    void sensorCanBeToldMotionWasDetected() {
        sensor.setMotionDetected(true);
        assertTrue(sensor.isMotionDetected());
    }

}
