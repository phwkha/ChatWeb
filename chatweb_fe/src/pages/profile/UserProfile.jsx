import React, { useState, useEffect } from 'react';
import { userService } from '../../services';
import AddressManager from './AddressManager';

const ChangeContactModal = ({ type, onClose, onSuccess }) => {
    const isEmail = type === 'email';
    const [step, setStep] = useState(1);
    
    // Step 1 state
    const [newValue, setNewValue] = useState('');
    const [currentPassword, setCurrentPassword] = useState('');
    
    // Step 2 state
    const [otp, setOtp] = useState('');
    
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState('');
    
    const handleInitiate = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage('');
        try {
            if (isEmail) {
                await userService.initiateEmailChange(newValue, currentPassword);
            } else {
                await userService.initiatePhoneChange(newValue, currentPassword);
            }
            setStep(2);
            setMessage(`OTP sent to ${isEmail ? 'new email' : 'new phone number'}`);
        } catch (error) {
            setMessage(error.message || `Failed to initiate ${type} change`);
        } finally {
            setLoading(false);
        }
    };
    
    const handleVerify = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage('');
        try {
            if (isEmail) {
                await userService.verifyEmailChange(newValue, otp);
            } else {
                await userService.verifyPhoneChange(newValue, otp);
            }
            alert(`${isEmail ? 'Email' : 'Phone'} updated successfully!`);
            onSuccess();
        } catch (error) {
            setMessage(error.message || `Failed to verify OTP`);
        } finally {
            setLoading(false);
        }
    };
    
    const handleResend = async () => {
        setLoading(true);
        try {
            if (isEmail) {
                await userService.resendEmailChangeOtp();
            } else {
                await userService.resendPhoneChangeOtp();
            }
            setMessage("OTP resent successfully!");
        } catch (error) {
            setMessage(error.message || "Failed to resend OTP");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
            <div className="glass-panel p-6 rounded-2xl w-full max-w-md animate-enter border border-white/10 shadow-2xl relative overflow-hidden">
                <div className="absolute top-0 right-0 w-32 h-32 bg-indigo-500/20 rounded-full blur-[40px]"></div>
                
                <h3 className="text-xl font-bold text-white mb-6 relative z-10">
                    Change {isEmail ? 'Email' : 'Phone'}
                </h3>
                
                {message && (
                    <div className={`p-3 rounded-lg mb-4 text-sm ${message.includes('sent') || message.includes('success') ? 'bg-emerald-500/10 text-emerald-400' : 'bg-rose-500/10 text-rose-400'} relative z-10`}>
                        {message}
                    </div>
                )}
                
                <div className="relative z-10">
                    {step === 1 ? (
                        <form onSubmit={handleInitiate} className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-1">New {isEmail ? 'Email' : 'Phone'}</label>
                                <input
                                    type={isEmail ? "email" : "text"}
                                    value={newValue}
                                    onChange={(e) => setNewValue(e.target.value)}
                                    className="w-full glass-input"
                                    required
                                    placeholder={isEmail ? "Enter new email" : "Enter new phone"}
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-1">Current Password</label>
                                <input
                                    type="password"
                                    value={currentPassword}
                                    onChange={(e) => setCurrentPassword(e.target.value)}
                                    className="w-full glass-input"
                                    required
                                    placeholder="Enter current password to verify"
                                />
                            </div>
                            <div className="flex gap-3 pt-2">
                                <button type="button" onClick={onClose} className="flex-1 py-2 rounded-lg text-slate-300 hover:bg-white/5 transition-colors">
                                    Cancel
                                </button>
                                <button type="submit" disabled={loading} className="flex-1 btn-primary py-2 rounded-lg font-medium shadow-lg disabled:opacity-50">
                                    {loading ? "Sending..." : "Send OTP"}
                                </button>
                            </div>
                        </form>
                    ) : (
                        <form onSubmit={handleVerify} className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-1">OTP Code</label>
                                <input
                                    type="text"
                                    value={otp}
                                    onChange={(e) => setOtp(e.target.value)}
                                    className="w-full glass-input text-center tracking-[0.2em]"
                                    maxLength={6}
                                    required
                                    placeholder="6-digit code"
                                />
                            </div>
                            <div className="text-center">
                                <button type="button" onClick={handleResend} className="text-sm text-indigo-400 hover:text-indigo-300 transition-colors">
                                    Resend OTP
                                </button>
                            </div>
                            <div className="flex gap-3 pt-2">
                                <button type="button" onClick={onClose} className="flex-1 py-2 rounded-lg text-slate-300 hover:bg-white/5 transition-colors">
                                    Cancel
                                </button>
                                <button type="submit" disabled={loading} className="flex-1 btn-primary py-2 rounded-lg font-medium shadow-lg disabled:opacity-50">
                                    {loading ? "Verifying..." : "Verify & Update"}
                                </button>
                            </div>
                        </form>
                    )}
                </div>
            </div>
        </div>
    );
};

