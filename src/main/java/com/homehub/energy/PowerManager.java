package com.homehub.energy;

public class PowerManager implements PowerPolicy{

    private final double limitWatts;

    public PowerManager(double limitWatts) {
        if (limitWatts < 0){
            throw new IllegalArgumentException("limitWatts cannot be negative" + limitWatts);
        }
        this.limitWatts = limitWatts;
    }

    public double getLimitWatts() {
        return this.limitWatts;
    }

    @Override 
    public boolean allows(double currentWatts, double additionalWatts){
        return currentWatts + additionalWatts <= limitWatts;
    }

    
}
