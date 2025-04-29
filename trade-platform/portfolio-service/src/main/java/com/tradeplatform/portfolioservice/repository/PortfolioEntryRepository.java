package com.tradeplatform.portfolioservice.repository;

import com.tradeplatform.portfolioservice.entity.PortfolioEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for accessing and manipulating portfolio entries in the database.
 */
@Repository
public interface PortfolioEntryRepository extends JpaRepository<PortfolioEntry, UUID> {

    /**
     * Finds all portfolio entries for a specific user.
     *
     * @param userId the user ID
     * @return the list of portfolio entries
     */
    List<PortfolioEntry> findByUserId(String userId);

    /**
     * Finds a portfolio entry for a specific user and symbol.
     *
     * @param userId the user ID
     * @param symbol the symbol/ticker
     * @return the portfolio entry, or empty if not found
     */
    Optional<PortfolioEntry> findByUserIdAndSymbol(String userId, String symbol);

    /**
     * Finds a portfolio entry for a specific user and symbol with a pessimistic write lock.
     * This is used to prevent concurrent updates to the same portfolio entry.
     *
     * @param userId the user ID
     * @param symbol the symbol/ticker
     * @return the portfolio entry, or empty if not found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PortfolioEntry p WHERE p.userId = ?1 AND p.symbol = ?2")
    Optional<PortfolioEntry> findByUserIdAndSymbolWithLock(String userId, String symbol);

    /**
     * Finds all portfolio entries for a specific symbol.
     *
     * @param symbol the symbol/ticker
     * @return the list of portfolio entries
     */
    List<PortfolioEntry> findBySymbol(String symbol);

    /**
     * Counts the number of portfolio entries for a specific user.
     *
     * @param userId the user ID
     * @return the count of portfolio entries
     */
    long countByUserId(String userId);

    /**
     * Deletes all portfolio entries for a specific user.
     *
     * @param userId the user ID
     */
    void deleteByUserId(String userId);

    /**
     * Deletes a portfolio entry for a specific user and symbol.
     *
     * @param userId the user ID
     * @param symbol the symbol/ticker
     */
    void deleteByUserIdAndSymbol(String userId, String symbol);
}