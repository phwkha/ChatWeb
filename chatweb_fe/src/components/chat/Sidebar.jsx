import React from 'react';
import { useTranslation } from 'react-i18next';
import { User, LogOut, Settings, Users, MessageSquare } from 'lucide-react';
import { motion } from 'framer-motion';

const Sidebar = ({ activeTab, setActiveTab, handleLogout, contacts, activeChat, setActiveChat, onLoadMoreContacts, hasMoreContacts }) => {
  const { t } = useTranslation();

  return (
    <>
      {/* Slim Navigation */}
      <nav className="w-20 glass-dark border-r border-white/5 flex flex-col items-center py-6 gap-8 z-20">
        <div className="w-12 h-12 bg-primary rounded-xl flex items-center justify-center font-display font-bold text-xl shadow-lg shadow-primary/20 text-white">
          CW
        </div>
        
        <div className="flex-1 flex flex-col items-center gap-6 w-full">
          <button 
            onClick={() => setActiveTab('chats')}
            className={`w-12 h-12 rounded-xl flex items-center justify-center transition-all ${activeTab === 'chats' ? 'bg-white/10 text-primary' : 'text-gray-400 hover:text-white hover:bg-white/5'}`}
          >
            <MessageSquare size={24} />
          </button>
          
          <button 
            onClick={() => setActiveTab('friends')}
            className={`w-12 h-12 rounded-xl flex items-center justify-center transition-all ${activeTab === 'friends' ? 'bg-white/10 text-primary' : 'text-gray-400 hover:text-white hover:bg-white/5'}`}
          >
            <Users size={24} />
          </button>
        </div>
        
        <div className="flex flex-col items-center gap-4">
          <button className="w-12 h-12 rounded-xl flex items-center justify-center text-gray-400 hover:text-white hover:bg-white/5 transition-all">
            <Settings size={24} />
          </button>
          <button 
            onClick={handleLogout}
            className="w-12 h-12 rounded-xl flex items-center justify-center text-red-400 hover:text-red-300 hover:bg-red-500/10 transition-all"
          >
            <LogOut size={24} />
          </button>
        </div>
      </nav>

      {/* List Panel */}
      <aside className="w-80 glass border-r border-white/5 flex flex-col z-10 relative">
        <div className="p-6 border-b border-white/5 text-white">
          <h2 className="text-2xl font-display font-bold">
            {activeTab === 'chats' ? 'Messages' : 'Friends'}
          </h2>
          <div className="mt-4 relative">
            <input 
              type="text" 
              placeholder={t('chat.search')}
              className="w-full bg-black/20 border border-white/5 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all"
            />
          </div>
        </div>
        
        <div 
          className="flex-1 overflow-y-auto custom-scrollbar p-4 space-y-2"
          onScroll={(e) => {
            const { scrollTop, scrollHeight, clientHeight } = e.target;
            if (scrollHeight - scrollTop <= clientHeight + 10 && hasMoreContacts) {
              onLoadMoreContacts();
            }
          }}
        >
          {contacts.map((contact, idx) => {
            const isSelected = activeChat === contact.username;
            return (
              <motion.div 
                key={contact.username || idx}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: idx * 0.05 }}
                onClick={() => setActiveChat(contact.username)}
                className={`flex items-center gap-4 p-3 rounded-xl cursor-pointer transition-all group ${isSelected ? 'bg-white/10' : 'hover:bg-white/5'}`}
              >
                <div className="relative">
                  <div className="w-12 h-12 rounded-full bg-gradient-to-tr from-primary to-secondary p-[2px]">
                    <div className="w-full h-full bg-bg-dark rounded-full flex items-center justify-center overflow-hidden">
                      {contact.avatar ? (
                        <img src={contact.avatar} alt="Avatar" className="w-full h-full object-cover" />
                      ) : (
                        <User size={20} className="text-gray-400" />
                      )}
                    </div>
                  </div>
                  {contact.isOnline && (
                    <div className="absolute bottom-0 right-0 w-3.5 h-3.5 bg-green-500 border-2 border-bg-dark rounded-full"></div>
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className={`font-medium truncate transition-colors ${isSelected ? 'text-primary-light' : 'text-white group-hover:text-primary-light'}`}>
                    {contact.firstName && contact.lastName ? `${contact.firstName} ${contact.lastName}` : contact.username}
                  </h3>
                  <p className="text-sm text-gray-400 truncate">{contact.lastMessage || '...'}</p>
                </div>
              </motion.div>
            );
          })}
          
          {hasMoreContacts && (
            <div className="py-2 text-center text-xs text-gray-500">
              Loading more...
            </div>
          )}
          
          {!hasMoreContacts && contacts.length === 0 && (
            <div className="p-4 text-center text-sm text-gray-500">
              No contacts found.
            </div>
          )}
        </div>
      </aside>
    </>
  );
};

export default Sidebar;
