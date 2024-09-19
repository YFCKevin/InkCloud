package com.yfckevin.InkCloud.config;

import com.yfckevin.InkCloud.ConfigProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    private final ConfigProperties configProperties;
    public static final String LLM_QUEUE = "llm-queue";
    public static final String AUDIO_QUEUE = "audio-queue";
    public static final String IMAGE_QUEUE = "image-queue";
    public static final String VIDEO_QUEUE = "video-queue";
    public static final String ERROR_QUEUE = "error-queue";
    public static final String WORKFLOW_EXCHANGE = "workflow-exchange";

    public RabbitMQConfig(ConfigProperties configProperties) {
        this.configProperties = configProperties;
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(configProperties.getRabbitmqHost());
        connectionFactory.setPort(5672);
        connectionFactory.setUsername(configProperties.getRabbitmqUserName());
        connectionFactory.setPassword(configProperties.getRabbitmqPassword());
        return connectionFactory;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter(); // JSON 消息转换器
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public Queue errorQueue() {
        return new Queue(ERROR_QUEUE, false);
    }

    @Bean
    public Queue llmQueue() {
        return new Queue(LLM_QUEUE, false);
    }

    @Bean
    public Queue audioQueue() {
        return new Queue(AUDIO_QUEUE, false);
    }

    @Bean
    public Queue imageQueue() {
        return new Queue(IMAGE_QUEUE, false);
    }

    @Bean
    public Queue videoQueue() {
        return new Queue(VIDEO_QUEUE, false);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(WORKFLOW_EXCHANGE);
    }

    @Bean
    public Binding bindingErrorQueue(@Qualifier("errorQueue") Queue errorQueue, TopicExchange exchange) {
        return BindingBuilder.bind(errorQueue).to(exchange).with("error.#");
    }

    @Bean
    public Binding bindingLLM(@Qualifier("llmQueue") Queue llmQueue, TopicExchange exchange) {
        return BindingBuilder.bind(llmQueue).to(exchange).with("workflow.llm");
    }

    @Bean
    public Binding bindingAudio(@Qualifier("audioQueue") Queue audioQueue, TopicExchange exchange) {
        return BindingBuilder.bind(audioQueue).to(exchange).with("workflow.audio");
    }

    @Bean
    public Binding bindingImage(@Qualifier("imageQueue") Queue imageQueue, TopicExchange exchange) {
        return BindingBuilder.bind(imageQueue).to(exchange).with("workflow.image");
    }

    @Bean
    public Binding bindingVideo(@Qualifier("videoQueue") Queue videoQueue, TopicExchange exchange) {
        return BindingBuilder.bind(videoQueue).to(exchange).with("workflow.video");
    }
}
