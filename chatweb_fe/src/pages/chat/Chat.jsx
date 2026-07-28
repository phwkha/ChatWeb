import { useNavigate } from "react-router-dom";
import { authService } from "../../services";
import { useEffect, useRef, useLayoutEffect } from "react";
import PrivateChat from "./PrivateChat";
import { useCrypto } from "../../context/CryptoContext";
import { KeyUnlockModal } from "../../components/common/KeyUnlockModal";
import MessageItem from "../../components/chat/MessageItem";
import Sidebar from "../../components/chat/Sidebar";
import { useWorldChat } from "../../hooks/useWorldChat";

const emojis = [
  "😀",
  "😂",
  "😍",
  "😎",
  "😭",
  "😡",
  "👍",
  "🙏",
  "🎉",
  "💔",
  "🔥",
  "🌟",
  "💯",
  "🎶",
  "🍕",
  "⚽",
  "🏆",
  "🚀",
  "🌈",
  "☀️",
];

const formatTime = (timestamp) => {
  return new Date(timestamp).toLocaleTimeString("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh",
    hour12: false,
    hour: "2-digit",
    minute: "2-digit",
  });
};

const Chat = () => {
  const navigate = useNavigate();
  const { isUnlockModalVisible } = useCrypto();
  const currentUser = authService.getCurrentUser();

  useEffect(() => {
    if (!currentUser) {
      navigate("/login");
    }
  }, [currentUser, navigate]);

  const { username = "", color: userColor = "" } = currentUser || {};
  const canChat =
    currentUser?.role === "ADMIN" ||
    (currentUser?.permissions &&
      currentUser.permissions.includes("ADMIN_SEND-MESSAGE"));

  const {
    messages,
    setMessages,
    message,
    showEmojiPicker,
    setShowEmojiPicker,
    activeChat,
    setActiveChat,
    unreadMessages,
    onlineUsers,
    notifications,
    nextCursor,
    setNextCursor,
    hasMore,
    setHasMore,
    isLoadingHistory,
    setIsLoadingHistory,
    stompClient,
    fetchGroupMessagesFromApi,
    handleNotificationClick,
    removeNotification,
    openPrivateChat,
    closePrivateChat,
    registerPrivateMessageHandler,
    unregisterPrivateMessageHandler,
    sendMessage,
    handleTyping,
    addEmoji,
    isUploading,
    uploadMedia,
    sendFriendRequest,
    acceptFriendRequest,
    editMessage,
    revokeMessage,
  } = useWorldChat(currentUser);

  const messageEndRef = useRef(null);
  const fileInputRef = useRef(null);
  const chatContainerRef = useRef(null);
  const isFirstLoadRef = useRef(true);
  const prevScrollHeightRef = useRef(0);
  const prevScrollTopRef = useRef(0);
  const isPrependingRef = useRef(false);

  useLayoutEffect(() => {
    if (isPrependingRef.current && chatContainerRef.current) {
      const container = chatContainerRef.current;
      const newScrollHeight = container.scrollHeight;
      const heightDifference = newScrollHeight - prevScrollHeightRef.current;
      container.scrollTop = prevScrollTopRef.current + heightDifference;
      isPrependingRef.current = false;
      setIsLoadingHistory(false);
    }
  }, [messages, setIsLoadingHistory]);

  useLayoutEffect(() => {
    if (isFirstLoadRef.current && messages.length > 0) {
      messageEndRef.current?.scrollIntoView({ behavior: "auto" });
      isFirstLoadRef.current = false;
    }
  }, [messages]);

  const scrollToBottom = () => {
    requestAnimationFrame(() => {
      messageEndRef.current?.scrollIntoView({ behavior: "smooth" });
    });
  };

  useEffect(() => {
    if (!isFirstLoadRef.current && !isPrependingRef.current) {
      setTimeout(scrollToBottom, 50);
    }
  }, [messages.length]);

  const handleScroll = async () => {
    const container = chatContainerRef.current;
    if (!container) return;

    if (container.scrollTop < 20 && hasMore && !isLoadingHistory) {
      prevScrollHeightRef.current = container.scrollHeight;
      prevScrollTopRef.current = container.scrollTop;
      setIsLoadingHistory(true);

      const result = await fetchGroupMessagesFromApi(nextCursor, 20);

      if (result && result.messages.length > 0) {
        isPrependingRef.current = true;
        setMessages((prev) => [...result.messages, ...prev]);
        setNextCursor(result.nextCursor);
        setHasMore(result.hasMore);
      } else {
        setIsLoadingHistory(false);
      }
    }
  };

  if (!currentUser) return null;

  return (
    <div className="flex h-[calc(100vh-4rem)] bg-slate-900 relative overflow-hidden">
      {/* Background Decorations */}
      <div className="absolute top-0 right-1/4 w-[400px] h-[400px] bg-indigo-600/10 rounded-full blur-[100px] pointer-events-none"></div>

      {/* Notifications */}
      <div className="absolute top-4 right-4 z-50 flex flex-col gap-2">
        {notifications.map((notification) => (
          <div
            key={notification.id}
            className="bg-slate-800 border border-indigo-500/30 shadow-lg shadow-indigo-500/20 rounded-xl p-4 flex items-start gap-4 animate-enter cursor-pointer hover:bg-slate-700/80 transition-colors max-w-sm"
            onClick={() => handleNotificationClick(notification.sender)}
          >
            <div className="flex-1">
              <div className="text-indigo-400 font-semibold text-sm mb-1">
                📩 Tin nhắn mới
              </div>
              <div className="text-slate-200 text-sm">
                <strong>{notification.sender}</strong> {notification.content}
              </div>
            </div>
            <button
              className="text-slate-400 hover:text-white"
              onClick={(e) => {
                e.stopPropagation();
                removeNotification(notification.id);
              }}
            >
              &times;
            </button>
          </div>
        ))}
      </div>

      {/* Sidebar */}
      <Sidebar
        onlineUsers={onlineUsers}
        username={username}
        userColor={userColor}
        openPrivateChat={openPrivateChat}
        unreadMessages={unreadMessages}
        activeChat={activeChat}
        sendFriendRequest={sendFriendRequest}
        acceptFriendRequest={acceptFriendRequest}
      />

      {/* Main Chat Area */}
      {activeChat === "WORLD" ? (
        <div className="flex-1 flex flex-col h-full bg-slate-900/50 relative z-10">
          <div className="h-16 border-b border-white/10 flex items-center px-6 bg-slate-800/40 backdrop-blur-md">
            <h4 className="text-lg font-semibold text-slate-200">
              Kênh Chat Thế Giới 🌍
            </h4>
          </div>

          <div
            className="flex-1 overflow-y-auto p-4 md:p-6 custom-scrollbar scroll-smooth"
            ref={chatContainerRef}
            onScroll={handleScroll}
          >
            {isLoadingHistory && (
              <div className="text-center py-4">
                <span className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-slate-800/50 text-slate-400 text-sm border border-white/5">
                  <svg
                    className="animate-spin h-4 w-4 text-indigo-400"
                    viewBox="0 0 24 24"
                  >
                    <circle
                      className="opacity-25"
                      cx="12"
                      cy="12"
                      r="10"
                      stroke="currentColor"
                      strokeWidth="4"
                      fill="none"
                    ></circle>
                    <path
                      className="opacity-75"
                      fill="currentColor"
                      d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                    ></path>
                  </svg>
                  Loading older messages...
                </span>
              </div>
            )}

            <div className="flex flex-col gap-4">
              {messages.map((msg) => (
                <MessageItem
                  key={msg.id}
                  msg={msg}
                  username={username}
                  formatTime={formatTime}
                  onEdit={editMessage}
                  onRevoke={revokeMessage}
                />
              ))}
              <div ref={messageEndRef} />
            </div>
          </div>

          <div className="p-4 bg-slate-800/40 backdrop-blur-md border-t border-white/10">
            <div className="relative">
              {showEmojiPicker && (
                <div className="absolute bottom-full left-0 mb-2 p-2 bg-slate-800 border border-white/10 rounded-xl shadow-xl flex flex-wrap gap-1 max-w-[280px]">
                  {emojis.map((emoji) => (
                    <button
                      key={emoji}
                      onClick={() => addEmoji(emoji)}
                      className="w-10 h-10 flex items-center justify-center hover:bg-slate-700 rounded-lg text-xl transition-colors"
                    >
                      {emoji}
                    </button>
                  ))}
                </div>
              )}
              {canChat ? (
                <form onSubmit={sendMessage} className="flex items-center gap-2">
                  <button
                    type="button"
                    className="w-12 h-12 flex items-center justify-center rounded-xl bg-slate-800 hover:bg-slate-700 border border-white/10 text-xl transition-colors"
                    onClick={() => setShowEmojiPicker(!showEmojiPicker)}
                  >
                    😊
                  </button>
                  <input
                    type="file"
                    ref={fileInputRef}
                    className="hidden"
                    accept="image/*,video/*"
                    onChange={(e) => {
                      const file = e.target.files[0];
                      if (file) {
                        const type = file.type.startsWith("image/")
                          ? "IMAGE"
                          : "VIDEO";
                        uploadMedia(file, type);
                      }
                      e.target.value = null; // reset
                    }}
                  />
                  <button
                    type="button"
                    className="w-12 h-12 flex items-center justify-center rounded-xl bg-slate-800 hover:bg-slate-700 border border-white/10 text-xl transition-colors disabled:opacity-50"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={isUploading}
                    title="Attach Image or Video"
                  >
                    {isUploading ? (
                      <svg
                        className="animate-spin h-5 w-5 text-indigo-400"
                        viewBox="0 0 24 24"
                      >
                        <circle
                          className="opacity-25"
                          cx="12"
                          cy="12"
                          r="10"
                          stroke="currentColor"
                          strokeWidth="4"
                          fill="none"
                        ></circle>
                        <path
                          className="opacity-75"
                          fill="currentColor"
                          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                        ></path>
                      </svg>
                    ) : (
                      "📎"
                    )}
                  </button>
                  <input
                    type="text"
                    className="flex-1 h-12 bg-slate-900/50 border border-white/10 rounded-xl px-4 text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 transition-all"
                    placeholder="Type a message..."
                    value={message}
                    onChange={handleTyping}
                    maxLength={500}
                    disabled={isUploading}
                  />
                  <button
                    type="submit"
                    className="px-6 h-12 bg-indigo-600 hover:bg-indigo-500 text-white font-medium rounded-xl transition-all shadow-lg shadow-indigo-500/30 flex items-center gap-2 disabled:opacity-50"
                    disabled={!message.trim()}
                  >
                    Send
                  </button>
                </form>
              ) : (
                <div className="w-full text-center p-4 bg-rose-500/10 text-rose-400 rounded-xl border border-rose-500/20 animate-enter">
                  <strong>Thông báo:</strong> Bạn không có quyền gửi tin nhắn vào
                  Kênh Chat Thế Giới.
                </div>
              )}
            </div>
          </div>
        </div>
      ) : (
        <PrivateChat
          key={activeChat}
          currentUser={username}
          recipientUser={activeChat}
          userColor={userColor}
          stompClient={stompClient}
          onClose={() => closePrivateChat(activeChat)}
          registerPrivateMessageHandler={registerPrivateMessageHandler}
          unregisterPrivateMessageHandler={unregisterPrivateMessageHandler}
        />
      )}

      {isUnlockModalVisible && <KeyUnlockModal />}
    </div>
  );
};

export default Chat;
