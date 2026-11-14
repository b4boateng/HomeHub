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
    void sensorsAreNotControllable() {
        assertFalse(new TemperatureSensor("tp1", "T", "A", true, "R", "M", 1) instanceof Controllable);
        assertFalse(new HumiditySensor("h1", "H", "A", true, "R", "M", 1) instanceof Controllable);
    }
}
