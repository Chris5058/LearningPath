import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Button, Navbar, Spinner } from 'react-bootstrap';
import axios from 'axios';

const Dashboard = ({ onLogout }) => {
  const [stocks, setStocks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [refreshing, setRefreshing] = useState(false);

  const fetchStockPrices = async () => {
    try {
      setRefreshing(true);
      const response = await axios.get('/api/stocks');
      setStocks(response.data);
      setError('');
    } catch (err) {
      console.error('Error fetching stock prices:', err);
      setError('Failed to fetch stock prices. Please try again.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchStockPrices();
    // Refresh stock prices every 30 seconds
    const interval = setInterval(fetchStockPrices, 30000);
    return () => clearInterval(interval);
  }, []);

  const handleRefresh = () => {
    fetchStockPrices();
  };

  return (
    <div>
      <Navbar bg="primary" variant="dark" className="mb-4">
        <Container>
          <Navbar.Brand>Stock Price Dashboard</Navbar.Brand>
          <Navbar.Toggle />
          <Navbar.Collapse className="justify-content-end">
            <Navbar.Text className="me-3">
              Logged in as: <strong>Chaitu786</strong>
            </Navbar.Text>
            <Button variant="outline-light" className="btn-logout" onClick={onLogout}>
              Logout
            </Button>
          </Navbar.Collapse>
        </Container>
      </Navbar>

      <Container>
        <div className="dashboard-header d-flex justify-content-between align-items-center">
          <h2>Current Stock Prices</h2>
          <Button 
            variant="primary" 
            onClick={handleRefresh} 
            disabled={refreshing}
          >
            {refreshing ? (
              <>
                <Spinner as="span" animation="border" size="sm" role="status" aria-hidden="true" />
                <span className="ms-2">Refreshing...</span>
              </>
            ) : (
              'Refresh Prices'
            )}
          </Button>
        </div>

        {loading ? (
          <div className="text-center my-5">
            <Spinner animation="border" role="status" variant="primary">
              <span className="visually-hidden">Loading...</span>
            </Spinner>
            <p className="mt-3">Loading stock prices...</p>
          </div>
        ) : error ? (
          <div className="alert alert-danger">{error}</div>
        ) : (
          <Row>
            {stocks.map((stock) => (
              <Col key={stock.symbol} md={6} lg={3} className="mb-4">
                <Card className="h-100 stock-card">
                  <Card.Body>
                    <Card.Title className="stock-symbol">${stock.symbol}</Card.Title>
                    <Card.Subtitle className="mb-3 stock-name">{stock.name}</Card.Subtitle>
                    <Card.Text className="stock-price">${stock.price}</Card.Text>
                  </Card.Body>
                </Card>
              </Col>
            ))}
          </Row>
        )}
      </Container>
    </div>
  );
};

export default Dashboard;