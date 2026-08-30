package io.github.developeranalytics.observability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {

    @Test
    void acceptsSafeCallerCorrelationIds() {
        assertTrue(CorrelationIdFilter.valid("req-1234abcd"));
        assertTrue(CorrelationIdFilter.valid("client.trace_123:abc"));
    }

    @Test
    void rejectsUnsafeOrUnboundedCorrelationIds() {
        assertFalse(CorrelationIdFilter.valid(null));
        assertFalse(CorrelationIdFilter.valid("short"));
        assertFalse(CorrelationIdFilter.valid("contains spaces"));
        assertFalse(CorrelationIdFilter.valid("x".repeat(129)));
    }
}
