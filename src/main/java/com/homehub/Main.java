package com.homehub;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        SmartBulb bulb = new SmartBulb(50, 128, "b1", "LB-100", "Desk Bulb", "Philips", "Office", true, 9.0, "hello");
        SmartLock lock = new SmartLock("l1", "Front Door", "Yale", true, "Hallway", "YL-5", 2.0, true, "1234", 90);
        SmartTv tv = new SmartTv("t1", "Living Room TV", "Samsung", true, "Living Room", "QN90", 120.0, 20, false, "Netflix", 5);
        SmartSpeaker speaker = new SmartSpeaker("s1", "Kitchen Speaker", "Sonos", true, "Kitchen", "One", 15.0, 30, false, "Radio");
        HumiditySensor humidity = new HumiditySensor("h1", "HS-1", "Bathroom Humidity", "Aqara", true, "Bathroom", 1.0, 40);
        AirQualityMonitor air = new AirQualityMonitor("a1", "Air Monitor", "Awair", true, "Bedroom", "AW-2", 21, 10, 1200, 22, 8, 3.0);

        System.out.println("=== SmartBulb ===");
        bulb.execute(new Command(Action.BRIGHTEN, 30));
        System.out.println("Brightness after BRIGHTEN 30 (expect 80): " + bulb.getBrightness());
        bulb.execute(new Command(Action.BRIGHTEN, 50));
        System.out.println("Brightness after BRIGHTEN 50 (expect clamp 100): " + bulb.getBrightness());
        bulb.execute(new Command(Action.DIM, 200));
        System.out.println("Brightness after DIM 200 (expect clamp 0): " + bulb.getBrightness());
        bulb.execute(new Command(Action.TURN_OFF, 0));
        System.out.println("Online after TURN_OFF (expect false): " + bulb.isOnline());
        bulb.setColorTemperature(300);
        System.out.println("Color temp after set 300 (expect clamp 255): " + bulb.getColorTemperature());
        System.out.println("Bulb supports LOCK (expect false): " + bulb.supports(Action.LOCK));
        tryExecute(bulb, new Command(Action.LOCK, 0));

        System.out.println("\n=== SmartLock ===");
        lock.execute(new Command(Action.UNLOCK, 0));
        System.out.println("Locked after UNLOCK (expect false): " + lock.getLockStatus());
        lock.execute(new Command(Action.LOCK, 0));
        System.out.println("Locked after LOCK (expect true): " + lock.getLockStatus());
        System.out.println("Lock supports SOUND_ALARM (expect true): " + lock.supports(Action.SOUND_ALARM));
        System.out.println("Lock supports PLAY (expect false): " + lock.supports(Action.PLAY));

        System.out.println("\n=== SmartTv ===");
        tv.execute(new Command(Action.PLAY, 0));
        System.out.println("State after PLAY (expect PLAYING): " + tv.getPlaybackState());
        tv.execute(new Command(Action.PAUSE, 0));
        System.out.println("State after PAUSE (expect PAUSED): " + tv.getPlaybackState());
        tv.execute(new Command(Action.RESUME, 0));
        System.out.println("State after RESUME (expect PLAYING): " + tv.getPlaybackState());
        tv.execute(new Command(Action.TURN_OFF, 0));
        System.out.println("State after TURN_OFF (expect STOP): " + tv.getPlaybackState() + ", online=" + tv.isOnline());
        tryExecute(tv, new Command(Action.PLAY, 0));
        tv.setVolume(150);
        System.out.println("Volume after set 150 (expect clamp 100): " + tv.getVolume());
        tv.setChannel(-3);
        System.out.println("Channel after set -3 (expect clamp 0): " + tv.getChannel());

        System.out.println("\n=== SmartSpeaker ===");
        speaker.execute(new Command(Action.PLAY, 0));
        System.out.println("State after PLAY (expect PLAYING): " + speaker.getPlaybackState());
        speaker.execute(new Command(Action.STOP, 0));
        System.out.println("State after STOP (expect STOP): " + speaker.getPlaybackState());
        speaker.pause();
        System.out.println("State after PAUSE while stopped (expect STOP): " + speaker.getPlaybackState());
        speaker.setVolume(-10);
        System.out.println("Volume after set -10 (expect clamp 0): " + speaker.getVolume());

        System.out.println("\n=== Sensors ===");
        humidity.readValue();
        System.out.println("Stored humidity (expect 10-100): " + humidity.getHumidity());
        air.readValue();
        System.out.println("Air: CO2=" + air.getCarbonDioxideLevel() + " CO=" + air.getCarbonMonoxideLevel()
                + " O2=" + air.getOxygenLevel() + " temp=" + air.getTemperature() + " pm2.5=" + air.getPm25());

        System.out.println("\n=== energy report ===");
        List<Devices> all = List.of(bulb, lock, tv, speaker, humidity, air);
        double total = 0;
        for (Devices d : all) {
            String kind = d.getClass().getSimpleName();
            if (d instanceof EnergyReporting e) {
                System.out.printf("%-18s %-18s %6.1f W%n", kind, d.getName(), e.currentPowerWatts());
                total += e.currentPowerWatts();
            } else {
                System.out.printf("%-18s %-18s (no energy reporting)%n", kind, d.getName());
            }
        }
        System.out.println("Total reported watts: " + total);

        System.out.println("\n===: broadcast TURN_ON ===");
        for (Devices d : all) {
            if (d instanceof Controllable c) {
                if (c.supports(Action.TURN_ON)) {
                    c.execute(new Command(Action.TURN_ON, 0));
                    System.out.println(d.getName() + " -> TURN_ON, online=" + d.isOnline());
                } else {
                    System.out.println(d.getName() + " does not support TURN_ON");
                }
            }
        }
    }

    private static void tryExecute(Controllable device, Command command) {
        try {
            device.execute(command);
            System.out.println("Executed " + command.action() + " without error");
        } catch (IllegalStateException ex) {
            System.out.println("Caught IllegalStateException for " + command.action()
                    + (ex.getMessage() != null ? ": " + ex.getMessage() : ""));
        }
    }
}
