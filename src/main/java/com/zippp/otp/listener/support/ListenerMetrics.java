package com.zippp.otp.listener.support;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ListenerMetrics {

    private final MeterRegistry registry;
    private final ConcurrentMap<String, Timer> timerCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> counterCache = new ConcurrentHashMap<>();

    public ListenerMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void recordSuccess(String queue, Timer.Sample sample) {
        sample.stop(timer(queue, "success"));
        counter(queue, "success").increment();
    }

    public void recordParseError(String queue, Timer.Sample sample) {
        sample.stop(timer(queue, "parse_error"));
        counter(queue, "parse_error").increment();
    }

    public void recordInternalError(String queue, Timer.Sample sample) {
        sample.stop(timer(queue, "internal_error"));
        counter(queue, "internal_error").increment();
    }

    public void recordDuplicate(String queue, Timer.Sample sample) {
        sample.stop(timer(queue, "duplicate"));
        counter(queue, "duplicate").increment();
    }

    private Timer timer(String queue, String outcome) {
        return timerCache.computeIfAbsent(queue + "|" + outcome,
                key -> Timer.builder("otp.listener.latency")
                        .description("End-to-end listener processing latency")
                        .tag("queue", queue)
                        .tag("outcome", outcome)
                        .register(registry));
    }

    private Counter counter(String queue, String outcome) {
        return counterCache.computeIfAbsent(queue + "|" + outcome,
                key -> Counter.builder("otp.listener.count")
                        .description("Listener invocation count by outcome")
                        .tag("queue", queue)
                        .tag("outcome", outcome)
                        .register(registry));
    }
}
