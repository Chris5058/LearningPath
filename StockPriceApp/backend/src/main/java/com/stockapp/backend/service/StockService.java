package com.stockapp.backend.service;

import com.stockapp.backend.model.Stock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class StockService {

    private final AlphaVantageService alphaVantageService;

    @Autowired
    public StockService(AlphaVantageService alphaVantageService) {
        this.alphaVantageService = alphaVantageService;
    }

    /**
     * Get current stock prices for the specified symbols
     * @return list of stocks with current prices
     */
    public List<Stock> getCurrentStockPrices() {
        List<Stock> stocks = new ArrayList<>();

        // Clear the cache to ensure fresh data (optional, remove if you want to use cached data)
        alphaVantageService.clearCache();

        // Add the required stocks with real-time prices from Alpha Vantage
        stocks.add(createStock("TSLA", "Tesla Inc.", alphaVantageService.getStockPrice("TSLA")));
        stocks.add(createStock("NVDA", "NVIDIA Corporation", alphaVantageService.getStockPrice("NVDA")));
        stocks.add(createStock("ORCL", "Oracle Corporation", alphaVantageService.getStockPrice("ORCL")));
        stocks.add(createStock("CCJ", "Cameco Corporation", alphaVantageService.getStockPrice("CCJ")));

        return stocks;
    }

    /**
     * Create a stock with the given parameters
     */
    private Stock createStock(String symbol, String name, BigDecimal price) {
        return new Stock(symbol, name, price);
    }
}
