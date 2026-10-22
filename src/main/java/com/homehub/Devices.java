package com.homehub;

abstract class Devices{
    protected String id;
    protected String modelNumber;
    protected String name;
    protected String brand;
    protected boolean onlineStatus;
    protected String room;
    protected final double ratedWatts;

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

    public void setId(String id){
        this.id = id;

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

    public double getRatedWatts(){
        return ratedWatts;

    }

}
    

