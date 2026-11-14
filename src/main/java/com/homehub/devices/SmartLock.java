package com.homehub.devices;

import com.homehub.exceptions.HomeHubException;

public class SmartLock extends Devices implements Controllable{
    private boolean isLocked;
    private final String code;
    private int batteryLevel;

    public SmartLock(String id, String name, String brand, boolean onlineStatus, String room, String modelNumber, double ratedWatts, boolean isLocked, String code, int batteryLevel){
        super(id, name, brand, onlineStatus, room, modelNumber, ratedWatts);
        this.isLocked = isLocked;
        this.code = code;
        this.batteryLevel = batteryLevel;

    }

    public boolean getLockStatus(){
        return isLocked;

    }

    public void setLockStatus(boolean value){
        this.isLocked = value;

    }

    // The code is never handed out, only checked, so the console can ask for it before unlocking.
    public boolean codeMatches(String attempt){
        return code.equals(attempt);
    }

    public int getBatteryLevel(){
        return batteryLevel;
    }

    @Override
    public void execute(Command command) {
        if (batteryLevel == 0) {
            throw new HomeHubException(getName() + " battery is flat");
        }
        switch(command.action()){
            case LOCK ->   setLockStatus(true);

            case UNLOCK ->   setLockStatus(false);

            default -> throw new IllegalStateException("Unsupported action: " + command.action());
        }
        // Every lock or unlock uses one percent of the battery.
        batteryLevel--;
    }



   @Override
    public boolean supports(Action action){
       return switch (action) {
        case  LOCK , UNLOCK -> true;
        default -> false;
    };

}

}
