package com.zippp.otp.listener;

import com.zippp.otp.listener.support.AbstractOtpListener;
import com.zippp.otp.listener.support.IdempotencyGuard;
import com.zippp.otp.listener.support.ListenerMetrics;
import com.zippp.otp.service.OtpRequestService;
import com.zippp.otpapi.dto.message.OtpRequestMessage;
import com.zippp.otpapi.dto.message.OtpResponseMessage;
import com.zippp.otpapi.enums.OtpErrorCode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;


@Component
public class OtpRequestListener extends AbstractOtpListener<OtpRequestMessage, OtpResponseMessage> {

    private static final String QUEUE = "otp.request";

    private final OtpRequestService service;

    public OtpRequestListener(JsonMapper jsonMapper,
                              IdempotencyGuard idempotencyGuard,
                              ListenerMetrics metrics,
                              OtpRequestService service) {
        super(jsonMapper, idempotencyGuard, metrics);
        this.service = service;
    }

    @RabbitListener(queues = "${otp.rabbit.request.queue}")
    public OtpResponseMessage onMessage(Message amqpMessage) {
        return run(amqpMessage, OtpRequestMessage.class, service::handle);
    }

    @Override
    protected String queueName() {
        return QUEUE;
    }

    @Override
    protected OtpResponseMessage errorResponse(OtpErrorCode code, String correlationId, String message) {
        return OtpResponseMessage.error(correlationId, code, message);
    }
}
