import React, { useState, useEffect } from 'react';
import { Shield, Key, Mail, Smartphone, Loader2, Check, AlertTriangle, MonitorX, Trash2 } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import apiClient from '../../services/apiClient';
// Assuming logout action exists in authSlice
import { logout, updateProfile } from '../../store/slices/authSlice';
import { toast } from '../../utils/toast';

const SecuritySettings = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { user } = useSelector(state => state.auth);

  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');

  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });

  // Modal states
  const [modalType, setModalType] = useState(null); // 'email', 'phone', 'delete'
  const [modalStep, setModalStep] = useState(1); // 1: initiate, 2: verify
  const [modalInput, setModalInput] = useState('');
  const [otp, setOtp] = useState('');
  const [modalLoading, setModalLoading] = useState(false);
  const [modalError, setModalError] = useState('');
  const [countdown, setCountdown] = useState(0);

  useEffect(() => {
    let timer;
    if (countdown > 0) {
      timer = setInterval(() => {
        setCountdown((prev) => prev - 1);
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [countdown]);

  const handlePasswordChange = async (e) => {
    e.preventDefault();
    if (passwordForm.newPassword.length < 8) {
      setError("Password must be at least 8 characters long.");
      return;
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setError("New passwords do not match.");
      return;
    }
    setLoading(true);
    setError('');
    setSuccess('');
    try {
      await apiClient.post('/api/users/change-password', {
        oldPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword
      });
      setSuccess("Password updated successfully.");
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to change password');
    } finally {
      setLoading(false);
    }
  };

  const handleLogoutAll = async () => {
    if (!window.confirm("Are you sure you want to log out from all devices?")) return;
    try {
      setLoading(true);
      await apiClient.post('/api/auth/logout-all-devices');
      toast.success("Logged out from all devices successfully.");
      dispatch(logout());
      navigate('/login');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to logout from all devices');
      setLoading(false);
    }
  };

  const openModal = (type) => {
    setModalType(type);
    setModalStep(1);
    setModalInput('');
    setOtp('');
    setModalError('');
    setCountdown(0);
  };

  const closeModal = () => {
    setModalType(null);
  };

  const handleInitiateChange = async (e) => {
    e.preventDefault();
    setModalError('');
    
    if (modalType === 'phone') {
      const phoneRegex = /^(0[0-9]{9}|\+84[0-9]{9})$/;
      if (!phoneRegex.test(modalInput)) {
        setModalError("Invalid phone number. Must be 10 digits starting with 0 or +84.");
        return;
      }
    }

    setModalLoading(true);
    try {
      const endpoint = modalType === 'email' ? '/api/users/initiate-email-change' : '/api/users/initiate-phone-change';
      const payload = modalType === 'email' ? { newEmail: modalInput } : { newPhoneNumber: modalInput };
      await apiClient.post(endpoint, payload);
      setModalStep(2);
      setCountdown(60);
    } catch (err) {
      setModalError(err.response?.data?.message || `Failed to initiate ${modalType} change`);
    } finally {
      setModalLoading(false);
    }
  };

  const handleVerifyChange = async (e) => {
    e.preventDefault();
    setModalError('');
    setModalLoading(true);
    try {
      const endpoint = modalType === 'email' ? '/api/users/verify-email-change' : '/api/users/verify-phone-change';
      const payload = modalType === 'email' ? { newEmail: modalInput, code: otp } : { newPhoneNumber: modalInput, code: otp };
      await apiClient.post(endpoint, payload);
      
      // Update local user state
      if (modalType === 'email') {
        dispatch(updateProfile({ email: modalInput }));
      } else {
        dispatch(updateProfile({ phoneNumber: modalInput }));
      }
      
      toast.success(`${modalType === 'email' ? 'Email' : 'Phone number'} updated successfully.`);
      closeModal();
    } catch (err) {
      setModalError(err.response?.data?.message || `Failed to verify ${modalType} change`);
    } finally {
      setModalLoading(false);
    }
  };

  const handleResendOtp = async () => {
    if (countdown > 0) return;
    setModalError('');
    try {
      const endpoint = modalType === 'email' ? '/api/users/resend-email-verification' : '/api/users/resend-phone-change-verification';
      await apiClient.post(endpoint);
      toast.success('Verification code resent successfully.');
      setCountdown(60);
    } catch (err) {
      setModalError(err.response?.data?.message || 'Failed to resend verification code');
    }
  };

  const handleDeleteAccount = async () => {
    setModalError('');
    setModalLoading(true);
    try {
      await apiClient.delete('/api/users/me');
      toast.success("Account deleted successfully.");
      closeModal();
      dispatch(logout());
      navigate('/login');
    } catch (err) {
      setModalError(err.response?.data?.message || 'Failed to delete account');
      setModalLoading(false);
    }
  };

  return (
    <motion.div 
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="max-w-2xl relative"
    >
      <h3 className="text-2xl font-display font-semibold mb-6 text-white flex items-center gap-3">
        <Shield className="text-primary" size={28} />
        Security & Privacy
      </h3>

      {error && <div className="bg-red-500/10 border border-red-500/50 text-red-400 p-3 rounded-xl mb-6 text-sm">{error}</div>}
      {success && <div className="bg-green-500/10 border border-green-500/50 text-green-400 p-3 rounded-xl mb-6 text-sm flex items-center gap-2"><Check size={16}/> {success}</div>}

      {/* Change Password */}
      <div className="glass-dark p-6 rounded-2xl border border-white/5 mb-8">
        <h4 className="text-lg font-medium text-white mb-4 flex items-center gap-2">
          <Key size={20} className="text-gray-400" />
          Change Password
        </h4>
        <form onSubmit={handlePasswordChange} className="space-y-4">
          <div>
            <label className="text-sm font-medium text-gray-300 block mb-1">Current Password</label>
            <input 
              type="password" 
              value={passwordForm.currentPassword}
              onChange={(e) => setPasswordForm({...passwordForm, currentPassword: e.target.value})}
              required
              className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-primary/50" 
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium text-gray-300 block mb-1">New Password</label>
              <input 
                type="password" 
                value={passwordForm.newPassword}
                onChange={(e) => setPasswordForm({...passwordForm, newPassword: e.target.value})}
                required
                className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-primary/50" 
                placeholder="Min 8 characters"
              />
            </div>
            <div>
              <label className="text-sm font-medium text-gray-300 block mb-1">Confirm New Password</label>
              <input 
                type="password" 
                value={passwordForm.confirmPassword}
                onChange={(e) => setPasswordForm({...passwordForm, confirmPassword: e.target.value})}
                required
                className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-primary/50" 
              />
            </div>
          </div>
          <div className="pt-2">
            <button 
              type="submit"
              disabled={loading}
              className="px-6 py-2.5 bg-primary/20 text-primary hover:bg-primary hover:text-white rounded-xl font-medium transition-colors disabled:opacity-70 flex items-center gap-2"
            >
              {loading ? <Loader2 className="animate-spin" size={18} /> : 'Update Password'}
            </button>
          </div>
        </form>
      </div>

      {/* Two-Factor / Email Phone */}
      <div className="space-y-4 mb-8">
        <div className="glass-dark p-4 rounded-xl border border-white/5 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 rounded-full bg-secondary/20 text-secondary flex items-center justify-center">
              <Mail size={20} />
            </div>
            <div>
              <h5 className="font-medium text-white">Email Address</h5>
              <p className="text-xs text-gray-400">{user?.email || 'Not provided'}</p>
            </div>
          </div>
          <button 
            onClick={() => openModal('email')}
            className="px-4 py-2 bg-white/5 hover:bg-white/10 text-white text-sm rounded-lg transition-colors border border-white/5"
          >
            Change
          </button>
        </div>

        <div className="glass-dark p-4 rounded-xl border border-white/5 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 rounded-full bg-accent/20 text-accent flex items-center justify-center">
              <Smartphone size={20} />
            </div>
            <div>
              <h5 className="font-medium text-white">Phone Number</h5>
              <p className="text-xs text-gray-400">{user?.phoneNumber || 'Not provided'}</p>
            </div>
          </div>
          <button 
            onClick={() => openModal('phone')}
            className="px-4 py-2 bg-white/5 hover:bg-white/10 text-white text-sm rounded-lg transition-colors border border-white/5"
          >
            {user?.phoneNumber ? 'Change' : 'Add'}
          </button>
        </div>
      </div>

      {/* Advanced Security */}
      <div className="glass-dark p-6 rounded-2xl border border-white/5 mb-8">
        <h4 className="text-lg font-medium text-white mb-4 flex items-center gap-2">
          <MonitorX size={20} className="text-gray-400" />
          Active Sessions
        </h4>
        <p className="text-sm text-gray-400 mb-4">
          Log out from all other devices if you notice suspicious activity. This will invalidate all current sessions.
        </p>
        <button 
          onClick={handleLogoutAll}
          disabled={loading}
          className="px-6 py-2.5 bg-white/5 hover:bg-white/10 text-white rounded-xl font-medium transition-colors disabled:opacity-70 flex items-center gap-2"
        >
          {loading ? <Loader2 className="animate-spin" size={18} /> : 'Logout All Devices'}
        </button>
      </div>

      {/* Danger Zone */}
      <div className="bg-red-500/5 border border-red-500/20 p-6 rounded-2xl">
        <h4 className="text-lg font-medium text-red-400 mb-2 flex items-center gap-2">
          <AlertTriangle size={20} />
          Danger Zone
        </h4>
        <p className="text-sm text-gray-400 mb-4">
          Once you delete your account, there is no going back. Please be certain.
        </p>
        <button 
          onClick={() => openModal('delete')}
          className="px-6 py-2.5 bg-red-500/10 text-red-500 hover:bg-red-500 hover:text-white rounded-xl font-medium transition-colors flex items-center gap-2"
        >
          <Trash2 size={18} />
          Delete Account
        </button>
      </div>

      {/* Modals */}
      <AnimatePresence>
        {modalType && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
            <motion.div 
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-bg-dark border border-white/10 rounded-2xl p-6 w-full max-w-md shadow-2xl relative"
            >
              {modalType === 'delete' ? (
                <div>
                  <h3 className="text-xl font-bold text-white mb-2">Delete Account</h3>
                  <p className="text-gray-400 text-sm mb-6">
                    This action is permanent and cannot be undone. Are you sure you want to proceed?
                  </p>
                  {modalError && <div className="text-red-400 text-sm mb-4 bg-red-500/10 p-2 rounded">{modalError}</div>}
                  <div className="flex justify-end gap-3">
                    <button 
                      onClick={closeModal}
                      className="px-4 py-2 bg-white/5 hover:bg-white/10 text-white rounded-lg text-sm"
                    >
                      Cancel
                    </button>
                    <button 
                      onClick={handleDeleteAccount}
                      disabled={modalLoading}
                      className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg text-sm flex items-center gap-2"
                    >
                      {modalLoading ? <Loader2 size={16} className="animate-spin"/> : 'Yes, Delete My Account'}
                    </button>
                  </div>
                </div>
              ) : (
                <div>
                  <h3 className="text-xl font-bold text-white mb-2">
                    {modalType === 'email' ? 'Change Email Address' : 'Change Phone Number'}
                  </h3>
                  
                  {modalError && <div className="text-red-400 text-sm mb-4 bg-red-500/10 p-2 rounded">{modalError}</div>}
                  
                  {modalStep === 1 ? (
                    <form onSubmit={handleInitiateChange}>
                      <div className="mb-4">
                        <label className="text-sm text-gray-400 block mb-1">
                          New {modalType === 'email' ? 'Email' : 'Phone Number'}
                        </label>
                        <input 
                          type={modalType === 'email' ? 'email' : 'text'}
                          required
                          value={modalInput}
                          onChange={e => setModalInput(e.target.value)}
                          placeholder={modalType === 'phone' ? 'e.g. 0912345678 or +84912345678' : ''}
                          className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                        />
                      </div>
                      <div className="flex justify-end gap-3 mt-6">
                        <button 
                          type="button"
                          onClick={closeModal}
                          className="px-4 py-2 bg-white/5 hover:bg-white/10 text-white rounded-lg text-sm"
                        >
                          Cancel
                        </button>
                        <button 
                          type="submit"
                          disabled={modalLoading}
                          className="px-4 py-2 bg-primary hover:bg-primary-dark text-white rounded-lg text-sm flex items-center gap-2"
                        >
                          {modalLoading ? <Loader2 size={16} className="animate-spin"/> : 'Send Code'}
                        </button>
                      </div>
                    </form>
                  ) : (
                    <form onSubmit={handleVerifyChange}>
                      <p className="text-sm text-gray-400 mb-4">
                        Enter the verification code sent to {modalInput}.
                      </p>
                      <div className="mb-4">
                        <label className="text-sm text-gray-400 block mb-1">Verification Code</label>
                        <input 
                          type="text"
                          required
                          value={otp}
                          onChange={e => setOtp(e.target.value)}
                          className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                        />
                      </div>
                      <div className="flex justify-between items-center mt-6">
                        <button 
                          type="button"
                          onClick={handleResendOtp}
                          disabled={countdown > 0}
                          className="text-sm text-primary hover:text-primary-dark disabled:text-gray-500 transition-colors"
                        >
                          {countdown > 0 ? `Resend code in ${countdown}s` : 'Resend code'}
                        </button>
                        <div className="flex gap-3">
                          <button 
                            type="button"
                            onClick={() => {
                              setModalStep(1);
                              setCountdown(0);
                            }}
                            className="px-4 py-2 bg-white/5 hover:bg-white/10 text-white rounded-lg text-sm"
                          >
                            Back
                          </button>
                          <button 
                            type="submit"
                            disabled={modalLoading}
                            className="px-4 py-2 bg-primary hover:bg-primary-dark text-white rounded-lg text-sm flex items-center gap-2"
                          >
                            {modalLoading ? <Loader2 size={16} className="animate-spin"/> : 'Verify & Save'}
                          </button>
                        </div>
                      </div>
                    </form>
                  )}
                </div>
              )}
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </motion.div>
  );
};

export default SecuritySettings;
