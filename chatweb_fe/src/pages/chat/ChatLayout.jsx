import React, { useEffect, useState, useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useTranslation } from 'react-i18next';
import { User, MessageSquare } from 'lucide-react';

import webSocketClient from '../../services/webSocketClient';
import apiClient from '../../services/apiClient';
import { logout } from '../../store/slices/authSlice';
import { toast } from '../../utils/toast';

import Sidebar from '../../components/chat/Sidebar';
import MessageList from '../../components/chat/MessageList';
import MessageInput from '../../components/chat/MessageInput';
import SystemMessageModal from '../../components/chat/SystemMessageModal';

const ChatLayout = () => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const { user: currentUser } = useSelector(state => state.auth);
  
  const [activeTab, setActiveTab] = useState('chats'); // 'chats' or 'friends'
  const [activeChat, setActiveChat] = useState(null);
  const [contacts, setContacts] = useState([]);
  const [contactsPage, setContactsPage] = useState(0);
  const [hasMoreContacts, setHasMoreContacts] = useState(true);
  const [loadingContacts, setLoadingContacts] = useState(false);
  
  const [messages, setMessages] = useState([]);
  const [hasMoreMessages, setHasMoreMessages] = useState(false);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [currentSystemMessage, setCurrentSystemMessage] = useState(null);
  const [showSystemModal, setShowSystemModal] = useState(false);

  const [cursor, setCursor] = useState(null);
  const [unreadCounts, setUnreadCounts] = useState({}); 


  // Fetch contacts list

  const loadUnreadCounts = async () => {
    try {
      const res = await apiClient.get('/api/messages/unread-counts');
      // res.data.data.unreadCounts is a Map/Object
      setUnreadCounts(res.data.data?.unreadCounts || {});
    } catch (err) {
      console.error("Failed to load unread counts", err);
    }
  };

  useEffect(() => {
    loadUnreadCounts();
  }, []);

  const loadContacts = async (isLoadMore = false) => {
    if (loadingContacts || (!hasMoreContacts && isLoadMore)) return;
    
    setLoadingContacts(true);
    try {
      const pageToFetch = isLoadMore ? contactsPage + 1 : 0;
      const res = await apiClient.get('/api/friends', { params: { size: 20, page: pageToFetch } });
      const data = Array.isArray(res.data.data?.content) ? res.data.data.content : (Array.isArray(res.data.data) ? res.data.data : []);
      
      if (isLoadMore) {
        setContacts(prev => [...prev, ...data]);
      } else {
        setContacts(data);
      }
      
      setContactsPage(pageToFetch);
      // Assuming res.data.data.last is a boolean for last page, or check length
      const isLast = res.data.data?.last ?? data.length < 20;
      setHasMoreContacts(!isLast);
    } catch (err) {
      console.error("Failed to load contacts", err);
    } finally {
      setLoadingContacts(false);
    }
  };

  useEffect(() => {
    if (activeTab === 'requests') return; // Handled by Sidebar internally
    
    setContacts([]);
    setContactsPage(0);
    setHasMoreContacts(true);
    loadContacts(false);
  }, [activeTab]);

  // Mark messages as read when opening a chat with unread messages
  useEffect(() => {
    const markAsRead = async () => {
      if (activeChat && unreadCounts[activeChat] > 0) {
        try {
          await apiClient.post('/api/messages/mark-as-read', { sender: activeChat });
          setUnreadCounts(prev => ({ ...prev, [activeChat]: 0 }));
        } catch (err) {
          console.error("Failed to mark messages as read", err);
        }
      }
    };
    markAsRead();
  }, [activeChat, unreadCounts]);

  // Handle WebSocket connection
  useEffect(() => {
    webSocketClient.connect(() => {
      console.log("WebSocket connected from ChatLayout");
    });
      
      webSocketClient.on('onSystemMessage', (msg) => {
      setCurrentSystemMessage(msg);
    });

    webSocketClient.on('onMessageReceived', (socketResponse) => {
        const msg = socketResponse.data;
        
        // Handle STATUS message (Read receipts)
        if (msg.messageType === 'STATUS' && msg.status === 'READ') {
          if (msg.sender === activeChat || msg.recipient === activeChat) {
            setMessages(prev => prev.map(m => 
              (!m.isOwn) ? { ...m, status: 'read' } : m
            ));
          }
          return;
        }

        if (msg.sender === activeChat || msg.recipient === activeChat) {
          setMessages(prev => {
            // Check if we have an optimistic message with matching localId or id
            const existingIndex = prev.findIndex(m => (m.id && m.id === msg.id) || (m.localId && m.localId === msg.localId));
            
            if (msg.messageType === 'REACTION') {
              if (existingIndex > -1) {
                const updated = [...prev];
                const target = updated[existingIndex];
                const newReactions = { ...(target.reactions || {}) };
                if (msg.content) {
                  newReactions[msg.sender] = msg.content;
                } else {
                  delete newReactions[msg.sender];
                }
                updated[existingIndex] = { ...target, reactions: newReactions };
                return updated;
              }
              return prev;
            }
            
            const newMsg = {
              ...msg, 
              content: msg.content, 
              time: msg.timestamp ? new Date(msg.timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) : '...', 
              isOwn: msg.sender === currentUser?.username,
              status: 'sent'
            };

            if (existingIndex > -1) {
              const updated = [...prev];
              updated[existingIndex] = newMsg;
              return updated;
            } else {
              return [...prev, newMsg];
            }
          });
        } else {
          // If message is from someone else, increase unread count if it's a new message
          if (msg.messageType === 'CHAT' && msg.sender !== currentUser?.username) {
            setUnreadCounts(prev => ({
              ...prev,
              [msg.sender]: (prev[msg.sender] || 0) + 1
            }));
          }
        }
      });

      
      webSocketClient.on('onNotification', (socketResponse) => {
        const notif = socketResponse.data;
        if (notif && notif.status) {
          // E.g., notif.status === 'FRIEND_REQUEST'
          toast.success(socketResponse.message || 'New notification');
        }
      });

      webSocketClient.on('onMessageError', (socketResponse) => {
        const errorRequest = socketResponse.data;
        // Find the message with the matching localId and mark it as error
        if (errorRequest && errorRequest.localId) {
          setMessages(prev => {
            const existingIndex = prev.findIndex(m => m.localId === errorRequest.localId);
            if (existingIndex > -1) {
              const updated = [...prev];
              updated[existingIndex] = { ...updated[existingIndex], status: 'error' };
              return updated;
            }
            return prev;
          });
        }
      });
    
    return () => {
      webSocketClient.disconnect();
    };
  }, [activeChat, currentUser]);

  // Fetch messages when activeChat changes
  const loadMessages = useCallback(async (isLoadMore = false) => {
    if (!activeChat || !currentUser?.username) return;
    if (loadingMessages) return;

    setLoadingMessages(true);
    try {
      const params = {
        user1: currentUser.username,
        user2: activeChat,
        size: 20
      };
      if (isLoadMore && cursor) {
        params.cursor = cursor;
      }

      const res = await apiClient.get('/api/messages/private', { params });
      // The API returns an ApiResponseListMessageResponse. The messages are likely in res.data.data.content or res.data.data
      const newMessages = Array.isArray(res.data.data?.content) ? res.data.data.content : (Array.isArray(res.data.data) ? res.data.data : []);
      
      // Format messages if needed
      const formatted = newMessages.map(m => ({
        ...m,
        isOwn: m.sender === currentUser.username,
        time: m.timestamp ? new Date(m.timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) : '...',
      })).reverse(); // Assuming API returns newest first, reverse them for display

      if (isLoadMore) {
        setMessages(prev => [...formatted, ...prev]);
      } else {
        setMessages(formatted);
      }
      
      // Set cursor for next load (assuming oldest message ID or date)
      if (newMessages.length === params.size) {
        setHasMoreMessages(true);
        setCursor(newMessages[newMessages.length - 1].id); // or createdAt depending on backend implementation
      } else {
        setHasMoreMessages(false);
      }
    } catch (err) {
      console.error("Failed to load messages", err);
    } finally {
      setLoadingMessages(false);
    }
  }, [activeChat, currentUser, cursor, loadingMessages]);

  useEffect(() => {
    setMessages([]);
    setCursor(null);
    setHasMoreMessages(false);
    loadMessages(false);
  }, [activeChat]); // Removed loadMessages from dependency array to prevent infinite loops

  const handleLogout = async () => {
    try {
      await apiClient.post('/api/auth/logout');
    } catch (e) {
      console.error(e);
    }
    dispatch(logout());
  };
  const handleUnfriend = async (username) => {
    try {
      await apiClient.delete(`/api/friends/${username}`);
      setContacts(prev => prev.filter(c => c.username !== username));
      if (activeChat === username) setActiveChat(null);
      toast.success("Unfriended successfully");
    } catch (err) {
      console.error(err);
      toast.error("Failed to unfriend");
    }
  };

  const handleBlock = async (username) => {
    try {
      await apiClient.post(`/api/friends/block/${username}`);
      setContacts(prev => prev.filter(c => c.username !== username));
      if (activeChat === username) setActiveChat(null);
      toast.success("User blocked");
    } catch (err) {
      console.error(err);
      toast.error("Failed to block user");
    }
  };

  
  const handleReaction = (messageId, reactionType) => {
    webSocketClient.sendMessage('/app/chat/reaction', { 
      messageId, 
      recipient: activeChat, 
      reactionType 
    });
  };

  const handleEditMessage = (messageId, newContent) => {
    webSocketClient.sendMessage('/app/chat/editMessage', { 
      messageId, 
      newContent 
    });
  };

  const handleRevokeMessage = (messageId) => {
    webSocketClient.sendMessage('/app/chat/revokeMessage', { 
      messageId 
    });
  };

  const handleSendMessage = async (text, file = null) => {
    if (activeChat) {
      let fileUrl = null;
      let contentType = 'TEXT';
      
      if (file) {
        try {
          const formData = new FormData();
          const isVideo = file.type.startsWith('video/');
          const fieldName = isVideo ? 'video' : 'image';
          formData.append(fieldName, file);
          
          const endpoint = isVideo ? '/api/chat/video' : '/api/chat/image';
          
          const uploadRes = await apiClient.post(endpoint, formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
          });
          
          fileUrl = uploadRes.data.data;
          contentType = isVideo ? 'VIDEO' : 'IMAGE';
        } catch (error) {
          console.error("Failed to upload file", error);
          toast.error("Failed to upload file");
          return;
        }
      }

      const localId = Date.now().toString(); // Generate unique localId
      const payload = {
        recipient: activeChat,
        content: text || '',
        messageType: 'CHAT',
        contentType: contentType,
        fileUrl: fileUrl,
        localId: localId
      };
      
      // Optimistic Update
      const optimisticMsg = {
        id: localId,
        content: text || '',
        sender: currentUser?.username,
        recipient: activeChat,
        contentType: contentType,
        fileUrl: fileUrl,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        isOwn: true,
        status: 'SENDING'
      };
      
      setMessages(prev => [...prev, optimisticMsg]);
      
      webSocketClient.sendMessage('/app/chat/sendPrivateMessage', payload);
    }
  };

  const contactData = contacts.map(c => ({
    username: c.username || c.friendUsername, // depends on API
    firstName: c.firstName,
    lastName: c.lastName,
    avatar: c.avatar,
    isOnline: false // Can hook up presence later
  }));

  const activeContact = contactData.find(c => c.username === activeChat);

  // Add admin check based on typical permission or role structures
  const isAdmin = currentUser?.permissions?.includes("ADMIN_SEND-MESSAGE") || currentUser?.role === "ADMIN" || currentUser?.role === "ROLE_ADMIN";


  return (
    <div className="h-screen w-full flex flex-col overflow-hidden bg-bg-dark text-white font-sans">
      {/* System Message Marquee */}
      {currentSystemMessage && (
        <div className="w-full bg-primary/20 border-b border-primary/30 text-primary-light py-1 px-4 text-sm overflow-hidden flex items-center shrink-0">
           <div 
             className="whitespace-nowrap animate-marquee cursor-pointer"
             onAnimationEnd={() => setCurrentSystemMessage(null)}
             onClick={() => {
               setCurrentSystemMessage(null);
               setShowSystemModal(true);
             }}
           >
             <span className="font-bold mr-2">Hệ thống ({currentSystemMessage.sender}):</span> 
             {currentSystemMessage.content}
           </div>
        </div>
      )}

      <SystemMessageModal 
        isOpen={showSystemModal} 
        onClose={() => setShowSystemModal(false)} 
        isAdmin={isAdmin}
      />

      <div className="flex-1 w-full flex overflow-hidden">
        {/* Sidebar Component handles Left Nav and List Panel */}
        <Sidebar 
          activeTab={activeTab}
          setActiveTab={setActiveTab}
          handleLogout={handleLogout}
          contacts={contacts}
          activeChat={activeChat}
          setActiveChat={setActiveChat}
          onLoadMoreContacts={() => loadContacts(true)}
          hasMoreContacts={hasMoreContacts}
          onUnfriend={handleUnfriend}
          onBlock={handleBlock}
          unreadCounts={unreadCounts}
          onOpenSystemModal={() => setShowSystemModal(true)}
        />

      {/* Main Content Area */}
      <div className="flex-1 flex relative">
        <div className="absolute inset-0 bg-[url('https://www.transparenttextures.com/patterns/cubes.png')] opacity-5 pointer-events-none"></div>

        {/* Active Chat Panel */}
        <main className="flex-1 flex flex-col relative z-10 glass">
          
          {/* Chat Header */}
          <header className="h-20 border-b border-white/5 px-6 flex items-center justify-between glass-dark backdrop-blur-md sticky top-0">
            {activeChat ? (
              <div className="flex items-center gap-4">
                <div className="w-10 h-10 rounded-full bg-primary/20 flex items-center justify-center text-primary overflow-hidden">
                  {activeContact?.avatar ? (
                    <img src={activeContact.avatar} alt="Avatar" className="w-full h-full object-cover" />
                  ) : (
                    <User size={20} />
                  )}
                </div>
                <div>
                  <h3 className="font-medium text-lg font-display">
                    {activeContact?.firstName && activeContact?.lastName 
                      ? `${activeContact.firstName} ${activeContact.lastName}` 
                      : activeChat}
                  </h3>
                  {activeContact?.isOnline && <p className="text-xs text-green-400">{t('chat.online')}</p>}
                </div>
              </div>
            ) : (
              <div className="text-gray-400">Select a conversation to start chatting</div>
            )}
          </header>

          {/* Messages Area Component */}
          {activeChat ? (
            <>
              <MessageList 
                messages={messages} 
                onLoadMore={() => loadMessages(true)} 
                hasMore={hasMoreMessages} 
                onReaction={handleReaction}
                onEdit={handleEditMessage}
                onRevoke={handleRevokeMessage}
              />
              <MessageInput onSend={handleSendMessage} />
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center text-gray-500">
              <div className="text-center">
                <div className="w-20 h-20 bg-white/5 rounded-full flex items-center justify-center mx-auto mb-4">
                  <MessageSquare size={32} className="text-gray-400" />
                </div>
                <p>Pick a chat from the sidebar</p>
              </div>
            </div>
          )}
          
        </main>
      </div>
      </div>
    </div>
  );
};

export default ChatLayout;