import React, { useState, useEffect } from 'react';
import { MapPin, Plus, Trash2, Edit2, Loader2, X } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import apiClient from '../../services/apiClient';
import { toast } from '../../utils/toast';

const AddressManager = () => {
  const [addresses, setAddresses] = useState([]);
  const [loading, setLoading] = useState(true);

  // Modal states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingAddressId, setEditingAddressId] = useState(null);
  const [modalLoading, setModalLoading] = useState(false);
  const [formData, setFormData] = useState({
    street: '',
    city: '',
    country: '',
    isDefault: false
  });

  useEffect(() => {
    fetchAddresses();
  }, []);

  const fetchAddresses = async () => {
    try {
      const res = await apiClient.get('/api/users/addresses');
      // Extract array from standard response
      const data = Array.isArray(res.data.data) ? res.data.data : [];
      setAddresses(data);
    } catch (e) {
      console.error("Failed to fetch addresses", e);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Are you sure you want to delete this address?")) return;
    try {
      await apiClient.delete(`/api/users/address/${id}`);
      setAddresses(addresses.filter(a => a.id !== id));
      toast.success("Address deleted successfully.");
    } catch (e) {
      toast.error(e.response?.data?.message || "Failed to delete address");
    }
  };

  const openAddModal = () => {
    setEditingAddressId(null);
    setFormData({ street: '', city: '', country: '', isDefault: false });
    setIsModalOpen(true);
  };

  const openEditModal = (address) => {
    setEditingAddressId(address.id);
    setFormData({
      street: address.street || '',
      city: address.city || '',
      country: address.country || '',
      isDefault: address.isDefault || false
    });
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setModalLoading(true);
    try {
      if (editingAddressId) {
        // Edit
        await apiClient.put(`/api/users/address/${editingAddressId}`, formData);
        toast.success("Address updated successfully");
      } else {
        // Add
        await apiClient.post('/api/users/address', formData);
        toast.success("Address added successfully");
      }
      closeModal();
      fetchAddresses();
    } catch (err) {
      toast.error(err.response?.data?.message || "Failed to save address");
    } finally {
      setModalLoading(false);
    }
  };

  return (
    <motion.div 
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="max-w-3xl relative"
    >
      <div className="flex items-center justify-between mb-6">
        <h3 className="text-2xl font-display font-semibold text-white flex items-center gap-3">
          <MapPin className="text-secondary" size={28} />
          Your Addresses
        </h3>
        <button 
          onClick={openAddModal}
          className="flex items-center gap-2 px-4 py-2 bg-white/10 hover:bg-white/20 text-white rounded-xl transition-colors border border-white/5 text-sm font-medium"
        >
          <Plus size={16} />
          Add New
        </button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center p-8 text-gray-400"><Loader2 className="animate-spin" /></div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {addresses.map(address => (
            <div key={address.id} className="glass-dark p-5 rounded-2xl border border-white/5 relative group flex flex-col">
              {address.isDefault && (
                <span className="absolute top-4 right-4 text-[10px] font-bold uppercase tracking-wider bg-primary/20 text-primary px-2 py-1 rounded-md">
                  Default
                </span>
              )}
              <MapPin size={24} className="text-gray-400 mb-3" />
              <p className="text-white font-medium mb-1">{address.street}</p>
              <p className="text-sm text-gray-400 mb-4">{address.city}, {address.country}</p>
              
              <div className="flex items-center gap-2 pt-4 mt-auto border-t border-white/5">
                <button 
                  onClick={() => openEditModal(address)}
                  className="flex-1 py-1.5 flex items-center justify-center gap-2 text-sm text-gray-400 hover:text-white bg-white/5 hover:bg-white/10 rounded-lg transition-colors"
                >
                  <Edit2 size={14} /> Edit
                </button>
                <button 
                  onClick={() => handleDelete(address.id)}
                  className="flex-1 py-1.5 flex items-center justify-center gap-2 text-sm text-red-400 hover:text-red-300 bg-red-500/10 hover:bg-red-500/20 rounded-lg transition-colors"
                >
                  <Trash2 size={14} /> Delete
                </button>
              </div>
            </div>
          ))}
          {addresses.length === 0 && (
            <div className="col-span-full p-8 text-center text-gray-400 glass-dark rounded-2xl border border-white/5 border-dashed">
              No addresses found. Add one to complete your profile.
            </div>
          )}
        </div>
      )}

      {/* Add/Edit Modal */}
      <AnimatePresence>
        {isModalOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
            <motion.div 
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-bg-dark border border-white/10 rounded-2xl p-6 w-full max-w-md shadow-2xl relative"
            >
              <button 
                onClick={closeModal}
                className="absolute top-4 right-4 text-gray-400 hover:text-white transition-colors"
              >
                <X size={20} />
              </button>
              <h3 className="text-xl font-bold text-white mb-6">
                {editingAddressId ? 'Edit Address' : 'Add New Address'}
              </h3>
              
              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="text-sm font-medium text-gray-300 block mb-1">Street Address</label>
                  <input 
                    type="text" 
                    required
                    value={formData.street}
                    onChange={(e) => setFormData({...formData, street: e.target.value})}
                    className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-primary/50" 
                  />
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-300 block mb-1">City</label>
                  <input 
                    type="text" 
                    required
                    value={formData.city}
                    onChange={(e) => setFormData({...formData, city: e.target.value})}
                    className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-primary/50" 
                  />
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-300 block mb-1">Country</label>
                  <input 
                    type="text" 
                    required
                    value={formData.country}
                    onChange={(e) => setFormData({...formData, country: e.target.value})}
                    className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-primary/50" 
                  />
                </div>
                <div className="flex items-center gap-3 pt-2">
                  <input 
                    type="checkbox" 
                    id="isDefault"
                    checked={formData.isDefault}
                    onChange={(e) => setFormData({...formData, isDefault: e.target.checked})}
                    className="w-4 h-4 rounded border-gray-300 text-primary focus:ring-primary" 
                  />
                  <label htmlFor="isDefault" className="text-sm text-gray-300">Set as default address</label>
                </div>

                <div className="flex justify-end gap-3 mt-6 pt-4 border-t border-white/10">
                  <button 
                    type="button"
                    onClick={closeModal}
                    className="px-4 py-2 bg-white/5 hover:bg-white/10 text-white rounded-lg text-sm transition-colors"
                  >
                    Cancel
                  </button>
                  <button 
                    type="submit"
                    disabled={modalLoading}
                    className="px-6 py-2 bg-primary hover:bg-primary-dark text-white rounded-lg text-sm font-medium flex items-center gap-2 transition-colors disabled:opacity-70"
                  >
                    {modalLoading ? <Loader2 size={16} className="animate-spin" /> : 'Save Address'}
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </motion.div>
  );
};

export default AddressManager;
