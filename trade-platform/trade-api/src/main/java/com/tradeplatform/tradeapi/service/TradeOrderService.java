package com.tradeplatform.tradeapi.service;

import com.tradeplatform.common.dto.OrderStatus;
import com.tradeplatform.common.dto.TradeOrderDTO;
import com.tradeplatform.common.exception.OrderProcessingException;
import com.tradeplatform.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for processing trade orders.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TradeOrderService {

    private final KafkaTemplate<String, TradeOrderDTO> kafkaTemplate;

    @Value("${spring.kafka.topic.trade-orders}")
    private String tradeOrdersTopic;

    /**
     * Creates a new trade order and publishes it to Kafka.
     *
     * @param orderDTO the trade order to create
     * @return the created trade order with updated fields
     * @throws ValidationException if the order fails validation
     * @throws OrderProcessingException if there is an error publishing the order to Kafka
     */
    public TradeOrderDTO createOrder(TradeOrderDTO orderDTO) {
        validateOrder(orderDTO);

        // Set initial values for new order
        orderDTO.setOrderId(UUID.randomUUID());
        orderDTO.setStatus(OrderStatus.CREATED);
        orderDTO.setCreatedAt(LocalDateTime.now());
        orderDTO.setUpdatedAt(LocalDateTime.now());
        
        if (orderDTO.getQuantity() != null) {
            orderDTO.setFilledQuantity(0);
            orderDTO.setRemainingQuantity(orderDTO.getQuantity());
        }

        // Publish to Kafka
        try {
            CompletableFuture<SendResult<String, TradeOrderDTO>> future = 
                kafkaTemplate.send(tradeOrdersTopic, orderDTO.getOrderId().toString(), orderDTO);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Order sent to Kafka: {}", orderDTO.getOrderId());
                } else {
                    log.error("Failed to send order to Kafka: {}", orderDTO.getOrderId(), ex);
                }
            });
            
            // Update status to PENDING after successful submission
            orderDTO.setStatus(OrderStatus.PENDING);
            orderDTO.setUpdatedAt(LocalDateTime.now());
            
            return orderDTO;
        } catch (Exception e) {
            log.error("Error publishing order to Kafka", e);
            throw new OrderProcessingException("Failed to publish order to processing queue", 
                    orderDTO.getOrderId(), "Kafka publishing error");
        }
    }

    /**
     * Validates a trade order.
     *
     * @param orderDTO the trade order to validate
     * @throws ValidationException if the order fails validation
     */
    private void validateOrder(TradeOrderDTO orderDTO) {
        Map<String, String> errors = new HashMap<>();

        if (orderDTO.getUserId() == null || orderDTO.getUserId().trim().isEmpty()) {
            errors.put("userId", "User ID is required");
        }

        if (orderDTO.getSymbol() == null || orderDTO.getSymbol().trim().isEmpty()) {
            errors.put("symbol", "Symbol is required");
        }

        if (orderDTO.getOrderType() == null) {
            errors.put("orderType", "Order type is required");
        }

        if (orderDTO.getQuantity() == null || orderDTO.getQuantity() <= 0) {
            errors.put("quantity", "Quantity must be positive");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Trade order validation failed", errors);
        }
    }
}