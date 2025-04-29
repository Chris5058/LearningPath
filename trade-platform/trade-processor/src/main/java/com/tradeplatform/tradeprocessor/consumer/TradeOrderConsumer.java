package com.tradeplatform.tradeprocessor.consumer;

import com.tradeplatform.common.dto.TradeOrderDTO;
import com.tradeplatform.common.exception.OrderProcessingException;
import com.tradeplatform.tradeprocessor.service.TradeOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for trade orders.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TradeOrderConsumer {

    private final TradeOrderService tradeOrderService;

    /**
     * Consumes trade orders from the Kafka topic and processes them.
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

        try {
            TradeOrderDTO processedOrder = tradeOrderService.processOrder(orderDTO);
            log.info("Successfully processed trade order: {}, new status: {}", 
                    processedOrder.getOrderId(), processedOrder.getStatus());
        } catch (OrderProcessingException e) {
            log.error("Error processing trade order: {}, reason: {}", 
                    orderDTO.getOrderId(), e.getReason(), e);
            // The error handler in KafkaConfig will handle retries and DLT publishing
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error processing trade order: {}", orderDTO.getOrderId(), e);
            // The error handler in KafkaConfig will handle retries and DLT publishing
            throw e;
        }
    }
}