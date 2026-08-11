package com.zippp.otp.provider;

/**
 * Thrown by {@link NotificationProvider} when delivery fails.
 * Marked as a runtime exception so it can propagate through Spring AMQP
 * listener methods where checked exceptions would be awkward.
 */
public class NotificationDeliveryException extends RuntimeException {
    public NotificationDeliveryException(String message) {
        super(message);
    }
    public NotificationDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
