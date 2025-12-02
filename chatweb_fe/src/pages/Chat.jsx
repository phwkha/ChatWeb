// src/pages/Chat.jsx
import { useNavigate } from "react-router-dom";
import { authService } from "../services/authService";
import { useCallback, useEffect, useState, useRef, memo, useLayoutEffect } from "react";
import { Stomp } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import PrivateChat from "./PrivateChat";
import { useCrypto } from "../context/CryptoContext";
import { KeyUnlockModal } from "../components/KeyUnlockModal";
import "../styles/Chat.css";

const Chat = () => {
    const navigate = useNavigate();
    const { isUnlockModalVisible, requestUnlock, isUnlocked } = useCrypto();
    const currentUser = authService.getCurrentUser();

    // --- States ---
    const [messages, setMessages] = useState([]);
    const [message, setMessage] = useState("");
    const [showEmojiPicker, setShowEmojiPicker] = useState(false);
    const [isTyping, setIsTyping] = useState('');
    
    // Chat Lists & Notifications
    const [privateChats, setPrivateChats] = useState(new Map());
    const [unreadMessages, setUnreadMessages] = useState(new Map());
    const [onlineUsers, setOnlineUsers] = useState(new Set());

    const [notifications, setNotifications] = useState([]);

    // Pagination States (Group Chat)
    const [nextCursor, setNextCursor] = useState(null);
    const [hasMore, setHasMore] = useState(true);
    const [isLoadingHistory, setIsLoadingHistory] = useState(false);

    // --- Refs ---
    const stompClient = useRef(null);
    const messageEndRef = useRef(null);
    const chatContainerRef = useRef(null); 
    const typingTimeoutRef = useRef(null);
    const privateMessageHandlers = useRef(new Map());
    const messageIdsRef = useRef(new Set()); 
    
    // --- Refs cho Scroll Logic (Giống PrivateChat) ---
    const isFirstLoadRef = useRef(true); 
    const prevScrollHeightRef = useRef(0);
    const prevScrollTopRef = useRef(0);
    const isPrependingRef = useRef(false);

    const emojis = [
        "😀", "😂", "😍", "😎", "😭", "😡", "👍", "🙏", "🎉", "💔",
        "🔥", "🌟", "💯", "🎶", "🍕", "⚽", "🏆", "🚀", "🌈", "☀️"
    ];

    // Redirect if not logged in
    useEffect(() => {
        if (!currentUser) {
            navigate("/login");
        }
    }, [currentUser, navigate]);

    if (!currentUser) return null;

    const { username, color: userColor } = currentUser;

    // --- Helper Functions ---

    const formatTime = (timestamp) => {
        return new Date(timestamp).toLocaleTimeString('vi-VN', {
            timeZone: 'Asia/Ho_Chi_Minh',
            hour12: false,
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    const scrollToBottom = () => {
        requestAnimationFrame(() => {
            messageEndRef.current?.scrollIntoView({ behavior: "smooth" });
        });
    };

    const showNotification = (sender, content = "Bạn có tin nhắn mới") => {
        const id = Date.now();
        const newNotification = { id, sender, content };
        
        setNotifications(prev => [...prev, newNotification]);

        // Tự động ẩn sau 5 giây
        setTimeout(() => {
            removeNotification(id);
        }, 5000);
    };


    const removeNotification = (id) => {
        setNotifications(prev => prev.filter(n => n.id !== id));
    };

    const handleNotificationClick = (sender) => {
        openPrivateChat(sender);
        // Xóa tất cả thông báo của người này
        setNotifications(prev => prev.filter(n => n.sender !== sender));
    };

    // --- Scroll Logic: useLayoutEffect ---
    
    // 1. Xử lý cuộn khi LOAD MORE (Prepend tin nhắn cũ)
    useLayoutEffect(() => {
        if (isPrependingRef.current && chatContainerRef.current) {
            const container = chatContainerRef.current;
            
            // Tính toán vị trí mới để giữ nguyên tầm nhìn
            const newScrollHeight = container.scrollHeight;
            const heightDifference = newScrollHeight - prevScrollHeightRef.current;
            
            container.scrollTop = prevScrollTopRef.current + heightDifference;

            // Reset cờ và tắt loading
            isPrependingRef.current = false;
            setIsLoadingHistory(false);
        }
    }, [messages]);

    // 2. Xử lý cuộn khi LOAD FIRST TIME (Cuộn xuống đáy)
    useLayoutEffect(() => {
        if (isFirstLoadRef.current && messages.length > 0) {
            messageEndRef.current?.scrollIntoView({ behavior: "auto" });
            isFirstLoadRef.current = false;
        }
    }, [messages]);

    // --- Fetch Logic ---

    // Hàm helper để gọi API và xử lý dữ liệu
    const fetchGroupMessagesFromApi = async (cursor, size = 20) => {
        try {
            const data = await authService.fetchGroupChat(cursor, size);
            if (data && Array.isArray(data.content)) {
                // Backend trả về DESC (Mới -> Cũ), đảo ngược thành ASC (Cũ -> Mới)
                const newMessages = data.content.map(m => ({
                    ...m,
                    timestamp: m.timestamp || new Date().toISOString(),
                    id: m.id || `msg-${Date.now()}-${Math.random()}`
                })).reverse();

                // Lọc trùng
                const uniqueMessages = newMessages.filter(m => !messageIdsRef.current.has(m.id));
                uniqueMessages.forEach(m => messageIdsRef.current.add(m.id));

                return {
                    messages: uniqueMessages,
                    nextCursor: data.nextCursor,
                    hasMore: data.hasMore
                };
            }
            return null;
        } catch (error) {
            console.error("Error fetching group messages:", error);
            return null;
        }
    };

    // Hàm xử lý sự kiện cuộn (Load More)
    const handleScroll = async () => {
        const container = chatContainerRef.current;
        if (!container) return;

        // Trigger khi cuộn lên đỉnh
        if (container.scrollTop < 20 && hasMore && !isLoadingHistory) {
            
            // 1. Lưu vị trí hiện tại (Snapshot)
            prevScrollHeightRef.current = container.scrollHeight;
            prevScrollTopRef.current = container.scrollTop;
            
            setIsLoadingHistory(true);

            // 2. Gọi API
            // (Optional: await new Promise(resolve => setTimeout(resolve, 1000)); để test loading)
            const result = await fetchGroupMessagesFromApi(nextCursor, 20);

            if (result && result.messages.length > 0) {
                // 3. Đánh dấu đang prepend để useLayoutEffect xử lý scroll
                isPrependingRef.current = true;
                
                setMessages(prev => [...result.messages, ...prev]);
                setNextCursor(result.nextCursor);
                setHasMore(result.hasMore);
            } else {
                setIsLoadingHistory(false);
            }
        }
    };

    // Component MessageItem
    const MessageItem = memo(function MessageItem({ msg, username, formatTime }) {
        const ref = useRef(null);
        useEffect(() => {
            const el = ref.current;
            if (!el) return;
            el.classList.add('enter');
            requestAnimationFrame(() => el.classList.remove('enter'));
        }, []);

        return (
            <div ref={ref} className={`message ${msg.type || ""}`}>
                {msg.messageType === "JOIN" && (
                    <div className="system-message">{msg.sender} has joined the chat.</div>
                )}
                {msg.messageType === "LEAVE" && (
                    <div className="system-message">{msg.sender} has left the chat.</div>
                )}
                {msg.messageType === "CHAT" && (
                    <div className={`chat-message ${msg.sender === username ? "own-message" : ""}`}>
                        <div className="message-info">
                            <span className="sender" style={{ color: msg.color || '#007bff' }}>{msg.sender}</span>
                            <span className="time">{formatTime(msg.timestamp)}</span>
                        </div>
                        <div className="message-text">{msg.content}</div>
                    </div>
                )}
            </div>
        );
    });

    // --- WebSocket & Initial Load Logic ---

    useEffect(() => {
        let reconnectInterval;

        const connectAndFetch = async () => {
            if (!username) return;

            // 1. Tải tin nhắn lần đầu (Initial Load)
            const initialData = await fetchGroupMessagesFromApi(null, 20);
            if (initialData) {
                setMessages(initialData.messages);
                setNextCursor(initialData.nextCursor);
                setHasMore(initialData.hasMore);
                // Đánh dấu là lần đầu để useLayoutEffect cuộn xuống đáy
                isFirstLoadRef.current = true;
            }

            // 2. Tải danh sách online & unread
            try {
                const onlineData = await authService.getOnlineUsers();
                if (onlineData) {
                    setOnlineUsers(new Set([...Object.keys(onlineData), username]));
                }
                const unreadData = await authService.getUnreadCounts();
                if (unreadData) {
                    setUnreadMessages(new Map(Object.entries(unreadData)));
                }
            } catch (e) {
                console.error("Error fetching initial data:", e);
            }

            // 3. Kết nối WebSocket
            stompClient.current = Stomp.over(() => new SockJS(`${import.meta.env.VITE_SOCKET_URL}`));
            
            stompClient.current.connect(
                {
                    "client-id": username,
                    "username": username,
                },
                (frame) => {
                    console.log("STOMP connected");
                    if (!stompClient.current?.connected) return;
                    clearInterval(reconnectInterval);

                    // Subscribe Public Chat
                    stompClient.current.subscribe("/topic/public", (msg) => {
                        const chatMessage = JSON.parse(msg.body);

                        if (chatMessage.messageType === "JOIN") {
                            setOnlineUsers(prev => new Set(prev).add(chatMessage.sender));
                        } else if (chatMessage.messageType === "LEAVE") {
                            setOnlineUsers(prev => {
                                const newSet = new Set(prev);
                                newSet.delete(chatMessage.sender);
                                return newSet;
                            });
                        }

                        if (chatMessage.messageType === "TYPING") {
                            setIsTyping(chatMessage.sender);
                            clearTimeout(typingTimeoutRef.current);
                            typingTimeoutRef.current = setTimeout(() => setIsTyping(''), 2000);
                            scrollToBottom(); // Cuộn khi có người gõ (tuỳ chọn)
                            return;
                        }

                        // Xử lý tin nhắn mới
                        const messageId = chatMessage.id || `ws-${Date.now()}-${Math.random()}`;
                        if (!messageIdsRef.current.has(messageId)) {
                            messageIdsRef.current.add(messageId);
                            setMessages(prev => [...prev, {
                                ...chatMessage,
                                timestamp: chatMessage.timestamp || new Date().toISOString(),
                                id: messageId
                            }]);
                            // Cuộn xuống khi có tin nhắn mới
                            setTimeout(scrollToBottom, 50);
                        }
                    });

                    // Subscribe Private Chat
                    stompClient.current.subscribe(`/user/${username}/queue/private`, (msg) => {
                        const privateMessage = JSON.parse(msg.body);
                        const otherUser = privateMessage.sender === username 
                            ? privateMessage.recipient 
                            : privateMessage.sender;

                        if (privateMessage.messageType === "PRIVATE_CHAT") {
                            const handler = privateMessageHandlers.current.get(otherUser);
                            if (handler) {
                                handler(privateMessage);
                            } else if (privateMessage.sender !== username) {
                                setUnreadMessages(prev => {
                                    const newUnread = new Map(prev);
                                    newUnread.set(otherUser, (newUnread.get(otherUser) || 0) + 1);
                                    return newUnread;
                                });
                                showNotification(otherUser, "đã gửi cho bạn một tin nhắn mật 🔒");
                            }
                        }
                    });

                    stompClient.current.send("/app/chat/addUser", {}, JSON.stringify({
                        sender: username, messageType: "JOIN", color: userColor
                    }));
                },
                (error) => {
                    console.error("STOMP error:", error);
                    if (!reconnectInterval) {
                        reconnectInterval = setInterval(connectAndFetch, 5000);
                    }
                }
            );
        };

        connectAndFetch();

        return () => {
            if (stompClient.current?.connected) {
                stompClient.current.disconnect();
            }
            clearInterval(reconnectInterval);
            clearTimeout(typingTimeoutRef.current);
        };
    }, [username, userColor]);

    // --- Keys & Encryption Logic (Giữ nguyên) ---
    useEffect(() => {
        const checkKeysOnLoad = async () => {
            if (isUnlocked) return;
            try {
                const existingKey = await authService.getEncryptedRsaKey();
                if (!existingKey) {
                    requestUnlock();
                }
            } catch (e) {}
        };
        checkKeysOnLoad();
    }, []);

    // --- UI Actions (Giữ nguyên) ---

    const openPrivateChat = (otherUser) => {
        if (otherUser === username) return;
        requestUnlock(() => {
            setUnreadMessages(prev => {
                const newUnread = new Map(prev);
                newUnread.delete(otherUser);
                return newUnread;
            });
            setPrivateChats(prev => new Map(prev).set(otherUser, true));
        });
    };

    const closePrivateChat = (otherUser) => {
        setPrivateChats(prev => {
            const newChats = new Map(prev);
            newChats.delete(otherUser);
            return newChats;
        });
        unregisterPrivateMessageHandler(otherUser);
    };

    const registerPrivateMessageHandler = useCallback((otherUser, handler) => {
        privateMessageHandlers.current.set(otherUser, handler);
    }, []);

    const unregisterPrivateMessageHandler = useCallback((otherUser) => {
        privateMessageHandlers.current.delete(otherUser);
    }, []);

    const sendMessage = (e) => {
        e.preventDefault();
        if (message.trim() && stompClient.current?.connected) {
            const chatMessage = {
                sender: username,
                content: message,
                messageType: "CHAT",
                color: userColor,
            };
            stompClient.current.send("/app/chat/sendMessage", {}, JSON.stringify(chatMessage));
            setMessage("");
            setShowEmojiPicker(false);
        }
    };

    const handleTyping = (e) => {
        setMessage(e.target.value);
        if (stompClient.current?.connected && e.target.value.trim()) {
            const typingMessage = {
                sender: username,
                messageType: "TYPING",
            };
            stompClient.current.send("/app/chat/sendMessage", {}, JSON.stringify(typingMessage));
        }
    };

    const addEmoji = (emoji) => {
        setMessage(prev => prev + emoji);
        setShowEmojiPicker(false);
    };

    return (
        <div className="chat-container">
            <div className="notification-container">
                {notifications.map(notification => (
                    <div 
                        key={notification.id} 
                        className="notification-toast"
                        onClick={() => handleNotificationClick(notification.sender)}
                    >
                        <div className="toast-content">
                            <div className="toast-title">
                                <span>📩 Tin nhắn mới</span>
                            </div>
                            <div className="toast-message">
                                <strong>{notification.sender}</strong> {notification.content}
                            </div>
                        </div>
                        <button 
                            className="toast-close" 
                            onClick={(e) => {
                                e.stopPropagation(); // Ngăn không cho click xuyên qua để mở chat
                                removeNotification(notification.id);
                            }}
                        >
                            &times;
                        </button>
                    </div>
                ))}
            </div>
            <div className="sidebar">
                <div className="sidebar-header">
                    <h2>Online Users</h2>
                </div>
                <div className="user-list">
                    {Array.from(onlineUsers)
                        .sort((a, b) => {
                            if (a === username) return -1;
                            if (b === username) return 1;
                            return a.localeCompare(b);
                        }).map((user) => (
                        <div
                            key={user}
                            className={'user-item' + (user === username ? ' current-user' : '')}
                            onClick={() => openPrivateChat(user)}
                        >
                            <div className="user-avatar" style={{ backgroundColor: user === username ? userColor : '#007bff' }}>
                                {user.charAt(0).toUpperCase()}
                            </div>
                            <span>{user}</span>
                            {user === username && <span className="you-label">(You)</span>}
                            {unreadMessages.has(user) && !privateChats.has(user) && (
                                <span className="unread-count">
                                    {unreadMessages.get(user)}
                                </span>
                            )}
                        </div>
                    ))}
                </div>
            </div>
            
            <div className="main-chat">
                <div className="chat-header">
                    <h4>Welcome, {username}</h4>
                </div>
                
                <div 
                    className="messages-container" 
                    ref={chatContainerRef}
                    onScroll={handleScroll}
                >
                    {isLoadingHistory && (
                        <div className="system-message">... Loading history ...</div>
                    )}
                    
                    {messages.map((msg) => (
                        <MessageItem 
                            key={msg.id} 
                            msg={msg} 
                            username={username} 
                            formatTime={formatTime} 
                        />
                    ))}
                    <div ref={messageEndRef} />
                </div>

                <div className="input-area">
                    {isTyping && isTyping !== username && (
                        <div className="typing-indicator" style={{ marginBottom: 8 }}>
                            {isTyping} is typing...
                        </div>
                    )}
                    {showEmojiPicker && (
                        <div className="emoji-picker">
                            {emojis.map((emoji) => (
                                <button key={emoji} onClick={() => addEmoji(emoji)}>
                                    {emoji}
                                </button>
                            ))}
                        </div>
                    )}
                    <form onSubmit={sendMessage} className="message-form">
                        <button
                            type="button"
                            className="emoji-button"
                            onClick={() => setShowEmojiPicker(!showEmojiPicker)}
                        >
                            😊
                        </button>
                        <input
                            type="text"
                            className="message-input"
                            placeholder="Type a message..."
                            value={message}
                            onChange={handleTyping}
                            maxLength={500}
                        />
                        <button type="submit" className="send-button" disabled={!message.trim()}>
                            Send
                        </button>
                    </form>
                </div>
            </div>
            
            {Array.from(privateChats.keys()).map((otherUser) => (
                <PrivateChat
                    key={otherUser}
                    currentUser={username}
                    recipientUser={otherUser}
                    userColor={userColor}
                    stompClient={stompClient}
                    onClose={() => closePrivateChat(otherUser)}
                    registerPrivateMessageHandler={registerPrivateMessageHandler}
                    unregisterPrivateMessageHandler={unregisterPrivateMessageHandler}
                />
            ))}
            
            {isUnlockModalVisible && <KeyUnlockModal />}
        </div>
    );
};

export default Chat;