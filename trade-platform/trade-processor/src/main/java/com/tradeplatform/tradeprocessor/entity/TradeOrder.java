package com.tradeplatform.tradeprocessor.entity;

import com.tradeplatform.common.dto.OrderStatus;
import com.tradeplatform.common.dto.OrderType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a trade order in the database.
 */
@Entity
@Table(name = "trade_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeOrder {

    /**
     * Unique identifier for the order.
     */
    @Id
    private UUID orderId;

    /**
     * User ID of the trader who placed the order.
     */
    @Column(nullable = false)
    private String userId;

    /**
     * Symbol/ticker of the security being traded.
     */
    @Column(nullable = false)
    private String symbol;

    /**
     * Type of the order (BUY, SELL, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;

    /**
     * Current status of the order.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    /**
     * Quantity of securities to trade.
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Price at which the order should be executed (for LIMIT orders).
     */
    private BigDecimal price;

    /**
     * Stop price that triggers a STOP or STOP_LIMIT order.
     */
    private BigDecimal stopPrice;

    /**
     * Time when the order was created.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Time when the order was last updated.
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Time when the order was executed (filled).
     */
    private LocalDateTime executedAt;

    /**
     * Time when the order expires (for orders with time constraints).
     */
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
    @Column(length = 1000)
    private String notes;

    /**
     * Number of processing attempts.
     */
    @Column(nullable = false)
    private Integer processingAttempts;

    /**
     * Last error message during processing.
     */
    @Column(length = 1000)
    private String lastErrorMessage;

    /**
     * Time when the last processing attempt was made.
     */
    private LocalDateTime lastProcessingAttempt;
}