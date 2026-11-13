package com.homehub;


public class PowerLimitExceededException extends HomeHubException {

    public PowerLimitExceededException(double currentWatts, double additionalWatts) {
        super("Adding " + additionalWatts + " W to the current " + currentWatts + " W would exceed the limit");
    }
}
