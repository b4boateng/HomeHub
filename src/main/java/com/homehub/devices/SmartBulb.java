package com.homehub.devices;

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
        // long maths, so a huge value cannot overflow and wrap around to the wrong end of the range.
        case BRIGHTEN -> setBrightness((int) Math.min(100L, (long) brightness + command.value()));
        case DIM      -> setBrightness((int) Math.max(0L, (long) brightness - command.value()));
        // WARMER lowers the colour temperature, COOLER raises it (0 to 255, same overflow care as above).
        case WARMER   -> setColorTemperature((int) Math.max(0L, (long) colorTemperature - command.value()));
        case COOLER   -> setColorTemperature((int) Math.min(255L, (long) colorTemperature + command.value()));
        default       -> throw new IllegalStateException();

        }
        // Remember what was last done, so the display can say why the bulb is the way it is.
        setMessage("last command: " + command.action() + (command.value() != 0 ? " " + command.value() : ""));
    }

    @Override 
    public boolean supports(Action action){
       return switch (action) {
        case TURN_ON, TURN_OFF, BRIGHTEN, DIM, WARMER, COOLER -> true;
        default -> false;
    };
         
    }


    @Override 
    public double currentPowerWatts(){
        return  isOnline() ? getRatedWatts() : 0;
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

    void setMessage(String message){
        this.message = message;
    }

    public String getMessage(){
        return message;
    }

}
