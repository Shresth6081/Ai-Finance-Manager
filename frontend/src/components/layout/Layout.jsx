import React, { useState } from 'react';
import Sidebar from './Sidebar';
import { useAuth } from '../../context/AuthContext';
import { Navigate, Outlet } from 'react-router-dom';
import AIAssistant from '../ui/AIAssistant';

const Layout = () => {
    const { isAuthenticated, loading } = useAuth();
    const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

    if (loading) return <div className="flex items-center justify-center h-screen">Loading...</div>;
    if (!isAuthenticated) return <Navigate to="/login" replace />;

    return (
        <div className="flex bg-gray-50 min-h-screen">
            <Sidebar
                collapsed={sidebarCollapsed}
                onToggle={() => setSidebarCollapsed(prev => !prev)}
            />
            {/* Left margin matches sidebar width — transitions in sync */}
            <div
                className="flex-1 p-8 transition-all duration-300 ease-in-out"
                style={{ marginLeft: sidebarCollapsed ? '4rem' : '16rem' }}
            >
                <Outlet />
            </div>
            <AIAssistant />
        </div>
    );
};

export default Layout;
