import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';

// Auth Pages
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import VerifyAccountPage from './pages/auth/VerifyAccountPage';
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage';

// Main App Pages
import ChatLayout from './pages/chat/ChatLayout';
import ProfilePage from './pages/profile/ProfilePage';

// Admin Pages
import AdminLayout from './pages/admin/AdminLayout';
import AdminRoute from './router/AdminRoute';

import AuthInitializer from './components/auth/AuthInitializer';
import ToastContainer from './components/ui/ToastContainer';

const ProtectedRoute = ({ children }) => {
  const isAuthenticated = useSelector((state) => state.auth.isAuthenticated);
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return children;
};

function App() {
  const { i18n } = useTranslation();

  useEffect(() => {
    // Sync document direction and lang attribute with i18n
    document.documentElement.lang = i18n.language;
  }, [i18n.language]);

  return (
    <Router>
      <div className="min-h-screen w-full bg-transparent flex flex-col">
        <AuthInitializer>
          <Routes>
            {/* Public Routes */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/verify-account" element={<VerifyAccountPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            
            {/* Protected Routes */}
            <Route 
              path="/" 
              element={
                <ProtectedRoute>
                  <ChatLayout />
                </ProtectedRoute>
              } 
            />
            <Route 
              path="/profile" 
              element={
                <ProtectedRoute>
                  <ProfilePage />
                </ProtectedRoute>
              } 
            />

            {/* Admin Routes */}
            <Route 
              path="/admin/*" 
              element={
                <AdminRoute>
                  <AdminLayout />
                </AdminRoute>
              } 
            />
            
            {/* Fallback */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AuthInitializer>
        <ToastContainer />
      </div>
    </Router>
  );
}

export default App;
