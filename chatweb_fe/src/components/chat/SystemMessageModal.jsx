import React, { useState, useEffect } from 'react';
import { X, Send } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import apiClient from '../../services/apiClient';
import webSocketClient from '../../services/webSocketClient';

const SystemMessageModal = ({ isOpen, onClose, isAdmin }) => {
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);
  const [inputText, setInputText] = useState('');

  useEffect(() => {
    if (isOpen) {
      fetchMessages();
    }
  }, [isOpen]);

  const fetchMessages = async () => {
    setLoading(true);
    try {
      const res = await apiClient.get('/api/systems/message', { params: { size: 50 } });
      const data = res.data.data?.content || [];
      setMessages(data);
    } catch (error) {
      console.error("Failed to fetch system messages", error);
    } finally {
      setLoading(false);
    }
  };

  const handleSend = () => {
    if (!inputText.trim()) return;
    
    // Broadcast message via WS
    webSocketClient.sendMessage('/app/chat/sendMessageSystem', {
      content: inputText,
      survivalTime: null
    });
    
    setInputText('');
    // Optionally refetch immediately or rely on socket listener
    setTimeout(fetchMessages, 500); 
  };

  if (!isOpen) return null;

  return (
    <AnimatePresence>
      <motion.div 
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
      >
        <motion.div 
          initial={{ scale: 0.95, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          exit={{ scale: 0.95, opacity: 0 }}
          className="w-full max-w-lg bg-bg-dark border border-white/10 rounded-2xl shadow-2xl flex flex-col max-h-[80vh] overflow-hidden"
        >
          {/* Header */}
          <div className="p-4 border-b border-white/5 flex justify-between items-center bg-white/5">
            <h2 className="text-lg font-bold text-white">Lịch sử Thông báo</h2>
            <button onClick={onClose} className="p-2 text-gray-400 hover:text-white rounded-full transition-colors">
              <X size={20} />
            </button>
          </div>

          {/* Messages List */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4 custom-scrollbar">
            {loading ? (
              <div className="text-center text-gray-500 py-10">Đang tải...</div>
            ) : messages.length > 0 ? (
              messages.map((msg, idx) => (
                <div key={idx} className="bg-white/5 border border-white/10 rounded-xl p-4">
                  <div className="flex justify-between items-center mb-2">
                    <span className="text-sm font-bold text-primary-light">Hệ thống ({msg.sender})</span>
                    <span className="text-xs text-gray-500">
                      {new Date(msg.timestamp).toLocaleString()}
                    </span>
                  </div>
                  <p className="text-sm text-gray-200">{msg.content}</p>
                </div>
              ))
            ) : (
              <div className="text-center text-gray-500 py-10">Chưa có thông báo hệ thống nào.</div>
            )}
          </div>

          {/* Admin Input Area */}
          {isAdmin && (
            <div className="p-4 border-t border-white/5 bg-black/20 flex gap-2">
              <input 
                type="text" 
                value={inputText}
                onChange={(e) => setInputText(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSend()}
                placeholder="Nhập thông báo hệ thống..."
                className="flex-1 bg-white/5 border border-white/10 rounded-xl px-4 py-2 text-sm text-white focus:outline-none focus:border-primary/50 transition-colors"
              />
              <button 
                onClick={handleSend}
                className="px-4 py-2 bg-gradient-to-r from-primary to-secondary rounded-xl text-white hover:opacity-90 transition-opacity flex items-center justify-center"
              >
                <Send size={18} />
              </button>
            </div>
          )}
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
};

export default SystemMessageModal;
