package com.zippp.otp.listener;

import com.zippp.otp.service.OtpRequestService;
import com.zippp.otpapi.dto.message.OtpRequestMessage;
import com.zippp.otpapi.dto.message.OtpResponseMessage;
import com.zippp.otpapi.enums.OtpErrorCode;
import com.zippp.rabbitconsumer.exception.RabbitConsumerFailedToParseException;
import com.zippp.rabbitconsumer.handler.MessageParser;
import com.zippp.rabbitconsumer.model.ConsumerParsedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;


@Slf4j
@Component
@RequiredArgsConstructor
public class OtpRequestListener {

    private final OtpRequestService service;
    private final JsonMapper jsonMapper;

    @RabbitListener(queues = "${otp.rabbit.request.queue}")
    public OtpResponseMessage onMessage(Message amqpMessage) {
        String correlationId = null;
        try {
            ConsumerParsedMessage<OtpRequestMessage> result = MessageParser.parsedMessage(
                    amqpMessage, OtpRequestMessage.class, jsonMapper);

            correlationId = result.correlationId();
            log.debug("Received Signup Request message corrId={} payload={}", result.correlationId(), result.payload());

            return service.handle(result.correlationId(), result.payload());

        } catch (RabbitConsumerFailedToParseException e) {
            log.error("Failed to parse signup request corrId={}", correlationId == null ? "UNKOWN" : correlationId, e);
            return getOtpErrMessage(OtpErrorCode.PARSE_MESSAGE, correlationId, ExceptionUtils.getMessage(e.getCause()));
        } catch (RuntimeException e) {
            log.error("Failed to process signup request corrId={}", correlationId == null ? "UNKOWN" : correlationId, e);
            return getOtpErrMessage(OtpErrorCode.INTERNAL_ERROR, correlationId, ExceptionUtils.getMessage(e));
        }
    }

    private static @NonNull OtpResponseMessage getOtpErrMessage(
            OtpErrorCode errorCode,
            String correlationId,
            String message) {
        return OtpResponseMessage.error(
                correlationId == null ? "UNKOWN" : correlationId,
                errorCode,
                message);
    }
}
