import axios from 'axios';

const API_URL = 'http://localhost:8081/api/v1';

class AuthService {
  async login(username, password) {
    try {
      console.log('Attempting login with username:', username);

      // Store the credentials for future requests
      this.setAuthHeader(username, password);

      console.log('Auth header set, making test request to:', `${API_URL}/orders`);

      // Make a test request to verify credentials
      const response = await axios.get(`${API_URL}/orders`);

      console.log('Login successful, response:', response.status);

      // If the request is successful, store the credentials in local storage
      localStorage.setItem('user', JSON.stringify({ username }));
      return true;
    } catch (error) {
      // If the request fails, clear the auth header
      this.clearAuthHeader();
      console.error('Login failed:', error.message);
      if (error.response) {
        console.error('Response status:', error.response.status);
        console.error('Response data:', error.response.data);
      }
      throw error;
    }
  }

  logout() {
    localStorage.removeItem('user');
    this.clearAuthHeader();
  }

  getCurrentUser() {
    return JSON.parse(localStorage.getItem('user'));
  }

  isAuthenticated() {
    return !!localStorage.getItem('user');
  }

  setAuthHeader(username, password) {
    try {
      const token = btoa(`${username}:${password}`);
      console.log('Generated token for Basic Auth');
      axios.defaults.headers.common['Authorization'] = `Basic ${token}`;
      console.log('Authorization header set');
    } catch (error) {
      console.error('Error setting auth header:', error.message);
    }
  }

  clearAuthHeader() {
    delete axios.defaults.headers.common['Authorization'];
  }

  setupAxiosInterceptors() {
    axios.interceptors.response.use(
      response => response,
      error => {
        if (error.response && error.response.status === 401) {
          this.logout();
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }
}

const authService = new AuthService();
authService.setupAxiosInterceptors();

export default authService;