const UserProfile = () => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [isEditing, setIsEditing] = useState(false);
    
    // Core profile data (firstName, lastName, birthday, gender) 
    // BUT since we don't know if the backend fully implements all fields in fetchCurrentUser, we'll keep it simple
    const [formData, setFormData] = useState({
        fullName: ''
    });

    const [isChangingPassword, setIsChangingPassword] = useState(false);
    const [passwordData, setPasswordData] = useState({
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
    });

    const [contactModalType, setContactModalType] = useState(null);

    useEffect(() => {
        loadUserProfile();
    }, []);

    const loadUserProfile = async () => {
        try {
            setLoading(true);
            const userData = await userService.fetchCurrentUser();
            setUser(userData);
            setFormData({
                fullName: userData.fullName || userData.firstName + ' ' + userData.lastName || ''
            });
        } catch (error) {
            console.error("Error loading profile:", error);
        } finally {
            setLoading(false);
        }
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            // Note: updateUserProfile takes UpdateUserRequest which requires firstName, lastName, phone, etc.
            // Since we split the flow, we might need to send dummy or existing data for other fields.
            // Assuming the backend handles partial updates or ignores unchanged fields.
            await userService.updateUserProfile(user.username, {
                firstName: formData.fullName.split(' ')[0] || '',
                lastName: formData.fullName.split(' ').slice(1).join(' ') || '',
                phone: user.phone || '0000000000'
            });
            alert("Profile updated successfully!");
            setIsEditing(false);
            loadUserProfile();
        } catch (error) {
            alert("Update error: " + error.message);
        }
    };

    const handlePasswordChangeInput = (e) => {
        const { name, value } = e.target;
        setPasswordData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmitPasswordChange = async (e) => {
        e.preventDefault();
        if (passwordData.newPassword !== passwordData.confirmPassword) {
            alert("Passwords do not match!");
            return;
        }

        try {
            await userService.changePassword(passwordData.currentPassword, passwordData.newPassword);
            alert("Password changed successfully!");
            setIsChangingPassword(false);
            setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
        } catch (error) {
            const msg = typeof error === 'string' ? error : (error.message || JSON.stringify(error));
            alert("Error: " + msg);
        }
    };

    if (loading) return <div className="min-h-screen flex items-center justify-center text-slate-300">Loading profile...</div>;
    if (!user) return <div className="min-h-screen flex items-center justify-center text-rose-400">User not found.</div>;

    return (
        <div className="min-h-[calc(100vh-4rem)] p-4 md:p-8 flex justify-center animate-enter">
            <div className="glass-panel rounded-3xl p-8 max-w-2xl w-full h-fit mt-8 border border-white/10 shadow-2xl relative overflow-hidden">
                <div className="absolute top-0 right-0 w-64 h-64 bg-indigo-500/10 rounded-full blur-[80px]"></div>

                <div className="flex flex-col md:flex-row items-center md:items-start gap-6 border-b border-white/10 pb-8 mb-8 relative z-10">
                    <div className="w-24 h-24 rounded-full bg-indigo-600 flex items-center justify-center text-3xl font-bold text-white shadow-xl shadow-indigo-500/30">
                        {user.username.charAt(0).toUpperCase()}
                    </div>
                    <div className="text-center md:text-left flex-1">
                        <h2 className="text-3xl font-bold text-white mb-2">{user.username}</h2>
                        <span className={`inline-block px-3 py-1 text-xs font-semibold rounded-full ${user.role === 'ADMIN' ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30' : 'bg-slate-700/50 text-slate-300 border border-slate-600'}`}>
                            {user.role}
                        </span>
                    </div>
                </div>

                <div className="relative z-10">
                    {!isChangingPassword ? (
                    <form onSubmit={handleSubmit} className="space-y-6">
                        <div>
                            <label className="block text-sm font-medium text-slate-400 mb-2">Full Name</label>
                            {isEditing ? (
                                <input className="w-full glass-input" name="fullName" value={formData.fullName} onChange={handleInputChange} placeholder="Update full name" />
                            ) : (
                                <div className="text-lg text-slate-200 font-medium py-2">{formData.fullName || "Not set"}</div>
                            )}
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-slate-400 mb-2">Email</label>
                            <div className="flex items-center justify-between">
                                <div className="text-lg text-slate-200 font-medium py-2">{user.email}</div>
                                {!isEditing && (
                                    <button type="button" onClick={() => setContactModalType('email')} className="text-sm text-indigo-400 hover:text-indigo-300 font-medium transition-colors">
                                        Change Email
                                    </button>
                                )}
                            </div>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-slate-400 mb-2">Phone Number</label>
                            <div className="flex items-center justify-between">
                                <div className="text-lg text-slate-200 font-medium py-2">{user.phone || "Not set"}</div>
                                {!isEditing && (
                                    <button type="button" onClick={() => setContactModalType('phone')} className="text-sm text-indigo-400 hover:text-indigo-300 font-medium transition-colors">
                                        Change Phone
                                    </button>
                                )}
                            </div>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-slate-400 mb-2">Status</label>
                            <div className={`text-lg font-bold py-2 ${user.userStatus === 'ACTIVE' ? 'text-emerald-400' : 'text-rose-400'}`}>
                                {user.userStatus}
                            </div>
                        </div>

                        <div className="flex gap-4 pt-4 border-t border-white/10 mt-8">
                            {isEditing ? (
                                <>
                                    <button
                                        type="button"
                                        className="px-6 py-3 rounded-xl font-medium text-slate-300 border border-slate-600 hover:bg-slate-700 hover:text-white transition-all flex-1"
                                        onClick={() => {
                                            setIsEditing(false);
                                            setFormData({ fullName: user.fullName || user.firstName + ' ' + user.lastName || '' });
                                        }}
                                    >
                                        Cancel
                                    </button>
                                    <button type="submit" className="glass-button flex-1">
                                        Save Changes
                                    </button>
                                </>
                            ) : (
                                <>
                                    <button type="button" className="glass-button flex-1" onClick={() => setIsEditing(true)}>
                                        Edit Profile
                                    </button>
                                    <button type="button" className="px-6 py-3 rounded-xl font-medium bg-amber-500/20 text-amber-400 border border-amber-500/30 hover:bg-amber-500/30 transition-all flex-1" onClick={() => setIsChangingPassword(true)}>
                                        Change Password
                                    </button>
                                </>
                            )}
                        </div>
                    </form>
                    ) : (
                        <form onSubmit={handleSubmitPasswordChange} className="space-y-6 animate-enter">
                            <h3 className="text-xl font-bold text-white mb-6 border-b border-white/10 pb-4">Change Password</h3>
                            
                            <div>
                                <label className="block text-sm font-medium text-slate-400 mb-2">Current Password</label>
                                <input className="w-full glass-input" type="password" name="currentPassword" value={passwordData.currentPassword} onChange={handlePasswordChangeInput} required placeholder="Enter current password" />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-400 mb-2">New Password</label>
                                <input className="w-full glass-input" type="password" name="newPassword" value={passwordData.newPassword} onChange={handlePasswordChangeInput} required placeholder="Enter new password" />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-400 mb-2">Confirm New Password</label>
                                <input className="w-full glass-input" type="password" name="confirmPassword" value={passwordData.confirmPassword} onChange={handlePasswordChangeInput} required placeholder="Confirm new password" />
                            </div>

                            <div className="flex gap-4 pt-4 border-t border-white/10 mt-8">
                                <button type="button" className="px-6 py-3 rounded-xl font-medium text-slate-300 border border-slate-600 hover:bg-slate-700 hover:text-white transition-all flex-1" onClick={() => {
                                    setIsChangingPassword(false);
                                    setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
                                }}>
                                    Cancel
                                </button>
                                <button type="submit" className="glass-button bg-amber-600 hover:bg-amber-500 shadow-[0_0_15px_rgba(245,158,11,0.3)] hover:shadow-[0_0_25px_rgba(245,158,11,0.5)] flex-1">
                                    Confirm Change
                                </button>
                            </div>
                        </form>
                    )}
                    
                    {/* Add AddressManager component here */}
                    <AddressManager />
                </div>

                {contactModalType && (
                    <ChangeContactModal 
                        type={contactModalType} 
                        onClose={() => setContactModalType(null)} 
                        onSuccess={() => {
                            setContactModalType(null);
                            loadUserProfile();
                        }}
                    />
                )}
            </div>
        </div>
    );
};

export default UserProfile;