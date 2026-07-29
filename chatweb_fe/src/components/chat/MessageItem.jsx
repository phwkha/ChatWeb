import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Smile, Edit2, Trash2, Check, X } from 'lucide-react';

const MessageItem = ({ message, isOwn, onReaction, onEdit, onRevoke }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [editContent, setEditContent] = useState(message.content || '');
  const [showActions, setShowActions] = useState(false);
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);

  const emojis = ['LIKE', 'HEART', 'LAUGH', 'SAD', 'ANGRY'];
  const emojiMap = {
    'LIKE': '👍',
    'HEART': '❤️',
    'LAUGH': '😂',
    'SAD': '😢',
    'ANGRY': '😡'
  };

  const handleEditSubmit = () => {
    if (editContent.trim() && editContent !== message.content) {
      onEdit(message.id, editContent);
    }
    setIsEditing(false);
  };

  const renderMedia = () => {
    if (message.contentType === 'IMAGE' && message.fileUrl) {
      return (
        <img 
          src={message.fileUrl} 
          alt="attachment" 
          className={`max-w-full rounded-lg mb-2 cursor-pointer hover:opacity-90 transition-opacity ${message.isDeleted ? 'opacity-30' : ''}`}
          style={{ maxHeight: '300px', objectFit: 'contain' }}
        />
      );
    }
    if (message.contentType === 'VIDEO' && message.fileUrl) {
      return (
        <video 
          src={message.fileUrl} 
          controls 
          className={`max-w-full rounded-lg mb-2 ${message.isDeleted ? 'opacity-30' : ''}`}
          style={{ maxHeight: '300px' }}
        />
      );
    }
    return null;
  };

  return (
    <motion.div 
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className={`flex gap-4 max-w-[80%] ${isOwn ? 'ml-auto flex-row-reverse' : ''}`}
      onMouseEnter={() => setShowActions(true)}
      onMouseLeave={() => { setShowActions(false); setShowEmojiPicker(false); }}
    >
      <div className={`w-8 h-8 rounded-full flex-shrink-0 ${isOwn ? 'bg-primary/20' : 'bg-white/10'}`}></div>
      
      <div className="relative group flex flex-col">
        {/* Message Bubble */}
        <div className={`p-4 border relative ${isOwn 
          ? 'bg-primary/20 rounded-2xl rounded-tr-sm border-primary/30' 
          : 'bg-white/5 rounded-2xl rounded-tl-sm border-white/5'}`
        }>
          {renderMedia()}
          
          {message.isDeleted ? (
            <p className={`text-sm italic text-gray-500`}>
              Message revoked
            </p>
          ) : isEditing ? (
            <div className="flex flex-col gap-2">
              <input 
                type="text" 
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleEditSubmit()}
                className="bg-black/20 border border-white/10 rounded px-2 py-1 text-sm text-white focus:outline-none"
                autoFocus
              />
              <div className="flex gap-2 justify-end">
                <button onClick={() => setIsEditing(false)} className="text-gray-400 hover:text-white"><X size={14}/></button>
                <button onClick={handleEditSubmit} className="text-green-400 hover:text-green-300"><Check size={14}/></button>
              </div>
            </div>
          ) : (
            <p className={`text-sm ${isOwn ? 'text-white' : 'text-gray-200'}`}>
              {message.content}
            </p>
          )}

          <div className="flex justify-between items-center mt-2 gap-2">
            <span className={`text-[10px] ${isOwn ? 'text-primary-light/70' : 'text-gray-500'}`}>
              {message.time || '12:00 PM'}
              {message.isEdited && !message.isDeleted && <span className="ml-1 italic">(edited)</span>}
            </span>
          </div>

          {/* Reactions Display */}
          {message.reactions && Object.keys(message.reactions).length > 0 && !message.isDeleted && (
             <div className={`absolute -bottom-3 ${isOwn ? 'left-2' : 'right-2'} bg-bg-dark border border-white/10 rounded-full px-2 py-0.5 text-xs flex gap-1 shadow-lg`}>
                {Object.values(message.reactions).map((type, i) => (
                  <span key={i}>{emojiMap[type] || '👍'}</span>
                ))}
             </div>
          )}
        </div>

        {/* Floating Actions Menu */}
        {showActions && !message.isDeleted && (
          <div className={`absolute top-0 ${isOwn ? '-left-20' : '-right-10'} flex items-center gap-1 bg-black/40 backdrop-blur-md rounded-lg p-1 border border-white/5 opacity-0 group-hover:opacity-100 transition-opacity`}>
            {/* Reaction Button */}
            {!isOwn && (
              <div className="relative">
                <button onClick={() => setShowEmojiPicker(!showEmojiPicker)} className="p-1.5 text-gray-400 hover:text-white hover:bg-white/10 rounded-md transition-colors">
                  <Smile size={14} />
                </button>
                {showEmojiPicker && (
                  <div className="absolute top-8 left-0 flex bg-bg-dark border border-white/10 rounded-lg p-1 shadow-xl z-50">
                    {emojis.map(e => (
                      <button 
                        key={e}
                        onClick={() => { onReaction(message.id, e); setShowEmojiPicker(false); }}
                        className="p-1 hover:bg-white/10 rounded text-lg transition-transform hover:scale-125"
                      >
                        {emojiMap[e]}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            )}
            
            {/* Edit & Revoke Buttons */}
            {isOwn && (
              <>
                <button 
                  onClick={() => setIsEditing(true)} 
                  className="p-1.5 text-gray-400 hover:text-white hover:bg-white/10 rounded-md transition-colors"
                  title="Edit"
                >
                  <Edit2 size={14} />
                </button>
                <button 
                  onClick={() => onRevoke(message.id)} 
                  className="p-1.5 text-gray-400 hover:text-red-400 hover:bg-red-500/10 rounded-md transition-colors"
                  title="Revoke"
                >
                  <Trash2 size={14} />
                </button>
              </>
            )}
          </div>
        )}
      </div>
    </motion.div>
  );
};

export default MessageItem;
