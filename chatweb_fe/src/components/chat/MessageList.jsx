import React, { useRef, useEffect } from 'react';
import MessageItem from './MessageItem';

const MessageList = ({ messages, onLoadMore, hasMore, onReaction, onEdit, onRevoke }) => {
  const containerRef = useRef(null);

  const handleScroll = (e) => {
    if (e.target.scrollTop === 0 && hasMore) {
      onLoadMore();
    }
  };

  // Auto-scroll to bottom on initial load or new messages if we are near bottom
  useEffect(() => {
    if (containerRef.current) {
      // Just a simple scroll to bottom for now
      // A more robust implementation would preserve scroll position when loading older messages
      // containerRef.current.scrollTop = containerRef.current.scrollHeight;
    }
  }, [messages]);

  return (
    <div 
      ref={containerRef}
      onScroll={handleScroll}
      className="flex-1 overflow-y-auto custom-scrollbar p-6 space-y-6 flex flex-col"
    >
      {hasMore && (
        <div className="text-center text-xs text-gray-500 py-2">
          Loading older messages...
        </div>
      )}
      {messages && messages.length > 0 ? (
        messages.map((msg, idx) => (
          <MessageItem key={msg.id || idx} message={msg} isOwn={msg.isOwn} onReaction={onReaction} onEdit={onEdit} onRevoke={onRevoke} />
        ))
      ) : (
        <div className="h-full flex items-center justify-center text-gray-500 text-sm flex-1">
          No messages yet. Say hello!
        </div>
      )}
    </div>
  );
};

export default MessageList;
