package com.tradeplatform.common.exception;

import java.util.UUID;

/**
 * Exception thrown when an error occurs during order processing.
 */
public class OrderProcessingException extends TradePlatformException {

    /**
     * The ID of the order that failed processing.
     */
    private final UUID orderId;

    /**
     * The reason for the processing failure.
     */
    private final String reason;

    /**
     * Constructs a new OrderProcessingException with the specified detail message.
     *
     * @param message the detail message
     */
    public OrderProcessingException(String message) {
        super(message, "ORDER_PROCESSING_ERROR");
        this.orderId = null;
        this.reason = "Unknown";
    }

    /**
     * Constructs a new OrderProcessingException with the specified detail message and order ID.
     *
     * @param message the detail message
     * @param orderId the ID of the order that failed processing
     */
    public OrderProcessingException(String message, UUID orderId) {
        super(message, "ORDER_PROCESSING_ERROR");
        this.orderId = orderId;
        this.reason = "Unknown";
    }

    /**
     * Constructs a new OrderProcessingException with the specified detail message, order ID, and reason.
     *
     * @param message the detail message
     * @param orderId the ID of the order that failed processing
     * @param reason  the reason for the processing failure
     */
    public OrderProcessingException(String message, UUID orderId, String reason) {
        super(message, "ORDER_PROCESSING_ERROR");
        this.orderId = orderId;
        this.reason = reason;
    }

    /**
     * Constructs a new OrderProcessingException with the specified detail message, cause, order ID, and reason.
     *
     * @param message the detail message
     * @param cause   the cause
     * @param orderId the ID of the order that failed processing
     * @param reason  the reason for the processing failure
     */
    public OrderProcessingException(String message, Throwable cause, UUID orderId, String reason) {
        super(message, cause, "ORDER_PROCESSING_ERROR");
        this.orderId = orderId;
        this.reason = reason;
    }

    /**
     * Returns the ID of the order that failed processing.
     *
     * @return the order ID, or null if not available
     */
    public UUID getOrderId() {
        return orderId;
    }

    /**
     * Returns the reason for the processing failure.
     *
     * @return the reason
     */
    public String getReason() {
        return reason;
    }
}