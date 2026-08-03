import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { User, LogOut, Settings, Users, MessageSquare, UserPlus, Search, Check, X, MoreVertical, Filter } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import apiClient from '../../services/apiClient';
import webSocketClient from '../../services/webSocketClient';

const Sidebar = ({ activeTab, setActiveTab, handleLogout, contacts, activeChat, setActiveChat, onLoadMoreContacts, hasMoreContacts, onUnfriend, onBlock, unreadCounts = {}, onOpenSystemModal }) => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
  
  // Advanced Filter state
  const [showAdvancedFilter, setShowAdvancedFilter] = useState(false);
  const [filterData, setFilterData] = useState({ userFilter: '', addressFilter: '' });

  // Friend Requests state
  const [requestType, setRequestType] = useState('received'); // 'received' or 'sent'
  const [friendRequests, setFriendRequests] = useState([]);
  const [loadingRequests, setLoadingRequests] = useState(false);
  
  const [dropdownOpen, setDropdownOpen] = useState(null);

  // Load friend requests when tab or type changes
  useEffect(() => {
    if (activeTab === 'requests') {
      loadFriendRequests();
    }
  }, [activeTab, requestType]);

  const loadFriendRequests = async () => {
    setLoadingRequests(true);
    try {
      const endpoint = requestType === 'sent' ? '/api/friends/sent' : '/api/friends/requests';
      const res = await apiClient.get(endpoint, { params: { size: 50 } });
      const data = Array.isArray(res.data.data?.content) ? res.data.data.content : (Array.isArray(res.data.data) ? res.data.data : []);
      setFriendRequests(data);
    } catch (err) {
      console.error("Failed to load requests", err);
    } finally {
      setLoadingRequests(false);
    }
  };

  // Handle Global Search (Debounced)
  useEffect(() => {
    const delayDebounceFn = setTimeout(async () => {
      // Regular search if query is not empty and advanced filter is closed
      if (searchQuery.trim().length > 0 && !showAdvancedFilter) {
        setIsSearching(true);
        try {
          const res = await apiClient.get('/api/search/users', { params: { keyword: searchQuery, size: 20 } });
          const data = Array.isArray(res.data.data?.content) ? res.data.data.content : (Array.isArray(res.data.data) ? res.data.data : []);
          setSearchResults(data);
        } catch (err) {
          console.error("Search failed", err);
        } finally {
          setIsSearching(false);
        }
      } else if (!showAdvancedFilter) {
        setSearchResults([]);
      }
    }, 500);

    return () => clearTimeout(delayDebounceFn);
  }, [searchQuery, showAdvancedFilter]);

  const handleAdvancedSearch = async (e) => {
    e?.preventDefault();
    setIsSearching(true);
    try {
      const params = new URLSearchParams();
      if (filterData.userFilter) params.append('user', filterData.userFilter);
      if (filterData.addressFilter) params.append('address', filterData.addressFilter);
      
      const res = await apiClient.get(`/api/search/users/filter?${params.toString()}`);
      const data = Array.isArray(res.data.data?.content) ? res.data.data.content : (Array.isArray(res.data.data) ? res.data.data : []);
      setSearchResults(data);
      // set searchQuery to a non-empty value to trigger the search UI rendering
      setSearchQuery('Advanced Search'); 
    } catch (err) {
      console.error("Advanced search failed", err);
    } finally {
      setIsSearching(false);
    }
  };

  const handleSendFriendRequest = (username) => {
    webSocketClient.sendMessage('/app/friend/request', username);
    setSearchResults(prev => prev.filter(u => u.username !== username));
  };

  const handleAcceptRequest = (username) => {
    webSocketClient.sendMessage('/app/friend/accept', username);
    setFriendRequests(prev => prev.filter(r => r.username !== username));
  };

  const handleDeclineRequest = (username) => {
    webSocketClient.sendMessage('/app/friend/decline', username);
    setFriendRequests(prev => prev.filter(r => r.username !== username));
  };

  const renderContent = () => {
    if (searchQuery.trim().length > 0) {
      return (
        <div className="flex-1 overflow-y-auto custom-scrollbar p-4 space-y-2">
          {isSearching ? (
             <div className="text-center text-gray-500 text-sm mt-4">Searching...</div>
          ) : searchResults.length > 0 ? (
             searchResults.map((user, idx) => (
               <motion.div 
                 key={user.username || idx}
                 initial={{ opacity: 0, y: 10 }}
                 animate={{ opacity: 1, y: 0 }}
                 className="flex items-center justify-between p-3 rounded-xl bg-white/5 hover:bg-white/10 transition-all"
               >
                 <div className="flex items-center gap-3">
                   <div className="w-10 h-10 rounded-full bg-primary/20 flex items-center justify-center overflow-hidden">
                     {user.avatar ? <img src={user.avatar} className="w-full h-full object-cover" /> : <User size={18} className="text-gray-400" />}
                   </div>
                   <div>
                     <h3 className="font-medium text-white text-sm">{user.firstName ? `${user.firstName} ${user.lastName}` : user.username}</h3>
                     <p className="text-xs text-gray-400">@{user.username}</p>
                   </div>
                 </div>
                 <button 
                   onClick={() => handleSendFriendRequest(user.username)}
                   className="p-2 bg-primary/20 text-primary hover:bg-primary/40 rounded-lg transition-colors"
                   title="Add Friend"
                 >
                   <UserPlus size={16} />
                 </button>
               </motion.div>
             ))
          ) : (
            <div className="text-center text-gray-500 text-sm mt-4">No users found.</div>
          )}
        </div>
      );
    }

    if (activeTab === 'requests') {
       return (
         <div className="flex-1 flex flex-col min-h-0">
           <div className="flex bg-white/5 p-1 rounded-lg mx-4 mt-4 shrink-0">
             <button 
               onClick={() => setRequestType('received')} 
               className={`flex-1 py-1.5 text-sm rounded-md transition-colors ${requestType === 'received' ? 'bg-primary text-white shadow' : 'text-gray-400 hover:text-white'}`}
             >
               Received
             </button>
             <button 
               onClick={() => setRequestType('sent')} 
               className={`flex-1 py-1.5 text-sm rounded-md transition-colors ${requestType === 'sent' ? 'bg-primary text-white shadow' : 'text-gray-400 hover:text-white'}`}
             >
               Sent
             </button>
           </div>
           
           <div className="flex-1 overflow-y-auto custom-scrollbar p-4 space-y-2 mt-2">
             {loadingRequests ? (
               <div className="text-center text-gray-500 text-sm mt-4">Loading requests...</div>
             ) : friendRequests.length > 0 ? (
               friendRequests.map((req, idx) => (
                 <motion.div 
                   key={req.username || idx}
                   initial={{ opacity: 0, x: -20 }}
                   animate={{ opacity: 1, x: 0 }}
                   className="p-3 rounded-xl bg-white/5 flex flex-col gap-3"
                 >
                   <div className="flex items-center gap-3">
                     <div className="w-10 h-10 rounded-full bg-primary/20 flex items-center justify-center overflow-hidden">
                       {req.avatar ? <img src={req.avatar} className="w-full h-full object-cover" /> : <User size={18} className="text-gray-400" />}
                     </div>
                     <div>
                       <h3 className="font-medium text-white text-sm">{req.firstName ? `${req.firstName} ${req.lastName}` : req.username}</h3>
                       <p className="text-xs text-gray-400">{requestType === 'received' ? 'Wants to be friends' : 'Request sent'}</p>
                     </div>
                   </div>
                   {requestType === 'received' ? (
                     <div className="flex gap-2">
                       <button 
                         onClick={() => handleAcceptRequest(req.username)}
                         className="flex-1 py-1.5 bg-green-500/20 text-green-400 hover:bg-green-500/30 rounded-lg text-sm transition-colors flex items-center justify-center gap-1"
                       >
                         <Check size={16} /> Accept
                       </button>
                       <button 
                         onClick={() => handleDeclineRequest(req.username)}
                         className="flex-1 py-1.5 bg-red-500/20 text-red-400 hover:bg-red-500/30 rounded-lg text-sm transition-colors flex items-center justify-center gap-1"
                       >
                         <X size={16} /> Decline
                       </button>
                     </div>
                   ) : (
                     <div className="flex gap-2">
                       <div className="flex-1 py-1.5 bg-white/5 text-gray-400 rounded-lg text-sm text-center">
                         Pending
                       </div>
                     </div>
                   )}
                 </motion.div>
               ))
             ) : (
               <div className="text-center text-gray-500 text-sm mt-4">
                 {requestType === 'received' ? 'No pending requests.' : 'No sent requests.'}
               </div>
             )}
           </div>
         </div>
       );
    }

    return (
      <div 
          className="flex-1 overflow-y-auto custom-scrollbar p-4 space-y-2"
          onScroll={(e) => {
            const { scrollTop, scrollHeight, clientHeight } = e.target;
            if (scrollHeight - scrollTop <= clientHeight + 10 && hasMoreContacts) {
              onLoadMoreContacts();
            }
          }}
        >
          {contacts.map((contact, idx) => {
            const isSelected = activeChat === contact.username;
            return (
              <motion.div 
                key={contact.username || idx}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: idx * 0.05 }}
                onClick={() => setActiveChat(contact.username)}
                className={`flex items-center gap-4 p-3 rounded-xl cursor-pointer transition-all group ${isSelected ? 'bg-white/10' : 'hover:bg-white/5'}`}
              >
                <div className="relative">
                  <div className="w-12 h-12 rounded-full bg-gradient-to-tr from-primary to-secondary p-[2px]">
                    <div className="w-full h-full bg-bg-dark rounded-full flex items-center justify-center overflow-hidden">
                      {contact.avatar ? (
                        <img src={contact.avatar} alt="Avatar" className="w-full h-full object-cover" />
                      ) : (
                        <User size={20} className="text-gray-400" />
                      )}
                    </div>
                  </div>
                  {contact.isOnline && (
                    <div className="absolute bottom-0 right-0 w-3.5 h-3.5 bg-green-500 border-2 border-bg-dark rounded-full"></div>
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className={`font-medium truncate transition-colors ${isSelected ? 'text-primary-light' : 'text-white group-hover:text-primary-light'}`}>
                    {contact.firstName && contact.lastName ? `${contact.firstName} ${contact.lastName}` : contact.username}
                  </h3>
                  <p className="text-sm text-gray-400 truncate">{contact.lastMessage || '...'}</p>
                </div>
                {unreadCounts[contact.username] > 0 && (
                  <div className="w-5 h-5 rounded-full bg-red-500 text-white text-[10px] font-bold flex items-center justify-center mr-2 shadow-lg">
                    {unreadCounts[contact.username]}
                  </div>
                )}
                {activeTab === 'friends' && (
                  <div className="relative">
                    <button 
                      onClick={(e) => { e.stopPropagation(); setDropdownOpen(dropdownOpen === contact.username ? null : contact.username); }}
                      className="p-2 text-gray-400 hover:text-white rounded-full hover:bg-white/10 transition-colors"
                    >
                      <MoreVertical size={16} />
                    </button>
                    {dropdownOpen === contact.username && (
                      <div className="absolute right-0 mt-2 w-32 bg-bg-dark border border-white/10 rounded-lg shadow-xl z-50 overflow-hidden">
                        <button 
                          onClick={(e) => { e.stopPropagation(); onUnfriend(contact.username); setDropdownOpen(null); }}
                          className="w-full text-left px-4 py-2 text-sm text-gray-300 hover:bg-white/10 transition-colors"
                        >
                          Unfriend
                        </button>
                        <button 
                          onClick={(e) => { e.stopPropagation(); onBlock(contact.username); setDropdownOpen(null); }}
                          className="w-full text-left px-4 py-2 text-sm text-red-400 hover:bg-red-500/10 transition-colors"
                        >
                          Block
                        </button>
                      </div>
                    )}
                  </div>
                )}
              </motion.div>
            );
          })}
          
          {hasMoreContacts && (
            <div className="py-2 text-center text-xs text-gray-500">
              Loading more...
            </div>
          )}
          
          {!hasMoreContacts && contacts.length === 0 && (
            <div className="p-4 text-center text-sm text-gray-500">
              No contacts found.
            </div>
          )}
        </div>
    );
  };

  return (
    <>
      {/* Slim Navigation */}
      <nav className="w-20 glass-dark border-r border-white/5 flex flex-col items-center py-6 gap-8 z-20 shrink-0">
        <div className="w-12 h-12 bg-primary rounded-xl flex items-center justify-center font-display font-bold text-xl shadow-lg shadow-primary/20 text-white">
          CW
        </div>
        
        <div className="flex-1 flex flex-col items-center gap-6 w-full">
          <button 
            onClick={() => { setActiveTab('chats'); setSearchQuery(''); setShowAdvancedFilter(false); }}
            className={`w-12 h-12 rounded-xl flex items-center justify-center transition-all ${activeTab === 'chats' ? 'bg-white/10 text-primary' : 'text-gray-400 hover:text-white hover:bg-white/5'}`}
            title="Messages"
          >
            <MessageSquare size={24} />
          </button>
          
          <button 
            onClick={() => { setActiveTab('friends'); setSearchQuery(''); setShowAdvancedFilter(false); }}
            className={`w-12 h-12 rounded-xl flex items-center justify-center transition-all ${activeTab === 'friends' ? 'bg-white/10 text-primary' : 'text-gray-400 hover:text-white hover:bg-white/5'}`}
            title="Friends"
          >
            <Users size={24} />
          </button>

          <button 
            onClick={() => { setActiveTab('requests'); setSearchQuery(''); setShowAdvancedFilter(false); }}
            className={`w-12 h-12 rounded-xl flex items-center justify-center transition-all ${activeTab === 'requests' ? 'bg-white/10 text-primary' : 'text-gray-400 hover:text-white hover:bg-white/5'}`}
            title="Friend Requests"
          >
            <UserPlus size={24} />
          </button>
        </div>
        
        <div className="flex flex-col items-center gap-4">
          <button 
            onClick={() => navigate('/profile')}
            className="w-12 h-12 rounded-xl flex items-center justify-center text-gray-400 hover:text-white hover:bg-white/5 transition-all"
            title="Settings"
          >
            <Settings size={24} />
          </button>
          <button 
            onClick={handleLogout}
            className="w-12 h-12 rounded-xl flex items-center justify-center text-red-400 hover:text-red-300 hover:bg-red-500/10 transition-all"
          >
            <LogOut size={24} />
          </button>
        </div>
      </nav>

      {/* List Panel */}
      <aside className="w-80 glass border-r border-white/5 flex flex-col z-10 relative shrink-0">
        <div className="p-6 border-b border-white/5 text-white">
          <h2 className="text-2xl font-display font-bold">
            {activeTab === 'chats' ? 'Messages' : activeTab === 'requests' ? 'Friend Requests' : 'Friends'}
          </h2>
          <div className="mt-4 flex gap-2">
            <div className="relative flex-1">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-500">
                <Search size={16} />
              </div>
              <input 
                type="text" 
                placeholder="Search users..."
                value={searchQuery}
                onChange={(e) => { setSearchQuery(e.target.value); setShowAdvancedFilter(false); }}
                className="w-full bg-black/20 border border-white/5 rounded-xl pl-9 pr-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all text-white placeholder-gray-500"
              />
              {searchQuery && !showAdvancedFilter && (
                <button onClick={() => setSearchQuery('')} className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-white">
                  <X size={16} />
                </button>
              )}
            </div>
            {(activeTab === 'friends' || activeTab === 'chats') && (
              <button 
                onClick={() => setShowAdvancedFilter(!showAdvancedFilter)}
                className={`p-2.5 rounded-xl border transition-all flex items-center justify-center ${showAdvancedFilter ? 'bg-primary/20 text-primary border-primary/50' : 'bg-white/5 text-gray-400 border-white/5 hover:text-white hover:bg-white/10'}`}
                title="Advanced Filter"
              >
                <Filter size={18} />
              </button>
            )}
          </div>
          
          <AnimatePresence>
            {showAdvancedFilter && (
              <motion.form 
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                exit={{ opacity: 0, height: 0 }}
                onSubmit={handleAdvancedSearch}
                className="mt-3 space-y-3 overflow-hidden"
              >
                <div className="bg-black/20 p-3 rounded-xl border border-white/5 space-y-3">
                  <div>
                    <label className="text-xs text-gray-400 mb-1 block">User Filter (e.g. firstName:John)</label>
                    <input 
                      type="text" 
                      value={filterData.userFilter}
                      onChange={(e) => setFilterData({...filterData, userFilter: e.target.value})}
                      placeholder="field:value"
                      className="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-1.5 text-sm text-white focus:outline-none focus:ring-1 focus:ring-primary/50"
                    />
                  </div>
                  <div>
                    <label className="text-xs text-gray-400 mb-1 block">Address Filter (e.g. city:Hanoi)</label>
                    <input 
                      type="text" 
                      value={filterData.addressFilter}
                      onChange={(e) => setFilterData({...filterData, addressFilter: e.target.value})}
                      placeholder="field:value"
                      className="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-1.5 text-sm text-white focus:outline-none focus:ring-1 focus:ring-primary/50"
                    />
                  </div>
                  <button 
                    type="submit"
                    className="w-full py-1.5 bg-primary/20 text-primary hover:bg-primary hover:text-white rounded-lg text-sm font-medium transition-colors mt-2"
                  >
                    Apply Filter
                  </button>
                </div>
              </motion.form>
            )}
          </AnimatePresence>
        </div>
        
        {renderContent()}

      </aside>
    </>
  );
};

export default Sidebar;
