package com.homehub.devices;

import com.homehub.exceptions.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

// Removing devices from the registry, and the brightness limits the console relies on.
class RegistryRemoveAndBulbLimitsTest {

    private static SmartBulb bulb(String id, int brightness) {
        return new SmartBulb(brightness, 128, id, "LB-100", "Lamp " + id, "Philips", "Hall", true, 10, "");
    }

    // ---- DeviceRegistry.remove ----

    @Test
    void removeReturnsTheDeviceAndForgetsIt() {
        DeviceRegistry registry = new DeviceRegistry();
        SmartBulb b1 = bulb("b1", 50);
        registry.register(b1);

        assertSame(b1, registry.remove("b1"));
        assertFalse(registry.contains("b1"));
        assertTrue(registry.all().isEmpty());
    }

    @Test
    void removeOfAnUnknownIdThrowsAndChangesNothing() {
        DeviceRegistry registry = new DeviceRegistry();
        registry.register(bulb("b1", 50));

        assertThrows(DeviceNotFoundException.class, () -> registry.remove("zzz"));
        assertEquals(1, registry.all().size());
    }

    @Test
    void removingTwiceFailsTheSecondTime() {
        DeviceRegistry registry = new DeviceRegistry();
        registry.register(bulb("b1", 50));
        registry.remove("b1");

        assertThrows(DeviceNotFoundException.class, () -> registry.remove("b1"));
    }

    @Test
    void anIdCanBeReusedAfterRemoval() {
        DeviceRegistry registry = new DeviceRegistry();
        registry.register(bulb("b1", 50));
        registry.remove("b1");

        assertDoesNotThrow(() -> registry.register(bulb("b1", 20)));
        assertEquals(20, ((SmartBulb) registry.get("b1")).getBrightness());
    }

    @Test
    void removeKeepsTheOrderOfTheRest() {
        DeviceRegistry registry = new DeviceRegistry();
        registry.register(bulb("b1", 50));
        registry.register(bulb("b2", 50));
        registry.register(bulb("b3", 50));
        registry.remove("b2");

        assertEquals(java.util.List.of("b1", "b3"), registry.all().stream().map(Devices::getId).toList());
    }

    // ---- brightness arithmetic ----

    @Test
    void brightenByAHugeAmountStopsAtTheTopInsteadOfWrappingAround() {
        SmartBulb b = bulb("b1", 50);
        b.execute(new Command(Action.BRIGHTEN, Integer.MAX_VALUE));
        assertEquals(100, b.getBrightness());
    }

    @Test
    void dimByAHugeAmountStopsAtZero() {
        SmartBulb b = bulb("b1", 50);
        b.execute(new Command(Action.DIM, Integer.MAX_VALUE));
        assertEquals(0, b.getBrightness());
    }

    @Test
    void brightenAndDimMoveByTheAmountWithinTheRange() {
        SmartBulb b = bulb("b1", 50);
        b.execute(new Command(Action.BRIGHTEN, 30));
        assertEquals(80, b.getBrightness());
        b.execute(new Command(Action.DIM, 20));
        assertEquals(60, b.getBrightness());
        b.execute(new Command(Action.BRIGHTEN, 100));
        assertEquals(100, b.getBrightness());
        b.execute(new Command(Action.DIM, 100));
        assertEquals(0, b.getBrightness());
    }

    // ---- the Action enum after removing the unused ones ----

    @Test
    void everyActionIsSupportedBySomeDevice() {
        Controllable bulb = bulb("b1", 50);
        Controllable lock = new SmartLock("l1", "Door", "Yale", true, "Hall", "Y1", 2, true, "0000", 100);
        Controllable speaker = new SmartSpeaker("s1", "Speaker", "Sonos", true, "Kitchen", "One", 15, 20, false, "");
        for (Action a : Action.values()) {
            assertTrue(bulb.supports(a) || lock.supports(a) || speaker.supports(a), a + " is not supported by any device");
        }
    }

    @Test
    void theRemovedActionsAreGone() {
        for (Action a : Action.values()) {
            assertNotEquals("NOTIFY", a.name());
            assertNotEquals("SOUND_ALARM", a.name());
        }
    }
}
