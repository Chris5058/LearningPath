# Setting Up Kafka in IntelliJ for the Trade Platform Project

This guide will help you set up Apache Kafka for local development with the Trade Platform project in IntelliJ IDEA.

## Prerequisites

- IntelliJ IDEA (Community or Ultimate edition)
- Java 11 or higher
- Maven
- Docker (recommended for running Kafka)

## Step 1: Install Kafka Locally

### Option 1: Using Docker (Recommended)

1. Create a `docker-compose.yml` file in the project root with the following content:

```yaml
version: '3'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
```

2. Start Kafka using Docker Compose:

```bash
docker-compose up -d
```

### Option 2: Manual Installation

1. Download Apache Kafka from the [official website](https://kafka.apache.org/downloads)
2. Extract the downloaded archive
3. Start ZooKeeper:

```bash
bin/zookeeper-server-start.sh config/zookeeper.properties
```

4. Start Kafka:

```bash
bin/kafka-server-start.sh config/server.properties
```

## Step 2: Verify Kafka Configuration in the Project

The project already has the necessary Kafka configuration in place:

1. **Dependencies**: All services have the required Kafka dependencies in their `pom.xml` files:
   - `spring-kafka` for Kafka integration
   - `spring-kafka-test` for testing

2. **Configuration Files**: Each service has Kafka configuration in:
   - `application.properties` - Contains Kafka connection settings
   - `KafkaConfig.java` - Contains Java-based Kafka configuration

3. **Kafka Settings**: The default settings in all services are:
   - Bootstrap servers: `localhost:9092`
   - Topic: `trade-orders`

## Step 3: Create Run Configurations in IntelliJ

1. **Create a Run Configuration for Kafka** (if using Docker):
   - Go to `Run` > `Edit Configurations`
   - Click the `+` button and select `Docker-compose`
   - Name it "Kafka"
   - Select the `docker-compose.yml` file
   - Click `OK`

2. **Create Run Configurations for Each Service**:
   - Go to `Run` > `Edit Configurations`
   - Click the `+` button and select `Spring Boot`
   - Create configurations for each service:
     - Trade API: `com.tradeplatform.tradeapi.TradeApiApplication`
     - Trade Processor: `com.tradeplatform.tradeprocessor.TradeProcessorApplication`
     - Portfolio Service: `com.tradeplatform.portfolioservice.PortfolioServiceApplication`
   - Ensure each has the correct module selected
   - Click `OK`

## Step 4: Running the Application

1. Start Kafka first (using the Docker Compose run configuration or manually)
2. Start each service in the following order:
   - Portfolio Service
   - Trade Processor
   - Trade API

## Step 5: Testing Kafka Integration

1. **Using Kafka Tools in IntelliJ** (Ultimate Edition only):
   - Install the "Kafka Tool" plugin from the JetBrains Marketplace
   - Go to `Tools` > `Kafka Tool` > `Add Kafka Connection`
   - Configure the connection to `localhost:9092`
   - Use the tool to view topics, messages, and more

2. **Using Command Line Tools**:
   - List topics:
     ```bash
     docker exec -it trade-platform_kafka_1 kafka-topics --list --bootstrap-server localhost:9092
     ```
   - Create a topic:
     ```bash
     docker exec -it trade-platform_kafka_1 kafka-topics --create --topic test-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
     ```
   - Produce messages:
     ```bash
     docker exec -it trade-platform_kafka_1 kafka-console-producer --topic trade-orders --bootstrap-server localhost:9092
     ```
   - Consume messages:
     ```bash
     docker exec -it trade-platform_kafka_1 kafka-console-consumer --topic trade-orders --from-beginning --bootstrap-server localhost:9092
     ```

## Troubleshooting

1. **Connection Issues**:
   - Ensure Kafka is running: `docker ps` or check the process
   - Verify the bootstrap server address in `application.properties` matches your Kafka setup
   - Check logs for connection errors

2. **Message Serialization Issues**:
   - Ensure the producer and consumer are using compatible serialization formats
   - Check that the `TradeOrderDTO` class is the same across all services

3. **Topic Not Found**:
   - The Trade API service is configured to create the topic automatically
   - You can manually create the topic using the command line tools

## Additional Resources

- [Spring for Apache Kafka Documentation](https://docs.spring.io/spring-kafka/reference/html/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Confluent Kafka Docker Images](https://docs.confluent.io/platform/current/installation/docker/image-reference.html)