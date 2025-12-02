import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import Navbar from "./components/Navbar";
import ProtectedRoute from "./components/ProtectedRoute";
import MainPage from "./pages/MainPage";
import Login from "./pages/Login";
import Signup from "./pages/Signup";
import Chat from "./pages/Chat";
import AdminRoute from "./components/AdminRoute";
import AdminDashboard from "./pages/AdminDashboard";
import UserProfile from "./pages/UserProfile";
import { authService } from "./services/authService";
import "./styles/Auth.css";
import "./styles/Chat.css";
import "./styles/ChatArea.css";
import "./styles/MainPage.css";
import "./styles/Navbar.css";
import "./styles/PrivateChat.css";


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