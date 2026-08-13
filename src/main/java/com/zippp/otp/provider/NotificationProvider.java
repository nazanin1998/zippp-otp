package com.zippp.otp.provider;

import com.zippp.otpapi.enums.OtpChannel;

/**
 * Strategy for delivering an OTP via an external channel.
 *
 * Implementations are responsible for:
 *   - Translating the {@code target} (phone / device token) into a provider call
 *   - Throwing {@link NotificationDeliveryException} on transient/permanent failure
 *   - Returning normally on success (no return value)
 *
 * Real implementations (Twilio, Firebase, Kavenegar, …) plug in here.
 */
public interface NotificationProvider {

    OtpChannel channel();

    void send(String target, String message) throws NotificationDeliveryException;
}
