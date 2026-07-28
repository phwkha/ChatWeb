import { useState, useRef, useEffect, memo } from 'react';
import { PencilSquareIcon, TrashIcon, CheckIcon, XMarkIcon } from '@heroicons/react/24/outline';

const MessageItem = memo(function MessageItem({ msg, username, formatTime, onEdit, onRevoke }) {
    const isOwn = msg.sender === username;
    const [isEditing, setIsEditing] = useState(false);
    const [editContent, setEditContent] = useState(msg.content);
    
    const handleEditSubmit = () => {
        if (editContent.trim() !== msg.content && editContent.trim() !== '') {
            onEdit?.(msg.id, editContent);
        }
        setIsEditing(false);
    };
    
    return (
        <div className="animate-enter flex flex-col mb-1 w-full">
            {msg.messageType === "JOIN" && (
                <div className="flex justify-center my-2">
                    <span className="bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 px-3 py-1 rounded-full text-xs font-medium">
                        {msg.sender} has joined the chat.
                    </span>
                </div>
            )}
            {msg.messageType === "LEAVE" && (
                <div className="flex justify-center my-2">
                    <span className="bg-rose-500/10 text-rose-400 border border-rose-500/20 px-3 py-1 rounded-full text-xs font-medium">
                        {msg.sender} has left the chat.
                    </span>
                </div>
            )}
            {["CHAT", "IMAGE", "VIDEO"].includes(msg.messageType) && (
                <div className={`flex w-full ${isOwn ? 'justify-end' : 'justify-start'}`}>
                    <div className={`flex flex-col max-w-[75%] sm:max-w-[65%] md:max-w-[55%] ${isOwn ? 'items-end' : 'items-start'}`}>
                        {!isOwn && (
                            <div className="flex items-center gap-2 mb-1 ml-1">
                                <span className="text-sm font-semibold" style={{ color: msg.color || '#818cf8' }}>
                                    {msg.sender}
                                </span>
                                <span className="text-xs text-slate-500">{formatTime(msg.timestamp)}</span>
                            </div>
                        )}

                        {isOwn && (
                            <div className="flex items-center gap-2 mb-1 mr-1">
                                <span className="text-xs text-slate-500">{formatTime(msg.timestamp)}</span>
                                {msg.isEdited && <span className="text-[10px] text-slate-400 italic">(edited)</span>}
                            </div>
                        )}
                        <div className="flex items-center gap-2 relative group w-full justify-end">
                            {isOwn && msg.messageType === "CHAT" && !msg.isDeleted && onEdit && onRevoke && !isEditing && (
                                <div className="hidden group-hover:flex items-center gap-1 absolute right-[calc(100%+8px)] bg-slate-800 rounded-lg p-1 border border-white/10 shadow-lg">
                                    <button onClick={() => setIsEditing(true)} className="p-1.5 text-slate-400 hover:text-indigo-400 hover:bg-white/5 rounded-md transition-colors" title="Edit">
                                        <PencilSquareIcon className="w-4 h-4" />
                                    </button>
                                    <button onClick={() => onRevoke(msg.id)} className="p-1.5 text-slate-400 hover:text-rose-400 hover:bg-white/5 rounded-md transition-colors" title="Revoke">
                                        <TrashIcon className="w-4 h-4" />
                                    </button>
                                </div>
                            )}

                            <div 
                                className={`px-4 py-2.5 rounded-2xl shadow-sm overflow-hidden ${
                                    isOwn 
                                        ? 'bg-indigo-600 text-white rounded-tr-sm' 
                                        : 'bg-slate-800 border border-white/5 text-slate-200 rounded-tl-sm'
                                } ${msg.messageType !== "CHAT" ? 'p-1' : ''} ${msg.isDeleted ? 'bg-transparent border border-dashed border-slate-600 text-slate-500' : ''}`}
                            >
                                {msg.isDeleted ? (
                                    <p className="italic text-sm">Message revoked</p>
                                ) : (
                                    <>
                                        {msg.messageType === "CHAT" && !isEditing && (
                                            <p className="whitespace-pre-wrap break-words text-[15px] leading-relaxed">
                                                {msg.content}
                                            </p>
                                        )}
                                        {msg.messageType === "CHAT" && isEditing && (
                                            <div className="flex flex-col gap-2 min-w-[200px]">
                                                <textarea 
                                                    value={editContent}
                                                    onChange={(e) => setEditContent(e.target.value)}
                                                    className="w-full bg-black/20 text-white rounded p-2 text-sm outline-none resize-none"
                                                    rows="2"
                                                    autoFocus
                                                />
                                                <div className="flex justify-end gap-1">
                                                    <button onClick={() => { setIsEditing(false); setEditContent(msg.content); }} className="p-1 text-slate-300 hover:bg-white/10 rounded">
                                                        <XMarkIcon className="w-4 h-4" />
                                                    </button>
                                                    <button onClick={handleEditSubmit} className="p-1 text-emerald-400 hover:bg-emerald-400/20 rounded">
                                                        <CheckIcon className="w-4 h-4" />
                                                    </button>
                                                </div>
                                            </div>
                                        )}
                                        {msg.messageType === "IMAGE" && (
                                            <img 
                                                src={msg.content} 
                                                alt="Chat attachment" 
                                                className="max-w-full rounded-xl cursor-pointer hover:opacity-90 transition-opacity"
                                                onClick={() => window.open(msg.content, '_blank')}
                                            />
                                        )}
                                        {msg.messageType === "VIDEO" && (
                                            <video 
                                                src={msg.content} 
                                                controls 
                                                className="max-w-full rounded-xl"
                                                preload="metadata"
                                            />
                                        )}
                                    </>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
});

export default MessageItem;
