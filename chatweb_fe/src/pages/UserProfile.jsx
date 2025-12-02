import React, { useState, useEffect } from 'react';
import { authService } from '../services/authService';
import '../styles/UserProfile.css';

const UserProfile = () => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [isEditing, setIsEditing] = useState(false);
    const [formData, setFormData] = useState({
        fullName: '',
        email: '',
        phone: ''
    });

    const [isChangingPassword, setIsChangingPassword] = useState(false);
    const [passwordData, setPasswordData] = useState({
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
    });

    useEffect(() => {
        loadUserProfile();
    }, []);

    const loadUserProfile = async () => {
        try {
            setLoading(true);
            const userData = await authService.fetchCurrentUser();
            setUser(userData);
            setFormData({
                fullName: userData.fullName || '',
                email: userData.email || '',
                phone: userData.phone || ''
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
            await authService.updateUserProfile(user.username, formData);
            alert("Cập nhật thông tin thành công!");
            setIsEditing(false);
            loadUserProfile();
        } catch (error) {
            alert("Lỗi cập nhật: " + (error.response?.data?.message || error.message));
        }
    };

    const handlePasswordChangeInput = (e) => {
        const { name, value } = e.target;
        setPasswordData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmitPasswordChange = async (e) => {
        e.preventDefault();
        if (passwordData.newPassword !== passwordData.confirmPassword) {
            alert("Mật khẩu xác nhận không khớp!");
            return;
        }

        try {
            await authService.changePassword(passwordData.currentPassword, passwordData.newPassword);
            alert("Đổi mật khẩu thành công!");
            setIsChangingPassword(false);
            setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
        } catch (error) {
            const msg = typeof error === 'string' ? error : (error.message || JSON.stringify(error));
            alert("Lỗi: " + msg);
        }
    };

    if (loading) return <div className="loading">Đang tải thông tin...</div>;
    if (!user) return <div className="error">Không tìm thấy thông tin người dùng.</div>;

    return (
        <div className="profile-container">
            <div className="profile-card">
                <div className="profile-header">
                    <div className="profile-avatar">
                        {user.username.charAt(0).toUpperCase()}
                    </div>
                    <h2>{user.username}</h2>
                    <span className="profile-role">{user.role}</span>
                </div>

                <div className="profile-body">
                    {!isChangingPassword ? (
                    <form onSubmit={handleSubmit}>
                        <div className="profile-info-group">
                            <label className="profile-label">Họ và Tên</label>
                            {isEditing ? (
                                <input
                                    className="profile-input"
                                    name="fullName"
                                    value={formData.fullName}
                                    onChange={handleInputChange}
                                    placeholder="Cập nhật họ tên"
                                />
                            ) : (
                                <div className="profile-value">{user.fullName || "Chưa cập nhật"}</div>
                            )}
                        </div>

                        <div className="profile-info-group">
                            <label className="profile-label">Email</label>
                            {isEditing ? (
                                <input
                                    className="profile-input"
                                    type="email"
                                    name="email"
                                    value={formData.email}
                                    onChange={handleInputChange}
                                    required
                                />
                            ) : (
                                <div className="profile-value">{user.email}</div>
                            )}
                        </div>

                        <div className="profile-info-group">
                            <label className="profile-label">Số điện thoại</label>
                            {isEditing ? (
                                <input
                                    className="profile-input"
                                    name="phone"
                                    value={formData.phone}
                                    onChange={handleInputChange}
                                    placeholder="Cập nhật số điện thoại"
                                />
                            ) : (
                                <div className="profile-value">{user.phone || "Chưa cập nhật"}</div>
                            )}
                        </div>

                        <div className="profile-info-group">
                            <label className="profile-label">Trạng thái</label>
                            <div className="profile-value" style={{ color: user.userStatus === 'ACTIVE' ? 'green' : 'red' }}>
                                {user.userStatus}
                            </div>
                        </div>

                        <div className="profile-actions">
                            {isEditing ? (
                                <>
                                    <button
                                        type="button"
                                        className="btn-cancel"
                                        onClick={() => {
                                            setIsEditing(false);
                                            setFormData({
                                                fullName: user.fullName || '',
                                                email: user.email || '',
                                                phone: user.phone || ''
                                            });
                                        }}
                                    >
                                        Hủy
                                    </button>
                                    <button type="submit" className="btn-save">
                                        Lưu thay đổi
                                    </button>
                                </>
                            ) : (
                                <>
                                    <button
                                        type="button"
                                        className="btn-edit-profile"
                                        onClick={() => setIsEditing(true)}
                                    >
                                        Chỉnh sửa thông tin
                                    </button>
                                    <button
                                        type="button"
                                        className="btn-edit-profile"
                                        style={{ marginLeft: '10px', background: '#ff9800' }}
                                        onClick={() => setIsChangingPassword(true)}
                                    >
                                        Đổi mật khẩu
                                    </button>
                                </>
                            )}
                        </div>
                    </form>
                    ) : (
                        <form onSubmit={handleSubmitPasswordChange}>
                            <h3 style={{marginBottom: '20px', color: '#333'}}>Đổi Mật Khẩu</h3>
                            
                            <div className="profile-info-group">
                                <label className="profile-label">Mật khẩu hiện tại</label>
                                <input
                                    className="profile-input"
                                    type="password"
                                    name="currentPassword"
                                    value={passwordData.currentPassword}
                                    onChange={handlePasswordChangeInput}
                                    required
                                    placeholder="Nhập mật khẩu cũ"
                                />
                            </div>

                            <div className="profile-info-group">
                                <label className="profile-label">Mật khẩu mới</label>
                                <input
                                    className="profile-input"
                                    type="password"
                                    name="newPassword"
                                    value={passwordData.newPassword}
                                    onChange={handlePasswordChangeInput}
                                    required
                                    placeholder="Nhập mật khẩu mới"
                                />
                            </div>

                            <div className="profile-info-group">
                                <label className="profile-label">Xác nhận mật khẩu mới</label>
                                <input
                                    className="profile-input"
                                    type="password"
                                    name="confirmPassword"
                                    value={passwordData.confirmPassword}
                                    onChange={handlePasswordChangeInput}
                                    required
                                    placeholder="Nhập lại mật khẩu mới"
                                />
                            </div>

                            <div className="profile-actions">
                                <button 
                                    type="button" 
                                    className="btn-cancel"
                                    onClick={() => {
                                        setIsChangingPassword(false);
                                        setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
                                    }}
                                >
                                    Quay lại
                                </button>
                                <button type="submit" className="btn-save" style={{background: '#ff9800'}}>
                                    Xác nhận đổi
                                </button>
                            </div>
                        </form>
                    )}
                </div>
            </div>
        </div>
    );
};

export default UserProfile;