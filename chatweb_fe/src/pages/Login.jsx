import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { authService } from "../services/authService";
import "../styles/Auth.css";

const Login = () => {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [message, setMessage] = useState("");
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        setMessage("");

        try {
            const response = await authService.login(username, password);
            if (response.success) {
                setMessage("Login successfully! Redirecting...");
                setTimeout(() => {
                    navigate("/chat");
                }, 1500);
            }
        } catch (error) {
            setMessage(error.message || "Login failed. Please try again.");
            console.error("Login error:", error);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="login-container">
            <div className="login-box">
                <div className="login-header">
                    <h2>Login</h2>
                    <p>Sign in to your account</p>
                </div>
                <form onSubmit={handleLogin} className="login-form">
                    <input
                        type="text"
                        placeholder="Username"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        className="username-input"
                        maxLength={20}
                        required
                        disabled={isLoading}
                    />
                    <input
                        type="password"
                        placeholder="Password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        className="username-input"
                        maxLength={20}
                        required
                        disabled={isLoading}
                    />
                    <button
                        type="submit"
                        className="login-button"
                        disabled={!username.trim() || !password.trim() || isLoading}
                    >
                        {isLoading ? "Logging in..." : "Login"}
                    </button>
                    {message && (
                        <p
                            className="auth-message"
                            style={{ color: message.includes('successfully') ? '#4caF50' : '#ff6b6b' }}
                        >
                            {message}
                        </p>
                    )}
                </form>
            </div>
        </div>
    );
};

export default Login;