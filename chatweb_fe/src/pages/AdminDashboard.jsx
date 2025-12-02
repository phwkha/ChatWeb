import React, { useEffect, useState, useRef } from 'react';
import { authService } from '../services/authService';
import { Stomp } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import '../styles/AdminDashboard.css';

const AdminDashboard = () => {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const stompClientRef = useRef(null);

    // Modal State
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingUser, setEditingUser] = useState(null); // null = create mode
    const [formData, setFormData] = useState({
        username: '',
        password: '',
        email: '',
        fullName: '',
        phone: '',
        role: 'USER'
    });

    const fetchUsers = async () => {
        try {
            setLoading(true);
            const data = await authService.getAllUsers();
            setUsers(data);
        } catch (err) {
            setError("Không thể tải danh sách user.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        // 1. Tải danh sách user ban đầu
        fetchUsers();

        // 2. Kết nối WebSocket để nghe sự kiện Online/Offline
        const client = Stomp.over(() => new SockJS("http://localhost:8080/ws"));                                                                                                                

        // Tắt debug log của Stomp để đỡ rối console (tuỳ chọn)
        client.debug = () => { };

        client.connect({}, () => {
            console.log("Admin connected to WebSocket");

            // Subscribe vào topic chung để biết ai ra/vào
            client.subscribe("/topic/public", (payload) => {
                const msg = JSON.parse(payload.body);

                if (msg.messageType === 'JOIN') {
                    // User vừa Online -> Cập nhật state
                    setUsers(prevUsers => prevUsers.map(user =>
                        user.username === msg.sender
                            ? { ...user, online: true }
                            : user
                    ));
                } else if (msg.messageType === 'LEAVE') {
                    // User vừa Offline -> Cập nhật state
                    setUsers(prevUsers => prevUsers.map(user =>
                        user.username === msg.sender
                            ? { ...user, online: false }
                            : user
                    ));
                }
            });
        });

        stompClientRef.current = client;

        // Cleanup khi rời trang Admin
        return () => {
            if (client && client.connected) {
                client.disconnect();
            }
        };
    }, []);

    useEffect(() => {
        fetchUsers();
    }, []);

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const openAddModal = () => {
        setEditingUser(null);
        setFormData({ username: '', password: '', email: '', fullName: '', phone: '', role: 'USER' });
        setIsModalOpen(true);
    };

    const openEditModal = (user) => {
        setEditingUser(user);
        setFormData({
            username: user.username,
            email: user.email,
            fullName: user.fullName || '',
            phone: user.phone || '',
            role: user.role
        });
        setIsModalOpen(true);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            if (editingUser) {
                // Update
                await authService.adminUpdateUser(editingUser.username, {
                    fullName: formData.fullName,
                    email: formData.email,
                    phone: formData.phone,
                    role: formData.role
                });
                alert("Cập nhật thành công!");
            } else {
                // Create
                await authService.adminCreateUser({
                    username: formData.username,
                    password: formData.password,
                    email: formData.email,
                    role: formData.role
                });
                alert("Tạo user thành công!");
            }
            setIsModalOpen(false);
            fetchUsers();
        } catch (err) {
            alert("Lỗi: " + (err.response?.data || err.message));
        }
    };

    const handleLockUnlock = async (user) => {
        const isLocked = user.userStatus === 'LOCKED';
        const action = isLocked ? 'unlock' : 'lock';
        if (!window.confirm(`Bạn có chắc muốn ${action} user ${user.username}?`)) return;

        try {
            if (isLocked) {
                await authService.unlockUser(user.username);
            } else {
                await authService.lockUser(user.username);
            }
            fetchUsers();
        } catch (err) {
            alert("Thao tác thất bại");
        }
    };

    const handleDelete = async (username) => {
        if (!window.confirm(`Xóa vĩnh viễn user ${username}?`)) return;
        try {
            await authService.deleteUserAdmin(username);
            fetchUsers();
        } catch (err) {
            alert("Không thể xóa user: " + (err.response?.data || err.message));
        }
    };

    if (loading) return <div className="loading">Loading users...</div>;

    return (
        <div className="admin-container">
            <div className="admin-header">
                <h2>Admin Dashboard</h2>
                <button className="btn-add" onClick={openAddModal}>+ Thêm User</button>
            </div>

            {error && <p style={{ color: 'red' }}>{error}</p>}

            <table className="admin-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Username</th>
                        <th>Email</th>
                        <th>Role</th>
                        <th>Status</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {users.map(user => (
                        <tr key={user.id}>
                            <td>{user.id}</td>
                            <td>
                                {user.username}
                                {user.online && <span style={{ color: 'green', marginLeft: 5, fontSize: '1.2em' }}>●</span>}                            </td>
                            <td>{user.email}</td>
                            <td>
                                <span className={`role-badge ${user.role === 'ADMIN' ? 'role-admin' : ''}`}>
                                    {user.role}
                                </span>
                            </td>
                            <td>
                                <span className={`status-badge ${user.userStatus === 'ACTIVE' ? 'status-active' : 'status-locked'}`}>
                                    {user.userStatus}
                                </span>
                            </td>
                            <td className="action-buttons">
                                <button className="btn-sm btn-edit" onClick={() => openEditModal(user)}>Sửa</button>
                                <button
                                    className={`btn-sm ${user.userStatus === 'LOCKED' ? 'btn-unlock' : 'btn-lock'}`}
                                    onClick={() => handleLockUnlock(user)}
                                >
                                    {user.userStatus === 'LOCKED' ? 'Mở' : 'Khóa'}
                                </button>
                                <button className="btn-sm btn-delete" onClick={() => handleDelete(user.username)}>Xóa</button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>

            {isModalOpen && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <h3>{editingUser ? 'Cập nhật User' : 'Tạo User mới'}</h3>
                        <form onSubmit={handleSubmit}>
                            {!editingUser && (
                                <>
                                    <div className="form-group">
                                        <label>Username</label>
                                        <input required name="username" value={formData.username} onChange={handleInputChange} />
                                    </div>
                                    <div className="form-group">
                                        <label>Password</label>
                                        <input required type="password" name="password" value={formData.password} onChange={handleInputChange} />
                                    </div>
                                </>
                            )}
                            <div className="form-group">
                                <label>Email</label>
                                <input required type="email" name="email" value={formData.email} onChange={handleInputChange} />
                            </div>
                            <div className="form-group">
                                <label>Full Name</label>
                                <input name="fullName" value={formData.fullName} onChange={handleInputChange} />
                            </div>
                            <div className="form-group">
                                <label>Phone</label>
                                <input name="phone" value={formData.phone} onChange={handleInputChange} />
                            </div>
                            <div className="form-group">
                                <label>Role</label>
                                <select name="role" value={formData.role} onChange={handleInputChange}>
                                    <option value="USER">USER</option>
                                    <option value="ADMIN">ADMIN</option>
                                </select>
                            </div>
                            <div className="modal-actions">
                                <button type="button" onClick={() => setIsModalOpen(false)}>Hủy</button>
                                <button type="submit" className="btn-add">Lưu</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AdminDashboard;