import React, { useState, useEffect } from 'react';
import { Users, Activity, MessageSquare } from 'lucide-react';
import { motion } from 'framer-motion';
import apiClient from '../../services/apiClient';

const DashboardOverview = () => {
  const [stats, setStats] = useState({
    totalUsers: 0,
    onlineUsers: 0,
    messagesSent: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const [onlineRes, usersRes] = await Promise.all([
          apiClient.get('/api/admin/online'),
          apiClient.get('/api/admin/users')
        ]);
        
        const onlineCount = Array.isArray(onlineRes.data.data) ? onlineRes.data.data.length : 0;
        const totalUsersCount = Array.isArray(usersRes.data.data) ? usersRes.data.data.length : 0;
        
        setStats({ totalUsers: totalUsersCount, onlineUsers: onlineCount, messagesSent: 0 }); 
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    };
    fetchStats();
  }, []);

  const statCards = [
    { title: 'Total Users', value: stats.totalUsers, icon: Users, color: 'from-blue-500 to-cyan-500' },
    { title: 'Online Users', value: stats.onlineUsers, icon: Activity, color: 'from-green-500 to-emerald-500' },
    { title: 'Messages Sent', value: stats.messagesSent, icon: MessageSquare, color: 'from-primary to-secondary' },
  ];

  if (loading) return <div className="p-8 text-gray-400">Loading dashboard...</div>;

  return (
    <div className="p-8">
      <div className="mb-8">
        <h2 className="text-2xl font-display font-bold text-white">Dashboard Overview</h2>
        <p className="text-gray-400 text-sm mt-1">Here is what's happening with ChatWeb today.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        {statCards.map((card, idx) => {
          const Icon = card.icon;
          return (
            <motion.div 
              key={idx}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: idx * 0.1 }}
              className="glass p-6 rounded-3xl border border-white/5 relative overflow-hidden group"
            >
              <div className={`absolute -right-6 -top-6 w-24 h-24 bg-gradient-to-br ${card.color} rounded-full opacity-20 group-hover:scale-150 transition-transform duration-500`}></div>
              
              <div className="flex justify-between items-start relative z-10">
                <div>
                  <p className="text-gray-400 text-sm font-medium mb-1">{card.title}</p>
                  <h3 className="text-4xl font-display font-bold text-white">{card.value}</h3>
                </div>
                <div className={`w-12 h-12 rounded-xl flex items-center justify-center bg-gradient-to-br ${card.color} text-white shadow-lg`}>
                  <Icon size={24} />
                </div>
              </div>
            </motion.div>
          );
        })}
      </div>
      
      {/* Additional charts or tables could go here */}
      <div className="glass p-6 rounded-3xl border border-white/5 min-h-[300px] flex items-center justify-center text-gray-500">
        Chart Placeholder (e.g. Activity over time)
      </div>
    </div>
  );
};

export default DashboardOverview;
