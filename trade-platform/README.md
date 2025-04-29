# Trade Platform

A microservices-based trading platform that uses Apache Kafka for event-driven communication between services.

## Project Structure

- **trade-api**: REST API for submitting trade orders
- **trade-processor**: Service for processing and executing trade orders
- **portfolio-service**: Service for managing user portfolios
- **common-lib**: Shared library with DTOs and common utilities

## Quick Start Guide

### Prerequisites

- Java 11 or higher
- Maven
- Docker and Docker Compose

### Step 1: Start Kafka

```bash
# From the project root
docker-compose up -d
```

This will start Zookeeper and Kafka containers using the provided `docker-compose.yml` file.

### Step 2: Build the Project

```bash
# From the project root
mvn clean install
```

### Step 3: Run the Services

Start each service in the following order:

1. Portfolio Service:
```bash
cd portfolio-service
mvn spring-boot:run
```

2. Trade Processor:
```bash
cd trade-processor
mvn spring-boot:run
```

3. Trade API:
```bash
cd trade-api
mvn spring-boot:run
```

Alternatively, you can use IntelliJ IDEA to run each service. See the [Kafka Setup Guide](KAFKA_SETUP_GUIDE.md) for detailed instructions.

## Kafka Setup

For detailed instructions on setting up Kafka in IntelliJ IDEA, see the [Kafka Setup Guide](KAFKA_SETUP_GUIDE.md).

## API Documentation

The Trade API service exposes the following endpoints:

- `POST /api/orders`: Submit a new trade order
- `GET /api/orders/{orderId}`: Get a trade order by ID
- `GET /api/orders`: Get all trade orders

## Development

### Running Tests

```bash
mvn test
```

### Building Docker Images

```bash
mvn spring-boot:build-image
```

## Troubleshooting

If you encounter issues with Kafka connectivity:

1. Ensure Kafka is running: `docker ps`
2. Check the logs: `docker-compose logs kafka`
3. Verify the bootstrap server address in `application.properties` matches your Kafka setup

For more troubleshooting tips, see the [Kafka Setup Guide](KAFKA_SETUP_GUIDE.md).