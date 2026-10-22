package com.homehub;
import java.util.Random;
import java.util.random.*;

public class HumiditySensor  extends Devices implements Sensing{
    private int humidity;
    private static int minHumidity = 10;
    private static int maxHumidity = 100;


    public HumiditySensor(String id, String modelNumber, String name, String brand, boolean onlineStatus, String room, double ratedWatts, int humidity){
        super( id, name, brand,  onlineStatus, room, modelNumber, ratedWatts);
        this.humidity = humidity;
    }

    public int getHumidity(){
        return humidity;
    }

    
    @Override 
    public void readValue(){
     Random rand = new Random();
     humidity = rand.nextInt(maxHumidity - minHumidity + 1) + minHumidity;
     System.out.println("Humidity is " + humidity + "%");

    }
}
