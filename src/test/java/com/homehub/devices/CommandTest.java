package com.homehub.devices;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CommandTest {

    @Test
    void keepsItsActionAndValue() {
        Command c = new Command(Action.DIM, 10);
        assertEquals(Action.DIM, c.action());
        assertEquals(10, c.value());
    }

    @Test
    void valueDefaultsToZeroWhenOmitted() {
        assertEquals(0, new Command(Action.TURN_ON).value());
    }

    @Test
    void sameActionAndValueAreEqualWithTheSameHash() {
        Command a = new Command(Action.DIM, 10);
        Command b = new Command(Action.DIM, 10);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentActionOrValueAreNotEqual() {
        assertNotEquals(new Command(Action.DIM, 10), new Command(Action.DIM, 11));
        assertNotEquals(new Command(Action.DIM, 10), new Command(Action.BRIGHTEN, 10));
    }

    @Test
    void toStringMentionsTheAction() {
        assertTrue(new Command(Action.LOCK).toString().contains("LOCK"));
    }

    @Test
    void nullActionIsRejected() {
        assertThrows(NullPointerException.class, () -> new Command(null));
    }
}
