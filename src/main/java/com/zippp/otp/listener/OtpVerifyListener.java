package com.zippp.otp.listener;

import com.zippp.otp.listener.support.AbstractOtpListener;
import com.zippp.otp.listener.support.IdempotencyGuard;
import com.zippp.otp.listener.support.ListenerMetrics;
import com.zippp.otp.service.OtpVerifyService;
import com.zippp.otpapi.dto.message.OtpVerifyRequestMessage;
import com.zippp.otpapi.dto.message.OtpVerifyResponseMessage;
import com.zippp.otpapi.enums.OtpErrorCode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;


@Component
public class OtpVerifyListener extends AbstractOtpListener<OtpVerifyRequestMessage, OtpVerifyResponseMessage> {

    private static final String QUEUE = "otp.verify";

    private final OtpVerifyService service;

    public OtpVerifyListener(JsonMapper jsonMapper,
                             IdempotencyGuard idempotencyGuard,
                             ListenerMetrics metrics,
                             OtpVerifyService service) {
        super(jsonMapper, idempotencyGuard, metrics);
        this.service = service;
    }

    @RabbitListener(queues = "${otp.rabbit.verify.queue}")
    public OtpVerifyResponseMessage onMessage(Message amqpMessage) {
        return run(amqpMessage, OtpVerifyRequestMessage.class, service::handle);
    }

    @Override
    protected String queueName() {
        return QUEUE;
    }

    @Override
    protected OtpVerifyResponseMessage errorResponse(OtpErrorCode code, String correlationId, String message) {
        return OtpVerifyResponseMessage.error(code, message, correlationId, null);
    }
}
