package com.tradeplatform.portfolioservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Portfolio Service.
 * This service manages user portfolios and updates them based on executed trade orders.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class PortfolioServiceApplication {

    /**
     * Main method to start the Portfolio Service application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(PortfolioServiceApplication.class, args);
    }
}