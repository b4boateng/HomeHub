package com.homehub.energy;

import com.homehub.exceptions.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PowerPolicyTest {

    private final PowerPolicy limitTen = (current, additional) -> current + additional <= 10;

    @Test
    void requireDoesNothingWhenAllowed() {
        assertDoesNotThrow(() -> limitTen.require(5, 5));
    }

    @Test
    void requireThrowsWhenNotAllowed() {
        assertThrows(HomeHubException.class, () -> limitTen.require(8, 5));
    }

    @Test
    void theMessageShowsBothNumbers() {
        HomeHubException ex = assertThrows(HomeHubException.class, () -> limitTen.require(8, 5));
        assertTrue(ex.getMessage().contains("8.0") && ex.getMessage().contains("5.0"), ex.getMessage());
    }

}
