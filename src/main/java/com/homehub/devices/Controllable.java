package com.homehub.devices;

public interface  Controllable {

    public void execute(Command command);
    public boolean supports(Action action);
    
}
