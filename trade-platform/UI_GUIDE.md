# Trade Platform UI Guide

This guide provides step-by-step instructions on how to open and use the Trade Platform UI.

## Opening the UI

### Prerequisites
- Node.js (v14 or higher)
- npm (v6 or higher)
- Java 11 or higher
- Maven
- Docker and Docker Compose

### Step 1: Start Kafka
From the project root directory:
```bash
docker-compose up -d
```

### Step 2: Start the Backend Services
Start each service in the following order:

1. Portfolio Service:
```bash
cd trade-platform/portfolio-service
mvn spring-boot:run
```

2. Trade Processor:
```bash
cd trade-platform/trade-processor
mvn spring-boot:run
```

3. Trade API:
```bash
cd trade-platform/trade-api
mvn spring-boot:run
```

### Step 3: Install UI Dependencies (First Time Only)
```bash
cd trade-platform/trade-ui
npm install
```

### Step 4: Start the UI
```bash
cd trade-platform/trade-ui
npm start
```

The application will automatically open in your default browser at [http://localhost:3000](http://localhost:3000).

If it doesn't open automatically, you can manually navigate to [http://localhost:3000](http://localhost:3000) in your browser.

## Logging In

Use the following credentials to log in:
- Username: Chaitu786
- Password: Test123

## Using the UI

### Viewing Orders
After logging in, you'll be redirected to the Orders page where you can see all trade orders in a card-based layout.

Each order card displays:
- Symbol and order type
- Order status (color-coded)
- Order details (ID, user, quantity, price, etc.)
- Execution details (execution price, filled quantity, etc.)
- Timestamps (created, updated, executed)

### Order Status Colors
- Created: Gray
- Pending: Blue
- Processing: Yellow
- Filled: Green
- Partially Filled: Light Blue
- Cancelled/Rejected/Failed: Red
- Expired: Gray

### Logging Out
Click the "Logout" button in the top-right corner to log out of the application.

## Troubleshooting

### UI Can't Connect to Backend
If the UI can't connect to the backend services:
1. Ensure all backend services are running
2. Check that the API URL in the UI code is correct (default is http://localhost:8081/api/v1)
3. Verify there are no CORS issues by checking the browser console

### Login Issues
If you can't log in:
1. Verify you're using the correct credentials
2. Ensure the trade-processor service is running (it handles authentication)
3. Check the browser console for any error messages
