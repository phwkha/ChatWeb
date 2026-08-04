import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ShieldCheck, ArrowRight, Loader2, RefreshCw } from 'lucide-react';
import apiClient from '../../services/apiClient';

const VerifyAccountPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const email = location.state?.email;

  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (!email) {
      navigate('/login');
    }
  }, [email, navigate]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);

    try {
      await apiClient.post('/api/auth/verify-account', { email, otp: code });
      setMessage('Account verified successfully! Redirecting...');
      setTimeout(() => navigate('/login'), 2000);
    } catch (err) {
      setError(err.response?.data?.message || 'Verification failed');
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    setError('');
    setMessage('');
    setResending(true);
    try {
      await apiClient.post('/api/auth/resend-otp', { email });
      setMessage('A new code has been sent to your email.');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to resend code');
    } finally {
      setResending(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 relative overflow-hidden">
      {/* Animated Background Elements */}
      <div className="absolute top-1/4 left-1/4 w-72 h-72 bg-accent/30 rounded-full mix-blend-screen filter blur-[100px] animate-pulse"></div>

      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.5, ease: 'easeOut' }}
        className="w-full max-w-md"
      >
        <div className="glass-dark p-8 rounded-3xl relative z-10 text-center">
          <div className="w-16 h-16 bg-accent/20 rounded-full flex items-center justify-center mx-auto mb-6 text-accent">
            <ShieldCheck size={32} />
          </div>
          
          <h1 className="text-3xl font-display font-bold text-white mb-2">Verify Account</h1>
          <p className="text-gray-400 text-sm mb-8">
            We've sent a verification code to <span className="text-white font-medium">{email}</span>.
          </p>

          {error && (
            <div className="bg-red-500/10 border border-red-500/50 text-red-400 p-3 rounded-xl mb-6 text-sm">
              {error}
            </div>
          )}
          {message && (
            <div className="bg-green-500/10 border border-green-500/50 text-green-400 p-3 rounded-xl mb-6 text-sm">
              {message}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            <input
              type="text"
              required
              maxLength={6}
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/[^0-9]/g, ''))}
              className="w-full text-center text-3xl tracking-[1em] py-4 bg-white/5 border border-white/10 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-accent/50 transition-all font-mono"
              placeholder="••••••"
            />

            <button
              type="submit"
              disabled={loading || code.length < 6}
              className="w-full py-3 px-4 bg-gradient-to-r from-accent to-primary hover:from-accent hover:to-primary-dark text-white rounded-xl font-medium flex items-center justify-center gap-2 transform hover:scale-[1.02] transition-all disabled:opacity-70 disabled:cursor-not-allowed"
            >
              {loading ? (
                <Loader2 className="animate-spin" size={20} />
              ) : (
                <>
                  Verify Account
                  <ArrowRight size={18} />
                </>
              )}
            </button>
          </form>

          <div className="mt-8">
            <button
              onClick={handleResend}
              disabled={resending}
              className="text-sm text-gray-400 hover:text-white transition-colors flex items-center justify-center gap-2 mx-auto disabled:opacity-50"
            >
              {resending ? <Loader2 className="animate-spin" size={14} /> : <RefreshCw size={14} />}
              Resend Code
            </button>
          </div>
          
          <div className="mt-6 text-sm">
            <Link to="/login" className="text-accent hover:text-white transition-colors">
              Back to Login
            </Link>
          </div>
        </div>
      </motion.div>
    </div>
  );
};

export default VerifyAccountPage;
