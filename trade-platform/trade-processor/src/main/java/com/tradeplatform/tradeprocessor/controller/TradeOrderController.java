package com.tradeplatform.tradeprocessor.controller;

import com.tradeplatform.common.dto.OrderStatus;
import com.tradeplatform.common.dto.TradeOrderDTO;
import com.tradeplatform.tradeprocessor.entity.TradeOrder;
import com.tradeplatform.tradeprocessor.repository.TradeOrderRepository;
import com.tradeplatform.tradeprocessor.service.TradeOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for trade order operations.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Allow requests from any origin for development
public class TradeOrderController {

    private final TradeOrderService tradeOrderService;
    private final TradeOrderRepository tradeOrderRepository;

    /**
     * Retrieves all trade orders.
     *
     * @return the list of all trade orders
     */
    @GetMapping
    public ResponseEntity<List<TradeOrderDTO>> getAllOrders() {
        log.info("Retrieving all orders");
        // Get all orders from all statuses
        List<TradeOrderDTO> pendingOrders = tradeOrderService.getOrdersByStatus(OrderStatus.PENDING);
        List<TradeOrderDTO> processingOrders = tradeOrderService.getOrdersByStatus(OrderStatus.PROCESSING);
        List<TradeOrderDTO> filledOrders = tradeOrderService.getOrdersByStatus(OrderStatus.FILLED);
        List<TradeOrderDTO> failedOrders = tradeOrderService.getOrdersByStatus(OrderStatus.FAILED);
        List<TradeOrderDTO> cancelledOrders = tradeOrderService.getOrdersByStatus(OrderStatus.CANCELLED);
        List<TradeOrderDTO> createdOrders = tradeOrderService.getOrdersByStatus(OrderStatus.CREATED);
        List<TradeOrderDTO> partiallyFilledOrders = tradeOrderService.getOrdersByStatus(OrderStatus.PARTIALLY_FILLED);
        List<TradeOrderDTO> rejectedOrders = tradeOrderService.getOrdersByStatus(OrderStatus.REJECTED);
        List<TradeOrderDTO> expiredOrders = tradeOrderService.getOrdersByStatus(OrderStatus.EXPIRED);

        // Combine all orders
        List<TradeOrderDTO> allOrders = new java.util.ArrayList<>();
        allOrders.addAll(pendingOrders);
        allOrders.addAll(processingOrders);
        allOrders.addAll(filledOrders);
        allOrders.addAll(failedOrders);
        allOrders.addAll(cancelledOrders);
        allOrders.addAll(createdOrders);
        allOrders.addAll(partiallyFilledOrders);
        allOrders.addAll(rejectedOrders);
        allOrders.addAll(expiredOrders);

        return ResponseEntity.ok(allOrders);
    }

    /**
     * Retrieves a trade order by ID.
     *
     * @param orderId the order ID
     * @return the trade order
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<TradeOrderDTO> getOrderById(@PathVariable UUID orderId) {
        log.info("Retrieving order by ID: {}", orderId);
        TradeOrderDTO order = tradeOrderService.getOrderById(orderId);
        return ResponseEntity.ok(order);
    }

    /**
     * Retrieves all trade orders for a user.
     *
     * @param userId the user ID
     * @return the list of trade orders
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TradeOrderDTO>> getOrdersByUserId(@PathVariable String userId) {
        log.info("Retrieving orders for user: {}", userId);
        List<TradeOrderDTO> orders = tradeOrderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }
}
