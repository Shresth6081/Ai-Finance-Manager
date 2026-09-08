import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Receipt, Tag, MessageSquare, LogOut, ChevronLeft, ChevronRight } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import clsx from 'clsx';

const Sidebar = ({ collapsed, onToggle }) => {
    const { logout } = useAuth();

    const navItems = [
        { icon: LayoutDashboard, label: 'Dashboard', path: '/dashboard' },
        { icon: Receipt, label: 'Transactions', path: '/transactions' },
        { icon: Tag, label: 'Categories', path: '/categories' },
        { icon: MessageSquare, label: 'AI Assistant', path: '/ai-chat' },
    ];

    return (
        <div
            className={clsx(
                'bg-white h-screen shadow-lg flex flex-col fixed left-0 top-0 transition-all duration-300 ease-in-out overflow-hidden z-20 border-r border-gray-100',
                collapsed ? 'w-16' : 'w-64'
            )}
        >
            {/* Header: logo + toggle button */}
            <div className="p-4 border-b border-gray-100 flex items-center justify-between min-h-[72px]">
                {!collapsed && (
                    <h1 className="text-xl font-bold text-primary flex items-center gap-2 whitespace-nowrap">
                        FinManager <span className="text-xs bg-primary text-white px-2 py-0.5 rounded-full font-semibold">AI</span>
                    </h1>
                )}
                <button
                    onClick={onToggle}
                    className={clsx(
                        'p-1.5 rounded-lg text-gray-500 hover:bg-gray-100 transition-colors',
                        collapsed && 'mx-auto'
                    )}
                    title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
                >
                    {collapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
                </button>
            </div>

            {/* Nav links */}
            <nav className="flex-1 p-2 space-y-1">
                {navItems.map((item) => (
                    <NavLink
                        key={item.path}
                        to={item.path}
                        title={collapsed ? item.label : undefined}
                        className={({ isActive }) =>
                            clsx(
                                'flex items-center gap-3 px-3 py-3 rounded-lg transition-colors font-medium',
                                collapsed ? 'justify-center' : '',
                                isActive
                                    ? 'bg-primary text-white shadow-md'
                                    : 'text-gray-600 hover:bg-gray-100'
                            )
                        }
                    >
                        <item.icon size={20} className="shrink-0" />
                        {!collapsed && <span className="whitespace-nowrap">{item.label}</span>}
                    </NavLink>
                ))}
            </nav>

            {/* Logout */}
            <div className="p-2 border-t border-gray-100">
                <button
                    onClick={logout}
                    title={collapsed ? 'Logout' : undefined}
                    className={clsx(
                        'flex items-center gap-3 px-3 py-3 w-full text-red-500 hover:bg-red-50 rounded-lg transition-colors font-medium',
                        collapsed && 'justify-center'
                    )}
                >
                    <LogOut size={20} className="shrink-0" />
                    {!collapsed && <span className="whitespace-nowrap font-medium">Logout</span>}
                </button>
            </div>
        </div>
    );
};

export default Sidebar;
