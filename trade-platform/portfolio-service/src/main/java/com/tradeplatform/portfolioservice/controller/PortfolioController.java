package com.tradeplatform.portfolioservice.controller;

import com.tradeplatform.common.dto.PortfolioEntryDTO;
import com.tradeplatform.common.exception.ResourceNotFoundException;
import com.tradeplatform.portfolioservice.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for portfolio operations.
 */
@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
@Slf4j
public class PortfolioController {

    private final PortfolioService portfolioService;

    /**
     * Gets all portfolio entries for a user.
     *
     * @param userId the user ID
     * @return the list of portfolio entries
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<PortfolioEntryDTO>> getPortfolioByUserId(@PathVariable String userId) {
        log.info("REST request to get portfolio for user: {}", userId);
        List<PortfolioEntryDTO> portfolio = portfolioService.getPortfolioByUserId(userId);
        return ResponseEntity.ok(portfolio);
    }

    /**
     * Gets a portfolio entry by ID.
     *
     * @param id the portfolio entry ID
     * @return the portfolio entry
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPortfolioEntryById(@PathVariable UUID id) {
        log.info("REST request to get portfolio entry by ID: {}", id);
        try {
            PortfolioEntryDTO entry = portfolioService.getPortfolioEntryById(id);
            return ResponseEntity.ok(entry);
        } catch (ResourceNotFoundException e) {
            log.warn("Portfolio entry not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting portfolio entry: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Error retrieving portfolio entry", e.getMessage()));
        }
    }

    /**
     * Gets a portfolio entry for a user and symbol.
     *
     * @param userId the user ID
     * @param symbol the symbol/ticker
     * @return the portfolio entry
     */
    @GetMapping("/users/{userId}/symbols/{symbol}")
    public ResponseEntity<?> getPortfolioEntryByUserIdAndSymbol(
            @PathVariable String userId,
            @PathVariable String symbol) {
        log.info("REST request to get portfolio entry for user: {} and symbol: {}", userId, symbol);
        try {
            PortfolioEntryDTO entry = portfolioService.getPortfolioEntryByUserIdAndSymbol(userId, symbol);
            return ResponseEntity.ok(entry);
        } catch (ResourceNotFoundException e) {
            log.warn("Portfolio entry not found for user: {} and symbol: {}", userId, symbol);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting portfolio entry for user: {} and symbol: {}", userId, symbol, e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Error retrieving portfolio entry", e.getMessage()));
        }
    }

    /**
     * Creates an error response with the given message and details.
     *
     * @param message the error message
     * @param details the error details
     * @return the error response
     */
    private Map<String, Object> createErrorResponse(String message, String details) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("message", message);
        if (details != null && !details.isEmpty()) {
            errorResponse.put("details", details);
        }
        return errorResponse;
    }
}