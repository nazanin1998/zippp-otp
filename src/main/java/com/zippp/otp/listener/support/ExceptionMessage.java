package com.zippp.otp.listener.support;

import java.util.Optional;


public final class ExceptionMessage {

    private ExceptionMessage() {}

    public static String of(Throwable t) {
        return Optional.ofNullable(t)
                .map(Throwable::getMessage)
                .filter(s -> !s.isBlank())
                .orElseGet(() -> t.getClass().getSimpleName());
    }
}
