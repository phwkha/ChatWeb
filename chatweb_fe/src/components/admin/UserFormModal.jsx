import React, { useState, useEffect } from 'react';
import { X } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import apiClient from '../../services/apiClient';

const UserFormModal = ({ isOpen, onClose, user, onSuccess }) => {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    phone: '',
    roleId: ''
  });
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (isOpen) {
      fetchRoles();
      if (user) {
        // Pre-fill for edit
        setFormData({
          username: user.username || '',
          email: user.email || '',
          password: '', // Leave empty for edit unless they want to change, but API doesn't support password in PUT
          firstName: user.firstName || '',
          lastName: user.lastName || '',
          phone: user.phone || '',
          roleId: user.roles?.length > 0 ? roles.find(r => `ROLE_${r.name}` === user.roles[0])?.id || '' : '' // Hacky role mapping, best if user object returned roleId
        });
      } else {
        setFormData({ username: '', email: '', password: '', firstName: '', lastName: '', phone: '', roleId: '' });
      }
      setError(null);
    }
  }, [isOpen, user]);

  const fetchRoles = async () => {
    try {
      const res = await apiClient.get('/api/roles');
      const data = Array.isArray(res.data.data) ? res.data.data : [];
      setRoles(data);
    } catch (e) {
      console.error(e);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      if (user) {
        // Update user
        const payload = {
          firstName: formData.firstName,
          lastName: formData.lastName,
          email: formData.email,
          phone: formData.phone,
          roleId: formData.roleId ? parseInt(formData.roleId) : null
        };
        await apiClient.put(`/api/admin/${user.username}`, payload);
      } else {
        // Create user
        const payload = { ...formData, roleId: formData.roleId ? parseInt(formData.roleId) : null };
        await apiClient.post('/api/admin/add', payload);
      }
      onSuccess();
    } catch (err) {
      setError(err.response?.data?.message || 'Operation failed');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <AnimatePresence>
      <motion.div 
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4"
      >
        <motion.div 
          initial={{ scale: 0.95, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} exit={{ scale: 0.95, opacity: 0 }}
          className="bg-bg-dark border border-white/10 w-full max-w-lg rounded-2xl shadow-2xl overflow-hidden flex flex-col"
        >
          <div className="p-6 border-b border-white/10 flex justify-between items-center">
            <h2 className="text-xl font-display font-bold">{user ? `Edit User: ${user.username}` : 'Add New User'}</h2>
            <button onClick={onClose} className="p-2 text-gray-400 hover:text-white rounded-lg transition-colors"><X size={20}/></button>
          </div>

          <div className="p-6 overflow-y-auto custom-scrollbar">
            {error && <div className="mb-4 p-3 bg-red-500/10 border border-red-500/20 text-red-400 text-sm rounded-lg">{error}</div>}
            
            <form id="userForm" onSubmit={handleSubmit} className="space-y-4">
              {!user && (
                <>
                  <div>
                    <label className="block text-sm text-gray-400 mb-1">Username *</label>
                    <input required type="text" value={formData.username} onChange={e => setFormData({...formData, username: e.target.value})} className="w-full bg-black/20 border border-white/10 rounded-lg px-4 py-2 text-white" />
                  </div>
                  <div>
                    <label className="block text-sm text-gray-400 mb-1">Password * (Min 8 chars)</label>
                    <input required minLength={8} type="password" value={formData.password} onChange={e => setFormData({...formData, password: e.target.value})} className="w-full bg-black/20 border border-white/10 rounded-lg px-4 py-2 text-white" />
                  </div>
                </>
              )}
              
              <div className="flex gap-4">
                <div className="flex-1">
                  <label className="block text-sm text-gray-400 mb-1">First Name *</label>
                  <input required type="text" value={formData.firstName} onChange={e => setFormData({...formData, firstName: e.target.value})} className="w-full bg-black/20 border border-white/10 rounded-lg px-4 py-2 text-white" />
                </div>
                <div className="flex-1">
                  <label className="block text-sm text-gray-400 mb-1">Last Name</label>
                  <input type="text" value={formData.lastName} onChange={e => setFormData({...formData, lastName: e.target.value})} className="w-full bg-black/20 border border-white/10 rounded-lg px-4 py-2 text-white" />
                </div>
              </div>

              <div>
                <label className="block text-sm text-gray-400 mb-1">Email *</label>
                <input required type="email" value={formData.email} onChange={e => setFormData({...formData, email: e.target.value})} className="w-full bg-black/20 border border-white/10 rounded-lg px-4 py-2 text-white" />
              </div>

              <div>
                <label className="block text-sm text-gray-400 mb-1">Phone Number (0... or +84...)</label>
                <input type="text" pattern="^(0[0-9]{9}|\+84[0-9]{9})$" value={formData.phone} onChange={e => setFormData({...formData, phone: e.target.value})} className="w-full bg-black/20 border border-white/10 rounded-lg px-4 py-2 text-white" />
              </div>

              <div>
                <label className="block text-sm text-gray-400 mb-1">Role</label>
                <select value={formData.roleId} onChange={e => setFormData({...formData, roleId: e.target.value})} className="w-full bg-black/20 border border-white/10 rounded-lg px-4 py-2 text-white appearance-none">
                  <option value="">Select a role (Optional)</option>
                  {roles.map(r => (
                    <option key={r.id} value={r.id}>{r.name}</option>
                  ))}
                </select>
              </div>
            </form>
          </div>

          <div className="p-6 border-t border-white/10 flex justify-end gap-3 bg-black/10">
            <button onClick={onClose} className="px-5 py-2 rounded-xl text-sm font-medium text-gray-400 hover:text-white transition-colors">Cancel</button>
            <button type="submit" form="userForm" disabled={loading} className="px-5 py-2 rounded-xl text-sm font-medium bg-primary text-white hover:bg-primary-dark transition-colors disabled:opacity-50">
              {loading ? 'Saving...' : 'Save User'}
            </button>
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
};

export default UserFormModal;
