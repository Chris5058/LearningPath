package com.tradeplatform.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object representing a trade order in the system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeOrderDTO {

    /**
     * Unique identifier for the order.
     */
    private UUID orderId;

    /**
     * User ID of the trader who placed the order.
     */
    @NotBlank(message = "User ID is required")
    private String userId;

    /**
     * Symbol/ticker of the security being traded.
     */
    @NotBlank(message = "Symbol is required")
    private String symbol;

    /**
     * Type of the order (BUY, SELL, etc.).
     */
    @NotNull(message = "Order type is required")
    private OrderType orderType;

    /**
     * Current status of the order.
     */
    private OrderStatus status;

    /**
     * Quantity of securities to trade.
     */
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    /**
     * Price at which the order should be executed (for LIMIT orders).
     */
    @Min(value = 0, message = "Price cannot be negative")
    private BigDecimal price;

    /**
     * Stop price that triggers a STOP or STOP_LIMIT order.
     */
    @Min(value = 0, message = "Stop price cannot be negative")
    private BigDecimal stopPrice;

    /**
     * Time when the order was created.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Time when the order was last updated.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * Time when the order was executed (filled).
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime executedAt;

    /**
     * Time when the order expires (for orders with time constraints).
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    /**
     * Average price at which the order was executed.
     */
    private BigDecimal executionPrice;

    /**
     * Quantity that has been filled so far.
     */
    private Integer filledQuantity;

    /**
     * Remaining quantity to be filled.
     */
    private Integer remainingQuantity;

    /**
     * Additional notes or comments about the order.
     */
    private String notes;
}