package com.tradeplatform.portfolioservice.consumer;

import com.tradeplatform.common.dto.OrderStatus;
import com.tradeplatform.common.dto.TradeOrderDTO;
import com.tradeplatform.portfolioservice.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for trade orders in the Portfolio Service.
 * This consumer listens for filled trade orders and updates the user's portfolio accordingly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TradeOrderConsumer {

    private final PortfolioService portfolioService;

    /**
     * Consumes filled trade orders from the Kafka topic and updates the user's portfolio.
     *
     * @param orderDTO the trade order to process
     * @param key the message key
     * @param partition the partition from which the message was received
     * @param topic the topic from which the message was received
     * @param offset the offset of the message
     */
    @KafkaListener(
            topics = "${spring.kafka.topic.trade-orders}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTradeOrder(
            @Payload TradeOrderDTO orderDTO,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received trade order: key={}, partition={}, topic={}, offset={}", key, partition, topic, offset);
        log.debug("Trade order details: {}", orderDTO);

        // Only process filled orders
        if (orderDTO.getStatus() != OrderStatus.FILLED) {
            log.info("Ignoring trade order with status {}: {}", orderDTO.getStatus(), orderDTO.getOrderId());
            return;
        }

        try {
            log.info("Processing filled trade order: {}", orderDTO.getOrderId());
            portfolioService.updatePortfolio(orderDTO);
            log.info("Successfully updated portfolio for trade order: {}", orderDTO.getOrderId());
        } catch (Exception e) {
            log.error("Error updating portfolio for trade order: {}", orderDTO.getOrderId(), e);
            // The error handler in KafkaConfig will handle retries and DLT publishing
            throw e;
        }
    }
}
