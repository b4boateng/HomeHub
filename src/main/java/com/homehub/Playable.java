package com.homehub;

public interface Playable{

    public void play();
    public void stop();
    public void pause();
    public void resume();
    
    public enum PlaybackState{
        PLAYING, PAUSED, STOP, RESUME,
    }

    
}
