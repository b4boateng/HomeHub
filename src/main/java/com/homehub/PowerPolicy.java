package com.homehub;

public interface PowerPolicy {
    boolean allows(double currentWatts, double additionalWatts);

    
    default void require(double currentWatts, double additionalWatts) {
        if (!allows(currentWatts, additionalWatts)) {
            throw new PowerLimitExceededException(currentWatts, additionalWatts);
        }
    }
}
