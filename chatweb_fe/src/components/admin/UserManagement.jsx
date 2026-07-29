import React, { useState, useEffect } from 'react';
import { ShieldAlert, Users, Lock, Unlock, ImageMinus } from 'lucide-react';
import apiClient from '../../services/apiClient';

const UserManagement = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchUsers = async () => {
    try {
      const res = await apiClient.get('/api/admin/users');
      const data = Array.isArray(res.data.data) ? res.data.data : [];
      setUsers(data);
    } catch (e) {
      console.error("Failed to fetch users", e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleToggleLock = async (username, isLocked) => {
    try {
      const endpoint = isLocked ? `/api/admin/${username}/unlock` : `/api/admin/${username}/lock`;
      await apiClient.post(endpoint);
      // Optimistic update
      setUsers(users.map(u => u.username === username ? { ...u, isLocked: !isLocked } : u));
    } catch (e) {
      console.error(e);
    }
  };

  const handleDeleteAvatar = async (username) => {
    try {
      await apiClient.post(`/api/admin/${username}/delete-avatar`);
      alert(`Avatar deleted for ${username}`);
    } catch (e) {
      console.error(e);
    }
  };

  if (loading) return <div className="p-8 text-gray-400">Loading users...</div>;

  return (
    <div className="p-8">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h2 className="text-2xl font-display font-bold text-white">User Management</h2>
          <p className="text-gray-400 text-sm mt-1">Manage user accounts and access.</p>
        </div>
        <div className="flex items-center gap-2 bg-primary/20 text-primary px-4 py-2 rounded-xl">
          <Users size={18} />
          <span className="font-semibold">{users.length} Users</span>
        </div>
      </div>

      <div className="glass-dark rounded-3xl border border-white/5 overflow-hidden">
        <table className="w-full text-left">
          <thead className="bg-white/5 border-b border-white/5">
            <tr>
              <th className="px-6 py-4 text-sm font-medium text-gray-400">User</th>
              <th className="px-6 py-4 text-sm font-medium text-gray-400">Roles</th>
              <th className="px-6 py-4 text-sm font-medium text-gray-400">Status</th>
              <th className="px-6 py-4 text-sm font-medium text-gray-400 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5">
            {users.map(user => (
              <tr key={user.id} className="hover:bg-white/5 transition-colors">
                <td className="px-6 py-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-primary to-secondary flex items-center justify-center font-bold">
                      {user.username[0].toUpperCase()}
                    </div>
                    <div>
                      <div className="font-medium text-white">{user.username}</div>
                      <div className="text-sm text-gray-400">{user.email}</div>
                    </div>
                  </div>
                </td>
                <td className="px-6 py-4">
                  <div className="flex gap-2">
                    {user.roles.map(role => (
                      <span key={role} className="px-2 py-1 bg-white/10 text-xs rounded-md border border-white/10">
                        {role.replace('ROLE_', '')}
                      </span>
                    ))}
                  </div>
                </td>
                <td className="px-6 py-4">
                  {user.isLocked ? (
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-red-500/10 text-red-400 border border-red-500/20">
                      <Lock size={12} /> Locked
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-green-500/10 text-green-400 border border-green-500/20">
                      <Unlock size={12} /> Active
                    </span>
                  )}
                </td>
                <td className="px-6 py-4 text-right">
                  <div className="flex items-center justify-end gap-2">
                    <button 
                      onClick={() => handleToggleLock(user.username, user.isLocked)}
                      className="p-2 hover:bg-white/10 rounded-lg text-gray-400 hover:text-white transition-colors"
                      title={user.isLocked ? "Unlock User" : "Lock User"}
                    >
                      {user.isLocked ? <Unlock size={18} /> : <Lock size={18} />}
                    </button>
                    <button 
                      onClick={() => handleDeleteAvatar(user.username)}
                      className="p-2 hover:bg-red-500/20 hover:text-red-400 rounded-lg text-gray-400 transition-colors"
                      title="Delete Avatar"
                    >
                      <ImageMinus size={18} />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default UserManagement;
