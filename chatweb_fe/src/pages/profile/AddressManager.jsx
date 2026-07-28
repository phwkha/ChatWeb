import React, { useState, useEffect } from 'react';
import { userService } from '../../services';
import { MapPinIcon, PencilSquareIcon, TrashIcon, PlusIcon, XMarkIcon, CheckIcon } from '@heroicons/react/24/outline';

const AddressManager = () => {
    const [addresses, setAddresses] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isEditing, setIsEditing] = useState(false);
    const [currentAddress, setCurrentAddress] = useState(null);
    const [formData, setFormData] = useState({
        houseNumber: '',
        street: '',
        ward: '',
        district: '',
        city: '',
        country: 'Vietnam'
    });
    const [error, setError] = useState('');

    useEffect(() => {
        fetchAddresses();
    }, []);

    const fetchAddresses = async () => {
        try {
            setLoading(true);
            const response = await userService.getAddresses();
            setAddresses(response.data || []);
        } catch (err) {
            console.error("Failed to fetch addresses:", err);
            setError("Failed to load addresses.");
        } finally {
            setLoading(false);
        }
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleAddNew = () => {
        setFormData({ houseNumber: '', street: '', ward: '', district: '', city: '', country: 'Vietnam' });
        setCurrentAddress(null);
        setIsEditing(true);
        setError('');
    };

    const handleEdit = (addr) => {
        setFormData({
            houseNumber: addr.houseNumber || '',
            street: addr.street || '',
            ward: addr.ward || '',
            district: addr.district || '',
            city: addr.city || '',
            country: addr.country || 'Vietnam'
        });
        setCurrentAddress(addr);
        setIsEditing(true);
        setError('');
    };

    const handleDelete = async (id) => {
        if (!window.confirm("Are you sure you want to delete this address?")) return;
        try {
            await userService.deleteAddress(id);
            fetchAddresses();
        } catch (err) {
            alert(err.message || "Failed to delete address");
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try {
            if (currentAddress) {
                await userService.updateAddress(currentAddress.id, formData);
            } else {
                await userService.addAddress(formData);
            }
            setIsEditing(false);
            fetchAddresses();
        } catch (err) {
            setError(err.message || "Failed to save address");
        }
    };

    if (loading && addresses.length === 0) {
        return <div className="py-4 text-center text-slate-400">Loading addresses...</div>;
    }

    return (
        <div className="mt-8 pt-8 border-t border-white/10">
            <div className="flex items-center justify-between mb-6">
                <h3 className="text-xl font-bold text-white flex items-center gap-2">
                    <MapPinIcon className="w-6 h-6 text-indigo-400" />
                    Address Book
                </h3>
                {!isEditing && (
                    <button 
                        onClick={handleAddNew}
                        className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-indigo-500/20 text-indigo-400 hover:bg-indigo-500/30 transition-colors text-sm font-medium border border-indigo-500/30"
                    >
                        <PlusIcon className="w-4 h-4" /> Add New
                    </button>
                )}
            </div>

            {error && !isEditing && (
                <div className="mb-4 p-3 rounded-lg bg-rose-500/10 text-rose-400 text-sm">{error}</div>
            )}

            {isEditing ? (
                <div className="bg-slate-800/50 p-6 rounded-2xl border border-white/5 animate-enter">
                    <h4 className="text-lg font-semibold text-white mb-4">
                        {currentAddress ? 'Edit Address' : 'Add New Address'}
                    </h4>
                    {error && <div className="mb-4 p-3 rounded-lg bg-rose-500/10 text-rose-400 text-sm">{error}</div>}
                    
                    <form onSubmit={handleSubmit} className="space-y-4">
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-xs font-medium text-slate-400 mb-1">House Number (Optional)</label>
                                <input type="text" name="houseNumber" value={formData.houseNumber} onChange={handleInputChange} className="w-full glass-input py-2 text-sm" placeholder="e.g. 123A" />
                            </div>
                            <div>
                                <label className="block text-xs font-medium text-slate-400 mb-1">Street</label>
                                <input type="text" name="street" value={formData.street} onChange={handleInputChange} className="w-full glass-input py-2 text-sm" required placeholder="Street name" />
                            </div>
                            <div>
                                <label className="block text-xs font-medium text-slate-400 mb-1">Ward</label>
                                <input type="text" name="ward" value={formData.ward} onChange={handleInputChange} className="w-full glass-input py-2 text-sm" required placeholder="Ward" />
                            </div>
                            <div>
                                <label className="block text-xs font-medium text-slate-400 mb-1">District</label>
                                <input type="text" name="district" value={formData.district} onChange={handleInputChange} className="w-full glass-input py-2 text-sm" required placeholder="District" />
                            </div>
                            <div>
                                <label className="block text-xs font-medium text-slate-400 mb-1">City/Province</label>
                                <input type="text" name="city" value={formData.city} onChange={handleInputChange} className="w-full glass-input py-2 text-sm" required placeholder="City" />
                            </div>
                            <div>
                                <label className="block text-xs font-medium text-slate-400 mb-1">Country</label>
                                <input type="text" name="country" value={formData.country} onChange={handleInputChange} className="w-full glass-input py-2 text-sm" required placeholder="Country" />
                            </div>
                        </div>
                        <div className="flex justify-end gap-3 pt-4">
                            <button type="button" onClick={() => setIsEditing(false)} className="flex items-center gap-1 px-4 py-2 rounded-lg text-slate-400 hover:text-white hover:bg-white/5 transition-colors text-sm">
                                <XMarkIcon className="w-4 h-4" /> Cancel
                            </button>
                            <button type="submit" className="flex items-center gap-1 px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white transition-colors text-sm font-medium shadow-lg shadow-emerald-500/20">
                                <CheckIcon className="w-4 h-4" /> Save
                            </button>
                        </div>
                    </form>
                </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {addresses.length === 0 ? (
                        <div className="col-span-full py-8 text-center text-slate-400 bg-slate-800/30 rounded-2xl border border-white/5 border-dashed">
                            No addresses saved yet.
                        </div>
                    ) : (
                        addresses.map(addr => (
                            <div key={addr.id} className="group relative bg-slate-800/40 hover:bg-slate-800/60 p-4 rounded-2xl border border-white/5 hover:border-indigo-500/30 transition-all">
                                <div className="absolute top-4 right-4 flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                                    <button onClick={() => handleEdit(addr)} className="p-1.5 rounded-md bg-white/5 hover:bg-indigo-500/20 text-slate-400 hover:text-indigo-400 transition-colors" title="Edit">
                                        <PencilSquareIcon className="w-4 h-4" />
                                    </button>
                                    <button onClick={() => handleDelete(addr.id)} className="p-1.5 rounded-md bg-white/5 hover:bg-rose-500/20 text-slate-400 hover:text-rose-400 transition-colors" title="Delete">
                                        <TrashIcon className="w-4 h-4" />
                                    </button>
                                </div>
                                <div className="pr-12 text-sm text-slate-300 leading-relaxed">
                                    {addr.houseNumber && <span>{addr.houseNumber}, </span>}
                                    <span>{addr.street}</span><br/>
                                    <span>{addr.ward}, {addr.district}</span><br/>
                                    <span>{addr.city}, {addr.country}</span>
                                </div>
                            </div>
                        ))
                    )}
                </div>
            )}
        </div>
    );
};

export default AddressManager;
