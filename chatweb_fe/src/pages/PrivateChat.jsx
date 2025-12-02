import { useState, useEffect, useRef, useCallback } from "react";
import { authService } from "../services/authService";
import { cryptoService } from "../services/cryptoService";
import { useCrypto } from "../context/CryptoContext";
import "../styles/PrivateChat.css";

const PrivateChat = ({
    currentUser,
    recipientUser,
    userColor,
    stompClient,
    onClose,
    registerPrivateMessageHandler,
    unregisterPrivateMessageHandler
}) => {
    const [messages, setMessages] = useState([]);
    const [decryptedContent, setDecryptedContent] = useState(new Map());

    const [message, setMessage] = useState("");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const [nextCursor, setNextCursor] = useState(null);
    const [hasMore, setHasMore] = useState(false);
    const [isLoadingMore, setIsLoadingMore] = useState(false);

    const { rsaKeyPair } = useCrypto();
    const [recipientPublicKey, setRecipientPublicKey] = useState(null);

    const sessionKeyRef = useRef(null);

    // Ref tham chiếu đến khung chứa tin nhắn (scrollable div)
    const chatContainerRef = useRef(null);
    // Ref tham chiếu đến điểm cuối (để auto scroll khi có tin mới)
    const messagesEndRef = useRef(null);
    // Ref để giữ giá trị scrollHeight cũ phục vụ việc giữ vị trí cuộn
    const previousScrollHeightRef = useRef(0);
    // Khi đang tải tin cũ, chúng ta tạm thời chặn auto-scroll xuống cuối
    const suppressAutoScrollRef = useRef(false);

    const scrollToBottom = () => {
        // Nếu đang tải tin cũ thì không auto-scroll xuống cuối
        if (suppressAutoScrollRef.current) return;

        requestAnimationFrame(() => {
            messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
        });
    };

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
                    plaintext = await cryptoService.decryptMessage(
                        msg.content,
                        msg.iv,
                        sessionKey
                    );
                } catch (e) {
                    plaintext = null;
                }
            }

            if (!plaintext) {
                sessionKey = await cryptoService.unwrapSessionKey(
                    keyToUnwrap,
                    rsaKeyPair.privateKey
                );

                sessionKeyRef.current = sessionKey;

                plaintext = await cryptoService.decryptMessage(
                    msg.content,
                    msg.iv,
                    sessionKey
                );
            }

            setDecryptedContent(prev => new Map(prev).set(msg.id, plaintext));

        } catch (e) {
            console.error("Lỗi giải mã tin nhắn:", e, msg);
            setDecryptedContent(prev => new Map(prev).set(msg.id, `⚠️ Lỗi: Không thể giải mã. (${e.message})`));
        }
    }, [currentUser, rsaKeyPair, decryptedContent]);

    const handleIncomingPrivateMessage = useCallback((msg) => {
        console.log("Nhận được tin nhắn STOMP:", msg);

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

        }
        else {
            setMessages(prev => {
                if (prev.find(m => m.id === msg.id)) return prev;
                return [...prev, msg];
            });
            decryptMessage(msg);
            // setTimeout(scrollToBottom, 100);
        }

    }, [currentUser, decryptMessage]);

    // src/pages/PrivateChat.jsx

    const handleScroll = async () => {
        const container = chatContainerRef.current;
        if (!container) return;

        // SỬA: Đổi '=== 0' thành '< 20' để trải nghiệm mượt hơn
        if (container.scrollTop < 20 && hasMore && !isLoadingMore) {

            const previousScrollHeight = container.scrollHeight;
            // Lưu lại scrollTop hiện tại để tính toán chính xác hơn
            const previousScrollTop = container.scrollTop; 

            suppressAutoScrollRef.current = true;
            setIsLoadingMore(true);

            try {
                const response = await authService.fetchPrivateChat(
                    currentUser,
                    recipientUser,
                    nextCursor,
                    20
                );

                const { content, nextCursor: newCursor, hasMore: newHasMore } = response;

                if (content && content.length > 0) {
                    const olderMessages = content.reverse();
                    olderMessages.forEach(msg => decryptMessage(msg));

                    // Cập nhật state
                    setMessages(prev => [...olderMessages, ...prev]);
                    setNextCursor(newCursor);
                    setHasMore(newHasMore);

                    // Giữ vị trí cuộn
                    requestAnimationFrame(() => {
                        if (chatContainerRef.current) {
                            const newScrollHeight = chatContainerRef.current.scrollHeight;
                            const heightDifference = newScrollHeight - previousScrollHeight;
                            
                            // Cộng thêm previousScrollTop để đảm bảo chính xác vị trí cũ
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
                const pubKey = await authService.getPublicKey(recipientUser);
                if (!pubKey) {
                    throw new Error(`Không thể lấy public key cho ${recipientUser}.`);
                }
                setRecipientPublicKey(pubKey);

                const response = await authService.fetchPrivateChat(currentUser, recipientUser, null, 20);
                console.log("goi goi goi")
                if (isMounted) {
                    // API trả về: { content: [...], nextCursor: "...", hasMore: true/false }
                    const { content, nextCursor, hasMore } = response;

                    const messagesForUI = Array.isArray(content) ? content.reverse() : [];

                    setMessages(messagesForUI);      // Lưu mảng tin nhắn
                    setNextCursor(nextCursor);       // Lưu mốc thời gian cho lần load sau
                    setHasMore(hasMore);             // Lưu trạng thái còn tin cũ không

                    messagesForUI.forEach(msg => decryptMessage(msg));

                    if (messagesForUI.length > 0) {
                        authService.markAsRead(recipientUser);
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
    }, [currentUser, recipientUser, registerPrivateMessageHandler, unregisterPrivateMessageHandler, decryptMessage, handleIncomingPrivateMessage]);

    // --- Gửi tin nhắn ---
    const sendPrivateMessage = async (e) => {
        e.preventDefault();
        const plaintext = message.trim();
        // THÊM rsaKeyPair VÀO ĐIỀU KIỆN KIỂM TRA
        if (!plaintext || !stompClient.current || !recipientPublicKey || !rsaKeyPair?.publicKey) return;

        try {
            // 1. Lấy hoặc tạo session key (AES)
            if (!sessionKeyRef.current) {
                console.log("Tạo session key mới...");
                sessionKeyRef.current = await cryptoService.generateSessionKey();
            }

            // 2. Mã hóa tin nhắn (AES)
            const { iv, ciphertext } = await cryptoService.encryptMessage(
                plaintext,
                sessionKeyRef.current
            );

            // 3. Mã hóa session key (RSA) - 2 LẦN

            // Lần 1: Cho người nhận
            const wrappedKeyForRecipient = await cryptoService.wrapSessionKey(
                sessionKeyRef.current,
                recipientPublicKey
            );

            // Lần 2: Cho chính mình (dùng public key của MÌNH)
            const wrappedKeyForSender = await cryptoService.wrapSessionKey(
                sessionKeyRef.current,
                rsaKeyPair.publicKey // Dùng public key của chính mình
            );

            // 4. Tạo payload
            const timestamp = new Date().toISOString();
            const localId = `local-${crypto.randomUUID()}`;

            const payload = {
                id: localId,
                localId: localId,
                sender: currentUser,
                recipient: recipientUser,
                content: ciphertext,
                iv: iv,

                // Gửi 2 khóa mới thay vì 1 khóa cũ
                // wrappedKey: wrappedKey, // XÓA DÒNG NÀY
                wrappedKeyRecipient: wrappedKeyForRecipient,
                wrappedKeySender: wrappedKeyForSender,

                messageType: "PRIVATE_CHAT",
                color: userColor,
                timestamp: timestamp,
            };

            // 5. Gửi qua STOMP
            stompClient.current.send("/app/chat/sendPrivateMessage", {}, JSON.stringify(payload));

            // 6. Cập nhật UI (giữ nguyên)
            setMessages(prev => [...prev, payload]);
            setDecryptedContent(prev => new Map(prev).set(localId, plaintext));

            setMessage("");

            setTimeout(scrollToBottom, 50);

        } catch (error) {
            console.error("Lỗi gửi tin nhắn E2EE:", error);
            setError("Không thể gửi tin nhắn: " + error.message);
        }
    };

    const formatTime = (timestamp) => {
        return new Date(timestamp).toLocaleTimeString('vi-VN', {
            hour12: false, hour: '2-digit', minute: '2-digit'
        });
    };

    if (loading) {
        return (
            <div className="private-chat-window">
                <div className="private-chat-header"><h3>{recipientUser}</h3><button onClick={onClose}>X</button></div>
                <div className="loading"><p>Đang tải khóa và lịch sử...</p></div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="private-chat-window">
                <div className="private-chat-header"><h3>{recipientUser}</h3><button onClick={onClose}>X</button></div>
                <div className="loading"><p style={{ color: 'red' }}>Lỗi: {error}</p></div>
            </div>
        );
    }

    return (
        <div className="private-chat-window">
            <div className="private-chat-header">
                <div className="recipient-info">
                    <div className="recipient-avatar">{recipientUser.charAt(0).toUpperCase()}</div>
                    <h3 className="recipient-username">🔒 {recipientUser}</h3>
                </div>
                <button className="close-button" onClick={onClose}>X</button>
            </div>
            <div
                className="private-chat-messages"
                ref={chatContainerRef}       // Gắn ref để tính toán cuộn
                onScroll={handleScroll}      // Gắn sự kiện cuộn
            >
                {isLoadingMore && (
                    <div className="loading-more-container">
                        <div className="loading-spinner"></div>
                    </div>
                )}                {messages.length === 0 ? (
                    <div className="no-messages"><p>Chưa có tin nhắn. Cuộc trò chuyện này được mã hóa đầu-cuối.</p></div>
                ) : (
                    messages.map((msg) => {
                        const plaintext = decryptedContent.get(msg.id) || "Đang giải mã...";
                        return (
                            <div key={msg.id} className={`private-message ${msg.sender === currentUser ? 'own-message' : 'received-message'}`}>
                                <div className="message-header">
                                    <span className="sender-name" style={{ color: msg.color || '#6b73FF' }}>{msg.sender === currentUser ? 'Bạn' : msg.sender}</span>
                                    <span className="timestamp">{formatTime(msg.timestamp)}</span>
                                </div>
                                <div className="message-content">
                                    {plaintext}
                                </div>
                            </div>
                        );
                    })
                )}
                <div ref={messagesEndRef}></div>
            </div>
            <div className="private-message-input-container">
                <form onSubmit={sendPrivateMessage} className="private-message-form">
                    <input
                        type="text"
                        placeholder={`Message ${recipientUser}...`}
                        value={message}
                        onChange={(e) => setMessage(e.target.value)}
                        className="private-message-input"
                        maxLength={500}
                        disabled={!recipientPublicKey} // Vô hiệu hóa nếu chưa lấy được key
                    />
                    <button type="submit" className="private-send-button" disabled={!message.trim() || !recipientPublicKey}>
                        Send
                    </button>
                </form>
            </div>
        </div>
    )
};

export default PrivateChat;