package com.tradeplatform.common.dto;

/**
 * Enum representing the type of trade order.
 */
public enum OrderType {
    /**
     * Buy order - to purchase securities.
     */
    BUY,
    
    /**
     * Sell order - to sell securities.
     */
    SELL,
    
    /**
     * Market order - executed at the current market price.
     */
    MARKET,
    
    /**
     * Limit order - executed at a specified price or better.
     */
    LIMIT,
    
    /**
     * Stop order - becomes a market order when a specified price is reached.
     */
    STOP,
    
    /**
     * Stop-limit order - becomes a limit order when a specified price is reached.
     */
    STOP_LIMIT
}