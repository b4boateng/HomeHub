package com.homehub;

public class SmartBulb extends Devices implements Controllable, EnergyReporting {
    private int brightness;
    private int colorTemperature;
    private String message;

    public SmartBulb(int brightness, int colorTemperature, String id, String modelNumber, String name, String brand, String room, boolean onlineStatus, double ratedWatts, String message){
        super( id, name, brand,  onlineStatus, room, modelNumber, ratedWatts);
        this.brightness = brightness;
        this.colorTemperature = colorTemperature;
        this.message = message;
    }

    @Override 
    public void execute(Command command) {
        switch(command.action()){
           case TURN_ON  -> setOnline(true);
        case TURN_OFF -> setOnline(false);
        case BRIGHTEN -> setBrightness(brightness + command.value());
        case DIM      -> setBrightness(brightness - command.value());
        default       -> throw new IllegalStateException();

        }

    }

    @Override 
    public boolean supports(Action action){
       return switch (action) {
        case TURN_ON, TURN_OFF, BRIGHTEN, DIM -> true;
        default -> false;
    };
         
    }

    // will implement this later
    @Override 
    public double currentPowerWatts(){
        return ratedWatts;
    }

    public int getBrightness() {
        return brightness;
    }
    public int getColorTemperature() {
        return colorTemperature;
        
    }

    public void setBrightness(int brightness){
        this.brightness = Math.max(0, Math.min(100, brightness));

    }

    public void setColorTemperature(int colorTemperature){
        this.colorTemperature = Math.max(0, Math.min(255, colorTemperature));
    }

    @Override 
    public double getRatedWatts(){
        return ratedWatts;
    }
    
    @Override 
    public String getId(){
        return  id;
    }

    @Override 
      public String getModelNumber(){
        return modelNumber;

    }

    void setMessage(String message){
        this.message = message;
    }

    public String getMessage(){
        return message;
    }


}
