package com.tradeplatform.tradeprocessor.repository;

import com.tradeplatform.common.dto.OrderStatus;
import com.tradeplatform.tradeprocessor.entity.TradeOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for accessing and manipulating trade orders in the database.
 */
@Repository
public interface TradeOrderRepository extends JpaRepository<TradeOrder, UUID> {

    /**
     * Finds all trade orders for a specific user.
     *
     * @param userId the user ID
     * @return the list of trade orders
     */
    List<TradeOrder> findByUserId(String userId);

    /**
     * Finds all trade orders with a specific status.
     *
     * @param status the order status
     * @return the list of trade orders
     */
    List<TradeOrder> findByStatus(OrderStatus status);

    /**
     * Finds all trade orders for a specific symbol.
     *
     * @param symbol the symbol/ticker
     * @return the list of trade orders
     */
    List<TradeOrder> findBySymbol(String symbol);

    /**
     * Finds all trade orders for a specific user and status.
     *
     * @param userId the user ID
     * @param status the order status
     * @return the list of trade orders
     */
    List<TradeOrder> findByUserIdAndStatus(String userId, OrderStatus status);

    /**
     * Finds all trade orders created between the specified dates.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return the list of trade orders
     */
    List<TradeOrder> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Finds all trade orders that have been stuck in processing for too long.
     *
     * @param status the processing status
     * @param cutoffTime the cutoff time
     * @return the list of stuck trade orders
     */
    @Query("SELECT t FROM TradeOrder t WHERE t.status = ?1 AND t.lastProcessingAttempt < ?2")
    List<TradeOrder> findStuckOrders(OrderStatus status, LocalDateTime cutoffTime);
}