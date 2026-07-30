import React, { useState, useEffect } from 'react';
import { Shield, Edit2, Trash2, ShieldPlus, X } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import apiClient from '../../services/apiClient';

const RoleManagement = () => {
  const [roles, setRoles] = useState([]);
  const [permissions, setPermissions] = useState([]);
  const [loading, setLoading] = useState(true);

  // Modal states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingRole, setEditingRole] = useState(null);
  const [formData, setFormData] = useState({ name: '', description: '', permissionIds: [] });
  const [modalLoading, setModalLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchRoles();
    fetchPermissions();
  }, []);

  const fetchRoles = async () => {
    setLoading(true);
    try {
      const res = await apiClient.get('/api/roles');
      const data = Array.isArray(res.data.data) ? res.data.data : [];
      setRoles(data);
    } catch (e) {
      console.error("Failed to fetch roles", e);
    } finally {
      setLoading(false);
    }
  };

  const fetchPermissions = async () => {
    try {
      const res = await apiClient.get('/api/roles/permissions');
      const data = Array.isArray(res.data.data) ? res.data.data : [];
      setPermissions(data);
    } catch (e) {
      console.error("Failed to fetch permissions", e);
    }
  };

  const openAddModal = () => {
    setEditingRole(null);
    setFormData({ name: '', description: '', permissionIds: [] });
    setError(null);
    setIsModalOpen(true);
  };

  const openEditModal = (role) => {
    setEditingRole(role);
    setFormData({
      name: role.name,
      description: role.description || '',
      permissionIds: role.permissions ? role.permissions.map(p => p.id) : []
    });
    setError(null);
    setIsModalOpen(true);
  };

  const handleDelete = async (id, name) => {
    if (!confirm(`Are you sure you want to delete the role '${name}'?`)) return;
    try {
      await apiClient.delete(`/api/roles/${id}`);
      setRoles(roles.filter(r => r.id !== id));
    } catch (e) {
      console.error(e);
      alert('Failed to delete role.');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setModalLoading(true);
    setError(null);
    try {
      if (editingRole) {
        await apiClient.put(`/api/roles/${editingRole.id}`, formData);
      } else {
        await apiClient.post('/api/roles', formData);
      }
      setIsModalOpen(false);
      fetchRoles();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save role');
    } finally {
      setModalLoading(false);
    }
  };

  const togglePermission = (permId) => {
    setFormData(prev => {
      const current = prev.permissionIds;
      if (current.includes(permId)) {
        return { ...prev, permissionIds: current.filter(id => id !== permId) };
      } else {
        return { ...prev, permissionIds: [...current, permId] };
      }
    });
  };

  if (loading) return <div className="p-8 text-gray-400">Loading roles...</div>;

  return (
    <div className="p-8">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h2 className="text-2xl font-display font-bold text-white">Role Management</h2>
          <p className="text-gray-400 text-sm mt-1">Define roles and manage their permissions.</p>
        </div>
        <button 
          onClick={openAddModal}
          className="flex items-center gap-2 bg-primary/20 hover:bg-primary hover:text-white text-primary transition-colors px-4 py-2 rounded-xl"
        >
          <ShieldPlus size={18} />
          <span className="font-medium">Create Role</span>
        </button>
      </div>

      <div className="glass-dark rounded-3xl border border-white/5 overflow-hidden">
        <table className="w-full text-left">
          <thead className="bg-white/5 border-b border-white/5">
            <tr>
              <th className="px-6 py-4 text-sm font-medium text-gray-400">Role Name</th>
              <th className="px-6 py-4 text-sm font-medium text-gray-400">Description</th>
              <th className="px-6 py-4 text-sm font-medium text-gray-400">Permissions</th>
              <th className="px-6 py-4 text-sm font-medium text-gray-400 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5">
            {roles.map(role => (
              <tr key={role.id} className="hover:bg-white/5 transition-colors">
                <td className="px-6 py-4">
                  <div className="flex items-center gap-3">
                    <div className="p-2 bg-primary/20 text-primary rounded-lg">
                      <Shield size={18} />
                    </div>
                    <div className="font-medium text-white">{role.name}</div>
                  </div>
                </td>
                <td className="px-6 py-4 text-sm text-gray-400">
                  {role.description || 'No description'}
                </td>
                <td className="px-6 py-4">
                  <div className="flex flex-wrap gap-1.5 max-w-xs">
                    {(role.permissions || []).slice(0, 3).map(p => (
                      <span key={p.id} className="px-2 py-0.5 bg-white/5 border border-white/10 rounded text-[10px] text-gray-300">
                        {p.name}
                      </span>
                    ))}
                    {(role.permissions?.length > 3) && (
                      <span className="px-2 py-0.5 bg-white/5 border border-white/10 rounded text-[10px] text-gray-500">
                        +{role.permissions.length - 3} more
                      </span>
                    )}
                  </div>
                </td>
                <td className="px-6 py-4 text-right">
                  <div className="flex items-center justify-end gap-2">
                    <button 
                      onClick={() => openEditModal(role)}
                      className="p-2 hover:bg-white/10 rounded-lg text-gray-400 hover:text-white transition-colors"
                      title="Edit Role"
                    >
                      <Edit2 size={16} />
                    </button>
                    <button 
                      onClick={() => handleDelete(role.id, role.name)}
                      className="p-2 hover:bg-red-500/20 rounded-lg text-gray-400 hover:text-red-400 transition-colors"
                      title="Delete Role"
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
            {roles.length === 0 && (
              <tr>
                <td colSpan="4" className="px-6 py-8 text-center text-gray-500">
                  No roles found.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <AnimatePresence>
        {isModalOpen && (
          <motion.div 
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4"
          >
            <motion.div 
              initial={{ scale: 0.95, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} exit={{ scale: 0.95, opacity: 0 }}
              className="bg-bg-dark border border-white/10 w-full max-w-2xl rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]"
            >
              <div className="p-6 border-b border-white/10 flex justify-between items-center">
                <h2 className="text-xl font-display font-bold">{editingRole ? 'Edit Role' : 'Create Role'}</h2>
                <button onClick={() => setIsModalOpen(false)} className="p-2 text-gray-400 hover:text-white rounded-lg transition-colors"><X size={20}/></button>
              </div>

              <div className="p-6 overflow-y-auto custom-scrollbar flex-1">
                {error && <div className="mb-4 p-3 bg-red-500/10 border border-red-500/20 text-red-400 text-sm rounded-lg">{error}</div>}
                
                <form id="roleForm" onSubmit={handleSubmit} className="space-y-6">
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm text-gray-400 mb-1">Role Name *</label>
                      <input required type="text" value={formData.name} onChange={e => setFormData({...formData, name: e.target.value})} className="w-full bg-black/20 border border-white/10 rounded-lg px-4 py-2 text-white" />
                    </div>
                    <div>
                      <label className="block text-sm text-gray-400 mb-1">Description</label>
                      <input type="text" value={formData.description} onChange={e => setFormData({...formData, description: e.target.value})} className="w-full bg-black/20 border border-white/10 rounded-lg px-4 py-2 text-white" />
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm text-gray-400 mb-3">Permissions</label>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3 bg-white/5 p-4 rounded-xl border border-white/5 max-h-64 overflow-y-auto custom-scrollbar">
                      {permissions.map(perm => (
                        <label key={perm.id} className="flex items-start gap-3 p-2 rounded hover:bg-white/5 cursor-pointer">
                          <input 
                            type="checkbox" 
                            className="mt-1 accent-primary"
                            checked={formData.permissionIds.includes(perm.id)}
                            onChange={() => togglePermission(perm.id)}
                          />
                          <div>
                            <div className="text-sm text-white font-medium">{perm.name}</div>
                            <div className="text-xs text-gray-500">{perm.description || 'No description'}</div>
                          </div>
                        </label>
                      ))}
                    </div>
                  </div>
                </form>
              </div>

              <div className="p-6 border-t border-white/10 flex justify-end gap-3 bg-black/10">
                <button onClick={() => setIsModalOpen(false)} className="px-5 py-2 rounded-xl text-sm font-medium text-gray-400 hover:text-white transition-colors">Cancel</button>
                <button type="submit" form="roleForm" disabled={modalLoading} className="px-5 py-2 rounded-xl text-sm font-medium bg-primary text-white hover:bg-primary-dark transition-colors disabled:opacity-50">
                  {modalLoading ? 'Saving...' : 'Save Role'}
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default RoleManagement;
