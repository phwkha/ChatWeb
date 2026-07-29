import React, { useState, useEffect } from 'react';
import { MapPin, Plus, Trash2, Edit2, Loader2 } from 'lucide-react';
import { motion } from 'framer-motion';
import apiClient from '../../services/apiClient';

const AddressManager = () => {
  const [addresses, setAddresses] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
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
    fetchAddresses();
  }, []);

  const handleDelete = async (id) => {
    try {
      await apiClient.delete(`/api/users/address/${id}`);
      setAddresses(addresses.filter(a => a.id !== id));
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <motion.div 
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="max-w-3xl"
    >
      <div className="flex items-center justify-between mb-6">
        <h3 className="text-2xl font-display font-semibold text-white flex items-center gap-3">
          <MapPin className="text-secondary" size={28} />
          Your Addresses
        </h3>
        <button className="flex items-center gap-2 px-4 py-2 bg-white/10 hover:bg-white/20 text-white rounded-xl transition-colors border border-white/5 text-sm font-medium">
          <Plus size={16} />
          Add New
        </button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center p-8 text-gray-400"><Loader2 className="animate-spin" /></div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {addresses.map(address => (
            <div key={address.id} className="glass-dark p-5 rounded-2xl border border-white/5 relative group">
              {address.isDefault && (
                <span className="absolute top-4 right-4 text-[10px] font-bold uppercase tracking-wider bg-primary/20 text-primary px-2 py-1 rounded-md">
                  Default
                </span>
              )}
              <MapPin size={24} className="text-gray-400 mb-3" />
              <p className="text-white font-medium mb-1">{address.street}</p>
              <p className="text-sm text-gray-400 mb-4">{address.city}, {address.country}</p>
              
              <div className="flex items-center gap-2 pt-4 border-t border-white/5">
                <button className="flex-1 py-1.5 flex items-center justify-center gap-2 text-sm text-gray-400 hover:text-white bg-white/5 hover:bg-white/10 rounded-lg transition-colors">
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
    </motion.div>
  );
};

export default AddressManager;
