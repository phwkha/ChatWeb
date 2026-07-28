import { useState, useEffect, useRef, useCallback } from "react";
import { chatService, keyService } from "../services";
import { cryptoService } from "../services/cryptoService";
import { useCrypto } from "../context/CryptoContext";
import { useChatPagination } from "./chat/useChatPagination";

export const useDirectMessage = ({
    currentUser,
    recipientUser,
    userColor,
    stompClient,
    registerPrivateMessageHandler,
    unregisterPrivateMessageHandler
}) => {
    const [decryptedContent, setDecryptedContent] = useState(new Map());
    const [message, setMessage] = useState("");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [recipientPublicKey, setRecipientPublicKey] = useState(null);

    const { rsaKeyPair } = useCrypto();
    const sessionKeyRef = useRef(null);
    const chatContainerRef = useRef(null);
    const messagesEndRef = useRef(null);
    const suppressAutoScrollRef = useRef(false);
    const [isLoadingMore, setIsLoadingMore] = useState(false);

    const scrollToBottom = useCallback(() => {
        if (suppressAutoScrollRef.current) return;
        requestAnimationFrame(() => {
            messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
        });
    }, []);

    const decryptMessage = useCallback(async (msg) => {
        if (decryptedContent.has(msg.id)) return;

        const isOwnMessage = msg.sender === currentUser;
        const keyToUnwrap = isOwnMessage ? msg.wrappedKeySender : msg.wrappedKeyRecipient;

        if (!keyToUnwrap || !msg.iv || !msg.content) {
            setDecryptedContent(prev => new Map(prev).set(msg.id, "Tin nhắn không hợp lệ (thiếu key)"));
            return;
        }

        try {
            if (!rsaKeyPair || !rsaKeyPair.privateKey) {
                throw new Error("Không tìm thấy private key!");
            }

            let sessionKey = sessionKeyRef.current;
            let plaintext = null;

            if (sessionKey) {
                try {
                    plaintext = await cryptoService.decryptMessage(msg.content, msg.iv, sessionKey);
                } catch (e) {
                    plaintext = null;
                }
            }

            if (!plaintext) {
                sessionKey = await cryptoService.unwrapSessionKey(keyToUnwrap, rsaKeyPair.privateKey);
                sessionKeyRef.current = sessionKey;
                plaintext = await cryptoService.decryptMessage(msg.content, msg.iv, sessionKey);
            }

            setDecryptedContent(prev => new Map(prev).set(msg.id, plaintext));
        } catch (e) {
            console.error("Lỗi giải mã tin nhắn:", e, msg);
            setDecryptedContent(prev => new Map(prev).set(msg.id, `⚠️ Lỗi: Không thể giải mã. (${e.message})`));
        }
    }, [currentUser, rsaKeyPair, decryptedContent]);

    const fetchApiFn = useCallback(async (cursor, size) => {
        const response = await chatService.fetchPrivateChat(currentUser, recipientUser, cursor, size);
        if (response && response.content) {
            response.content.forEach(msg => decryptMessage(msg));
        }
        return response;
    }, [currentUser, recipientUser, decryptMessage]);

    const {
        messages, setMessages, nextCursor, setNextCursor, hasMore, setHasMore, fetchHistory, loadInitialHistory
    } = useChatPagination(fetchApiFn);

    const handleIncomingPrivateMessage = useCallback((msg) => {
        if (msg.sender === currentUser && msg.localId) {
            setMessages(prev => {
                const newMessages = [...prev];
                const localIndex = newMessages.findIndex(m => m.id === msg.localId);
                if (localIndex !== -1) {
                    newMessages[localIndex] = msg;
                } else {
                    newMessages.push(msg);
                }
                return newMessages;
            });
            setDecryptedContent(prev => {
                const newContent = new Map(prev);
                const plaintext = newContent.get(msg.localId);
                if (plaintext) {
                    newContent.delete(msg.localId);
                    newContent.set(msg.id, plaintext);
                }
                return newContent;
            });
        } else {
            setMessages(prev => {
                const index = prev.findIndex(m => m.id === msg.id);
                if (index !== -1) {
                    const newArr = [...prev];
                    newArr[index] = msg;
                    return newArr;
                }
                return [...prev, msg];
            });
            decryptMessage(msg);
        }
    }, [currentUser, decryptMessage, setMessages]);

    const handleScroll = async () => {
        const container = chatContainerRef.current;
        if (!container) return;

        if (container.scrollTop < 20 && hasMore && !isLoadingMore) {
            const previousScrollHeight = container.scrollHeight;
            const previousScrollTop = container.scrollTop; 

            suppressAutoScrollRef.current = true;
            setIsLoadingMore(true);

            try {
                const data = await fetchHistory(nextCursor, 20);
                if (data && data.messages.length > 0) {
                    setMessages(prev => [...data.messages, ...prev]);
                    setNextCursor(data.nextCursor);
                    setHasMore(data.hasMore);

                    requestAnimationFrame(() => {
                        if (chatContainerRef.current) {
                            const newScrollHeight = chatContainerRef.current.scrollHeight;
                            const heightDifference = newScrollHeight - previousScrollHeight;
                            chatContainerRef.current.scrollTop = heightDifference + previousScrollTop;
                        }
                        setIsLoadingMore(false);
                        suppressAutoScrollRef.current = false;
                    });
                } else {
                    setIsLoadingMore(false);
                    suppressAutoScrollRef.current = false;
                }
            } catch (err) {
                console.error("Lỗi tải tin nhắn cũ:", err);
                setIsLoadingMore(false);
                suppressAutoScrollRef.current = false;
            }
        }
    };

    useEffect(() => {
        let isMounted = true;
        sessionKeyRef.current = null;

        const initialize = async () => {
            if (!isMounted) return;
            setLoading(true);
            setError("");
            try {
                const pubKey = await keyService.getPublicKey(recipientUser);
                if (!pubKey) throw new Error(`Không thể lấy public key cho ${recipientUser}.`);
                setRecipientPublicKey(pubKey);

                const data = await fetchHistory(null, 20);
                if (isMounted && data) {
                    setMessages(data.messages);      
                    setNextCursor(data.nextCursor);       
                    setHasMore(data.hasMore);             

                    if (data.messages.length > 0) {
                        chatService.markAsRead(recipientUser);
                        setTimeout(scrollToBottom, 100);
                    }
                }
            } catch (err) {
                console.error("Không thể tải chat E2EE:", err);
                if (isMounted) setError(err.message);
            } finally {
                if (isMounted) setLoading(false);
            }
        };

        initialize();
        registerPrivateMessageHandler(recipientUser, handleIncomingPrivateMessage);

        return () => {
            isMounted = false;
            unregisterPrivateMessageHandler(recipientUser);
        };
    }, [currentUser, recipientUser, registerPrivateMessageHandler, unregisterPrivateMessageHandler, handleIncomingPrivateMessage, scrollToBottom, fetchHistory, setMessages, setNextCursor, setHasMore]);

    const sendPrivateMessage = async (e) => {
        e.preventDefault();
        const plaintext = message.trim();
        if (!plaintext || !stompClient.current || !recipientPublicKey || !rsaKeyPair?.publicKey) return;

        try {
            if (!sessionKeyRef.current) sessionKeyRef.current = await cryptoService.generateSessionKey();
            const { iv, ciphertext } = await cryptoService.encryptMessage(plaintext, sessionKeyRef.current);
            const wrappedKeyForRecipient = await cryptoService.wrapSessionKey(sessionKeyRef.current, recipientPublicKey);
            const wrappedKeyForSender = await cryptoService.wrapSessionKey(sessionKeyRef.current, rsaKeyPair.publicKey);

            const timestamp = new Date().toISOString();
            const localId = `local-${crypto.randomUUID()}`;

            const payload = {
                id: localId, localId, sender: currentUser, recipient: recipientUser,
                content: ciphertext, iv, wrappedKeyRecipient: wrappedKeyForRecipient,
                wrappedKeySender: wrappedKeyForSender, messageType: "PRIVATE_CHAT",
                color: userColor, timestamp,
            };

            stompClient.current.send("/app/chat/sendPrivateMessage", {}, JSON.stringify(payload));
            setMessages(prev => [...prev, payload]);
            setDecryptedContent(prev => new Map(prev).set(localId, plaintext));
            setMessage("");
            setTimeout(scrollToBottom, 50);
        } catch (error) {
            console.error("Lỗi gửi tin nhắn E2EE:", error);
            setError("Không thể gửi tin nhắn: " + error.message);
        }
    };

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

    const reactMessage = useCallback((messageId, reactionType) => {
        if (stompClient.current?.connected) {
            stompClient.current.send("/app/chat/reaction", {}, JSON.stringify({ 
                messageId, recipient: recipientUser, reactionType 
            }));
        }
    }, [stompClient, recipientUser]);

    return {
        messages, message, setMessage, decryptedContent, loading, error, recipientPublicKey,
        isLoadingMore, chatContainerRef, messagesEndRef, handleScroll, sendPrivateMessage,
        editMessage, revokeMessage, reactMessage
    };
};
