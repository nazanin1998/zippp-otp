package com.zippp.otp.provider;

import com.zippp.otpapi.enums.OtpChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stub SMS provider. Logs the code instead of calling Twilio/SMS.ir/etc.
 * Replace with a real adapter when integrating.
 */
@Component
public class SmsProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(SmsProvider.class);

    @Override
    public OtpChannel channel() {
        return OtpChannel.SMS;
    }

    @Override
    public void send(String target, String code) {
        log.info("[SMS stub] target={} code={}", target, code);
        // TODO: integrate Twilio / Kavenegar / SMS.ir here.
        // On failure, throw NotificationDeliveryException so the listener
        // can NACK and let the message go to DLQ after retry exhaustion.
    }
}
