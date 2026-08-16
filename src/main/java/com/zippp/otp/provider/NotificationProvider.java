package com.zippp.otp.provider;

import com.zippp.otpapi.enums.OtpChannel;


public interface NotificationProvider {

    OtpChannel channel();

    void send(String target, String message) throws NotificationDeliveryException;
}
