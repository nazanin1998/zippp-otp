package com.zippp.otp.listener.support;

import org.slf4j.MDC;

/**
 * Push the current message's correlationId into SLF4J MDC so logger pattern
 * layouts (e.g. {@code %X{corrId}}) propagate it through every downstream call
 * made from the listener.
 * <p>
 * Always pair {@link #put(String)} with {@link #clear()} in a {@code finally}
 * block — MDC is thread-local, so an uncleared entry leaks onto the next
 * message a worker thread happens to receive.
 */
public final class CorrelationIdMdc {

    public static final String KEY = "corrId";

    private CorrelationIdMdc() {}

    public static void put(String correlationId) {
        MDC.put(KEY, correlationId);
    }

    public static void clear() {
        MDC.remove(KEY);
    }
}
