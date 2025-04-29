package com.tradeplatform.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object representing a portfolio entry (holding) in a user's portfolio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioEntryDTO {

    /**
     * Unique identifier for the portfolio entry.
     */
    private UUID id;

    /**
     * User ID of the portfolio owner.
     */
    @NotBlank(message = "User ID is required")
    private String userId;

    /**
     * Symbol/ticker of the security held in the portfolio.
     */
    @NotBlank(message = "Symbol is required")
    private String symbol;

    /**
     * Quantity of the security held.
     */
    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    /**
     * Average purchase price of the security.
     */
    @NotNull(message = "Average price is required")
    @Min(value = 0, message = "Average price cannot be negative")
    private BigDecimal averagePrice;

    /**
     * Current market price of the security.
     */
    private BigDecimal currentPrice;

    /**
     * Total cost basis of the holding (quantity * averagePrice).
     */
    private BigDecimal costBasis;

    /**
     * Current market value of the holding (quantity * currentPrice).
     */
    private BigDecimal marketValue;

    /**
     * Unrealized profit/loss (marketValue - costBasis).
     */
    private BigDecimal unrealizedPnL;

    /**
     * Percentage gain/loss ((marketValue - costBasis) / costBasis * 100).
     */
    private BigDecimal percentageGainLoss;

    /**
     * Time when the portfolio entry was last updated.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdated;

    /**
     * Additional notes or comments about the portfolio entry.
     */
    private String notes;
}