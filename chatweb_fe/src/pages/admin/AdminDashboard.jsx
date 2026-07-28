import React, { useEffect, useState, useRef } from 'react';
import {  adminService  } from '../../services';
import { Stomp } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

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
            const data = await adminService.getAllUsers();
            setUsers(data);
        } catch (err) {
            setError("Không thể tải danh sách user.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchUsers();

        const client = Stomp.over(() => new SockJS("http://localhost:8080/ws"));                                                                                                                
        client.debug = () => { };

        client.connect({}, () => {
            console.log("Admin connected to WebSocket");
            client.subscribe("/topic/public", (payload) => {
                const msg = JSON.parse(payload.body);
                if (msg.messageType === 'JOIN') {
                    setUsers(prevUsers => prevUsers.map(user =>
                        user.username === msg.sender ? { ...user, online: true } : user
                    ));
                } else if (msg.messageType === 'LEAVE') {
                    setUsers(prevUsers => prevUsers.map(user =>
                        user.username === msg.sender ? { ...user, online: false } : user
                    ));
                }
            });
        });

        stompClientRef.current = client;

        return () => {
            if (client && client.connected) {
                client.disconnect();
            }
        };
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
                await adminService.adminUpdateUser(editingUser.username, {
                    fullName: formData.fullName,
                    email: formData.email,
                    phone: formData.phone,
                    role: formData.role
                });
                alert("Cập nhật thành công!");
            } else {
                await adminService.adminCreateUser({
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
                await adminService.unlockUser(user.username);
            } else {
                await adminService.lockUser(user.username);
            }
            fetchUsers();
        } catch (err) {
            alert("Thao tác thất bại");
        }
    };

    const handleDelete = async (username) => {
        if (!window.confirm(`Xóa vĩnh viễn user ${username}?`)) return;
        try {
            await adminService.deleteUserAdmin(username);
            fetchUsers();
        } catch (err) {
            alert("Không thể xóa user: " + (err.response?.data || err.message));
        }
    };

    if (loading) return <div className="min-h-screen flex items-center justify-center text-slate-300">Loading users...</div>;

    return (
        <div className="max-w-7xl mx-auto p-4 md:p-8 animate-enter">
            <div className="flex flex-col md:flex-row justify-between items-center mb-8 gap-4">
                <h2 className="text-3xl font-bold bg-gradient-to-r from-indigo-400 to-purple-400 bg-clip-text text-transparent">
                    Admin Dashboard
                </h2>
                <button 
                    className="glass-button bg-emerald-600 hover:bg-emerald-500 shadow-[0_0_15px_rgba(16,185,129,0.3)] hover:shadow-[0_0_25px_rgba(16,185,129,0.5)]" 
                    onClick={openAddModal}
                >
                    + Thêm User
                </button>
            </div>

            {error && <p className="text-rose-400 mb-4 p-4 bg-rose-500/10 border border-rose-500/20 rounded-xl">{error}</p>}

            <div className="glass-panel overflow-x-auto rounded-2xl">
                <table className="w-full text-left border-collapse">
                    <thead>
                        <tr className="bg-slate-800/50 border-b border-white/10 text-slate-300 uppercase text-xs tracking-wider">
                            <th className="p-4 font-semibold">ID</th>
                            <th className="p-4 font-semibold">Username</th>
                            <th className="p-4 font-semibold">Email</th>
                            <th className="p-4 font-semibold">Role</th>
                            <th className="p-4 font-semibold">Status</th>
                            <th className="p-4 font-semibold text-center">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5">
                        {users.map(user => (
                            <tr key={user.id} className="hover:bg-slate-800/30 transition-colors">
                                <td className="p-4 text-slate-400">{user.id}</td>
                                <td className="p-4 font-medium text-slate-200">
                                    <div className="flex items-center gap-2">
                                        {user.username}
                                        {user.online && <span className="w-2.5 h-2.5 bg-emerald-500 rounded-full shadow-[0_0_8px_rgba(16,185,129,0.8)]"></span>}
                                    </div>
                                </td>
                                <td className="p-4 text-slate-300">{user.email}</td>
                                <td className="p-4">
                                    <span className={`px-2.5 py-1 text-xs font-semibold rounded-full ${user.role === 'ADMIN' ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30' : 'bg-slate-700/50 text-slate-300 border border-slate-600'}`}>
                                        {user.role}
                                    </span>
                                </td>
                                <td className="p-4">
                                    <span className={`px-2.5 py-1 text-xs font-semibold rounded-full ${user.userStatus === 'ACTIVE' ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-rose-500/20 text-rose-400 border border-rose-500/30'}`}>
                                        {user.userStatus}
                                    </span>
                                </td>
                                <td className="p-4 flex gap-2 justify-center">
                                    <button className="px-3 py-1.5 bg-indigo-500/20 text-indigo-400 hover:bg-indigo-500/30 rounded-lg text-sm font-medium transition-colors" onClick={() => openEditModal(user)}>Sửa</button>
                                    <button
                                        className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${user.userStatus === 'LOCKED' ? 'bg-emerald-500/20 text-emerald-400 hover:bg-emerald-500/30' : 'bg-amber-500/20 text-amber-400 hover:bg-amber-500/30'}`}
                                        onClick={() => handleLockUnlock(user)}
                                    >
                                        {user.userStatus === 'LOCKED' ? 'Mở' : 'Khóa'}
                                    </button>
                                    <button className="px-3 py-1.5 bg-rose-500/20 text-rose-400 hover:bg-rose-500/30 rounded-lg text-sm font-medium transition-colors" onClick={() => handleDelete(user.username)}>Xóa</button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {isModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/80 backdrop-blur-sm animate-enter">
                    <div className="glass-panel max-w-md w-full rounded-2xl p-6 md:p-8 shadow-2xl relative">
                        <h3 className="text-2xl font-bold text-white mb-6">
                            {editingUser ? 'Cập nhật User' : 'Tạo User mới'}
                        </h3>
                        <form onSubmit={handleSubmit} className="space-y-4">
                            {!editingUser && (
                                <>
                                    <div>
                                        <label className="block text-sm font-medium text-slate-300 mb-1">Username</label>
                                        <input className="w-full glass-input" required name="username" value={formData.username} onChange={handleInputChange} />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-slate-300 mb-1">Password</label>
                                        <input className="w-full glass-input" required type="password" name="password" value={formData.password} onChange={handleInputChange} />
                                    </div>
                                </>
                            )}
                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-1">Email</label>
                                <input className="w-full glass-input" required type="email" name="email" value={formData.email} onChange={handleInputChange} />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-1">Full Name</label>
                                <input className="w-full glass-input" name="fullName" value={formData.fullName} onChange={handleInputChange} />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-1">Phone</label>
                                <input className="w-full glass-input" name="phone" value={formData.phone} onChange={handleInputChange} />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-1">Role</label>
                                <select className="w-full glass-input appearance-none bg-slate-900/80" name="role" value={formData.role} onChange={handleInputChange}>
                                    <option value="USER">USER</option>
                                    <option value="ADMIN">ADMIN</option>
                                </select>
                            </div>
                            <div className="flex justify-end gap-3 mt-8">
                                <button type="button" className="px-5 py-2.5 rounded-xl font-medium text-slate-300 hover:text-white hover:bg-slate-700 transition-colors" onClick={() => setIsModalOpen(false)}>Hủy</button>
                                <button type="submit" className="glass-button">Lưu</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AdminDashboard;