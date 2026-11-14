package com.homehub.devices;

import java.util.Optional;

//  holds what all devices share; subclasses add their own behaviour.
public abstract class Devices{
    private final String id;
    private String modelNumber;
    private String name;
    private String brand;
    private boolean onlineStatus;
    private String room;
    private final double ratedWatts;

    public Devices(String id, String name, String brand, boolean onlineStatus, String room, String modelNumber, double ratedWatts){
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.onlineStatus = onlineStatus;
        this.room = room;
        this.ratedWatts = ratedWatts;
        this.modelNumber = modelNumber;

    }

    
    public String getId(){
        return id;

    }

    public void setModelNumber(String modelNumber){
        this.modelNumber = modelNumber;
    }

    public String getModelNumber(){
        return modelNumber;

    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getBrand(){
        return brand;

    }

    public void setBrand(String brand){
        this.brand = brand;
    }

    public String getRoom(){
        return room;
    }

    public void setRoom(String room){
        this.room = room;
    }

    public boolean isOnline(){
        return onlineStatus;
    }

   public void setOnline(boolean value){
    this.onlineStatus = value;
    }

    // Most devices need no code. A device that does overrides both of these.
    public boolean requiresCodeFor(Action action) {
        return false;
    }

    public boolean codeMatches(String attempt) {
        return true;
    }

    // Sensors return themselves here, so callers can read sensors without asking what class a device is.
    public Optional<Sensing> asSensor() {
        return Optional.empty();
    }

    // What the device adds to its one-line status, e.g. ", brightness 50". Subclasses override it.
    public String describeDetails() {
        return "";
    }

    public double getRatedWatts(){
        return ratedWatts;

    }

}
    

