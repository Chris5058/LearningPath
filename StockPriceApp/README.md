# Stock Price App

A modern web application that displays real-time stock prices for selected companies. The application includes a login page and a dashboard to view stock prices.

## Features

- Secure login with username and password authentication
- Real-time stock price display for TSLA, NVDA, ORCL, and CCJ using Alpha Vantage API
- Modern and responsive UI
- Automatic price refresh every 30 seconds

## Technology Stack

- **Frontend**: React.js with Bootstrap for styling
- **Backend**: Spring Boot microservices
- **External APIs**: Alpha Vantage for real-time stock market data

## Prerequisites

- Java 17 or higher
- Node.js 14 or higher
- npm 6 or higher
- Maven 3.6 or higher

## Setup and Running

### Backend

1. Navigate to the backend directory:
   ```
   cd StockPriceApp/backend
   ```

2. Build the application:
   ```
   mvn clean install
   ```

3. Run the application:
   ```
   mvn spring-boot:run
   ```

The backend server will start on http://localhost:8080.

### Frontend

1. Navigate to the frontend directory:
   ```
   cd StockPriceApp/frontend
   ```

2. Install dependencies:
   ```
   npm install
   ```

3. Start the development server:
   ```
   npm start
   ```

The frontend application will start on http://localhost:3000.

## Usage

1. Open your browser and go to http://localhost:3000
2. Login with the following credentials:
   - Username: Chaitu786
   - Password: Manu123$
3. After successful login, you will be redirected to the dashboard where you can view the current stock prices.
4. The stock prices will automatically refresh every 30 seconds, or you can click the "Refresh Prices" button to update them manually.
5. Click the "Logout" button to return to the login page.

## Project Structure

```
StockPriceApp/
├── backend/                  # Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/        # Java source files
│   │   │   └── resources/   # Configuration files
│   └── pom.xml              # Maven configuration
└── frontend/                # React frontend
    ├── public/              # Static files
    ├── src/                 # React source files
    │   ├── components/      # React components
    │   ├── App.js           # Main application component
    │   └── index.js         # Entry point
    └── package.json         # npm configuration
```

## Login Credentials

- Username: Chaitu786
- Password: Manu123$

## Alpha Vantage API Integration

This application uses the Alpha Vantage API to fetch real-time stock prices. To use this feature:

1. Get a free API key from [Alpha Vantage](https://www.alphavantage.co/support/#api-key)
2. Open the file `backend/src/main/resources/application.properties`
3. Replace `YOUR_API_KEY` with your actual Alpha Vantage API key:
   ```
   alphavantage.api.key=YOUR_ACTUAL_API_KEY
   ```

Note: If you don't provide a valid API key, the application will use fallback prices for demonstration purposes.
