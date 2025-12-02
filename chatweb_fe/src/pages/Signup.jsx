import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {authService} from "../services/authService";
import "../styles/Auth.css";

const Signup = () => {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [email, setEmail] = useState("");
    const [message, setMessage] = useState("");
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();

    const handleSignup = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        setMessage("");

        try {
            const response = await authService.signup({ username, email, password });
            if (response.success) {
                setMessage("Signup succesfully! Redirecting to login...");
                setTimeout(() => {
                    navigate("/login");
                }, 2000);
            }
        } catch (error) {
            setMessage(error.message || "Signup failed. Please try again.");
            console.error("Signup error:", error);
        }
        finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="signup-container">
            <div className="signup-box">
                <div className="signup-header">
                    <h2>Signup</h2>
                    <p>Create an account</p>
                </div>
                <form onSubmit={handleSignup} className="signup-form">
                    <input
                        type="text"
                        placeholder="Username"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        className="username-input"
                        maxLength={20}
                        required
                    />
                    <input
                        type="email"
                        placeholder="Email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
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
                    <button type="submit"
                        className="signup-button"
                        disabled={!username.trim() || !email.trim() || !password.trim() || isLoading}>
                        {isLoading ? "Signing up..." : "Signup"}
                    </button>
                    {message && (
                        <p className="auth-message"
                            style={{ color: message.includes('succesfully') ? '#4caF50' : '#ff6b6b' }}
                        >
                            {message}
                        </p>
                    )}
                </form>
            </div>
        </div>
    );

};
export default Signup;