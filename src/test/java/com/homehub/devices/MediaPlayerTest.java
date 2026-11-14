package com.homehub.devices;

import com.homehub.devices.Playable.PlaybackState;
import com.homehub.exceptions.*;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

// One class covers TVs and speakers, so these are the playback rules for both.
class MediaPlayerTest {

    private static MediaPlayer player(boolean online) {
        return new MediaPlayer("t1", "Living TV", "Samsung", online, "Living", "QN90", 120, 20, false);
    }

    @Test
    void startsStopped() {
        assertEquals(PlaybackState.STOP, player(true).getPlaybackState());
    }

    @Test
    void playMovesToPlaying() {
        MediaPlayer p = player(true);
        p.play();
        assertEquals(PlaybackState.PLAYING, p.getPlaybackState());
    }

    @Test
    void playWhileOfflineThrowsNamesTheDeviceAndStaysStopped() {
        MediaPlayer p = player(false);
        HomeHubException e = assertThrows(HomeHubException.class, p::play);
        assertEquals("Living TV is offline", e.getMessage());
        assertEquals(PlaybackState.STOP, p.getPlaybackState());
    }

    @Test
    void pauseOnlyWorksWhilePlaying() {
        MediaPlayer p = player(true);
        p.pause();                                          // stopped: nothing happens
        assertEquals(PlaybackState.STOP, p.getPlaybackState());

        p.play();
        p.pause();
        assertEquals(PlaybackState.PAUSED, p.getPlaybackState());

        p.pause();                                          // already paused: still paused
        assertEquals(PlaybackState.PAUSED, p.getPlaybackState());
    }

    @Test
    void resumeOnlyWorksWhilePaused() {
        MediaPlayer p = player(true);
        p.resume();                                         // stopped: nothing happens
        assertEquals(PlaybackState.STOP, p.getPlaybackState());

        p.play();
        p.resume();                                         // already playing: still playing
        assertEquals(PlaybackState.PLAYING, p.getPlaybackState());

        p.pause();
        p.resume();
        assertEquals(PlaybackState.PLAYING, p.getPlaybackState());
    }

    @Test
    void stopAlwaysEndsInStop() {
        MediaPlayer p = player(true);
        p.play();
        p.stop();
        assertEquals(PlaybackState.STOP, p.getPlaybackState());
        p.pause();
        assertEquals(PlaybackState.STOP, p.getPlaybackState());
    }

    @Test
    void turnOffStopsPlaybackAndTakesTheDeviceOffline() {
        MediaPlayer p = player(true);
        p.play();
        p.execute(new Command(Action.TURN_OFF));

        assertEquals(PlaybackState.STOP, p.getPlaybackState());
        assertFalse(p.isOnline());
    }

    @Test
    void turnOnBringsItOnline() {
        MediaPlayer p = player(false);
        p.execute(new Command(Action.TURN_ON));
        assertTrue(p.isOnline());
    }

    @Test
    void supportsPowerAndPlaybackActionsOnly() {
        MediaPlayer p = player(true);
        for (Action a : Action.values()) {
            boolean expected = List.of(Action.TURN_ON, Action.TURN_OFF, Action.PLAY, Action.STOP, Action.PAUSE, Action.RESUME).contains(a);
            assertEquals(expected, p.supports(a), a.toString());
        }
    }

    @Test
    void itDrawsPowerOnlyWhileOn() {
        MediaPlayer p = player(true);
        assertEquals(120, p.currentPowerWatts());
        p.setOnline(false);
        assertEquals(0, p.currentPowerWatts());
    }

    @Test
    void mutingAndVolumeWorkAndVolumeIsClamped() {
        MediaPlayer p = player(true);
        assertFalse(p.isMuted());
        p.setMuted(true);
        assertTrue(p.isMuted());

        p.setVolume(250);
        assertEquals(100, p.getVolume());
        p.setVolume(-5);
        assertEquals(0, p.getVolume());
    }

    // ---- the playback states, and the rule that a device's id never changes ----

}
