import { useState, useEffect } from 'react';
import SearchModal from './SearchModal';
import { friendService } from '../../services';

const Sidebar = ({ onlineUsers, username, userColor, openPrivateChat, unreadMessages, activeChat, sendFriendRequest, acceptFriendRequest }) => {
    const [activeTab, setActiveTab] = useState('online');
    const [isSearchOpen, setIsSearchOpen] = useState(false);
    const [friends, setFriends] = useState([]);
    const [requests, setRequests] = useState([]);

    useEffect(() => {
        if (activeTab === 'friends') {
            friendService.getFriendsList().then(res => setFriends(res.content || [])).catch(console.error);
        } else if (activeTab === 'requests') {
            friendService.getRequests().then(res => setRequests(res.content || [])).catch(console.error);
        }
    }, [activeTab]);

    const handleAccept = (reqUsername) => {
        try {
            acceptFriendRequest(reqUsername);
            setRequests(prev => prev.filter(r => r.username !== reqUsername));
            alert("Đã gửi yêu cầu chấp nhận kết bạn");
        } catch (e) {
            console.error(e);
        }
    };

    const handleReject = async (reqUsername) => {
        try {
            await friendService.deleteFriendship(reqUsername);
            setRequests(prev => prev.filter(r => r.username !== reqUsername));
        } catch (e) {
            console.error(e);
        }
    };

    return (
        <div className="w-64 lg:w-80 bg-slate-800/80 backdrop-blur-xl border-r border-white/10 flex flex-col z-20 h-full shadow-2xl">
            <div className="h-16 flex items-center justify-between px-4 border-b border-white/10 bg-slate-800/50">
                <div className="flex gap-1 bg-slate-900/50 p-1 rounded-lg">
                    <button 
                        className={`px-3 py-1 rounded-md text-sm font-medium transition-colors ${activeTab === 'online' ? 'bg-indigo-600 text-white' : 'text-slate-400 hover:text-white'}`}
                        onClick={() => setActiveTab('online')}
                    >
                        Online
                    </button>
                    <button 
                        className={`px-3 py-1 rounded-md text-sm font-medium transition-colors ${activeTab === 'friends' ? 'bg-indigo-600 text-white' : 'text-slate-400 hover:text-white'}`}
                        onClick={() => setActiveTab('friends')}
                    >
                        Bạn Bè
                    </button>
                    <button 
                        className={`px-3 py-1 rounded-md text-sm font-medium transition-colors ${activeTab === 'requests' ? 'bg-indigo-600 text-white' : 'text-slate-400 hover:text-white'}`}
                        onClick={() => setActiveTab('requests')}
                    >
                        Lời Mời
                    </button>
                </div>
                <button 
                    onClick={() => setIsSearchOpen(true)}
                    className="p-2 bg-slate-700 hover:bg-slate-600 rounded-full text-white transition-colors"
                    title="Tìm kiếm người dùng"
                >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
                </button>
            </div>
            
            <div className="flex-1 overflow-y-auto p-4 space-y-2 custom-scrollbar">
                {activeTab === 'online' && (
                    <>
                        <div
                            className={`flex items-center gap-3 p-3 rounded-xl cursor-pointer transition-all ${
                                activeChat === 'WORLD' 
                                    ? 'bg-slate-700 border border-white/10 shadow-lg' 
                                    : 'hover:bg-slate-700/50 border border-transparent hover:border-white/5'
                            }`}
                            onClick={() => openPrivateChat('WORLD')}
                        >
                            <div className="w-10 h-10 rounded-full flex items-center justify-center text-white font-bold shadow-lg text-lg bg-gradient-to-br from-indigo-500 to-purple-500">
                                🌍
                            </div>
                            <div className="flex-1 min-w-0">
                                <span className={`font-medium truncate ${activeChat === 'WORLD' ? 'text-white' : 'text-slate-200'}`}>
                                    Kênh Thế Giới
                                </span>
                            </div>
                        </div>

                        {Array.from(onlineUsers)
                            .sort((a, b) => {
                                if (a === username) return -1;
                                if (b === username) return 1;
                                return a.localeCompare(b);
                            }).map((user) => (
                            <div
                                key={user}
                                className={`flex items-center gap-3 p-3 rounded-xl cursor-pointer transition-all ${
                                    activeChat === user
                                        ? 'bg-slate-700 border border-white/10 shadow-lg'
                                        : user === username 
                                            ? 'bg-indigo-600/20 border border-indigo-500/30 opacity-70' 
                                            : 'hover:bg-slate-700/50 border border-transparent hover:border-white/5'
                                }`}
                                onClick={() => openPrivateChat(user)}
                            >
                                <div className="relative">
                                    <div 
                                        className="w-10 h-10 rounded-full flex items-center justify-center text-white font-bold shadow-lg"
                                        style={{ backgroundColor: user === username ? userColor : '#4f46e5' }}
                                    >
                                        {user.charAt(0).toUpperCase()}
                                    </div>
                                    <div className="absolute -bottom-1 -right-1 w-3.5 h-3.5 bg-emerald-500 border-2 border-slate-800 rounded-full"></div>
                                </div>
                                
                                <div className="flex-1 min-w-0">
                                    <div className="flex items-center justify-between">
                                        <span className={`font-medium truncate ${user === username ? 'text-indigo-300' : 'text-slate-200'}`}>
                                            {user} {user === username && '(Bạn)'}
                                        </span>
                                    </div>
                                </div>
                                
                                {unreadMessages.has(user) && activeChat !== user && (
                                    <div className="w-5 h-5 bg-rose-500 rounded-full flex items-center justify-center text-[10px] font-bold text-white shadow-lg animate-pulse">
                                        {unreadMessages.get(user)}
                                    </div>
                                )}
                            </div>
                        ))}
                    </>
                )}

                {activeTab === 'friends' && (
                    <>
                        {friends.length === 0 ? <div className="text-center text-slate-500 py-4 text-sm">Chưa có bạn bè</div> : friends.map(user => (
                            <div key={user.username} className="flex items-center gap-3 p-3 rounded-xl hover:bg-slate-700/50 border border-transparent hover:border-white/5 transition-all cursor-pointer" onClick={() => openPrivateChat(user.username)}>
                                <div className="w-10 h-10 rounded-full bg-indigo-500 flex items-center justify-center text-white font-bold">{user.username.charAt(0).toUpperCase()}</div>
                                <div className="flex-1 min-w-0 font-medium text-slate-200 truncate">{user.username}</div>
                            </div>
                        ))}
                    </>
                )}

                {activeTab === 'requests' && (
                    <>
                        {requests.length === 0 ? <div className="text-center text-slate-500 py-4 text-sm">Không có lời mời nào</div> : requests.map(req => (
                            <div key={req.username} className="flex flex-col gap-2 p-3 rounded-xl bg-slate-700/30 border border-white/5">
                                <div className="flex items-center gap-3">
                                    <div className="w-8 h-8 rounded-full bg-indigo-500 flex items-center justify-center text-white font-bold">{req.username.charAt(0).toUpperCase()}</div>
                                    <div className="flex-1 min-w-0 font-medium text-slate-200 truncate">{req.username}</div>
                                </div>
                                <div className="flex gap-2">
                                    <button onClick={() => handleAccept(req.username)} className="flex-1 bg-emerald-600 hover:bg-emerald-500 text-white py-1 rounded text-sm transition-colors">Chấp nhận</button>
                                    <button onClick={() => handleReject(req.username)} className="flex-1 bg-slate-600 hover:bg-slate-500 text-white py-1 rounded text-sm transition-colors">Xóa</button>
                                </div>
                            </div>
                        ))}
                    </>
                )}
            </div>

            <SearchModal 
                isOpen={isSearchOpen} 
                onClose={() => setIsSearchOpen(false)} 
                currentUser={username}
                sendFriendRequest={sendFriendRequest}
            />
        </div>
    );
};

export default Sidebar;
