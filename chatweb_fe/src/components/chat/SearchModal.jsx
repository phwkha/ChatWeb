import { useState } from 'react';
import { searchService, friendService } from '../../services';

const SearchModal = ({ isOpen, onClose, currentUser, sendFriendRequest }) => {
    const [keyword, setKeyword] = useState('');
    const [results, setResults] = useState([]);
    const [isSearching, setIsSearching] = useState(false);

    const handleSearch = async (e) => {
        e.preventDefault();
        if (!keyword.trim()) return;
        
        setIsSearching(true);
        try {
            const data = await searchService.searchUsers(keyword);
            if (data && data.content) {
                setResults(data.content.filter(u => u.username !== currentUser));
            }
        } catch (error) {
            console.error("Search failed", error);
        } finally {
            setIsSearching(false);
        }
    };

    const handleAddFriend = async (username) => {
        try {
            sendFriendRequest(username);
            alert('Đã gửi lời mời kết bạn');
            setResults(prev => prev.filter(u => u.username !== username));
        } catch (error) {
            alert('Gửi lời mời thất bại hoặc đã là bạn bè');
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm animate-enter">
            <div className="bg-slate-800 border border-white/10 rounded-2xl w-full max-w-lg shadow-2xl p-6 relative">
                <button 
                    onClick={onClose}
                    className="absolute top-4 right-4 text-slate-400 hover:text-white transition-colors"
                >
                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                </button>
                
                <h3 className="text-xl font-bold text-white mb-4">Tìm kiếm người dùng</h3>
                
                <form onSubmit={handleSearch} className="flex gap-2 mb-6">
                    <input 
                        type="text" 
                        placeholder="Nhập tên người dùng hoặc email..." 
                        className="flex-1 glass-input"
                        value={keyword}
                        onChange={e => setKeyword(e.target.value)}
                    />
                    <button type="submit" className="glass-button px-4 py-2 flex items-center justify-center" disabled={isSearching}>
                        {isSearching ? '...' : 'Tìm'}
                    </button>
                </form>

                <div className="max-h-80 overflow-y-auto custom-scrollbar space-y-2">
                    {results.length === 0 && !isSearching && (
                        <div className="text-center text-slate-400 py-4">Không có kết quả nào.</div>
                    )}
                    {results.map(user => (
                        <div key={user.username} className="flex items-center justify-between bg-slate-700/50 p-3 rounded-xl border border-white/5">
                            <div className="flex items-center gap-3">
                                <div className="w-10 h-10 rounded-full bg-indigo-500 flex items-center justify-center text-white font-bold">
                                    {user.username.charAt(0).toUpperCase()}
                                </div>
                                <div>
                                    <div className="font-medium text-white">{user.username}</div>
                                    <div className="text-sm text-slate-400">{user.email || 'No email'}</div>
                                </div>
                            </div>
                            <button 
                                onClick={() => handleAddFriend(user.username)}
                                className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-lg transition-colors"
                            >
                                Thêm bạn
                            </button>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
};

export default SearchModal;
