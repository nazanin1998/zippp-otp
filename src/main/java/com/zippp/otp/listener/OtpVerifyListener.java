package com.zippp.otp.listener;

import com.zippp.otp.service.OtpVerifyService;
import com.zippp.otpapi.dto.message.OtpResponseMessage;
import com.zippp.otpapi.dto.request.OtpVerifyRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class OtpVerifyListener {

    private final OtpVerifyService service;

    @RabbitListener(queues = "${otp.rabbit.verify.queue}")
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
