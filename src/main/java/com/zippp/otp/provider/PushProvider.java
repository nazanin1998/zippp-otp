package com.zippp.otp.provider;

import com.zippp.otpapi.enums.OtpChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stub push notification provider. Logs the code instead of calling
 * Firebase / OneSignal / APNs. Replace with a real adapter when integrating.
 */
@Component
public class PushProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(PushProvider.class);

    @Override
    public OtpChannel channel() {
        return OtpChannel.PUSH;
    }

    @Override
    public void send(String target, String code) {
        log.info("[PUSH stub] target={} code={}", target, code);
        // TODO: integrate Firebase / OneSignal / APNs here.
    }
}
