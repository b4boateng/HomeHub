package com.homehub.energy;

import com.homehub.exceptions.*;

// Interface so the limit rule can change without touching DeviceControl class
@FunctionalInterface
public interface PowerPolicy {
    boolean allows(double currentWatts, double additionalWatts);

    
    default void require(double currentWatts, double additionalWatts) {
        if (!allows(currentWatts, additionalWatts)) {
            throw new HomeHubException("Adding " + additionalWatts + " W to the current " + currentWatts
                    + " W would exceed the limit");
        }
    }
}
