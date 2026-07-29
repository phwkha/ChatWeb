import React, { useState } from 'react';
import { Shield, Key, Mail, Smartphone, Loader2, Check } from 'lucide-react';
import { motion } from 'framer-motion';
import apiClient from '../../services/apiClient';

const SecuritySettings = () => {
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');

  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });

  const handlePasswordChange = async (e) => {
    e.preventDefault();
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

  return (
    <motion.div 
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="max-w-2xl"
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

      {/* Two-Factor / Email Phone (Stubs) */}
      <div className="space-y-4">
        <div className="glass-dark p-4 rounded-xl border border-white/5 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 rounded-full bg-secondary/20 text-secondary flex items-center justify-center">
              <Mail size={20} />
            </div>
            <div>
              <h5 className="font-medium text-white">Email Address</h5>
              <p className="text-xs text-gray-400">user@example.com</p>
            </div>
          </div>
          <button className="px-4 py-2 bg-white/5 hover:bg-white/10 text-white text-sm rounded-lg transition-colors border border-white/5">
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
              <p className="text-xs text-gray-400">Not provided</p>
            </div>
          </div>
          <button className="px-4 py-2 bg-white/5 hover:bg-white/10 text-white text-sm rounded-lg transition-colors border border-white/5">
            Add
          </button>
        </div>
      </div>
    </motion.div>
  );
};

export default SecuritySettings;
