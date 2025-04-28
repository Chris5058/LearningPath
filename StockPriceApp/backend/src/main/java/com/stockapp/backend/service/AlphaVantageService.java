package com.stockapp.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for fetching real-time stock data from Alpha Vantage API
 */
@Service
public class AlphaVantageService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${alphavantage.api.key}")
    private String apiKey;

    @Value("${alphavantage.api.url}")
    private String apiUrl;

    // Cache to store stock prices to avoid excessive API calls
    private final Map<String, BigDecimal> priceCache = new HashMap<>();

    public AlphaVantageService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get the current stock price for a given symbol
     * @param symbol the stock symbol
     * @return the current price as a BigDecimal
     */
    public BigDecimal getStockPrice(String symbol) {
        try {
            // Check cache first to avoid excessive API calls
            if (priceCache.containsKey(symbol)) {
                return priceCache.get(symbol);
            }

            // Check if API key is the placeholder value or demo key
            if ("YOUR_API_KEY".equals(apiKey) || "DEMO_API_KEY".equals(apiKey)) {
                System.err.println("Error fetching stock price for " + symbol + ": API key not configured properly. Using fallback price.");
                return getFallbackPrice(symbol);
            }

            // Build the URL for the Alpha Vantage API
            String url = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("function", "GLOBAL_QUOTE")
                    .queryParam("symbol", symbol)
                    .queryParam("apikey", apiKey)
                    .build()
                    .toUriString();

            // Make the API call
            String response = restTemplate.getForObject(url, String.class);

            // Check if response is null
            if (response == null) {
                System.err.println("Error fetching stock price for " + symbol + ": Null response from API. Using fallback price.");
                return getFallbackPrice(symbol);
            }

            // Parse the JSON response
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode globalQuote = rootNode.path("Global Quote");

            // Check if Global Quote is empty or missing
            if (globalQuote.isEmpty() || globalQuote.isMissingNode()) {
                System.err.println("Error fetching stock price for " + symbol + ": Invalid response format or API limit reached. Using fallback price.");
                return getFallbackPrice(symbol);
            }

            // Extract the price
            String priceStr = globalQuote.path("05. price").asText();

            // Check if price string is empty
            if (priceStr == null || priceStr.isEmpty() || "null".equals(priceStr)) {
                System.err.println("Error fetching stock price for " + symbol + ": Price data not available. Using fallback price.");
                return getFallbackPrice(symbol);
            }

            BigDecimal price = new BigDecimal(priceStr).setScale(2, RoundingMode.HALF_UP);

            // Cache the price
            priceCache.put(symbol, price);

            return price;
        } catch (Exception e) {
            // Log the error and return a fallback price
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
            System.err.println("Error fetching stock price for " + symbol + ": " + errorMessage + ". Using fallback price.");
            return getFallbackPrice(symbol);
        }
    }

    /**
     * Clear the price cache to force fresh data on next request
     */
    public void clearCache() {
        priceCache.clear();
    }

    /**
     * Get a fallback price if the API call fails
     * @param symbol the stock symbol
     * @return a fallback price based on the symbol
     */
    private BigDecimal getFallbackPrice(String symbol) {
        // Provide reasonable fallback prices based on recent market values
        switch (symbol) {
            case "TSLA":
                return new BigDecimal("200.00");
            case "NVDA":
                return new BigDecimal("800.00");
            case "ORCL":
                return new BigDecimal("125.00");
            case "CCJ":
                return new BigDecimal("50.00");
            default:
                return new BigDecimal("100.00");
        }
    }
}
