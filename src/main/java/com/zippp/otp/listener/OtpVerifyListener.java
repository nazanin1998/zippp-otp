package com.zippp.otp.listener;

import com.zippp.otp.service.OtpVerifyService;
import com.zippp.otpapi.dto.message.OtpRequestMessage;
import com.zippp.otpapi.dto.message.OtpResponseMessage;
import com.zippp.otpapi.dto.message.OtpVerifyRequestMessage;
import com.zippp.otpapi.dto.message.OtpVerifyResponseMessage;
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
public class OtpVerifyListener {

    private final OtpVerifyService service;
    private final JsonMapper jsonMapper;

    @RabbitListener(queues = "${otp.rabbit.verify.queue}")
    public OtpVerifyResponseMessage onMessage(Message amqpMessage) {
        String correlationId = "UNKNOWN";
        try {
            ConsumerParsedMessage<OtpVerifyRequestMessage> result = MessageParser.parsedMessage(
                    amqpMessage, OtpVerifyRequestMessage.class, jsonMapper);

            correlationId = result.correlationId();

            return service.handle(result.correlationId(), result.payload());

        } catch (RabbitConsumerFailedToParseException e) {
            log.error("Failed to parse otp verify corrId={}", correlationId, e);
            return getOtpVerifyErrMessage(OtpErrorCode.PARSE_MESSAGE, correlationId, ExceptionUtils.getMessage(e.getCause()));
        } catch (RuntimeException e) {
            log.error("Failed to process otp verify corrId={}", correlationId, e);
            return getOtpVerifyErrMessage(OtpErrorCode.INTERNAL_ERROR, correlationId, ExceptionUtils.getMessage(e));
        }
    }
    private static @NonNull OtpVerifyResponseMessage getOtpVerifyErrMessage(
            OtpErrorCode errorCode,
            String correlationId,
            String message) {
        return OtpVerifyResponseMessage.error(
                errorCode,
                message,
                correlationId);
    }
}
