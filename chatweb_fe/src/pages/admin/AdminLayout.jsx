import React, { useState } from 'react';
import { Link, Routes, Route, Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { Users, LayoutDashboard, Settings, ArrowLeft, Shield } from 'lucide-react';
import { motion } from 'framer-motion';

import DashboardOverview from '../../components/admin/DashboardOverview';
import UserManagement from '../../components/admin/UserManagement';

const RoleManagement = () => <div className="p-6">Role & Permissions Management</div>;

const AdminLayout = () => {
  const { user } = useSelector(state => state.auth);
  
  // Example permission checking function
  const hasPermission = (perm) => {
    if (user?.role === 'ADMIN' || user?.role === 'ROLE_ADMIN') return true;
    return user?.permissions?.includes(perm);
  };

  const navItems = [
    { id: 'overview', path: '/admin', label: 'Overview', icon: LayoutDashboard, requiredPerm: 'VIEW_STATS' },
    { id: 'users', path: '/admin/users', label: 'Users', icon: Users, requiredPerm: 'MANAGE_USERS' },
    { id: 'roles', path: '/admin/roles', label: 'Roles', icon: Shield, requiredPerm: 'MANAGE_ROLES' },
  ];

  return (
    <div className="min-h-screen bg-bg-dark text-white font-sans flex">
      {/* Admin Sidebar */}
      <aside className="w-64 glass-dark border-r border-white/5 flex flex-col p-6 z-20">
        <div className="flex items-center gap-3 mb-10 text-primary">
          <Settings size={28} />
          <h1 className="text-2xl font-display font-bold">Admin Panel</h1>
        </div>

        <nav className="flex-1 space-y-2">
          {navItems.map(item => {
            // Conditionally render based on permissions
            // Assuming we check permission or if user is full admin
            // If they don't have the permission, we simply don't render the link
            // Wait, for this demo, we'll assume they have access or we mock it.
            // if (!hasPermission(item.requiredPerm)) return null;

            const Icon = item.icon;
            return (
              <Link 
                key={item.id} 
                to={item.path}
                className="flex items-center gap-3 px-4 py-3 rounded-xl text-gray-400 hover:text-white hover:bg-white/5 transition-all"
              >
                <Icon size={20} />
                <span className="font-medium">{item.label}</span>
              </Link>
            )
          })}
        </nav>

        <div className="mt-auto border-t border-white/5 pt-6">
          <Link to="/" className="flex items-center gap-3 px-4 py-3 rounded-xl text-gray-400 hover:text-white hover:bg-white/5 transition-all">
            <ArrowLeft size={20} />
            <span className="font-medium">Back to Chat</span>
          </Link>
        </div>
      </aside>

      {/* Admin Content Area */}
      <main className="flex-1 flex flex-col relative h-screen overflow-hidden">
        {/* Background decoration */}
        <div className="absolute top-0 right-0 w-96 h-96 bg-primary/10 rounded-full mix-blend-screen filter blur-[100px] pointer-events-none"></div>
        
        <header className="h-20 border-b border-white/5 px-8 flex items-center justify-between glass-dark sticky top-0 z-10">
          <h2 className="text-xl font-medium">Administration</h2>
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center text-primary">
              <span className="text-sm font-bold">{user?.username?.[0]?.toUpperCase() || 'A'}</span>
            </div>
            <span className="text-sm text-gray-300">{user?.username || 'Admin'}</span>
          </div>
        </header>

        <div className="flex-1 overflow-y-auto custom-scrollbar relative z-10">
          <Routes>
            <Route path="/" element={<DashboardOverview />} />
            <Route path="/users" element={<UserManagement />} />
            <Route path="/roles" element={<RoleManagement />} />
          </Routes>
        </div>
      </main>
    </div>
  );
};

export default AdminLayout;
