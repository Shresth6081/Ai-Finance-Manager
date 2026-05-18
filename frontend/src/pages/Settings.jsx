import React, { useState, useEffect } from 'react';
import { User, Mail, Lock, Bell, Globe, Moon, Sun } from 'lucide-react';

const Settings = () => {
    const username = localStorage.getItem('username') || '';

    // Initialize dark mode from localStorage
    const [darkMode, setDarkMode] = useState(() => {
        const saved = localStorage.getItem('darkMode');
        return saved === 'true';
    });

    const [notifications, setNotifications] = useState(true);
    const [language, setLanguage] = useState('en');
    const [profile, setProfile] = useState({
        firstName: '',
        lastName: '',
        email: '',
    });

    // Apply dark mode when it changes
    useEffect(() => {
        if (darkMode) {
            document.body.style.backgroundColor = '#1a202c';
            document.body.style.color = '#e2e8f0';
        } else {
            document.body.style.backgroundColor = '';
            document.body.style.color = '';
        }
        localStorage.setItem('darkMode', String(darkMode));
    }, [darkMode]);

    const handleSaveProfile = () => {
        // TODO: Implement profile update API call
        alert('Profile updated successfully!');
    };

    const handleChangePassword = () => {
        // TODO: Implement password change functionality
        alert('Password change functionality coming soon!');
    };

    return (
        <div className="space-y-6">
            <h2 className="text-3xl font-bold text-gray-800">Settings</h2>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Profile Settings */}
                <div className="card">
                    <h3 className="text-xl font-semibold mb-4 flex items-center gap-2">
                        <User size={20} />
                        Profile Information
                    </h3>
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">First Name</label>
                            <input
                                type="text"
                                value={profile.firstName}
                                onChange={(e) => setProfile({ ...profile, firstName: e.target.value })}
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Last Name</label>
                            <input
                                type="text"
                                value={profile.lastName}
                                onChange={(e) => setProfile({ ...profile, lastName: e.target.value })}
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                            <div className="flex items-center gap-2">
                                <Mail size={18} className="text-gray-400" />
                                <input
                                    type="email"
                                    value={profile.email}
                                    onChange={(e) => setProfile({ ...profile, email: e.target.value })}
                                    className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                                />
                            </div>
                        </div>
                        <button
                            onClick={handleSaveProfile}
                            className="w-full bg-primary text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
                        >
                            Save Changes
                        </button>
                    </div>
                </div>

                {/* Security Settings */}
                <div className="card">
                    <h3 className="text-xl font-semibold mb-4 flex items-center gap-2">
                        <Lock size={20} />
                        Security
                    </h3>
                    <div className="space-y-4">
                        <div className="p-4 bg-gray-50 rounded-lg">
                            <p className="text-sm text-gray-600 mb-2">Password</p>
                            <p className="text-sm font-medium mb-3">••••••••</p>
                            <button
                                onClick={handleChangePassword}
                                className="text-primary hover:text-blue-700 text-sm font-medium"
                            >
                                Change Password
                            </button>
                        </div>
                        <div className="p-4 bg-gray-50 rounded-lg">
                            <p className="text-sm text-gray-600 mb-2">Two-Factor Authentication</p>
                            <p className="text-sm text-gray-500 mb-3">Add an extra layer of security to your account</p>
                            <button className="text-primary hover:text-blue-700 text-sm font-medium">
                                Enable 2FA
                            </button>
                        </div>
                    </div>
                </div>

                {/* Preferences */}
                <div className="card">
                    <h3 className="text-xl font-semibold mb-4 flex items-center gap-2">
                        <Globe size={20} />
                        Preferences
                    </h3>
                    <div className="space-y-4">
                        <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                            <div className="flex items-center gap-3">
                                {darkMode ? <Moon size={18} /> : <Sun size={18} />}
                                <div>
                                    <p className="font-medium">Dark Mode</p>
                                    <p className="text-sm text-gray-500">Toggle dark theme</p>
                                </div>
                            </div>
                            <label className="relative inline-flex items-center cursor-pointer">
                                <input
                                    type="checkbox"
                                    checked={darkMode}
                                    onChange={(e) => setDarkMode(e.target.checked)}
                                    className="sr-only peer"
                                />
                                <div className="w-11 h-6 bg-gray-300 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary"></div>
                            </label>
                        </div>

                        <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                            <div className="flex items-center gap-3">
                                <Bell size={18} />
                                <div>
                                    <p className="font-medium">Notifications</p>
                                    <p className="text-sm text-gray-500">Receive email notifications</p>
                                </div>
                            </div>
                            <label className="relative inline-flex items-center cursor-pointer">
                                <input
                                    type="checkbox"
                                    checked={notifications}
                                    onChange={(e) => setNotifications(e.target.checked)}
                                    className="sr-only peer"
                                />
                                <div className="w-11 h-6 bg-gray-300 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary"></div>
                            </label>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Language</label>
                            <select
                                value={language}
                                onChange={(e) => setLanguage(e.target.value)}
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                            >
                                <option value="en">English</option>
                                <option value="es">Spanish</option>
                                <option value="fr">French</option>
                                <option value="de">German</option>
                            </select>
                        </div>
                    </div>
                </div>

                {/* About */}
                <div className="card">
                    <h3 className="text-xl font-semibold mb-4">About</h3>
                    <div className="space-y-3">
                        <div className="flex justify-between py-2 border-b">
                            <span className="text-gray-600">Version</span>
                            <span className="font-medium">1.0.0</span>
                        </div>
                        <div className="flex justify-between py-2 border-b">
                            <span className="text-gray-600">Account Created</span>
                            <span className="font-medium">Feb 14, 2026</span>
                        </div>
                        <div className="flex justify-between py-2">
                            <span className="text-gray-600">Username</span>
                            <span className="font-medium">{username || 'N/A'}</span>
                        </div>
                    </div>
                    <div className="mt-6 pt-6 border-t">
                        <button className="w-full text-red-600 hover:text-red-800 font-medium py-2">
                            Delete Account
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Settings;
