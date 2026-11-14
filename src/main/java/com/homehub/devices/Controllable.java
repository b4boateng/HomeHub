package com.homehub.devices;

// Separate from Devices because sensors cannot be commanded.
public interface  Controllable {

    public void execute(Command command);
    public boolean supports(Action action);
    
}
