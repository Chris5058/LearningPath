package com.tradeplatform.tradeprocessor.service;

import com.tradeplatform.common.dto.OrderStatus;
import com.tradeplatform.common.dto.TradeOrderDTO;
import com.tradeplatform.common.exception.OrderProcessingException;
import com.tradeplatform.common.exception.ResourceNotFoundException;
import com.tradeplatform.tradeprocessor.entity.TradeOrder;
import com.tradeplatform.tradeprocessor.repository.TradeOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Service for processing trade orders.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TradeOrderService {

    private final TradeOrderRepository tradeOrderRepository;
    private final Random random = new Random();

    /**
     * Processes a new trade order.
     *
     * @param orderDTO the trade order to process
     * @return the processed trade order
     * @throws OrderProcessingException if there is an error processing the order
     */
    @Transactional
    public TradeOrderDTO processOrder(TradeOrderDTO orderDTO) {
        log.info("Processing order: {}", orderDTO.getOrderId());

        // Convert DTO to entity
        TradeOrder order = convertToEntity(orderDTO);
        
        // Initialize processing metadata
        order.setProcessingAttempts(order.getProcessingAttempts() != null ? order.getProcessingAttempts() + 1 : 1);
        order.setLastProcessingAttempt(LocalDateTime.now());
        order.setStatus(OrderStatus.PROCESSING);
        order.setUpdatedAt(LocalDateTime.now());

        // Save the order to the database
        order = tradeOrderRepository.save(order);

        try {
            // Simulate order matching and execution
            simulateOrderExecution(order);

            // Update order status and save
            order.setStatus(OrderStatus.FILLED);
            order.setExecutedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            order.setLastErrorMessage(null);
            order = tradeOrderRepository.save(order);

            log.info("Order processed successfully: {}", order.getOrderId());
            return convertToDTO(order);
        } catch (Exception e) {
            // Handle processing error
            log.error("Error processing order: {}", order.getOrderId(), e);
            order.setStatus(OrderStatus.FAILED);
            order.setUpdatedAt(LocalDateTime.now());
            order.setLastErrorMessage(e.getMessage());
            tradeOrderRepository.save(order);
            throw new OrderProcessingException("Failed to process order", order.getOrderId(), e.getMessage());
        }
    }

    /**
     * Retrieves a trade order by ID.
     *
     * @param orderId the order ID
     * @return the trade order
     * @throws ResourceNotFoundException if the order is not found
     */
    public TradeOrderDTO getOrderById(UUID orderId) {
        TradeOrder order = tradeOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("TradeOrder", orderId.toString()));
        return convertToDTO(order);
    }

    /**
     * Retrieves all trade orders for a user.
     *
     * @param userId the user ID
     * @return the list of trade orders
     */
    public List<TradeOrderDTO> getOrdersByUserId(String userId) {
        return tradeOrderRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * Retrieves all trade orders with a specific status.
     *
     * @param status the order status
     * @return the list of trade orders
     */
    public List<TradeOrderDTO> getOrdersByStatus(OrderStatus status) {
        return tradeOrderRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * Simulates the execution of a trade order.
     * In a real system, this would involve matching with other orders, checking balances, etc.
     *
     * @param order the order to execute
     */
    private void simulateOrderExecution(TradeOrder order) {
        // Simulate some processing time
        try {
            Thread.sleep(100 + random.nextInt(500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OrderProcessingException("Order execution was interrupted", order.getOrderId());
        }

        // Simulate market price for the order
        BigDecimal marketPrice;
        if (order.getPrice() != null) {
            // Use the order price with a small random variation
            double variation = 0.98 + (random.nextDouble() * 0.04); // +/- 2%
            marketPrice = order.getPrice().multiply(BigDecimal.valueOf(variation))
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            // Generate a random price between 10 and 1000
            marketPrice = BigDecimal.valueOf(10 + random.nextInt(990))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Set execution details
        order.setExecutionPrice(marketPrice);
        order.setFilledQuantity(order.getQuantity());
        order.setRemainingQuantity(0);

        // Randomly fail some orders for testing
        if (random.nextInt(100) < 5) { // 5% failure rate
            throw new OrderProcessingException("Simulated random execution failure", order.getOrderId());
        }
    }

    /**
     * Converts a TradeOrderDTO to a TradeOrder entity.
     *
     * @param dto the DTO to convert
     * @return the entity
     */
    private TradeOrder convertToEntity(TradeOrderDTO dto) {
        return TradeOrder.builder()
                .orderId(dto.getOrderId())
                .userId(dto.getUserId())
                .symbol(dto.getSymbol())
                .orderType(dto.getOrderType())
                .status(dto.getStatus())
                .quantity(dto.getQuantity())
                .price(dto.getPrice())
                .stopPrice(dto.getStopPrice())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .executedAt(dto.getExecutedAt())
                .expiresAt(dto.getExpiresAt())
                .executionPrice(dto.getExecutionPrice())
                .filledQuantity(dto.getFilledQuantity())
                .remainingQuantity(dto.getRemainingQuantity())
                .notes(dto.getNotes())
                .processingAttempts(0)
                .lastErrorMessage(null)
                .lastProcessingAttempt(null)
                .build();
    }

    /**
     * Converts a TradeOrder entity to a TradeOrderDTO.
     *
     * @param entity the entity to convert
     * @return the DTO
     */
    private TradeOrderDTO convertToDTO(TradeOrder entity) {
        return TradeOrderDTO.builder()
                .orderId(entity.getOrderId())
                .userId(entity.getUserId())
                .symbol(entity.getSymbol())
                .orderType(entity.getOrderType())
                .status(entity.getStatus())
                .quantity(entity.getQuantity())
                .price(entity.getPrice())
                .stopPrice(entity.getStopPrice())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .executedAt(entity.getExecutedAt())
                .expiresAt(entity.getExpiresAt())
                .executionPrice(entity.getExecutionPrice())
                .filledQuantity(entity.getFilledQuantity())
                .remainingQuantity(entity.getRemainingQuantity())
                .notes(entity.getNotes())
                .build();
    }
}