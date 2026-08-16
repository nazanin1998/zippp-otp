package com.zippp.otp.listener.support;

import com.zippp.otpapi.enums.OtpErrorCode;
import com.zippp.rabbitconsumer.exception.RabbitConsumerFailedToParseException;
import com.zippp.rabbitconsumer.handler.MessageParser;
import com.zippp.rabbitconsumer.model.ConsumerParsedMessage;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import tools.jackson.databind.json.JsonMapper;

import java.util.function.BiFunction;


@Slf4j
@RequiredArgsConstructor
public abstract class AbstractOtpListener<Req, Res> {

    private final JsonMapper jsonMapper;
    private final IdempotencyGuard idempotencyGuard;
    private final ListenerMetrics metrics;

    protected abstract Res errorResponse(OtpErrorCode code, String correlationId, String message);

    protected abstract String queueName();

    protected Res run(Message amqpMessage, Class<Req> payloadType, BiFunction<String, Req, Res> serviceHandle) {
        String queue = queueName();
        Timer.Sample sample = metrics.startTimer();
        String correlationId = extractCorrelationId(amqpMessage);
        CorrelationIdMdc.put(correlationId);
        try {
            ConsumerParsedMessage<Req> result = MessageParser.parsedMessage(
                    amqpMessage, payloadType, jsonMapper);

            if (!idempotencyGuard.tryAcquire(correlationId)) {
                log.warn("Duplicate otp message discarded corrId={} queue={}", correlationId, queue);
                metrics.recordDuplicate(queue, sample);
                return errorResponse(OtpErrorCode.DUPLICATE_REQUEST, correlationId, "Duplicate request");
            }

            Res response = serviceHandle.apply(result.correlationId(), result.payload());
            metrics.recordSuccess(queue, sample);
            return response;

        } catch (RabbitConsumerFailedToParseException e) {
            log.error("Failed to parse otp message corrId={} queue={}", correlationId, queue, e);
            metrics.recordParseError(queue, sample);
            return errorResponse(OtpErrorCode.PARSE_MESSAGE, correlationId, ExceptionMessage.of(e));
        } catch (RuntimeException e) {
            log.error("Unexpected failure otp message corrId={} queue={}", correlationId, queue, e);
            metrics.recordInternalError(queue, sample);
            throw e;
        } finally {
            CorrelationIdMdc.clear();
        }
    }

    private static String extractCorrelationId(Message amqpMessage) {
        String headerId = amqpMessage.getMessageProperties() != null
                ? amqpMessage.getMessageProperties().getCorrelationId()
                : null;
        return headerId != null ? headerId : "UNKNOWN";
    }
}
