package com.homehub;

// devices.csv columns: type,id,name,brand,model,room,watts,online
// type is one of BULB, TV, SPEAKER, LOCK, HUMIDITY, AIR.
// Starting values the CSV doesn't carry (volume, brightness...) are the constants below;
// the devices change them while the simulation runs.
public class DevicesLoader extends CsvLoader<Devices> {

    private static final int START_BRIGHTNESS = 50;
    private static final int START_COLOR_TEMPERATURE = 128;
    private static final int START_VOLUME = 20;
    private static final int START_CHANNEL = 1;
    private static final String START_LOCK_CODE = "0000";
    private static final int START_BATTERY = 100;
    private static final int START_HUMIDITY = 50;
    private static final int START_OXYGEN = 21;
    private static final int START_CARBON_MONOXIDE = 0;
    private static final int START_CARBON_DIOXIDE = 400;
    private static final int START_AIR_TEMPERATURE = 21;
    private static final int START_PM25 = 5;

    public DevicesLoader() {
        super(8);
    }

    @Override
    protected Devices parseRow(String[] c) {
        String id = c[1];
        String name = c[2];
        String brand = c[3];
        String model = c[4];
        String room = c[5];
        double watts = Double.parseDouble(c[6]);
        boolean online = parseBoolean(c[7]);

        return switch (c[0]) {
            case "BULB"     -> new SmartBulb(START_BRIGHTNESS, START_COLOR_TEMPERATURE, id, model, name, brand, room, online, watts, "");
            case "TV"       -> new SmartTv(id, name, brand, online, room, model, watts, START_VOLUME, false, "", START_CHANNEL);
            case "SPEAKER"  -> new SmartSpeaker(id, name, brand, online, room, model, watts, START_VOLUME, false, "");
            case "LOCK"     -> new SmartLock(id, name, brand, online, room, model, watts, true, START_LOCK_CODE, START_BATTERY);
            case "HUMIDITY" -> new HumiditySensor(id, model, name, brand, online, room, watts, START_HUMIDITY);
            case "AIR"      -> new AirQualityMonitor(id, name, brand, online, room, model, START_OXYGEN, START_CARBON_MONOXIDE,
                                                     START_CARBON_DIOXIDE, START_AIR_TEMPERATURE, START_PM25, watts);
            default -> throw new IllegalArgumentException("unknown device type '" + c[0] + "'");
        };
    }

    // Boolean.parseBoolean would quietly turn a typo into false, so check it ourselves.
    private static boolean parseBoolean(String text) {
        if (text.equals("true")) return true;
        if (text.equals("false")) return false;
        throw new IllegalArgumentException("expected true or false but found '" + text + "'");
    }
}
