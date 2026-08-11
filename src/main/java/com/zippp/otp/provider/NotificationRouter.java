package com.zippp.otp.provider;

import com.zippp.otpapi.enums.OtpChannel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Routes an {@link OtpChannel} to the registered {@link NotificationProvider}.
 *
 * If you add new channels (EMAIL, etc.), drop in a new
 * {@code @Component implements NotificationProvider} — no changes here.
 */
@Component
public class NotificationRouter {

    private final Map<OtpChannel, NotificationProvider> providers;

    public NotificationRouter(List<NotificationProvider> providers) {
        this.providers = providers.stream()
                .collect(Collectors.toMap(NotificationProvider::channel, Function.identity()));
    }

    public NotificationProvider resolve(OtpChannel channel) {
        NotificationProvider p = providers.get(channel);
        if (p == null) {
            throw new IllegalStateException(
                    "No NotificationProvider registered for channel=" + channel);
        }
        return p;
    }
}
