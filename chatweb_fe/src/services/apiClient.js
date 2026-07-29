import axios from 'axios';
import { toast } from '../utils/toast';

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
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
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
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) throw new Error("No refresh token available");

        // The exact route based on BE grep: /api/auth/refresh-token (POST)
        const res = await axios.post(`${apiClient.defaults.baseURL}/api/auth/refresh-token`, {
          token: refreshToken
        });

        // Assuming response structure contains new token
        const newAccessToken = res.data.data?.accessToken || res.data.accessToken; 
        const newRefreshToken = res.data.data?.refreshToken || res.data.refreshToken;

        localStorage.setItem('accessToken', newAccessToken);
        if (newRefreshToken) {
          localStorage.setItem('refreshToken', newRefreshToken);
        }

        apiClient.defaults.headers.common['Authorization'] = 'Bearer ' + newAccessToken;
        originalRequest.headers['Authorization'] = 'Bearer ' + newAccessToken;
        
        processQueue(null, newAccessToken);
        return apiClient(originalRequest);
        
      } catch (err) {
        processQueue(err, null);
        // Dispatch logout action or redirect to login
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/login';
        return Promise.reject(err);
      } finally {
        isRefreshing = false;
      }
    } else if (isUnauthorized) {
      // If it's a 401 but NOT 4011 (e.g. Invalid Token)
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      window.location.href = '/login';
    }

    // Show a beautiful global toast for any API error!
    if (error.response?.data?.message) {
      toast.error(error.response.data.message);
    } else if (error.message) {
      toast.error(error.message);
    }

    return Promise.reject(error);
  }
);

export default apiClient;
