package com.homehub.devices;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SensorTest {

    @Test
    void motionReadingIsOneWhenMotionWasDetectedAndZeroOtherwise() {
        MotionSensor sensor = new MotionSensor("m1", "Hall Motion", "Aqara", true, "Hall", "MS-1", 1);
        assertEquals(0, sensor.getReading());
        sensor.setMotionDetected(true);
        assertEquals(1, sensor.getReading());
    }

    @Test
    void temperatureReadingsStayInsideTheirRangeAndMatchGetReading() {
        TemperatureSensor sensor = new TemperatureSensor("tp1", "Thermometer", "Aqara", true, "Hall", "TH-1", 1);
        assertEquals(20, sensor.getReading());                  // a sensible value before the first reading
        boolean sawLow = false;
        boolean sawHigh = false;
        for (int i = 0; i < 5000; i++) {
            sensor.readValue();
            int t = sensor.getTemperature();
            assertEquals(t, sensor.getReading());
            assertTrue(t >= TemperatureSensor.MIN_CELSIUS && t <= TemperatureSensor.MAX_CELSIUS, "out of range: " + t);
            sawLow |= t == TemperatureSensor.MIN_CELSIUS;
            sawHigh |= t == TemperatureSensor.MAX_CELSIUS;
        }
        assertTrue(sawLow && sawHigh, "both ends of the range should be reachable");
    }

    @Test
    void humidityReadingsStayInsideTheirRangeAndMatchGetReading() {
        HumiditySensor sensor = new HumiditySensor("h1", "Hygrometer", "Aqara", true, "Bath", "TH-1", 1);
        assertEquals(50, sensor.getReading());
        boolean sawLow = false;
        boolean sawHigh = false;
        for (int i = 0; i < 5000; i++) {
            sensor.readValue();
            int h = sensor.getHumidity();
            assertEquals(h, sensor.getReading());
            assertTrue(h >= HumiditySensor.MIN_PERCENT && h <= HumiditySensor.MAX_PERCENT, "out of range: " + h);
            sawLow |= h == HumiditySensor.MIN_PERCENT;
            sawHigh |= h == HumiditySensor.MAX_PERCENT;
        }
        assertTrue(sawLow && sawHigh, "both ends of the range should be reachable");
    }

    @Test
    void everySensorOffersItselfAsASensorAndOtherDevicesDoNot() {
        MotionSensor motion = new MotionSensor("m1", "M", "A", true, "R", "M", 1);
        TemperatureSensor temperature = new TemperatureSensor("tp1", "T", "A", true, "R", "M", 1);
        HumiditySensor humidity = new HumiditySensor("h1", "H", "A", true, "R", "M", 1);
        assertSame(motion, motion.asSensor().orElseThrow());
        assertSame(temperature, temperature.asSensor().orElseThrow());
        assertSame(humidity, humidity.asSensor().orElseThrow());
        assertTrue(new SmartBulb(50, 128, "b1", "M", "B", "P", "R", true, 10, "").asSensor().isEmpty());
        assertTrue(new SmartLock("l1", "L", "Y", true, "R", "M", 2, true, "0000", 100).asSensor().isEmpty());
    }

    @Test
    void eachSensorSaysWhichEventTypeItsReadingsProduce() {
        assertEquals(com.homehub.automation.EventType.MOTION, new MotionSensor("m1", "M", "A", true, "R", "M", 1).eventType());
        assertEquals(com.homehub.automation.EventType.TEMPERATURE, new TemperatureSensor("tp1", "T", "A", true, "R", "M", 1).eventType());
        assertEquals(com.homehub.automation.EventType.HUMIDITY, new HumiditySensor("h1", "H", "A", true, "R", "M", 1).eventType());
    }

    @Test
    void noMotionIsNotWorthPublishingButAMotionReadingIs() {
        MotionSensor motion = new MotionSensor("m1", "M", "A", true, "R", "M", 1);
        assertFalse(motion.hasNewEvent());
        motion.setMotionDetected(true);
        assertTrue(motion.hasNewEvent());
        assertTrue(new TemperatureSensor("tp1", "T", "A", true, "R", "M", 1).hasNewEvent());
    }

    @Test
    void sensorsAreNotControllable() {
        assertFalse(new TemperatureSensor("tp1", "T", "A", true, "R", "M", 1) instanceof Controllable);
        assertFalse(new HumiditySensor("h1", "H", "A", true, "R", "M", 1) instanceof Controllable);
    }
}
