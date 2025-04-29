package com.tradeplatform.portfolioservice.config;

import com.tradeplatform.common.dto.TradeOrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for Kafka consumer settings in the Portfolio Service.
 * This class sets up the Kafka consumer factory, listener container factory,
 * and error handling for consuming trade order events.
 */
@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    @Value("${spring.kafka.topic.dead-letter}")
    private String deadLetterTopic;

    @Value("${spring.kafka.consumer.max-attempts:3}")
    private int maxAttempts;

    @Value("${spring.kafka.consumer.backoff-interval:1000}")
    private long backoffInterval;

    /**
     * Creates a Kafka producer factory for sending messages to the dead-letter topic.
     *
     * @return the producer factory
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Creates a Kafka template for sending messages to the dead-letter topic.
     *
     * @return the Kafka template
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Creates a Kafka consumer factory for TradeOrderDTO objects.
     *
     * @return the consumer factory
     */
    @Bean
    public ConsumerFactory<String, TradeOrderDTO> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TradeOrderDTO.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.tradeplatform.common.dto");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Creates a Kafka listener container factory for TradeOrderDTO objects.
     *
     * @return the listener container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TradeOrderDTO> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TradeOrderDTO> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }

    /**
     * Creates an error handler for Kafka consumer errors.
     *
     * @return the error handler
     */
    @Bean
    public DefaultErrorHandler errorHandler() {
        // Configure the recoverer to send failed messages to the DLT
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate(),
                (consumerRecord, exception) -> {
                    log.error("Error processing record: {}", consumerRecord.toString(), exception);
                    return new org.apache.kafka.common.TopicPartition(deadLetterTopic, 0);
                });

        // Configure the backoff policy for retries
        FixedBackOff backOff = new FixedBackOff(backoffInterval, maxAttempts - 1);

        // Create the error handler with the recoverer and backoff policy
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // Add exception types that should not be retried
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

        return errorHandler;
    }
}
