package com.zippp.otp;

import com.zippp.otp.config.properties.OtpProperties;
import com.zippp.otp.config.properties.RabbitOtpProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({RabbitOtpProperties.class, OtpProperties.class})
public class OtpApplication {

    public static void main(String[] args) {
        SpringApplication.run(OtpApplication.class, args);
    }

}
