import { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { authService } from "../../services";

const VerifyAccount = () => {
    const [otp, setOtp] = useState("");
    const [email, setEmail] = useState("");
    const [message, setMessage] = useState("");
    const [isLoading, setIsLoading] = useState(false);
    const [countdown, setCountdown] = useState(0);
    const navigate = useNavigate();
    const location = useLocation();

    useEffect(() => {
        if (location.state?.email) {
            setEmail(location.state.email);
        }
    }, [location.state]);

    useEffect(() => {
        if (countdown > 0) {
            const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
            return () => clearTimeout(timer);
        }
    }, [countdown]);

    const handleVerify = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        setMessage("");
        try {
            await authService.verifyAccount(email, otp);
            setMessage("Account verified successfully! Redirecting to login...");
            setTimeout(() => navigate("/login"), 2000);
        } catch (error) {
            setMessage(error.message || "Verification failed.");
        } finally {
            setIsLoading(false);
        }
    };

    const handleResend = async () => {
        if (countdown > 0) return;
        setIsLoading(true);
        setMessage("");
        try {
            await authService.resendOtp(email);
            setMessage("A new OTP has been sent to your email.");
            setCountdown(60);
        } catch (error) {
            setMessage(error.message || "Failed to resend OTP.");
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
                    <h2 className="text-3xl font-bold text-white mb-2">Verify Account</h2>
                    <p className="text-slate-400">Enter the OTP sent to your email</p>
                </div>
                
                <form onSubmit={handleVerify} className="space-y-5">
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-1">Email</label>
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className="w-full glass-input"
                            required
                            disabled={isLoading}
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-1">OTP Code</label>
                        <input
                            type="text"
                            placeholder="6-digit code"
                            value={otp}
                            onChange={(e) => setOtp(e.target.value)}
                            className="w-full glass-input text-center text-xl tracking-[0.5em]"
                            maxLength={6}
                            required
                            disabled={isLoading}
                        />
                    </div>
                    
                    {message && (
                        <div className={`p-3 rounded-xl text-sm ${message.includes("success") || message.includes("sent") ? "bg-emerald-500/10 text-emerald-400" : "bg-rose-500/10 text-rose-400"}`}>
                            {message}
                        </div>
                    )}

                    <button
                        type="submit"
                        disabled={isLoading}
                        className="w-full btn-primary py-3 rounded-xl font-semibold text-white shadow-lg disabled:opacity-50"
                    >
                        {isLoading ? "Verifying..." : "Verify Account"}
                    </button>
                </form>

                <div className="mt-6 text-center">
                    <button 
                        onClick={handleResend}
                        disabled={countdown > 0 || isLoading || !email}
                        className={`text-sm ${countdown > 0 ? 'text-slate-500' : 'text-indigo-400 hover:text-indigo-300'} transition-colors`}
                    >
                        {countdown > 0 ? `Resend OTP in ${countdown}s` : "Didn't receive code? Resend"}
                    </button>
                </div>
            </div>
        </div>
    );
};
export default VerifyAccount;
