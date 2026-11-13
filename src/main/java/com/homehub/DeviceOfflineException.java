package com.homehub;


public class DeviceOfflineException extends HomeHubException {

    public DeviceOfflineException(String deviceName) {
        super(deviceName + " is offline");
    }
}
