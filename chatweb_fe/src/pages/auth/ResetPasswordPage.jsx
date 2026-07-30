import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import { KeyRound, Lock, ArrowRight, Loader2, Key } from 'lucide-react';
import apiClient from '../../services/apiClient';
import { toast } from '../../utils/toast';

const ResetPasswordPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const initialEmail = searchParams.get('email') || '';

  const [email, setEmail] = useState(initialEmail);
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [resendLoading, setResendLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (newPassword.length < 8) {
      setError('Password must be at least 8 characters long');
      return;
    }

    setLoading(true);
    try {
      await apiClient.post('/api/auth/reset-password', { email, otp, newPassword });
      toast.success('Password reset successfully! Please login with your new password.');
      navigate('/login');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to reset password');
    } finally {
      setLoading(false);
    }
  };

  const handleResendOtp = async () => {
    if (!email) {
      setError('Please enter your email to resend OTP');
      return;
    }
    setResendLoading(true);
    setError('');
    try {
      await apiClient.post('/api/auth/resend-forgot-password', { email });
      toast.success('A new OTP has been sent to your email.');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to resend OTP');
    } finally {
      setResendLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 relative overflow-hidden">
      <div className="absolute top-1/4 right-1/4 w-72 h-72 bg-secondary/30 rounded-full mix-blend-screen filter blur-[100px] animate-pulse"></div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="w-full max-w-md"
      >
        <div className="glass-dark p-8 rounded-3xl relative z-10 text-center">
          <div className="w-16 h-16 bg-secondary/20 rounded-full flex items-center justify-center mx-auto mb-6 text-secondary">
            <KeyRound size={32} />
          </div>
          
          <h1 className="text-3xl font-display font-bold text-white mb-2">Reset Password</h1>
          
          <p className="text-gray-400 text-sm mb-8">
            Enter the OTP sent to your email and your new password.
          </p>

          {error && (
            <div className="bg-red-500/10 border border-red-500/50 text-red-400 p-3 rounded-xl mb-6 text-sm">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6 text-left">
            <div className="relative">
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-secondary/50 transition-all"
                placeholder="Email address"
              />
            </div>

            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-500">
                <Key size={18} />
              </div>
              <input
                type="text"
                required
                value={otp}
                onChange={(e) => setOtp(e.target.value)}
                className="w-full pl-10 pr-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-secondary/50 transition-all"
                placeholder="6-digit OTP code"
              />
            </div>

            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-500">
                <Lock size={18} />
              </div>
              <input
                type="password"
                required
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className="w-full pl-10 pr-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-secondary/50 transition-all"
                placeholder="New Password (min 8 chars)"
              />
            </div>

            <button
              type="submit"
              disabled={loading || resendLoading}
              className="w-full py-3 px-4 bg-gradient-to-r from-secondary to-primary hover:from-secondary-dark hover:to-primary-dark text-white rounded-xl font-medium flex items-center justify-center gap-2 transform hover:scale-[1.02] transition-all disabled:opacity-70 disabled:cursor-not-allowed"
            >
              {loading ? (
                <Loader2 className="animate-spin" size={20} />
              ) : (
                <>
                  Reset Password
                  <ArrowRight size={18} />
                </>
              )}
            </button>
          </form>

          <div className="mt-6 text-sm">
            <button 
              onClick={handleResendOtp}
              disabled={resendLoading || loading}
              className="text-secondary hover:text-secondary-dark transition-colors flex items-center justify-center w-full gap-2"
            >
              {resendLoading ? <Loader2 className="animate-spin" size={14} /> : null}
              Didn't receive the code? Resend OTP
            </button>
          </div>

          <div className="mt-8 text-sm">
            <Link to="/login" className="text-gray-400 hover:text-white transition-colors">
              Back to Login
            </Link>
          </div>
        </div>
      </motion.div>
    </div>
  );
};

export default ResetPasswordPage;
