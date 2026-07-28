import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { authService } from "../../services";

const ForgotPassword = () => {
    const [email, setEmail] = useState("");
    const [message, setMessage] = useState("");
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();

    const handleForgot = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        setMessage("");
        try {
            await authService.forgotPassword(email);
            setMessage("Instructions sent! Redirecting to reset page...");
            setTimeout(() => navigate("/reset-password", { state: { email } }), 2000);
        } catch (error) {
            setMessage(error.message || "Failed to send reset instructions.");
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center p-4 relative overflow-hidden">
            <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-indigo-600/10 rounded-full blur-[120px] pointer-events-none"></div>
            <div className="absolute bottom-0 left-0 w-[500px] h-[500px] bg-purple-600/10 rounded-full blur-[120px] pointer-events-none"></div>
            
            <div className="w-full max-w-md glass-panel p-8 md:p-10 rounded-3xl animate-enter z-10">
                <div className="text-center mb-8">
                    <h2 className="text-3xl font-bold text-white mb-2">Forgot Password</h2>
                    <p className="text-slate-400">Enter your email to receive an OTP</p>
                </div>
                
                <form onSubmit={handleForgot} className="space-y-5">
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-1">Email</label>
                        <input
                            type="email"
                            placeholder="Enter your registered email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className="w-full glass-input"
                            required
                            disabled={isLoading}
                        />
                    </div>
                    
                    {message && (
                        <div className={`p-3 rounded-xl text-sm ${message.includes("sent") ? "bg-emerald-500/10 text-emerald-400" : "bg-rose-500/10 text-rose-400"}`}>
                            {message}
                        </div>
                    )}

                    <button
                        type="submit"
                        disabled={isLoading}
                        className="w-full btn-primary py-3 rounded-xl font-semibold text-white shadow-lg disabled:opacity-50"
                    >
                        {isLoading ? "Sending..." : "Send Reset Code"}
                    </button>
                </form>

                <div className="mt-6 text-center">
                    <Link to="/login" className="text-sm text-indigo-400 hover:text-indigo-300 transition-colors">
                        Back to Login
                    </Link>
                </div>
            </div>
        </div>
    );
};
export default ForgotPassword;
