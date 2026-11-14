package com.homehub.devices;

import java.util.Random;

public class AirQualityMonitor extends Devices implements Sensing{
    private int oxygenLevel;    
    private int carbonMonoxideLevel;
    private int carbonDioxideLevel;
    private int temperature;
    private int pm25;

    private static int minCarbonDioxide = 1000;
    private static int maxCarbonDioxide = 2000;
    private static int minCarbonMonoxide = 9;
    private static int maxCarbonMonoxide = 34;
    private static int minTemperature = 10;
    private static int maxTemperature = 29;

     

    public AirQualityMonitor(String id, String name, String brand, boolean onlineStatus, String room, String modelNumber, int oxygenLevel, int carbonMonoxideLevel, int carbonDioxideLevel, int temperature, int pm25, double ratedWatts){
        super( id,  name,  brand,  onlineStatus, room, modelNumber, ratedWatts);
        this.oxygenLevel = oxygenLevel;
        this.carbonDioxideLevel = carbonDioxideLevel;
        this.carbonMonoxideLevel = carbonMonoxideLevel;
        this.temperature = temperature;
        this.pm25 = pm25;
        
    }
    
    public int getOxygenLevel(){
        return oxygenLevel;

    }

    public int getCarbonMonoxideLevel(){
        return carbonMonoxideLevel;
    }

    
    public int getCarbonDioxideLevel(){
        return carbonDioxideLevel;
    }

    public int getTemperature(){
        return temperature;
    }

    public int getPm25(){
        return pm25;
        
    }

 
    private static int randomBetween(int min, int max) {
        return new Random().nextInt(max - min + 1) + min;
    }

    @Override
    public void readValue(){
        carbonDioxideLevel = randomBetween(minCarbonDioxide, maxCarbonDioxide);
        carbonMonoxideLevel = randomBetween(minCarbonMonoxide, maxCarbonMonoxide);
        temperature = randomBetween(minTemperature, maxTemperature);
        //System.out.println("Air: CO2=" + carbonDioxideLevel + " CO=" + carbonMonoxideLevel + " temp=" + temperature);
    }

}
