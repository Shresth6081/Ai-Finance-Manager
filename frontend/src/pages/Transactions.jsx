import React, { useEffect, useState, useRef } from 'react';
import api from '../services/api';
import { Plus, Pencil, Trash2, Camera } from 'lucide-react';

const EMPTY_FORM = {
    amount: '',
    description: '',
    date: new Date().toISOString().split('T')[0],
    type: 'EXPENSE',
    category: ''
};

const Transactions = () => {
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const [editingTransaction, setEditingTransaction] = useState(null); // null = Add mode, object = Edit mode
    const [formData, setFormData] = useState(EMPTY_FORM);
    const [submitting, setSubmitting] = useState(false);
    const [scanning, setScanning] = useState(false);
    const [scanError, setScanError] = useState('');
    const fileInputRef = useRef(null);

    const fetchTransactions = async () => {
        try {
            const response = await api.get('/transactions');
            setTransactions(response.data);
        } catch (error) {
            console.error("Failed to fetch transactions", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchTransactions();
    }, []);

    useEffect(() => {
        const hasPending = transactions.some(t => t.category?.toLowerCase() === 'pending');
        if (hasPending) {
            const interval = setInterval(() => {
                api.get('/transactions')
                    .then(response => {
                        setTransactions(response.data);
                    })
                    .catch(error => console.error("Polling transactions failed", error));
            }, 2000);
            return () => clearInterval(interval);
        }
    }, [transactions]);

    const openAddModal = () => {
        setEditingTransaction(null);
        setFormData(EMPTY_FORM);
        setShowModal(true);
    };

    const openEditModal = (t) => {
        setEditingTransaction(t);
        setFormData({
            amount: t.amount,
            description: t.description,
            date: t.date,
            type: t.type,
            category: t.category || ''
        });
        setShowModal(true);
    };

    const closeModal = () => {
        setShowModal(false);
        setEditingTransaction(null);
        setFormData(EMPTY_FORM);
    };

    const handleScanReceipt = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        setScanning(true);
        setScanError('');
        
        const uploadData = new FormData();
        uploadData.append('file', file);

        try {
            const response = await api.post('/receipts/scan', uploadData, {
                headers: {
                    'Content-Type': 'multipart/form-data'
                }
            });

            const parsed = response.data;
            setFormData({
                description: parsed.description || '',
                amount: parsed.amount ? parsed.amount.toString() : '',
                date: parsed.date || new Date().toISOString().split('T')[0],
                type: 'EXPENSE',
                category: parsed.category || ''
            });
            setEditingTransaction(null);
            setShowModal(true);
        } catch (error) {
            console.error("Receipt scan failed", error);
            const errorMsg = error.response?.data?.message || "Failed to read the receipt. Please make sure the image is clear and the AI server is active.";
            setScanError(errorMsg);
            alert(errorMsg);
        } finally {
            setScanning(false);
            if (fileInputRef.current) fileInputRef.current.value = '';
        }
    };

    const triggerFileSelect = () => {
        fileInputRef.current?.click();
    };

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (submitting) return;
        setSubmitting(true);
        try {
            if (editingTransaction) {
                await api.put(`/transactions/${editingTransaction.id}`, formData);
            } else {
                await api.post('/transactions', formData);
            }
            closeModal();
            fetchTransactions();
        } catch (error) {
            console.error('Failed to save transaction', error);
        } finally {
            setSubmitting(false);
        }
    };

    const handleDelete = async (id) => {
        if (!window.confirm('Are you sure you want to delete this transaction?')) return;
        try {
            await api.delete(`/transactions/${id}`);
            fetchTransactions();
        } catch (error) {
            console.error('Failed to delete transaction', error);
        }
    };

    const getCategoryBadge = (cat) => {
        const clean = (cat || 'Uncategorized').trim().toLowerCase();
        if (clean === 'pending') {
            return (
                <span className="px-2.5 py-0.5 inline-flex text-xs leading-5 font-semibold rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/20 animate-pulse">
                    Pending
                </span>
            );
        }
        if (clean === 'uncategorized') {
            return (
                <span className="px-2.5 py-0.5 inline-flex text-xs leading-5 font-semibold rounded-full bg-gray-100 text-gray-500 border border-gray-200/50">
                    Uncategorized
                </span>
            );
        }
        return (
            <span className="px-2.5 py-0.5 inline-flex text-xs leading-5 font-semibold rounded-full bg-primary/10 text-primary border border-primary/20">
                {cat}
            </span>
        );
    };

    if (loading) return <div className="text-gray-600">Loading transactions...</div>;

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h2 className="text-3xl font-bold text-gray-800">Transactions</h2>
                <div className="flex items-center gap-3">
                    <input 
                        type="file" 
                        ref={fileInputRef} 
                        onChange={handleScanReceipt} 
                        accept="image/png, image/jpeg, image/jpg" 
                        className="hidden" 
                    />
                    <button
                        onClick={triggerFileSelect}
                        disabled={scanning}
                        className="flex items-center gap-2 bg-white border border-gray-300 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-50 transition-colors disabled:opacity-60"
                    >
                        <Camera size={20} /> 
                        {scanning ? 'Reading Receipt...' : 'Scan Receipt'}
                    </button>
                    <button
                        onClick={openAddModal}
                        disabled={scanning}
                        className="flex items-center gap-2 bg-primary text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-60"
                    >
                        <Plus size={20} /> Add Transaction
                    </button>
                </div>
            </div>

            <div className="card overflow-hidden !p-0">
                <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-gray-50">
                            <tr>
                                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Date</th>
                                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Description</th>
                                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Category</th>
                                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Type</th>
                                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Amount</th>
                                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200 bg-white">
                            {transactions.map((t) => (
                                <tr key={t.id} className="hover:bg-gray-50 transition-colors">
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 font-mono">{t.date}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-950">{t.description}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        {getCategoryBadge(t.category)}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        <span className={`px-2.5 py-0.5 inline-flex text-xs leading-5 font-semibold rounded-full ${
                                            t.type === 'INCOME' 
                                                ? 'bg-green-100 text-green-800' 
                                                : 'bg-red-100 text-red-800'
                                        }`}>
                                            {t.type}
                                        </span>
                                    </td>
                                    <td className={`px-6 py-4 whitespace-nowrap text-sm font-bold font-mono ${t.type === 'INCOME' ? 'text-green-600' : 'text-red-600'}`}>
                                        {t.type === 'INCOME' ? '+' : '-'}${t.amount.toFixed(2)}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        <div className="flex items-center gap-3">
                                            <button
                                                onClick={() => openEditModal(t)}
                                                title="Edit"
                                                className="text-primary hover:text-blue-700 transition-colors"
                                            >
                                                <Pencil size={16} />
                                            </button>
                                            <button
                                                onClick={() => handleDelete(t.id)}
                                                title="Delete"
                                                className="text-red-600 hover:text-red-800 transition-colors"
                                            >
                                                <Trash2 size={16} />
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Add / Edit Modal */}
            {showModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
                    <div className="bg-white rounded-lg p-6 w-full max-w-md shadow-xl border border-gray-100">
                        <h3 className="text-xl font-bold mb-4 text-gray-800">
                            {editingTransaction ? 'Edit Transaction' : 'Add Transaction'}
                        </h3>
                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700">Description</label>
                                <input 
                                    type="text" 
                                    name="description" 
                                    value={formData.description} 
                                    onChange={handleChange} 
                                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm border p-2 text-gray-900 bg-white" 
                                    required 
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700">Amount</label>
                                <input 
                                    type="number" 
                                    name="amount" 
                                    value={formData.amount} 
                                    onChange={handleChange} 
                                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm border p-2 text-gray-900 bg-white font-mono" 
                                    required 
                                    min="0" 
                                    step="0.01" 
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700">Date</label>
                                <input 
                                    type="date" 
                                    name="date" 
                                    value={formData.date} 
                                    onChange={handleChange} 
                                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm border p-2 text-gray-900 bg-white font-mono" 
                                    required 
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700">Type</label>
                                <select 
                                    name="type" 
                                    value={formData.type} 
                                    onChange={handleChange} 
                                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm border p-2 text-gray-900 bg-white"
                                >
                                    <option value="INCOME">Income</option>
                                    <option value="EXPENSE">Expense</option>
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700">Category (Optional - AI will guess)</label>
                                <input 
                                    type="text" 
                                    name="category" 
                                    value={formData.category} 
                                    onChange={handleChange} 
                                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm border p-2 text-gray-900 bg-white" 
                                />
                            </div>
                            <div className="flex gap-4 mt-6">
                                <button 
                                    type="button" 
                                    onClick={closeModal} 
                                    className="flex-1 py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50"
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    disabled={submitting}
                                    className={`flex-1 py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary transition-opacity duration-200 ${
                                        submitting
                                            ? 'opacity-50 cursor-not-allowed'
                                            : 'hover:bg-blue-700 cursor-pointer'
                                    }`}
                                >
                                    {submitting
                                        ? (editingTransaction ? 'Updating...' : 'Saving...')
                                        : (editingTransaction ? 'Update' : 'Save')
                                    }
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Transactions;
