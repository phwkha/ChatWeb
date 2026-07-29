import React from 'react';
import { Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';

const AdminRoute = ({ children }) => {
  const { isAuthenticated, user } = useSelector(state => state.auth);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // Assuming user object has a 'role' or 'permissions' array
  // If user is Admin or has specific admin permissions
  const isAdmin = user?.role === 'ADMIN' || user?.role === 'ROLE_ADMIN';
  const hasAdminPrivileges = user?.permissions && user.permissions.length > 0; // Check specific permissions later

  if (!isAdmin && !hasAdminPrivileges) {
    // Redirect to home if they have no admin access whatsoever
    return <Navigate to="/" replace />;
  }

  return children;
};

export default AdminRoute;
