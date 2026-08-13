package com.zippp.otp.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@Getter
@ConfigurationProperties(prefix = "otp.rabbit")
@Validated
@AllArgsConstructor
public class RabbitOtpProperties {

    @NotNull
    private final Integer minConcurrency;

    @NotNull
    private final Integer maxConcurrency;

    @NotNull
    private final Integer prefetch;

    @NotBlank
    private final String exchange;

    @NotBlank
    private final String dlx;

    @NonNull
    private final Action request;

    @NonNull
    private final Action verify;

    @Getter
    @Validated
    @AllArgsConstructor
    public static class Action {

        @NotBlank
        private final String queue;

        @NotBlank
        private final String routingKey;

        @NotBlank
        private final String dlq;
    }

}