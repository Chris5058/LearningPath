package com.tradeplatform.tradeapi.config;

import com.tradeplatform.common.dto.TradeOrderDTO;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for Kafka producer settings.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.topic.trade-orders}")
    private String tradeOrdersTopic;

    @Value("${spring.kafka.topic.partitions:3}")
    private int partitions;

    @Value("${spring.kafka.topic.replication-factor:1}")
    private short replicationFactor;

    /**
     * Creates a Kafka producer factory for TradeOrderDTO objects.
     *
     * @return the producer factory
     */
    @Bean
    public ProducerFactory<String, TradeOrderDTO> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Enable idempotence for exactly-once semantics
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Creates a Kafka template for sending TradeOrderDTO objects.
     *
     * @return the Kafka template
     */
    @Bean
    public KafkaTemplate<String, TradeOrderDTO> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Creates the trade orders topic if it doesn't exist.
     *
     * @return the topic configuration
     */
    @Bean
    public NewTopic tradeOrdersTopic() {
        return TopicBuilder.name(tradeOrdersTopic)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }
}