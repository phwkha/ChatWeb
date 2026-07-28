import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import Navbar from './components/common/Navbar';
import ProtectedRoute from './routes/ProtectedRoute';
import MainPage from './pages/home/MainPage';
import Login from './pages/auth/Login';
import Signup from './pages/auth/Signup';
import Chat from './pages/chat/Chat';
import AdminRoute from './routes/AdminRoute';
import AdminDashboard from './pages/admin/AdminDashboard';
import UserProfile from './pages/profile/UserProfile';
import VerifyAccount from './pages/auth/VerifyAccount';
import ForgotPassword from './pages/auth/ForgotPassword';
import ResetPassword from './pages/auth/ResetPassword';
import {  authService  } from './services';
import './index.css';

function App() {
  return (
    <Router>
      <div className="App">
        <Navbar />
        <Routes>
          <Route path="/" element={<MainPage />} />

          <Route path="/login" element={
            authService.isAuthenticated() ?
              <Navigate to="/chat" replace /> :
              <Login />
          } />

          <Route path="/signup" element={
            authService.isAuthenticated() ?
              <Navigate to="/chat" replace /> :
              <Signup />
          } />

          <Route path="/verify-account" element={
            authService.isAuthenticated() ?
              <Navigate to="/chat" replace /> :
              <VerifyAccount />
          } />

          <Route path="/forgot-password" element={
            authService.isAuthenticated() ?
              <Navigate to="/chat" replace /> :
              <ForgotPassword />
          } />

          <Route path="/reset-password" element={
            authService.isAuthenticated() ?
              <Navigate to="/chat" replace /> :
              <ResetPassword />
          } />

          <Route path="/chat" element={
            <ProtectedRoute>
              <Chat />
            </ProtectedRoute>
          } />

          <Route path="/admin" element={
            <AdminRoute>
              <AdminDashboard />
            </AdminRoute>
          } />

          <Route path="/profile" element={
            <ProtectedRoute>
              <UserProfile />
            </ProtectedRoute>
          } />

          <Route path="*" element={<Navigate to="/" />} />
        </Routes>

      </div>
    </Router>
  );
}
export default App;