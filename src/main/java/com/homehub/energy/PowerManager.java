package com.homehub.energy;

//checks that turning a device on would not push the home's total power over its limit
public class PowerManager implements PowerPolicy{

    private final double limitWatts;

    public PowerManager(double limitWatts) {
        if (!Double.isFinite(limitWatts) || limitWatts < 0) {
            throw new IllegalArgumentException("Power limit must be finite and non-negative but was " + limitWatts);
        }
        this.limitWatts = limitWatts;
    }

    public double getLimitWatts() {
        return this.limitWatts;
    }

    @Override 
    public boolean allows(double currentWatts, double additionalWatts){
        // Exactly at the limit is allowed.
        return currentWatts + additionalWatts <= limitWatts;
    }

    
}
