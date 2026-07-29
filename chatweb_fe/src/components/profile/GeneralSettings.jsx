import React, { useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { motion } from 'framer-motion';
import { Camera, Save, Loader2 } from 'lucide-react';
import apiClient from '../../services/apiClient';
import { updateProfile } from '../../store/slices/authSlice';

const GeneralSettings = () => {
  const dispatch = useDispatch();
  const { user } = useSelector(state => state.auth);
  
  const [formData, setFormData] = useState({
    firstName: user?.firstName || '',
    lastName: user?.lastName || '',
    bio: user?.bio || '',
  });
  
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');
  const [avatarPreview, setAvatarPreview] = useState(user?.avatar || null);

  const fileInputRef = React.useRef(null);

  // Fetch real profile data on mount
  React.useEffect(() => {
    const fetchProfile = async () => {
      try {
        const res = await apiClient.get('/api/users/profile');
        const data = res.data.data;
        setFormData({
          firstName: data.firstName || '',
          lastName: data.lastName || '',
          bio: data.bio || '',
        });
        setAvatarPreview(data.avatar);
        dispatch(updateProfile(data));
      } catch (err) {
        console.error("Failed to fetch profile", err);
      }
    };
    fetchProfile();
  }, [dispatch]);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleAvatarChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    // Create a local preview
    const previewUrl = URL.createObjectURL(file);
    setAvatarPreview(previewUrl);

    // Upload to server
    const data = new FormData();
    data.append('file', file);

    try {
      setLoading(true);
      const res = await apiClient.patch('/api/users/avatar', data, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      // Assuming response returns updated user or just success
      dispatch(updateProfile({ avatar: res.data.data?.avatar || previewUrl }));
      setSuccess('Avatar updated successfully!');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update avatar');
      setAvatarPreview(user?.avatar); // Revert on failure
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');
    
    try {
      const res = await apiClient.put('/api/users/profile', formData);
      dispatch(updateProfile(res.data.data || formData));
      setSuccess('Profile updated successfully!');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update profile');
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
      <h3 className="text-2xl font-display font-semibold mb-6 text-white">General Info</h3>
      
      {/* Avatar Section */}
      <div className="flex items-center gap-6 mb-10 p-6 glass-dark rounded-2xl border border-white/5">
        <div className="relative group cursor-pointer" onClick={() => fileInputRef.current?.click()}>
          <div className="w-24 h-24 rounded-full bg-gradient-to-tr from-primary to-secondary p-[3px]">
            <div className="w-full h-full bg-bg-dark rounded-full flex items-center justify-center overflow-hidden">
              {avatarPreview ? (
                <img src={avatarPreview} alt="Avatar" className="w-full h-full object-cover" />
              ) : (
                <span className="text-3xl font-display font-bold text-gray-400">
                  {user?.username?.[0]?.toUpperCase() || 'U'}
                </span>
              )}
            </div>
          </div>
          <div className="absolute inset-0 bg-black/60 rounded-full opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
            <Camera className="text-white" size={24} />
          </div>
        </div>
        <div>
          <h4 className="text-lg font-medium text-white mb-1">Profile Picture</h4>
          <p className="text-sm text-gray-400 mb-3">JPG, GIF or PNG. Max size of 5MB.</p>
          <input 
            type="file" 
            ref={fileInputRef} 
            onChange={handleAvatarChange} 
            accept="image/*" 
            className="hidden" 
          />
          <button 
            onClick={() => fileInputRef.current?.click()}
            className="px-4 py-2 bg-white/10 hover:bg-white/20 text-white rounded-lg text-sm transition-colors border border-white/5"
          >
            Upload new picture
          </button>
        </div>
      </div>

      {error && <div className="bg-red-500/10 border border-red-500/50 text-red-400 p-3 rounded-xl mb-6 text-sm">{error}</div>}
      {success && <div className="bg-green-500/10 border border-green-500/50 text-green-400 p-3 rounded-xl mb-6 text-sm">{success}</div>}

      {/* Form Section */}
      <form onSubmit={handleSave} className="space-y-6">
        <div className="grid grid-cols-2 gap-6">
          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-300">First Name</label>
            <input 
              type="text" 
              name="firstName"
              value={formData.firstName}
              onChange={handleChange}
              className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white focus:outline-none focus:ring-2 focus:ring-primary/50" 
            />
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-300">Last Name</label>
            <input 
              type="text" 
              name="lastName"
              value={formData.lastName}
              onChange={handleChange}
              className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white focus:outline-none focus:ring-2 focus:ring-primary/50" 
            />
          </div>
        </div>

        <div className="space-y-2">
          <label className="text-sm font-medium text-gray-300">Bio</label>
          <textarea 
            name="bio"
            value={formData.bio}
            onChange={handleChange}
            rows={4}
            className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white focus:outline-none focus:ring-2 focus:ring-primary/50 custom-scrollbar" 
            placeholder="Tell us a little about yourself..."
          ></textarea>
        </div>

        <div className="flex justify-end pt-4">
          <button 
            type="submit"
            disabled={loading}
            className="px-6 py-3 bg-gradient-to-r from-primary to-secondary hover:from-primary-dark hover:to-secondary-dark text-white rounded-xl font-medium flex items-center gap-2 transform hover:scale-[1.02] transition-all disabled:opacity-70"
          >
            {loading ? <Loader2 className="animate-spin" size={18} /> : <Save size={18} />}
            Save Changes
          </button>
        </div>
      </form>
    </motion.div>
  );
};

export default GeneralSettings;
