# Trade Platform UI

A React-based frontend for the Trade Platform application that displays trade orders and provides user authentication.

## Prerequisites

- Node.js (v14 or higher)
- npm (v6 or higher)

## Getting Started

Follow these steps to set up and run the Trade Platform UI:

### Step 1: Install Dependencies

Navigate to the trade-ui directory and install the required dependencies:

```bash
cd trade-platform/trade-ui
npm install
```

### Step 2: Start the Backend Services

Before starting the UI, make sure the backend services are running:

1. Start Kafka (from the project root):
```bash
docker-compose up -d
```

2. Start the Portfolio Service:
```bash
cd trade-platform/portfolio-service
mvn spring-boot:run
```

3. Start the Trade Processor:
```bash
cd trade-platform/trade-processor
mvn spring-boot:run
```

4. Start the Trade API:
```bash
cd trade-platform/trade-api
mvn spring-boot:run
```

### Step 3: Start the UI Application

Start the React development server:

```bash
cd trade-platform/trade-ui
npm start
```

The application will be available at [http://localhost:3000](http://localhost:3000).

## Login Credentials

Use the following credentials to log in:

- Username: Chaitu786
- Password: Test123

## Features

- User authentication with Spring Security
- View all trade orders in a responsive, card-based layout
- Orders are color-coded by status for easy identification
- Detailed information for each order including execution price, filled quantity, etc.

## Development

### Building for Production

To create a production build:

```bash
npm run build
```

This will create optimized files in the `build` directory that can be deployed to a web server.
