package com.zippp.otp.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@Setter
@Getter
@ConfigurationProperties(prefix = "otp")
@Validated
public class OtpProperties {
    private Integer codeLength = 6;
    private Integer bound = 1_000_000;
}