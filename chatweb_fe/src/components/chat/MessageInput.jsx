import React, { useState, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Paperclip, Send, X, Image as ImageIcon, Video } from 'lucide-react';

const MessageInput = ({ onSend }) => {
  const { t } = useTranslation();
  const [text, setText] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);
  const fileInputRef = useRef(null);

  const handleSend = () => {
    if (text.trim() || selectedFile) {
      onSend(text, selectedFile);
      setText('');
      setSelectedFile(null);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      // Check if it's image or video
      if (file.type.startsWith('image/') || file.type.startsWith('video/')) {
        setSelectedFile(file);
      } else {
        alert("Only image and video files are supported.");
      }
    }
    // Reset the input value so the same file can be selected again if needed
    e.target.value = null;
  };

  return (
    <footer className="p-4 border-t border-white/5 glass-dark backdrop-blur-md flex flex-col gap-2">
      {selectedFile && (
        <div className="flex items-center gap-2 bg-white/10 w-fit px-3 py-1.5 rounded-lg">
          {selectedFile.type.startsWith('video/') ? <Video size={16} className="text-primary-light" /> : <ImageIcon size={16} className="text-primary-light" />}
          <span className="text-xs text-white truncate max-w-[200px]">{selectedFile.name}</span>
          <button onClick={() => setSelectedFile(null)} className="text-gray-400 hover:text-red-400 ml-1">
            <X size={14} />
          </button>
        </div>
      )}
      
      <div className="flex items-center gap-2 bg-black/20 border border-white/10 rounded-full p-2 pr-4">
        <input 
          type="file" 
          ref={fileInputRef} 
          className="hidden" 
          accept="image/*,video/*"
          onChange={handleFileChange}
        />
        <button 
          onClick={() => fileInputRef.current.click()}
          className="w-10 h-10 rounded-full hover:bg-white/10 flex items-center justify-center text-gray-400 transition-colors cursor-pointer"
        >
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
