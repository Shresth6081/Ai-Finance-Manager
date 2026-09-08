import React, { useState } from 'react';
import Sidebar from './Sidebar';
import { useAuth } from '../../context/AuthContext';
import { Navigate, Outlet } from 'react-router-dom';

const Layout = () => {
    const { isAuthenticated, loading } = useAuth();
    const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

    if (loading) return <div className="flex items-center justify-center h-screen bg-gray-50 text-gray-900">Loading...</div>;
    if (!isAuthenticated) return <Navigate to="/login" replace />;

    return (
        <div className="flex bg-gray-50 text-gray-900 min-h-screen">
            <Sidebar
                collapsed={sidebarCollapsed}
                onToggle={() => setSidebarCollapsed(prev => !prev)}
            />
            {/* Left margin matches sidebar width — transitions in sync */}
            <div
                className="flex-1 p-8 transition-all duration-300 ease-in-out bg-gray-50 text-gray-900"
                style={{ marginLeft: sidebarCollapsed ? '4rem' : '16rem' }}
            >
                <Outlet />
            </div>
        </div>
    );
};

export default Layout;
