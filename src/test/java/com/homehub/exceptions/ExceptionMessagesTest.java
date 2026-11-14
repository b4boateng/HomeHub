package com.homehub.exceptions;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ExceptionMessagesTest {

    @Test
    void baseKeepsItsMessage() {
        assertEquals("boom", new HomeHubException("boom").getMessage());
    }

    @Test
    void offlineNamesTheDevice() {
        assertEquals("Living TV is offline", new DeviceOfflineException("Living TV").getMessage());
    }

    @Test
    void notFoundNamesTheId() {
        assertEquals("No device with id: x9", new DeviceNotFoundException("x9").getMessage());
    }

    @Test
    void powerLimitShowsBothNumbers() {
        String message = new PowerLimitExceededException(80, 30).getMessage();
        assertTrue(message.contains("80.0") && message.contains("30.0"), message);
    }

    @Test
    void ruleConflictKeepsItsMessage() {
        assertEquals("clash", new RuleConflictException("clash").getMessage());
    }

    @Test
    void dataLoadShowsFileLineAndProblem() {
        assertEquals("rules.csv line 4: bad time", new DataLoadException("rules.csv", 4, "bad time").getMessage());
    }

    @Test
    void everySubclassIsAHomeHubExceptionAndUnchecked() {
        assertInstanceOf(HomeHubException.class, new DeviceOfflineException("x"));
        assertInstanceOf(HomeHubException.class, new DeviceNotFoundException("x"));
        assertInstanceOf(HomeHubException.class, new PowerLimitExceededException(1, 1));
        assertInstanceOf(HomeHubException.class, new RuleConflictException("x"));
        assertInstanceOf(HomeHubException.class, new DataLoadException("f", 1, "x"));
        assertInstanceOf(RuntimeException.class, new HomeHubException("x"));
    }
}
