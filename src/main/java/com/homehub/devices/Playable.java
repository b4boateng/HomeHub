package com.homehub.devices;
// devices which can play any form of media
public interface Playable{

    public void play();
    public void stop();
    public void pause();
    public void resume();
    
    public enum PlaybackState{
        PLAYING,
        PAUSED,
        STOP
    }

    
}
