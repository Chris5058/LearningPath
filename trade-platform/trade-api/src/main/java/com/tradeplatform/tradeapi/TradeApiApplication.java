package com.tradeplatform.tradeapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Trade API service.
 * This service provides REST endpoints for trade order submission and management.
 */
@SpringBootApplication
public class TradeApiApplication {

    /**
     * Main method to start the Trade API application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(TradeApiApplication.class, args);
    }
}