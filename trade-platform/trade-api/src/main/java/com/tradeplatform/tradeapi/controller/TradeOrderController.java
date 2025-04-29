package com.tradeplatform.tradeapi.controller;

import com.tradeplatform.common.dto.TradeOrderDTO;
import com.tradeplatform.common.exception.OrderProcessingException;
import com.tradeplatform.common.exception.ValidationException;
import com.tradeplatform.tradeapi.service.TradeOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for trade order operations.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class TradeOrderController {

    private final TradeOrderService tradeOrderService;

    /**
     * Creates a new trade order.
     *
     * @param orderDTO the trade order to create
     * @return the created trade order
     */
    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody TradeOrderDTO orderDTO) {
        try {
            TradeOrderDTO createdOrder = tradeOrderService.createOrder(orderDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
        } catch (ValidationException e) {
            log.warn("Validation error when creating order: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage(), e.getFieldErrors()));
        } catch (OrderProcessingException e) {
            log.error("Error processing order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(e.getMessage(), Map.of("reason", e.getReason())));
        } catch (Exception e) {
            log.error("Unexpected error when creating order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("An unexpected error occurred", null));
        }
    }

    /**
     * Creates an error response with the given message and details.
     *
     * @param message the error message
     * @param details the error details
     * @return the error response
     */
    private Map<String, Object> createErrorResponse(String message, Map<String, String> details) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("message", message);
        if (details != null && !details.isEmpty()) {
            errorResponse.put("details", details);
        }
        return errorResponse;
    }
}