import { useDirectMessage } from '../../hooks/useDirectMessage';
import { useState } from 'react';

const REACTION_EMOJIS = {
    LIKE: "👍",
    HEART: "❤️",
    LAUGH: "😂",
    SAD: "😢",
    ANGRY: "😡"
};

const PrivateChat = ({
    currentUser,
    recipientUser,
    userColor,
    stompClient,
    onClose,
    registerPrivateMessageHandler,
    unregisterPrivateMessageHandler
}) => {
    const {
        messages,
        message, setMessage,
        decryptedContent,
        loading,
        error,
        recipientPublicKey,
        isLoadingMore,
        chatContainerRef,
        messagesEndRef,
        handleScroll,
        sendPrivateMessage,
        editMessage,
        revokeMessage,
        reactMessage
    } = useDirectMessage({
        currentUser,
        recipientUser,
        userColor,
        stompClient,
        registerPrivateMessageHandler,
        unregisterPrivateMessageHandler
    });

    const formatTime = (timestamp) => {
        return new Date(timestamp).toLocaleTimeString('vi-VN', {
            hour12: false, hour: '2-digit', minute: '2-digit'
        });
    };

    if (loading) {
        return (
            <div className="flex-1 flex flex-col h-full bg-slate-900/50 relative z-10">
                <div className="h-16 border-b border-white/10 flex items-center justify-between px-6 bg-slate-800/40 backdrop-blur-md">
                    <h3 className="font-semibold text-slate-200">{recipientUser}</h3>
                    <button onClick={onClose} className="text-slate-400 hover:text-white transition-colors">&times;</button>
                </div>
                <div className="flex-1 flex items-center justify-center">
                    <p className="text-sm text-slate-400">Loading keys and history...</p>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="w-80 h-96 bg-slate-800/90 backdrop-blur-md rounded-t-xl shadow-[0_-10px_40px_rgba(0,0,0,0.5)] border border-white/10 flex flex-col pointer-events-auto">
                <div className="h-12 border-b border-white/10 flex items-center justify-between px-4 bg-slate-800/50 rounded-t-xl">
                    <h3 className="font-semibold text-slate-200">{recipientUser}</h3>
                    <button onClick={onClose} className="text-slate-400 hover:text-white transition-colors">&times;</button>
                </div>
                <div className="flex-1 flex items-center justify-center p-4 text-center">
                    <p className="text-sm text-rose-400 font-medium">Error: {error}</p>
                </div>
            </div>
        );
    }

    return (
        <div className="flex-1 flex flex-col h-full bg-slate-900/50 relative z-10">
            <div className="h-16 border-b border-white/10 flex items-center justify-between px-6 bg-slate-800/40 backdrop-blur-md">
                <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-indigo-500/20 flex items-center justify-center text-indigo-400 font-bold">
                        {recipientUser.charAt(0).toUpperCase()}
                    </div>
                    <div>
                        <h3 className="font-semibold text-slate-200 text-lg leading-tight">{recipientUser}</h3>
                        <div className="flex items-center gap-1.5 text-xs text-emerald-400 mt-0.5">
                            <div className="w-1.5 h-1.5 rounded-full bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.8)]"></div>
                            E2E Encrypted
                        </div>
                    </div>
                </div>
                <button onClick={onClose} className="text-slate-400 hover:text-white transition-colors bg-slate-800 p-2 rounded-xl border border-white/10">Đóng</button>
            </div>
            
            <div
                className="flex-1 overflow-y-auto p-4 md:p-6 custom-scrollbar scroll-smooth"
                ref={chatContainerRef}
                onScroll={handleScroll}
            >
                {isLoadingMore && (
                    <div className="flex justify-center my-2">
                        <span className="inline-flex h-1.5 w-1.5 rounded-full bg-indigo-400 animate-ping"></span>
                    </div>
                )}
                {messages.length === 0 ? (
                    <div className="h-full flex flex-col items-center justify-center text-center opacity-70">
                        <div className="w-12 h-12 rounded-full bg-indigo-500/20 flex items-center justify-center mb-3">
                            <svg className="w-6 h-6 text-indigo-400" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8V7a4 4 0 00-8 0v4h8z" /></svg>
                        </div>
                        <p className="text-xs text-slate-400 px-4 leading-relaxed">
                            End-to-End Encrypted.<br/>Only you and {recipientUser} can read these messages.
                        </p>
                    </div>
                ) : (
                    <div className="flex flex-col gap-3">
                        {messages.map((msg) => {
                            const displayContent = decryptedContent.get(msg.id) || "Decrypting...";
                            const isOwn = msg.sender === currentUser;
                            return (
                            <div key={msg.id} className={`flex w-full mb-1 ${isOwn ? 'justify-end' : 'justify-start'}`}>
                                <div className={`flex flex-col max-w-[85%] ${isOwn ? 'items-end' : 'items-start'}`}>
                                    {isOwn && (
                                        <div className="flex items-center gap-2 mb-1 mr-1">
                                            <span className="text-[10px] text-slate-500">{formatTime(msg.timestamp)}</span>
                                            {msg.isEdited && <span className="text-[9px] text-slate-400 italic">(edited)</span>}
                                        </div>
                                    )}
                                    {!isOwn && (
                                        <div className="flex items-center gap-2 mb-1 ml-1">
                                            <span className="text-[10px] text-slate-500">{formatTime(msg.timestamp)}</span>
                                        </div>
                                    )}
                                    
                                    <div className={`flex items-center gap-2 relative group w-full ${isOwn ? 'justify-end' : 'justify-start'}`}>
                                        {isOwn && !msg.isDeleted && (
                                            <div className="hidden group-hover:flex items-center gap-1 absolute right-[calc(100%+8px)] bg-slate-800 rounded-lg p-1 border border-white/10 shadow-lg z-10">
                                                <button onClick={() => {
                                                    const newContent = prompt("Edit message:", displayContent);
                                                    if (newContent && newContent !== displayContent) {
                                                        editMessage(msg.id, newContent);
                                                    }
                                                }} className="p-1 text-slate-400 hover:text-indigo-400 hover:bg-white/5 rounded-md transition-colors" title="Edit">
                                                    <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                                    </svg>
                                                </button>
                                                <button onClick={() => {
                                                    if(window.confirm("Revoke this message?")) revokeMessage(msg.id);
                                                }} className="p-1 text-slate-400 hover:text-rose-400 hover:bg-white/5 rounded-md transition-colors" title="Revoke">
                                                    <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                                    </svg>
                                                </button>
                                            </div>
                                        )}
                                        
                                        {!isOwn && !msg.isDeleted && (
                                            <div className="hidden group-hover:flex items-center gap-1 absolute left-[calc(100%+8px)] bg-slate-800 rounded-lg p-1 border border-white/10 shadow-lg z-10">
                                                {Object.entries(REACTION_EMOJIS).map(([type, emoji]) => (
                                                    <button key={type} onClick={() => reactMessage(msg.id, type)} className="p-1 hover:bg-white/5 rounded-md text-sm transition-transform hover:scale-125" title={type}>
                                                        {emoji}
                                                    </button>
                                                ))}
                                            </div>
                                        )}

                                        {isOwn && !msg.isDeleted && (
                                            <div className="hidden group-hover:flex items-center gap-1 absolute right-[calc(100%+60px)] bg-slate-800 rounded-lg p-1 border border-white/10 shadow-lg z-10">
                                                {Object.entries(REACTION_EMOJIS).map(([type, emoji]) => (
                                                    <button key={type} onClick={() => reactMessage(msg.id, type)} className="p-1 hover:bg-white/5 rounded-md text-sm transition-transform hover:scale-125" title={type}>
                                                        {emoji}
                                                    </button>
                                                ))}
                                            </div>
                                        )}
                                        <div 
                                            className={`relative px-3 py-2 rounded-2xl shadow-sm text-[13px] leading-relaxed break-words whitespace-pre-wrap ${
                                                isOwn 
                                                    ? 'bg-indigo-600 text-white rounded-tr-sm' 
                                                    : 'bg-slate-700 border border-white/5 text-slate-200 rounded-tl-sm'
                                            } ${msg.isDeleted ? 'bg-transparent border border-dashed border-slate-600 text-slate-500' : ''}`}
                                        >
                                            {msg.isDeleted ? <i className="text-xs">Message revoked</i> : displayContent}
                                            
                                            {msg.reactions && Object.keys(msg.reactions).length > 0 && !msg.isDeleted && (
                                                <div className={`absolute -bottom-2 ${isOwn ? 'right-0' : 'left-0'} flex gap-0.5 bg-slate-800 rounded-full px-1.5 py-0.5 border border-slate-700 shadow-sm z-10 text-[10px]`}>
                                                    {Array.from(new Set(Object.values(msg.reactions))).map(type => (
                                                        <span key={type} className="flex items-center">
                                                            {REACTION_EMOJIS[type]}
                                                        </span>
                                                    ))}
                                                    {Object.keys(msg.reactions).length > 1 && (
                                                        <span className="text-slate-400 font-medium ml-0.5">{Object.keys(msg.reactions).length}</span>
                                                    )}
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        );})}
                        <div ref={messagesEndRef}></div>
                    </div>
                )}
            </div>
            
            <div className="p-4 bg-slate-800/40 backdrop-blur-md border-t border-white/10">
                {error && <div className="text-xs text-rose-400 mb-2 px-2">{error}</div>}
                
                <form onSubmit={sendPrivateMessage} className="flex gap-2">
                    <input
                        type="text"
                        className="flex-1 h-12 bg-slate-900/50 border border-white/10 rounded-xl px-4 text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 transition-all text-sm"
                        placeholder="Tin nhắn mật..."
                        value={message}
                        onChange={(e) => setMessage(e.target.value)}
                        maxLength={500}
                        disabled={!recipientPublicKey}
                    />
                    <button 
                        type="submit" 
                        disabled={!message.trim() || !recipientPublicKey}
                        className="px-6 h-12 bg-indigo-600 hover:bg-indigo-500 disabled:bg-slate-700 disabled:text-slate-500 text-white font-medium rounded-xl transition-all shadow-lg flex items-center justify-center"
                    >
                        Send
                    </button>
                </form>
            </div>
        </div>
    );
};

export default PrivateChat;