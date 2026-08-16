package com.zippp.otp.provider;

import com.zippp.otpapi.enums.OtpChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class PushProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(PushProvider.class);

    @Override
    public OtpChannel channel() {
        return OtpChannel.PUSH;
    }

    @Override
    public void send(String target, String message) {
        log.info("[PUSH stub] target={} message={}", target, message);
        // TODO: integrate Firebase / OneSignal / APNs here.
    }
}
