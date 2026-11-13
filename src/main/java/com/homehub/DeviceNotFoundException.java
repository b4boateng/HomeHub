package com.homehub;


public class DeviceNotFoundException extends HomeHubException {

    public DeviceNotFoundException(String id) {
        super("No device with id: " + id);
    }
}
