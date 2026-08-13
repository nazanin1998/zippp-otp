package com.zippp.otp.config;

import com.zippp.otp.config.properties.RabbitOtpProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.json.JsonMapper;

/**
 * <pre>
 *   otp.exchange (topic)
 *     ├─ otp.request          (queue) ── DLX ──► otp.request.dlq
 *     └─ otp.verify          (queue) ── DLX ──► otp.verify.dlq
 * </pre>
 * RPC reply uses the built-in {@code amq.rabbitmq.reply-to} queue plus a
 * {@code correlationId} header — see {@code spring.rabbitmq.template.reply-timeout}
 */
@Configuration
@RequiredArgsConstructor
public class RabbitConfig {

    private final RabbitOtpProperties rabbitProperties;

    // ------------------------------------------------------------------------
    //  Exchanges
    // ------------------------------------------------------------------------

    @Bean
    public TopicExchange otpExchange() {
        return new TopicExchange(rabbitProperties.getExchange(), /*durable*/ true, /*autoDelete*/ false);
    }

    @Bean
    public TopicExchange otpDlx() {
        return new TopicExchange(rabbitProperties.getDlx(), true, false);
    }

    // ------------------------------------------------------------------------
    //  Main queues (with DLX wiring — dead-letter to DLX with same routing key)
    // ------------------------------------------------------------------------

    @Bean
    public Queue requestQueue() {
        return QueueBuilder.durable(rabbitProperties.getRequest().getQueue())
                .withArgument("x-dead-letter-exchange", rabbitProperties.getDlx())
                .withArgument("x-dead-letter-routing-key", rabbitProperties.getRequest().getRoutingKey())
                .build();
    }

    @Bean
    public Queue verifyQueue() {
        return QueueBuilder.durable(rabbitProperties.getVerify().getQueue())
                .withArgument("x-dead-letter-exchange", rabbitProperties.getDlx())
                .withArgument("x-dead-letter-routing-key", rabbitProperties.getVerify().getRoutingKey())
                .build();
    }

    // ------------------------------------------------------------------------
    //  Dead-letter queues (catch permanently-failed messages for inspection)
    // ------------------------------------------------------------------------

    @Bean
    public Queue requestDlq() {
        return QueueBuilder.durable(rabbitProperties.getRequest().getDlq()).build();
    }

    @Bean
    public Queue verifyDlq() {
        return QueueBuilder.durable(rabbitProperties.getVerify().getDlq()).build();
    }

    // ------------------------------------------------------------------------
    //  Bindings
    // ------------------------------------------------------------------------

    @Bean
    public Binding bindRequest(Queue requestQueue, TopicExchange otpExchange) {
        return BindingBuilder.bind(requestQueue).to(otpExchange).with(rabbitProperties.getRequest().getRoutingKey());
    }

    @Bean
    public Binding bindVerify(Queue verifyQueue, TopicExchange otpExchange) {
        return BindingBuilder.bind(verifyQueue).to(otpExchange).with(rabbitProperties.getVerify().getRoutingKey());
    }

    @Bean
    public Binding bindRequestDlq(Queue requestDlq, TopicExchange otpDlx) {
        return BindingBuilder.bind(requestDlq).to(otpDlx).with(rabbitProperties.getRequest().getRoutingKey());
    }

    @Bean
    public Binding bindVerifyDlq(Queue verifyDlq, TopicExchange otpDlx) {
        return BindingBuilder.bind(verifyDlq).to(otpDlx).with(rabbitProperties.getVerify().getRoutingKey());
    }

    // ------------------------------------------------------------------------
    //  Message converter (Jackson + java.time)
    // ------------------------------------------------------------------------

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder()
                .findAndAddModules().build();
    }


    @Bean
    public MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        ((DefaultJacksonJavaTypeMapper) converter.getJavaTypeMapper())
                .addTrustedPackages("com.zippp.otpapi.dto.message", "com.zippp.otpapi.dto.request");
        return converter;
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
            MessageConverter jsonMessageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setConcurrentConsumers(rabbitProperties.getMinConcurrency());
        factory.setMaxConcurrentConsumers(rabbitProperties.getMaxConcurrency());
        factory.setPrefetchCount(rabbitProperties.getPrefetch());
        // Manual ack — we want explicit control over retries / DLQ routing.
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        // Default reject behavior: requeue=false → message goes to DLX (then DLQ).
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

}
