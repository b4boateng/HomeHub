package com.homehub;

public class SmartSpeaker extends Devices implements Controllable, EnergyReporting, Playable{

    private int volume;
    private boolean isMuted;
    private String media;
    private PlaybackState playbackState = PlaybackState.STOP;

    public SmartSpeaker(String id, String name, String brand, boolean onlineStatus, String room, String modelNumber, double ratedWatts, int volume, boolean isMuted, String media){
        super(id, name, brand, onlineStatus, room, modelNumber, ratedWatts);
       this.volume = volume;
        this.isMuted = isMuted;
        this.media = media;


    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
       this.volume = Math.max(0, Math.min(100, volume));
    }
    

    public boolean isMuted() {
        return isMuted;
    }

    public void setMuted(boolean isMuted) {
        this.isMuted = isMuted;
    }

    public void setMedia(String media){
      this.media = media;
    }

    public String getMedia(){
      return media;
    }

    public PlaybackState getPlaybackState() {
        return playbackState;
    }

    @Override
    public void execute(Command command) {
        switch (command.action()) {
            case TURN_ON -> setOnline(true);
            case TURN_OFF -> {
                stop();
                setOnline(false);
            }
            case PLAY -> play();
            case STOP -> stop();
            case PAUSE -> pause();
            case RESUME -> resume();
            default -> throw new IllegalStateException();
        }
    }

    @Override
    public boolean supports(Action action) {
        return switch (action) {
            case TURN_ON, TURN_OFF, PLAY, STOP, PAUSE, RESUME -> true;
            default -> false;
        };
    }

    @Override
    public void play() {
        if (!isOnline()) {
            throw new IllegalStateException("Speaker is offline");
        }
        playbackState = PlaybackState.PLAYING;
    }

    @Override
    public void stop() {
        playbackState = PlaybackState.STOP;
    }

    @Override
    public void pause() {
        if (playbackState == PlaybackState.PLAYING) {
            playbackState = PlaybackState.PAUSED;
        }
    }

    @Override
    public void resume() {
        if (playbackState == PlaybackState.PAUSED) {
            playbackState = PlaybackState.PLAYING;
        }
    }

    @Override
    public double currentPowerWatts() {
        return ratedWatts;
    }


   



}
