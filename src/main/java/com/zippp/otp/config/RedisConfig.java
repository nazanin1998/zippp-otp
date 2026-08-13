package com.zippp.otp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.json.JsonMapper;


@Configuration
public class RedisConfig {

    public static final String OTP_REQUEST_KEY_PREFIX = "otp:request:";
    public static final String OTP_PASSED_KEY_PREFIX = "otp:passed:";

    @Bean
    public RedisTemplate<String, Object> otpRedisTemplate(
            RedisConnectionFactory connectionFactory,
            JsonMapper jsonMapper) {

        GenericJacksonJsonRedisSerializer valueSerializer =
                new GenericJacksonJsonRedisSerializer(jsonMapper);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.setEnableTransactionSupport(false);
        template.afterPropertiesSet();
        return template;
    }
}
