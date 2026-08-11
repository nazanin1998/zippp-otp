package com.zippp.otp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for the OTP consumer.
 *
 * <pre>
 *   otp.exchange (topic)
 *     ├─ otp.signup.request          (queue) ── DLX ──► otp.signup.request.dlq
 *     └─ otp.signup.verify          (queue) ── DLX ──► otp.signup.verify.dlq
 * </pre>
 *
 * Producers publish to {@code otp.exchange} with routing keys
 * {@code otp.signup.request} / {@code otp.signup.verify}.
 *
 * RPC reply uses the built-in {@code amq.rabbitmq.reply-to} queue plus a
 * {@code correlationId} header — see {@code spring.rabbitmq.template.reply-timeout}
 * in application.properties.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE              = "otp.exchange";
    public static final String DLX                   = "otp.exchange.dlx";

    public static final String QUEUE_SIGNUP_REQUEST  = "otp.signup.request";
    public static final String QUEUE_SIGNUP_VERIFY   = "otp.signup.verify";

    public static final String DLQ_SIGNUP_REQUEST    = "otp.signup.request.dlq";
    public static final String DLQ_SIGNUP_VERIFY     = "otp.signup.verify.dlq";

    public static final String RK_SIGNUP_REQUEST     = "otp.signup.request";
    public static final String RK_SIGNUP_VERIFY      = "otp.signup.verify";

    // ------------------------------------------------------------------------
    //  Exchanges
    // ------------------------------------------------------------------------

    @Bean
    public TopicExchange otpExchange() {
        return new TopicExchange(EXCHANGE, /*durable*/ true, /*autoDelete*/ false);
    }

    @Bean
    public TopicExchange otpDlx() {
        return new TopicExchange(DLX, true, false);
    }

    // ------------------------------------------------------------------------
    //  Main queues (with DLX wiring — dead-letter to DLX with same routing key)
    // ------------------------------------------------------------------------

    @Bean
    public Queue signupRequestQueue() {
        return QueueBuilder.durable(QUEUE_SIGNUP_REQUEST)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", RK_SIGNUP_REQUEST)
                .build();
    }

    @Bean
    public Queue signupVerifyQueue() {
        return QueueBuilder.durable(QUEUE_SIGNUP_VERIFY)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", RK_SIGNUP_VERIFY)
                .build();
    }

    // ------------------------------------------------------------------------
    //  Dead-letter queues (catch permanently-failed messages for inspection)
    // ------------------------------------------------------------------------

    @Bean
    public Queue signupRequestDlq() {
        return QueueBuilder.durable(DLQ_SIGNUP_REQUEST).build();
    }

    @Bean
    public Queue signupVerifyDlq() {
        return QueueBuilder.durable(DLQ_SIGNUP_VERIFY).build();
    }

    // ------------------------------------------------------------------------
    //  Bindings
    // ------------------------------------------------------------------------

    @Bean
    public Binding bindSignupRequest(Queue signupRequestQueue, TopicExchange otpExchange) {
        return BindingBuilder.bind(signupRequestQueue).to(otpExchange).with(RK_SIGNUP_REQUEST);
    }

    @Bean
    public Binding bindSignupVerify(Queue signupVerifyQueue, TopicExchange otpExchange) {
        return BindingBuilder.bind(signupVerifyQueue).to(otpExchange).with(RK_SIGNUP_VERIFY);
    }

    @Bean
    public Binding bindSignupRequestDlq(Queue signupRequestDlq, TopicExchange otpDlx) {
        return BindingBuilder.bind(signupRequestDlq).to(otpDlx).with(RK_SIGNUP_REQUEST);
    }

    @Bean
    public Binding bindSignupVerifyDlq(Queue signupVerifyDlq, TopicExchange otpDlx) {
        return BindingBuilder.bind(signupVerifyDlq).to(otpDlx).with(RK_SIGNUP_VERIFY);
    }

    // ------------------------------------------------------------------------
    //  Message converter (Jackson + java.time)
    // ------------------------------------------------------------------------

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter(objectMapper());
    }

    // ------------------------------------------------------------------------
    //  RabbitTemplate (with reply timeout for sendAndReceive)
    // ------------------------------------------------------------------------

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter,
            @Value("${spring.rabbitmq.template.reply-timeout:5s}") java.time.Duration replyTimeout) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setReplyTimeout(replyTimeout.toMillis());
        // Mandatory + callback for unroutable messages — surfaces misconfiguration loudly.
        template.setMandatory(true);
        return template;
    }

    // ------------------------------------------------------------------------
    //  Listener container factory
    // ------------------------------------------------------------------------

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter,
            @Value("${otp.rabbit.concurrency:2-8}") String concurrency,
            @Value("${otp.rabbit.prefetch:10}") int prefetch) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setConcurrentConsumers(parseMin(concurrency));
        factory.setMaxConcurrentConsumers(parseMax(concurrency));
        factory.setPrefetchCount(prefetch);
        // Manual ack — we want explicit control over retries / DLQ routing.
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        // Default reject behavior: requeue=false → message goes to DLX (then DLQ).
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    private static int parseMin(String range) {
        int dash = range.indexOf('-');
        return Integer.parseInt(dash < 0 ? range : range.substring(0, dash));
    }

    private static int parseMax(String range) {
        int dash = range.indexOf('-');
        return Integer.parseInt(dash < 0 ? range : range.substring(dash + 1));
    }
}
