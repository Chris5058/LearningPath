import axios from 'axios';

const API_URL = 'http://localhost:8081/api/v1';

class OrderService {
  async getAllOrders() {
    try {
      const response = await axios.get(`${API_URL}/orders`);
      return response.data;
    } catch (error) {
      console.error('Error fetching orders:', error);
      throw error;
    }
  }

  async getOrderById(orderId) {
    try {
      const response = await axios.get(`${API_URL}/orders/${orderId}`);
      return response.data;
    } catch (error) {
      console.error(`Error fetching order ${orderId}:`, error);
      throw error;
    }
  }

  async getOrdersByUserId(userId) {
    try {
      const response = await axios.get(`${API_URL}/orders/user/${userId}`);
      return response.data;
    } catch (error) {
      console.error(`Error fetching orders for user ${userId}:`, error);
      throw error;
    }
  }
}

export default new OrderService();
