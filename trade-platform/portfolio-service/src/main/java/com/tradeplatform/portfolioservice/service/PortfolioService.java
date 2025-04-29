package com.tradeplatform.portfolioservice.service;

import com.tradeplatform.common.dto.OrderType;
import com.tradeplatform.common.dto.PortfolioEntryDTO;
import com.tradeplatform.common.dto.TradeOrderDTO;
import com.tradeplatform.common.exception.OrderProcessingException;
import com.tradeplatform.common.exception.ResourceNotFoundException;
import com.tradeplatform.portfolioservice.entity.PortfolioEntry;
import com.tradeplatform.portfolioservice.repository.PortfolioEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing user portfolios.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

    private final PortfolioEntryRepository portfolioEntryRepository;

    /**
     * Updates a user's portfolio based on a trade order.
     *
     * @param orderDTO the trade order
     * @return the updated portfolio entry
     * @throws OrderProcessingException if there is an error processing the order
     */
    @Transactional
    public PortfolioEntryDTO updatePortfolio(TradeOrderDTO orderDTO) {
        log.info("Updating portfolio for order: {}", orderDTO.getOrderId());

        try {
            // Get the portfolio entry for the user and symbol, or create a new one
            PortfolioEntry entry = portfolioEntryRepository
                    .findByUserIdAndSymbolWithLock(orderDTO.getUserId(), orderDTO.getSymbol())
                    .orElseGet(() -> createNewPortfolioEntry(orderDTO));

            // Update the portfolio entry based on the order type
            if (orderDTO.getOrderType() == OrderType.BUY) {
                processBuyOrder(entry, orderDTO);
            } else if (orderDTO.getOrderType() == OrderType.SELL) {
                processSellOrder(entry, orderDTO);
            } else {
                log.warn("Unsupported order type: {}", orderDTO.getOrderType());
                throw new OrderProcessingException("Unsupported order type: " + orderDTO.getOrderType(),
                        orderDTO.getOrderId());
            }

            // Save the updated portfolio entry
            entry = portfolioEntryRepository.save(entry);
            log.info("Portfolio updated for order: {}", orderDTO.getOrderId());

            return convertToDTO(entry);
        } catch (Exception e) {
            log.error("Error updating portfolio for order: {}", orderDTO.getOrderId(), e);
            throw new OrderProcessingException("Failed to update portfolio",
                    orderDTO.getOrderId(), e.getMessage());
        }
    }

    /**
     * Gets all portfolio entries for a user.
     *
     * @param userId the user ID
     * @return the list of portfolio entries
     */
    @Transactional(readOnly = true)
    public List<PortfolioEntryDTO> getPortfolioByUserId(String userId) {
        log.info("Getting portfolio for user: {}", userId);
        return portfolioEntryRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets a portfolio entry by ID.
     *
     * @param id the portfolio entry ID
     * @return the portfolio entry
     * @throws ResourceNotFoundException if the portfolio entry is not found
     */
    @Transactional(readOnly = true)
    public PortfolioEntryDTO getPortfolioEntryById(UUID id) {
        log.info("Getting portfolio entry by ID: {}", id);
        return portfolioEntryRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("PortfolioEntry", id.toString()));
    }

    /**
     * Gets a portfolio entry for a user and symbol.
     *
     * @param userId the user ID
     * @param symbol the symbol/ticker
     * @return the portfolio entry
     * @throws ResourceNotFoundException if the portfolio entry is not found
     */
    @Transactional(readOnly = true)
    public PortfolioEntryDTO getPortfolioEntryByUserIdAndSymbol(String userId, String symbol) {
        log.info("Getting portfolio entry for user: {} and symbol: {}", userId, symbol);
        return portfolioEntryRepository.findByUserIdAndSymbol(userId, symbol)
                .map(this::convertToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("PortfolioEntry", userId + "/" + symbol));
    }

    /**
     * Creates a new portfolio entry for a trade order.
     *
     * @param orderDTO the trade order
     * @return the new portfolio entry
     */
    private PortfolioEntry createNewPortfolioEntry(TradeOrderDTO orderDTO) {
        log.info("Creating new portfolio entry for user: {} and symbol: {}", orderDTO.getUserId(), orderDTO.getSymbol());
        return PortfolioEntry.builder()
                .userId(orderDTO.getUserId())
                .symbol(orderDTO.getSymbol())
                .quantity(0)
                .averagePrice(BigDecimal.ZERO)
                .currentPrice(orderDTO.getExecutionPrice())
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    /**
     * Processes a buy order and updates the portfolio entry.
     *
     * @param entry the portfolio entry to update
     * @param orderDTO the buy order
     */
    private void processBuyOrder(PortfolioEntry entry, TradeOrderDTO orderDTO) {
        log.info("Processing buy order for user: {} and symbol: {}", orderDTO.getUserId(), orderDTO.getSymbol());

        // Calculate the new average price and quantity
        int oldQuantity = entry.getQuantity();
        int newQuantity = oldQuantity + orderDTO.getFilledQuantity();
        
        BigDecimal oldValue = entry.getAveragePrice().multiply(BigDecimal.valueOf(oldQuantity));
        BigDecimal newValue = orderDTO.getExecutionPrice().multiply(BigDecimal.valueOf(orderDTO.getFilledQuantity()));
        BigDecimal totalValue = oldValue.add(newValue);
        
        BigDecimal newAveragePrice = newQuantity > 0 
                ? totalValue.divide(BigDecimal.valueOf(newQuantity), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Update the portfolio entry
        entry.setQuantity(newQuantity);
        entry.setAveragePrice(newAveragePrice);
        entry.setCurrentPrice(orderDTO.getExecutionPrice());
        entry.setLastUpdated(LocalDateTime.now());
        
        // Add a note about the transaction
        String note = String.format("Bought %d shares at %s on %s (Order ID: %s)",
                orderDTO.getFilledQuantity(),
                orderDTO.getExecutionPrice(),
                orderDTO.getExecutedAt(),
                orderDTO.getOrderId());
        entry.setNotes(note);
    }

    /**
     * Processes a sell order and updates the portfolio entry.
     *
     * @param entry the portfolio entry to update
     * @param orderDTO the sell order
     * @throws OrderProcessingException if there are not enough shares to sell
     */
    private void processSellOrder(PortfolioEntry entry, TradeOrderDTO orderDTO) {
        log.info("Processing sell order for user: {} and symbol: {}", orderDTO.getUserId(), orderDTO.getSymbol());

        // Check if there are enough shares to sell
        if (entry.getQuantity() < orderDTO.getFilledQuantity()) {
            log.error("Not enough shares to sell. Available: {}, Requested: {}", 
                    entry.getQuantity(), orderDTO.getFilledQuantity());
            throw new OrderProcessingException("Not enough shares to sell",
                    orderDTO.getOrderId(), "Insufficient shares");
        }

        // Update the quantity (average price stays the same)
        int newQuantity = entry.getQuantity() - orderDTO.getFilledQuantity();
        entry.setQuantity(newQuantity);
        entry.setCurrentPrice(orderDTO.getExecutionPrice());
        entry.setLastUpdated(LocalDateTime.now());
        
        // Add a note about the transaction
        String note = String.format("Sold %d shares at %s on %s (Order ID: %s)",
                orderDTO.getFilledQuantity(),
                orderDTO.getExecutionPrice(),
                orderDTO.getExecutedAt(),
                orderDTO.getOrderId());
        entry.setNotes(note);
        
        // If quantity is zero, we could delete the entry, but we'll keep it for history
        if (newQuantity == 0) {
            log.info("Portfolio entry quantity is now zero for user: {} and symbol: {}", 
                    orderDTO.getUserId(), orderDTO.getSymbol());
        }
    }

    /**
     * Converts a PortfolioEntry entity to a PortfolioEntryDTO.
     *
     * @param entity the entity to convert
     * @return the DTO
     */
    private PortfolioEntryDTO convertToDTO(PortfolioEntry entity) {
        return PortfolioEntryDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .symbol(entity.getSymbol())
                .quantity(entity.getQuantity())
                .averagePrice(entity.getAveragePrice())
                .currentPrice(entity.getCurrentPrice())
                .costBasis(entity.getCostBasis())
                .marketValue(entity.getMarketValue())
                .unrealizedPnL(entity.getUnrealizedPnL())
                .percentageGainLoss(entity.getPercentageGainLoss())
                .lastUpdated(entity.getLastUpdated())
                .notes(entity.getNotes())
                .build();
    }
}