package com.homehub.automation;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventBusTest {

    private final Event event = new Event(EventType.MOTION, LocalDateTime.of(2026, 10, 27, 12, 0), "m1", 1);

    private EventBus bus;
    private List<Event> received;

    @BeforeEach
    void setUp() {
        bus = new EventBus();
        received = new ArrayList<>();
    }

    @Test
    void subscriberReceivesThePublishedEvent() {
        bus.subscribe(received::add);

        bus.publish(event);

        assertEquals(List.of(event), received);
    }

    @Test
    void everySubscriberReceivesItInSubscriptionOrder() {
        List<String> order = new ArrayList<>();
        bus.subscribe(e -> order.add("first"));
        bus.subscribe(e -> order.add("second"));

        bus.publish(event);

        assertEquals(List.of("first", "second"), order);
    }

    @Test
    void publishingWithNoSubscribersDoesNothing() {
        assertDoesNotThrow(() -> bus.publish(event));
    }

    @Test
    void eachPublishReachesTheSubscriberOnce() {
        bus.subscribe(received::add);

        bus.publish(event);
        bus.publish(event);

        assertEquals(2, received.size());
    }

    @Test
    void aNullSubscriberIsRefused() {
        assertThrows(NullPointerException.class, () -> bus.subscribe(null));
    }

    @Test
    void aNullEventIsRefused() {
        bus.subscribe(received::add);
        assertThrows(NullPointerException.class, () -> bus.publish(null));
        assertTrue(received.isEmpty());
    }

    @Test
    void aSubscriberAddedWhilePublishingDoesNotBreakTheLoopAndMissesThatEvent() {
        List<String> order = new ArrayList<>();
        bus.subscribe(e -> {
            order.add("first");
            bus.subscribe(x -> order.add("late"));
        });

        assertDoesNotThrow(() -> bus.publish(event));
        assertEquals(List.of("first"), order);          // the late one did not see the event being published

        order.clear();
        bus.publish(event);
        assertTrue(order.contains("late"));              // but it hears the next one
    }
}
