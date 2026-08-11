package com.zippp.otp.listener;

import com.zippp.otp.config.RabbitConfig;
import com.zippp.otp.service.OtpVerifyService;
import com.zippp.otpapi.dto.message.OtpResponseMessage;
import com.zippp.otpapi.dto.request.OtpVerifyRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens on {@code otp.signup.verify} and returns the verification result
 * via the RPC reply queue.
 */
@Component
public class SignupVerifyListener {

    private static final Logger log = LoggerFactory.getLogger(SignupVerifyListener.class);

    private final OtpVerifyService service;

    public SignupVerifyListener(OtpVerifyService service) {
        this.service = service;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_SIGNUP_VERIFY)
    public OtpResponseMessage onMessage(OtpVerifyRequest message) {
        try {
            return service.handle(message);
        } catch (RuntimeException ex) {
            log.error("Failed to process signup verify corrId={}",
                    message.correlationId(), ex);
            return OtpResponseMessage.error(
                    message.correlationId(),
                    com.zippp.otpapi.enums.OtpErrorCode.INTERNAL_ERROR,
                    ex.getMessage());
        }
    }
}
