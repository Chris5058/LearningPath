import React, { useState } from 'react';
import { Form, Button, Alert, Container, Card } from 'react-bootstrap';
import AuthService from '../services/AuthService';

const LoginPage = ({ onLogin }) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const success = await AuthService.login(username, password);
      onLogin(success);
    } catch (err) {
      setError('Invalid username or password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container className="login-container">
      <Card>
        <Card.Header as="h3" className="text-center">Trade Platform Login</Card.Header>
        <Card.Body>
          {error && <Alert variant="danger">{error}</Alert>}
          <Form onSubmit={handleSubmit}>
            <Form.Group className="mb-3" controlId="formUsername">
              <Form.Label>Username</Form.Label>
              <Form.Control
                type="text"
                placeholder="Enter username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </Form.Group>

            <Form.Group className="mb-3" controlId="formPassword">
              <Form.Label>Password</Form.Label>
              <Form.Control
                type="password"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </Form.Group>

            <div className="d-grid gap-2">
              <Button variant="primary" type="submit" disabled={loading}>
                {loading ? 'Logging in...' : 'Login'}
              </Button>
            </div>
          </Form>
        </Card.Body>
        <Card.Footer className="text-muted text-center">
          <small>Use the credentials provided by your administrator</small>
        </Card.Footer>
      </Card>
    </Container>
  );
};

export default LoginPage;