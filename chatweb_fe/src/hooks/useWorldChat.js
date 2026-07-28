import { useState, useEffect, useRef, useCallback } from "react";
import { chatService, userService, keyService } from "../services";
import { useCrypto } from "../context/CryptoContext";
import { useWebSocket } from "./chat/useWebSocket";
import { useChatPagination } from "./chat/useChatPagination";
import { useNotifications } from "./chat/useNotifications";

export const useWorldChat = (currentUser) => {
    const { requestUnlock, isUnlocked } = useCrypto();
    const username = currentUser?.username;
    const userColor = currentUser?.color;

    const { stompClient, isConnected } = useWebSocket(username);
    
    const {
        messages, setMessages, nextCursor, setNextCursor, hasMore, setHasMore,
        isLoadingHistory, setIsLoadingHistory, messageIdsRef, fetchHistory, loadInitialHistory
    } = useChatPagination(chatService.fetchGroupChat);

    const {
        notifications, unreadMessages, setUnreadMessages, showNotification, removeNotification, handleNotificationClick
    } = useNotifications((sender) => openPrivateChat(sender));

    const [message, setMessage] = useState("");
    const [showEmojiPicker, setShowEmojiPicker] = useState(false);
    const [isUploading, setIsUploading] = useState(false);
    const [activeChat, setActiveChat] = useState("WORLD");
    const [onlineUsers, setOnlineUsers] = useState(new Set());

    const privateMessageHandlers = useRef(new Map());

    // --- Initial Load ---
    useEffect(() => {
        if (!username) return;
        loadInitialHistory();
        
        userService.getOnlineUsers().then(onlineData => {
            if (onlineData) setOnlineUsers(new Set([...Object.keys(onlineData), username]));
        }).catch(console.error);

        chatService.getUnreadCounts().then(unreadData => {
            if (unreadData) setUnreadMessages(new Map(Object.entries(unreadData)));
        }).catch(console.error);
    }, [username, loadInitialHistory, setUnreadMessages]);

    // --- Subscriptions ---
    useEffect(() => {
        if (!isConnected || !stompClient.current) return;

        const publicSub = stompClient.current.subscribe("/topic/public", (msg) => {
            const chatMessage = JSON.parse(msg.body);
            const messageId = chatMessage.id || `ws-${Date.now()}-${Math.random()}`;
            if (!messageIdsRef.current.has(messageId)) {
                messageIdsRef.current.add(messageId);
                setMessages(prev => [...prev, {
                    ...chatMessage,
                    timestamp: chatMessage.timestamp || new Date().toISOString(),
                    id: messageId,
                }]);
            }
        });

        const privateSub = stompClient.current.subscribe(`/user/${username}/queue/private`, (msg) => {
            const privateMessage = JSON.parse(msg.body);
            const otherUser = privateMessage.sender === username ? privateMessage.recipient : privateMessage.sender;

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

        return () => {
            publicSub.unsubscribe();
            privateSub.unsubscribe();
        };
    }, [isConnected, username, setMessages, setUnreadMessages, showNotification, messageIdsRef]);

    // --- Keys Check ---
    useEffect(() => {
        if (!isUnlocked) {
            keyService.getEncryptedRsaKey().then(key => {
                if (!key) requestUnlock();
            }).catch(console.error);
        }
    }, [isUnlocked, requestUnlock]);

    // --- UI Actions ---
    const openPrivateChat = useCallback((otherUser) => {
        if (otherUser === 'WORLD') {
            setActiveChat('WORLD');
            return;
        }
        if (otherUser === username) return;
        requestUnlock(() => {
            setUnreadMessages(prev => {
                const newUnread = new Map(prev);
                newUnread.delete(otherUser);
                return newUnread;
            });
            setActiveChat(otherUser);
        });
    }, [username, requestUnlock, setUnreadMessages]);

    const closePrivateChat = useCallback((otherUser) => {
        setActiveChat("WORLD");
        privateMessageHandlers.current.delete(otherUser);
    }, []);

    const registerPrivateMessageHandler = useCallback((otherUser, handler) => {
        privateMessageHandlers.current.set(otherUser, handler);
    }, []);

    const unregisterPrivateMessageHandler = useCallback((otherUser) => {
        privateMessageHandlers.current.delete(otherUser);
    }, []);

    const uploadMedia = useCallback(async (file, type) => {
        if (!file || !stompClient.current?.connected) return;
        setIsUploading(true);
        try {
            const response = type === 'IMAGE' ? await chatService.uploadImage(file) : await chatService.uploadVideo(file);
            if (response && response.data) {
                stompClient.current.send("/app/chat/sendMessageSystem", {}, JSON.stringify({
                    content: response.data
                }));
            }
        } catch (error) {
            console.error("Upload failed", error);
        } finally {
            setIsUploading(false);
        }
    }, [stompClient]);

    const sendMessage = useCallback((e) => {
        e.preventDefault();
        if (message.trim() && stompClient.current?.connected) {
            stompClient.current.send("/app/chat/sendMessageSystem", {}, JSON.stringify({
                content: message
            }));
            setMessage("");
            setShowEmojiPicker(false);
        }
    }, [message, stompClient]);

    const handleTyping = useCallback((e) => {
        setMessage(e.target.value);
    }, []);

    const addEmoji = useCallback((emoji) => {
        setMessage(prev => prev + emoji);
        setShowEmojiPicker(false);
    }, []);

    const editMessage = useCallback((messageId, newContent) => {
        if (stompClient.current?.connected) {
            stompClient.current.send("/app/chat/editMessage", {}, JSON.stringify({ messageId, newContent }));
        }
    }, [stompClient]);

    const revokeMessage = useCallback((messageId) => {
        if (stompClient.current?.connected) {
            stompClient.current.send("/app/chat/revokeMessage", {}, JSON.stringify({ messageId }));
        }
    }, [stompClient]);

    const sendFriendRequest = useCallback((targetUsername) => {
        if (stompClient.current?.connected) {
            stompClient.current.send("/app/friend/request", {}, targetUsername);
        }
    }, [stompClient]);

    const acceptFriendRequest = useCallback((requesterUsername) => {
        if (stompClient.current?.connected) {
            stompClient.current.send("/app/friend/accept", {}, requesterUsername);
        }
    }, [stompClient]);

    return {
        messages, setMessages, message, setMessage, showEmojiPicker, setShowEmojiPicker,
        activeChat, setActiveChat, unreadMessages, onlineUsers, notifications,
        nextCursor, setNextCursor, hasMore, setHasMore, isLoadingHistory, setIsLoadingHistory,
        stompClient, fetchGroupMessagesFromApi: fetchHistory, handleNotificationClick, removeNotification,
        openPrivateChat, closePrivateChat, registerPrivateMessageHandler, unregisterPrivateMessageHandler,
        sendMessage, handleTyping, addEmoji, isUploading, uploadMedia, sendFriendRequest,
        acceptFriendRequest, editMessage, revokeMessage
    };
};
