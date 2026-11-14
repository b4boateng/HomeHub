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
}
