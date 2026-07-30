import React, { useState, useEffect } from 'react';
import { X, Edit2, Trash2, MapPin, Check } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import apiClient from '../../services/apiClient';

const AdminAddressManager = ({ isOpen, onClose, username }) => {
  const [addresses, setAddresses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [editForm, setEditForm] = useState({ street: '', city: '', country: '', isDefault: false });

  useEffect(() => {
    if (isOpen && username) {
      fetchAddresses();
    }
  }, [isOpen, username]);

  const fetchAddresses = async () => {
    setLoading(true);
    try {
      const res = await apiClient.get(`/api/admin/user/${username}/addresses`);
      const data = Array.isArray(res.data.data) ? res.data.data : [];
      setAddresses(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('Are you sure you want to delete this address?')) return;
    try {
      await apiClient.delete(`/api/admin/user/${username}/address/${id}`);
      setAddresses(addresses.filter(a => a.id !== id));
    } catch (e) {
      console.error(e);
    }
  };

  const handleEditClick = (address) => {
    setEditingId(address.id);
    setEditForm({
      street: address.street,
      city: address.city,
      country: address.country,
      isDefault: address.isDefault || false
    });
  };

  const handleUpdate = async () => {
    try {
      await apiClient.put(`/api/admin/user/${username}/address/${editingId}`, editForm);
      setAddresses(addresses.map(a => a.id === editingId ? { ...a, ...editForm } : a));
      setEditingId(null);
    } catch (e) {
      console.error(e);
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
          className="bg-bg-dark border border-white/10 w-full max-w-2xl rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]"
        >
          <div className="p-6 border-b border-white/10 flex justify-between items-center">
            <h2 className="text-xl font-display font-bold">Addresses for {username}</h2>
            <button onClick={onClose} className="p-2 text-gray-400 hover:text-white rounded-lg transition-colors"><X size={20}/></button>
          </div>

          <div className="p-6 overflow-y-auto custom-scrollbar flex-1 space-y-4">
            {loading ? (
              <div className="text-gray-400 text-center py-4">Loading addresses...</div>
            ) : addresses.length === 0 ? (
              <div className="text-gray-500 text-center py-4">No addresses found for this user.</div>
            ) : (
              addresses.map(address => (
                <div key={address.id} className="p-4 rounded-xl border border-white/10 bg-white/5 flex flex-col gap-3">
                  {editingId === address.id ? (
                    <div className="space-y-3">
                      <input type="text" value={editForm.street} onChange={e => setEditForm({...editForm, street: e.target.value})} placeholder="Street" className="w-full bg-black/20 border border-white/10 rounded-lg px-4 py-2 text-white text-sm" />
                      <div className="flex gap-3">
                        <input type="text" value={editForm.city} onChange={e => setEditForm({...editForm, city: e.target.value})} placeholder="City" className="flex-1 bg-black/20 border border-white/10 rounded-lg px-4 py-2 text-white text-sm" />
                        <input type="text" value={editForm.country} onChange={e => setEditForm({...editForm, country: e.target.value})} placeholder="Country" className="flex-1 bg-black/20 border border-white/10 rounded-lg px-4 py-2 text-white text-sm" />
                      </div>
                      <label className="flex items-center gap-2 text-sm text-gray-300">
                        <input type="checkbox" checked={editForm.isDefault} onChange={e => setEditForm({...editForm, isDefault: e.target.checked})} />
                        Set as default
                      </label>
                      <div className="flex gap-2 justify-end">
                        <button onClick={() => setEditingId(null)} className="px-4 py-1.5 rounded-lg text-sm bg-white/5 text-gray-300 hover:text-white">Cancel</button>
                        <button onClick={handleUpdate} className="px-4 py-1.5 rounded-lg text-sm bg-primary/20 text-primary hover:bg-primary hover:text-white">Save</button>
                      </div>
                    </div>
                  ) : (
                    <>
                      <div className="flex justify-between items-start">
                        <div className="flex gap-3 items-start">
                          <div className="p-2 bg-primary/20 text-primary rounded-lg shrink-0 mt-0.5"><MapPin size={18}/></div>
                          <div>
                            <p className="text-white font-medium">{address.street}</p>
                            <p className="text-gray-400 text-sm">{address.city}, {address.country}</p>
                            {address.isDefault && <span className="inline-block mt-1 px-2 py-0.5 bg-green-500/10 text-green-400 border border-green-500/20 text-xs rounded">Default</span>}
                          </div>
                        </div>
                        <div className="flex gap-2">
                          <button onClick={() => handleEditClick(address)} className="p-1.5 text-gray-400 hover:text-white hover:bg-white/10 rounded transition-colors"><Edit2 size={16}/></button>
                          <button onClick={() => handleDelete(address.id)} className="p-1.5 text-gray-400 hover:text-red-400 hover:bg-red-500/10 rounded transition-colors"><Trash2 size={16}/></button>
                        </div>
                      </div>
                    </>
                  )}
                </div>
              ))
            )}
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
};

export default AdminAddressManager;
