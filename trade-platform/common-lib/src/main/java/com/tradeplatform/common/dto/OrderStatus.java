package com.tradeplatform.common.dto;

/**
 * Enum representing the status of a trade order in its lifecycle.
 */
public enum OrderStatus {
    /**
     * Order has been created but not yet submitted for processing.
     */
    CREATED,
    
    /**
     * Order has been submitted and is awaiting processing.
     */
    PENDING,
    
    /**
     * Order is currently being processed.
     */
    PROCESSING,
    
    /**
     * Order has been partially filled.
     */
    PARTIALLY_FILLED,
    
    /**
     * Order has been completely filled.
     */
    FILLED,
    
    /**
     * Order has been cancelled.
     */
    CANCELLED,
    
    /**
     * Order has been rejected due to validation or business rule failures.
     */
    REJECTED,
    
    /**
     * Order processing has failed due to a system error.
     */
    FAILED,
    
    /**
     * Order has expired without being fully filled.
     */
    EXPIRED
}