import { Link } from "react-router-dom";
import { useNavigate } from "react-router-dom";
import { authService } from "../services/authService";
import { useCrypto } from "../context/CryptoContext";
import "../styles/Navbar.css";


const Navbar = () => {

    const navigate = useNavigate();
    const { lockKeys } = useCrypto();
    const isAuthenticated = authService.isAuthenticated();
    const currentUser = authService.getCurrentUser();
    const isAdmin = currentUser && (currentUser.role === 'ADMIN' || currentUser.role === 'ADMIN_PRO');

    const handleLogout = async() => {
        try {
            await authService.logout();
            lockKeys();
            navigate("/login");
        } catch (error) {
            console.error("Logout failed:", error);
            lockKeys();
            localStorage.clear();
            navigate("/login");
        }
    };

    return (
        <div className="navbar">
            <div className="navbar-container">
                <div className="navbar-left">
                    <Link to="/" className="navbar-brand">
                        <h1>ChatApp</h1>
                    </Link>
                    <div className="navbar-menu">
                        {isAuthenticated ? (
                            <>
                                <Link to="/chat" className="navbar-link">
                                    Chat area
                                </Link>
                                <Link to="/profile" className="navbar-link">
                                    Profile
                                </Link>
                                {isAdmin && (
                                    <Link to="/admin" className="navbar-link" style={{background: '#ff9800'}}>
                                        Admin
                                    </Link>
                                )}
                                <div className="navbar-user">
                                    <span className="user-info">
                                        Welcome {currentUser.username}
                                    </span>
                                    <button className="logout-button" onClick={handleLogout}>
                                        Logout
                                    </button>
                                </div>
                            </>
                        ) : (
                            <>
                                <Link to="/login" className="navbar-link">
                                    Login
                                </Link>
                                <Link to="/signup" className="navbar-link">
                                    Signup
                                </Link>
                            </>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};
export default Navbar;