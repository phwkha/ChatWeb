import React from 'react';
import { motion } from 'framer-motion';

const MessageItem = ({ message, isOwn }) => {
  return (
    <motion.div 
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className={`flex gap-4 max-w-[80%] ${isOwn ? 'ml-auto flex-row-reverse' : ''}`}
    >
      <div className={`w-8 h-8 rounded-full flex-shrink-0 ${isOwn ? 'bg-primary/20' : 'bg-white/10'}`}></div>
      <div className={`p-4 border ${isOwn 
        ? 'bg-primary/20 rounded-2xl rounded-tr-sm border-primary/30' 
        : 'bg-white/5 rounded-2xl rounded-tl-sm border-white/5'}`
      }>
        <p className={`text-sm ${isOwn ? 'text-white' : 'text-gray-200'}`}>
          {message.content}
        </p>
        <span className={`text-[10px] mt-2 block ${isOwn ? 'text-primary-light/70 text-right' : 'text-gray-500'}`}>
          {message.time || '12:00 PM'}
        </span>
      </div>
    </motion.div>
  );
};

export default MessageItem;
