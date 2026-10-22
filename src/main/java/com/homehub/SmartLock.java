package com.homehub;

public class SmartLock extends Devices implements Controllable{
    private boolean isLocked;
    private String code;
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

    public void setCode(String value){
        this.code = value;
    }


    public String getCode(){
        return code;
    }

    public void setBatteryLevel(int value){
        this.batteryLevel = value;
    }

    public int getBatteryLevel(){
        return batteryLevel;
    }



    @Override 
    public void execute(Command command) {
        switch(command.action()){
            case LOCK ->   setLockStatus(true);
            
            case UNLOCK ->   setLockStatus(false);
                

    };
        }

    

   @Override 
    public boolean supports(Action action){
       return switch (action) {
        case  LOCK , UNLOCK, SOUND_ALARM -> true;
        default -> false;
    };

}

}


