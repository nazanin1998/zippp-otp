package com.zippp.otp.listener;

import com.zippp.otp.config.RabbitConfig;
import com.zippp.otp.service.OtpRequestService;
import com.zippp.otpapi.dto.message.OtpRequestMessage;
import com.zippp.otpapi.dto.message.OtpResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens on {@code otp.signup.request} and returns the dispatch result
 * via the RPC reply queue.
 */
@Component
public class SignupRequestListener {

    private static final Logger log = LoggerFactory.getLogger(SignupRequestListener.class);

    private final OtpRequestService service;

    public SignupRequestListener(OtpRequestService service) {
        this.service = service;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_SIGNUP_REQUEST)
    public OtpResponseMessage onMessage(OtpRequestMessage message) {
        try {
            return service.handle(message);
        } catch (RuntimeException ex) {
            log.error("Failed to process signup request corrId={}",
                    message.correlationId(), ex);
            return OtpResponseMessage.error(
                    message.correlationId(),
                    com.zippp.otpapi.enums.OtpErrorCode.INTERNAL_ERROR,
                    ex.getMessage());
        }
    }
}
