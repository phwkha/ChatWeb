import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { loginSuccess, logout, setInitialized } from '../../store/slices/authSlice';
import apiClient from '../../services/apiClient';

const AuthInitializer = ({ children }) => {
  const dispatch = useDispatch();
  const { isInitialized } = useSelector(state => state.auth);

  useEffect(() => {
    const initAuth = async () => {
      try {
        // Because of withCredentials: true, HttpOnly cookies are automatically sent.
        // If the cookie is valid, the backend will return the user profile.
        const res = await apiClient.get('/api/users/profile');
        if (res.data && res.data.data) {
          dispatch(loginSuccess(res.data.data));
        } else {
          dispatch(logout());
        }
      } catch (err) {
        // Catch 401 Unauthorized or any other error and treat as logged out
        dispatch(logout());
      } finally {
        dispatch(setInitialized(true));
      }
    };

    initAuth();
  }, [dispatch]);

  if (!isInitialized) {
    return (
      <div className="min-h-screen w-full flex items-center justify-center bg-bg-dark text-white">
        <div className="flex flex-col items-center">
          <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin mb-4"></div>
          <p className="text-gray-400">Authenticating...</p>
        </div>
      </div>
    );
  }

  return children;
};

export default AuthInitializer;
