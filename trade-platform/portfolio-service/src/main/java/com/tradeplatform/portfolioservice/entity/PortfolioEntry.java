package com.tradeplatform.portfolioservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a portfolio entry (holding) in a user's portfolio.
 */
@Entity
@Table(name = "portfolio_entries", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "symbol"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioEntry {

    /**
     * Unique identifier for the portfolio entry.
     */
    @Id
    @GeneratedValue
    private UUID id;

    /**
     * User ID of the portfolio owner.
     */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /**
     * Symbol/ticker of the security held in the portfolio.
     */
    @Column(nullable = false)
    private String symbol;

    /**
     * Quantity of the security held.
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Average purchase price of the security.
     */
    @Column(name = "average_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal averagePrice;

    /**
     * Current market price of the security.
     */
    @Column(name = "current_price", precision = 19, scale = 4)
    private BigDecimal currentPrice;

    /**
     * Total cost basis of the holding (quantity * averagePrice).
     */
    @Column(name = "cost_basis", precision = 19, scale = 4)
    private BigDecimal costBasis;

    /**
     * Current market value of the holding (quantity * currentPrice).
     */
    @Column(name = "market_value", precision = 19, scale = 4)
    private BigDecimal marketValue;

    /**
     * Unrealized profit/loss (marketValue - costBasis).
     */
    @Column(name = "unrealized_pnl", precision = 19, scale = 4)
    private BigDecimal unrealizedPnL;

    /**
     * Percentage gain/loss ((marketValue - costBasis) / costBasis * 100).
     */
    @Column(name = "percentage_gain_loss", precision = 19, scale = 4)
    private BigDecimal percentageGainLoss;

    /**
     * Time when the portfolio entry was last updated.
     */
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    /**
     * Additional notes or comments about the portfolio entry.
     */
    @Column(length = 1000)
    private String notes;

    /**
     * Version for optimistic locking.
     */
    @Version
    private Long version;

    /**
     * Updates the calculated fields based on the current quantity, average price, and current price.
     */
    @PrePersist
    @PreUpdate
    public void updateCalculatedFields() {
        if (quantity != null && averagePrice != null) {
            costBasis = averagePrice.multiply(BigDecimal.valueOf(quantity));
        }
        
        if (quantity != null && currentPrice != null) {
            marketValue = currentPrice.multiply(BigDecimal.valueOf(quantity));
        }
        
        if (costBasis != null && marketValue != null && costBasis.compareTo(BigDecimal.ZERO) > 0) {
            unrealizedPnL = marketValue.subtract(costBasis);
            percentageGainLoss = unrealizedPnL.divide(costBasis, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        
        lastUpdated = LocalDateTime.now();
    }
}