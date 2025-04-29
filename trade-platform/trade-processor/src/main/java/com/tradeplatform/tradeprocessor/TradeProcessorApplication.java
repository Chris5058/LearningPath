package com.tradeplatform.tradeprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Trade Processor service.
 * This service consumes trade orders from Kafka and processes them.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class TradeProcessorApplication {

    /**
     * Main method to start the Trade Processor application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(TradeProcessorApplication.class, args);
    }
}