package com.homehub;

public interface PowerPolicy {
    boolean allows(double currentWatts, double additionalWatts);
    
}
