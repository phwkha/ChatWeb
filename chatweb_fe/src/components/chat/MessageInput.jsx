import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Paperclip, Send } from 'lucide-react';

const MessageInput = ({ onSend }) => {
  const { t } = useTranslation();
  const [text, setText] = useState('');

  const handleSend = () => {
    if (text.trim()) {
      onSend(text);
      setText('');
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <footer className="p-4 border-t border-white/5 glass-dark backdrop-blur-md">
      <div className="flex items-center gap-2 bg-black/20 border border-white/10 rounded-full p-2 pr-4">
        <button className="w-10 h-10 rounded-full hover:bg-white/10 flex items-center justify-center text-gray-400 transition-colors cursor-pointer">
          <Paperclip size={20} />
        </button>
        <input 
          type="text" 
          placeholder={t('chat.typeMessage')}
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={handleKeyDown}
          className="flex-1 bg-transparent border-none focus:outline-none text-white text-sm"
        />
        <button 
          onClick={handleSend}
          className="w-10 h-10 rounded-full bg-gradient-to-r from-primary to-secondary flex items-center justify-center text-white shadow-lg shadow-primary/30 hover:scale-105 transition-transform"
        >
          <Send size={18} className="ml-1" />
        </button>
      </div>
    </footer>
  );
};

export default MessageInput;
