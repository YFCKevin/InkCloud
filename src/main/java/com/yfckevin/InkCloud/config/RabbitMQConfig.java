package com.yfckevin.InkCloud.config;

import com.yfckevin.InkCloud.ConfigProperties;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    private final ConfigProperties configProperties;
    public static final String LLM_QUEUE = "llm.queue";
    public static final String AUDIO_QUEUE = "audio.queue";
    public static final String IMAGE_QUEUE = "image.queue";
    public static final String VIDEO_QUEUE = "video.queue";
    public static final String ERROR_QUEUE = "error.queue";
    public static final String DELAY_QUEUE = "delay.queue";
    public static final String WORKFLOW_EXCHANGE = "workflow-exchange";
    public static final String ERROR_EXCHANGE = "error-exchange";
    public static final String DELAY_EXCHANGE = "delay-exchange";


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
        return new Queue(ERROR_QUEUE, true);
    }

    @Bean
    public Queue llmQueue() {
        return new Queue(LLM_QUEUE, true);
    }

    @Bean
    public Queue audioQueue() {
        return new Queue(AUDIO_QUEUE, true);
    }

    @Bean
    public Queue imageQueue() {
        return new Queue(IMAGE_QUEUE, true);
    }

    @Bean
    public Queue videoQueue() {
        return new Queue(VIDEO_QUEUE, true);
    }

    @Bean
    public Queue delayQueue() {
        return new Queue(DELAY_QUEUE, true);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(WORKFLOW_EXCHANGE);
    }

    @Bean
    public TopicExchange errorExchange() {
        return new TopicExchange(ERROR_EXCHANGE);
    }

    @Bean
    public TopicExchange delayExchange(){
        return ExchangeBuilder
                .topicExchange(DELAY_EXCHANGE)
                .delayed()
                .durable(true)
                .build();
    }

    // 配置失敗重試策略：將失敗策略改為RepublishMessageRecoverer
    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, ERROR_EXCHANGE, "error.#");
    }

    @Bean
    public Binding bindingError(@Qualifier("errorQueue") Queue errorQueue, @Qualifier("errorExchange") TopicExchange errorExchange) {
        return BindingBuilder.bind(errorQueue).to(errorExchange).with("error.#");
    }

    @Bean
    public Binding bindingLLM(@Qualifier("llmQueue") Queue llmQueue, @Qualifier("exchange") TopicExchange exchange) {
        return BindingBuilder.bind(llmQueue).to(exchange).with("workflow.llm");
    }

    @Bean
    public Binding bindingAudio(@Qualifier("audioQueue") Queue audioQueue, @Qualifier("exchange") TopicExchange exchange) {
        return BindingBuilder.bind(audioQueue).to(exchange).with("workflow.audio");
    }

    @Bean
    public Binding bindingImage(@Qualifier("imageQueue") Queue imageQueue, @Qualifier("exchange") TopicExchange exchange) {
        return BindingBuilder.bind(imageQueue).to(exchange).with("workflow.image");
    }

    @Bean
    public Binding bindingVideo(@Qualifier("videoQueue") Queue videoQueue, @Qualifier("exchange") TopicExchange exchange) {
        return BindingBuilder.bind(videoQueue).to(exchange).with("workflow.video");
    }

    @Bean
    public Binding bindingDelay(@Qualifier("delayQueue") Queue delayQueue, @Qualifier("delayExchange") TopicExchange delayExchange) {
        return BindingBuilder.bind(delayQueue).to(delayExchange).with("notice.#");
    }
}
