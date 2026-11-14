package com.homehub.exceptions;

public class DeviceNotFoundException extends HomeHubException {

    public DeviceNotFoundException(String id) {
        super("No device with id: " + id);
    }
}
