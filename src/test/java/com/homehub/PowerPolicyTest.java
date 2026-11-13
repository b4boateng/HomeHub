package com.homehub;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

// Tests the default method require() on its own, using a tiny stand-in policy (limit 10 W).
class PowerPolicyTest {

    private final PowerPolicy limitTen = (current, additional) -> current + additional <= 10;

    @Test
    void requireDoesNothingWhenAllowed() {
        assertDoesNotThrow(() -> limitTen.require(5, 5));       // exactly at the limit
    }

    @Test
    void requireThrowsWhenNotAllowed() {
        assertThrows(PowerLimitExceededException.class, () -> limitTen.require(8, 5));
    }

    @Test
    void theMessageShowsBothNumbers() {
        PowerLimitExceededException ex = assertThrows(PowerLimitExceededException.class, () -> limitTen.require(8, 5));
        assertTrue(ex.getMessage().contains("8.0") && ex.getMessage().contains("5.0"), ex.getMessage());
    }

    @Test
    void powerManagerKeepsItsLimit() {
        assertEquals(100, new PowerManager(100).getLimitWatts());
    }
}
