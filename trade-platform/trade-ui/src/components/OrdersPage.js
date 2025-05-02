import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Badge, Button, Spinner, Alert } from 'react-bootstrap';
import OrderService from '../services/OrderService';
import AuthService from '../services/AuthService';
import { useNavigate } from 'react-router-dom';

const OrdersPage = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    fetchOrders();
  }, []);

  const fetchOrders = async () => {
    try {
      setLoading(true);
      const data = await OrderService.getAllOrders();
      setOrders(data);
      setError('');
    } catch (err) {
      console.error('Error fetching orders:', err);
      setError('Failed to load orders. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    AuthService.logout();
    navigate('/login');
  };

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'CREATED': return 'status-created';
      case 'PENDING': return 'status-pending';
      case 'PROCESSING': return 'status-processing';
      case 'FILLED': return 'status-filled';
      case 'PARTIALLY_FILLED': return 'status-partially-filled';
      case 'CANCELLED': return 'status-cancelled';
      case 'REJECTED': return 'status-rejected';
      case 'FAILED': return 'status-failed';
      case 'EXPIRED': return 'status-expired';
      default: return '';
    }
  };

  const formatDateTime = (dateTimeStr) => {
    if (!dateTimeStr) return 'N/A';
    const date = new Date(dateTimeStr);
    return date.toLocaleString();
  };

  return (
    <Container fluid className="orders-container">
      <Row className="mb-4">
        <Col>
          <h1 className="text-center">Trade Orders</h1>
        </Col>
        <Col xs="auto">
          <Button variant="outline-secondary" onClick={handleLogout}>Logout</Button>
        </Col>
      </Row>

      {error && <Alert variant="danger">{error}</Alert>}

      {loading ? (
        <div className="text-center my-5">
          <Spinner animation="border" role="status">
            <span className="visually-hidden">Loading...</span>
          </Spinner>
          <p className="mt-2">Loading orders...</p>
        </div>
      ) : orders.length === 0 ? (
        <Alert variant="info">No orders found.</Alert>
      ) : (
        <Row>
          {orders.map(order => (
            <Col key={order.orderId} xs={12} md={6} lg={4} className="mb-4">
              <Card className="order-card h-100">
                <Card.Header className="order-header">
                  <div>
                    <strong>{order.symbol}</strong> - {order.orderType}
                  </div>
                  <Badge className={`status-badge ${getStatusBadgeClass(order.status)}`}>
                    {order.status}
                  </Badge>
                </Card.Header>
                <Card.Body className="order-body">
                  <Row className="mb-2">
                    <Col xs={6}><strong>Order ID:</strong></Col>
                    <Col xs={6}>{order.orderId}</Col>
                  </Row>
                  <Row className="mb-2">
                    <Col xs={6}><strong>User ID:</strong></Col>
                    <Col xs={6}>{order.userId}</Col>
                  </Row>
                  <Row className="mb-2">
                    <Col xs={6}><strong>Quantity:</strong></Col>
                    <Col xs={6}>{order.quantity}</Col>
                  </Row>
                  <Row className="mb-2">
                    <Col xs={6}><strong>Price:</strong></Col>
                    <Col xs={6}>{order.price ? `$${order.price}` : 'N/A'}</Col>
                  </Row>
                  <Row className="mb-2">
                    <Col xs={6}><strong>Execution Price:</strong></Col>
                    <Col xs={6}>{order.executionPrice ? `$${order.executionPrice}` : 'N/A'}</Col>
                  </Row>
                  <Row className="mb-2">
                    <Col xs={6}><strong>Filled Quantity:</strong></Col>
                    <Col xs={6}>{order.filledQuantity || 0}</Col>
                  </Row>
                  <Row className="mb-2">
                    <Col xs={6}><strong>Remaining Quantity:</strong></Col>
                    <Col xs={6}>{order.remainingQuantity || 0}</Col>
                  </Row>
                </Card.Body>
                <Card.Footer className="order-footer text-muted">
                  <small>
                    Created: {formatDateTime(order.createdAt)}<br />
                    Updated: {formatDateTime(order.updatedAt)}<br />
                    {order.executedAt && `Executed: ${formatDateTime(order.executedAt)}`}
                  </small>
                </Card.Footer>
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </Container>
  );
};

export default OrdersPage;