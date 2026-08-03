import axios from 'axios';
import { toast } from '../utils/toast';
import webSocketClient from './webSocketClient';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true // Important if using cookies, otherwise can be false depending on token storage
});

// Request Interceptor
apiClient.interceptors.request.use(
  (config) => {
    // Add Accept-Language for backend i18n
    const language = localStorage.getItem('i18nextLng') || 'vi';
    config.headers['Accept-Language'] = language;
    
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor for handling 401 and refresh token
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    // Check specific custom code from backend
    const isTokenExpired = error.response?.data?.code === 4011;
    const isUnauthorized = error.response?.status === 401 || isTokenExpired;

    // Check if error is token expiration and request hasn't been retried yet
    if (isTokenExpired && !originalRequest._retry && originalRequest.url !== '/api/auth/login') {
      
      if (isRefreshing) {
        return new Promise(function(resolve, reject) {
          failedQueue.push({ resolve, reject });
        }).then(token => {
          originalRequest.headers['Authorization'] = 'Bearer ' + token;
          return apiClient(originalRequest);
        }).catch(err => {
          return Promise.reject(err);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // The exact route based on BE grep: /api/auth/refresh-token (POST)
        // Refresh token is automatically sent via HttpOnly cookie
        await axios.post(`${apiClient.defaults.baseURL}/api/auth/refresh-token`, {}, {
          withCredentials: true
        });

        // Backend automatically sets the new cookies
        // We can just retry the original request
        webSocketClient.reconnect(); // No need to pass token, WS relies on cookies

        processQueue(null, true);
        return apiClient(originalRequest);
        
      } catch (err) {
        processQueue(err, null);
        if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
          window.location.href = '/login';
        }
        return Promise.reject(err);
      } finally {
        isRefreshing = false;
      }
    } else if (isUnauthorized) {
      // If it's a 401 but NOT 4011 (e.g. Invalid Token)
      // Do not hard redirect if it's just the initial profile check or already on auth pages
      const isProfileCheck = originalRequest.url === '/api/users/profile';
      const isAuthPage = window.location.pathname === '/login' || window.location.pathname === '/register';
      
      if (!isProfileCheck && !isAuthPage) {
        window.location.href = '/login';
      }
    }

    // Show a beautiful global toast for any API error!
    // Skip toast for 401 on profile check as it's expected when not logged in
    const isProfileCheck = originalRequest.url === '/api/users/profile';
    if (!(isUnauthorized && isProfileCheck)) {
      if (error.response?.data?.message) {
        toast.error(error.response.data.message);
      } else if (error.message) {
        toast.error(error.message);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
