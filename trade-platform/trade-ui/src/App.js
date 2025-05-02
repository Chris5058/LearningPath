import React, { useState } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './components/LoginPage';
import OrdersPage from './components/OrdersPage';
import './App.css';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  
  const handleLogin = (success) => {
    setIsAuthenticated(success);
  };

  return (
    <div className="App">
      <Routes>
        <Route path="/login" element={
          isAuthenticated ? 
            <Navigate to="/orders" replace /> : 
            <LoginPage onLogin={handleLogin} />
        } />
        <Route path="/orders" element={
          isAuthenticated ? 
            <OrdersPage /> : 
            <Navigate to="/login" replace />
        } />
        <Route path="/" element={<Navigate to="/login" replace />} />
      </Routes>
    </div>
  );
}

export default App;