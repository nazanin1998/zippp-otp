package com.zippp.otp.provider;

import com.zippp.otpapi.enums.OtpChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmsProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(SmsProvider.class);

    @Override
    public OtpChannel channel() {
        return OtpChannel.SMS;
    }

    @Override
    public void send(String target, String message) {
        log.info("[SMS stub] target={} message={}", target, message);
        // TODO: integrate Twilio / Kavenegar / SMS.ir here.
    }
}
