package com.homehub.exceptions;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ExceptionMessagesTest {

    @Test
    void dataLoadShowsFileLineAndProblem() {
        assertEquals("rules.csv line 4: bad time", new DataLoadException("rules.csv", 4, "bad time").getMessage());
    }

}
