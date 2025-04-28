package com.example.elasticsearchlearner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String encryptedPassword;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        // For this implementation, we're assuming the password in application.properties
        // is already encrypted with BCrypt. In a real-world scenario, you might want to
        // use a more sophisticated encryption/decryption mechanism.

        // We're using an environment variable for the database password
        // This is more secure than hardcoding the password in the source code
        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbPassword == null || dbPassword.isEmpty()) {
            // Fallback to a default password or throw an exception
            throw new IllegalStateException("Database password environment variable (DB_PASSWORD) is not set");
        }

        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(dbPassword)
                .driverClassName(driverClassName)
                .build();
    }
}
