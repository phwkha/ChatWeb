import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { CheckCircle2, AlertCircle, Info, AlertTriangle, X } from 'lucide-react';
import { subscribeToToasts } from '../../utils/toast';

const icons = {
  success: <CheckCircle2 className="text-green-400" size={24} />,
  error: <AlertCircle className="text-red-400" size={24} />,
  info: <Info className="text-blue-400" size={24} />,
  warning: <AlertTriangle className="text-yellow-400" size={24} />
};

const bgColors = {
  success: 'bg-green-500/10 border-green-500/20',
  error: 'bg-red-500/10 border-red-500/20',
  info: 'bg-blue-500/10 border-blue-500/20',
  warning: 'bg-yellow-500/10 border-yellow-500/20'
};

const ToastContainer = () => {
  const [toasts, setToasts] = useState([]);

  useEffect(() => {
    const unsubscribe = subscribeToToasts((toast) => {
      setToasts(prev => [...prev, toast]);
      
      // Auto dismiss after 5s
      setTimeout(() => {
        removeToast(toast.id);
      }, 5000);
    });
    return unsubscribe;
  }, []);

  const removeToast = (id) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  };

  return (
    <div className="fixed bottom-6 right-6 z-50 flex flex-col gap-3 pointer-events-none">
      <AnimatePresence>
        {toasts.map(toast => (
          <motion.div
            key={toast.id}
            initial={{ opacity: 0, y: 50, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, scale: 0.9, transition: { duration: 0.2 } }}
            className={`pointer-events-auto flex items-start gap-3 p-4 glass-dark backdrop-blur-xl border rounded-2xl shadow-2xl min-w-[300px] max-w-sm ${bgColors[toast.type]}`}
          >
            <div className="flex-shrink-0 mt-0.5">
              {icons[toast.type]}
            </div>
            <div className="flex-1">
              <p className="text-white text-sm font-medium leading-relaxed">{toast.message}</p>
            </div>
            <button 
              onClick={() => removeToast(toast.id)}
              className="flex-shrink-0 text-gray-400 hover:text-white transition-colors"
            >
              <X size={18} />
            </button>
          </motion.div>
        ))}
      </AnimatePresence>
    </div>
  );
};

export default ToastContainer;
