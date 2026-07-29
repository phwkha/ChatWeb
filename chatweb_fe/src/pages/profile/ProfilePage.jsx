import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { User, Shield, MapPin, ArrowLeft } from 'lucide-react';
import GeneralSettings from '../../components/profile/GeneralSettings';
import SecuritySettings from '../../components/profile/SecuritySettings';
import AddressManager from '../../components/profile/AddressManager';

const ProfilePage = () => {
  const [activeTab, setActiveTab] = useState('general');

  const tabs = [
    { id: 'general', label: 'General', icon: User },
    { id: 'security', label: 'Security', icon: Shield },
    { id: 'addresses', label: 'Addresses', icon: MapPin },
  ];

  const renderContent = () => {
    switch (activeTab) {
      case 'general':
        return <GeneralSettings />;
      case 'security':
        return <SecuritySettings />;
      case 'addresses':
        return <AddressManager />;
      default:
        return <GeneralSettings />;
    }
  };

  return (
    <div className="min-h-screen bg-bg-dark text-white font-sans p-8">
      {/* Background decoration */}
      <div className="fixed top-0 left-0 w-full h-96 bg-gradient-to-b from-primary/10 to-transparent pointer-events-none"></div>

      <div className="max-w-6xl mx-auto relative z-10">
        
        <div className="mb-8 flex items-center gap-4">
          <Link to="/" className="w-10 h-10 rounded-full bg-white/5 border border-white/10 flex items-center justify-center text-gray-400 hover:text-white hover:bg-white/10 transition-colors">
            <ArrowLeft size={20} />
          </Link>
          <h1 className="text-3xl font-display font-bold">Account Settings</h1>
        </div>

        <div className="flex flex-col md:flex-row gap-8">
          {/* Navigation Sidebar */}
          <aside className="w-full md:w-64 flex-shrink-0">
            <div className="glass-dark rounded-2xl p-3 space-y-2 border border-white/5">
              {tabs.map(tab => {
                const Icon = tab.icon;
                const isActive = activeTab === tab.id;
                return (
                  <button
                    key={tab.id}
                    onClick={() => setActiveTab(tab.id)}
                    className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl transition-all ${
                      isActive 
                        ? 'bg-primary/20 text-primary border border-primary/30' 
                        : 'text-gray-400 hover:text-white hover:bg-white/5 border border-transparent'
                    }`}
                  >
                    <Icon size={20} />
                    <span className="font-medium">{tab.label}</span>
                  </button>
                );
              })}
            </div>
          </aside>

          {/* Content Area */}
          <main className="flex-1 glass p-8 rounded-3xl border border-white/5 min-h-[600px]">
            {renderContent()}
          </main>
        </div>
        
      </div>
    </div>
  );
};

export default ProfilePage;
