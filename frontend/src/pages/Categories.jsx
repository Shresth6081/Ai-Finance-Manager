import React, { useState, useEffect } from 'react';
import { Tag, Plus, Trash2 } from 'lucide-react';
import api from '../services/api';

const Categories = () => {
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const [newCategory, setNewCategory] = useState({ name: '', type: 'EXPENSE', color: '#ef4444' });
    const [error, setError] = useState('');
    const [initialized, setInitialized] = useState(false);

    // New state for viewing transactions by category
    const [selectedCategory, setSelectedCategory] = useState(null);
    const [categoryTransactions, setCategoryTransactions] = useState([]);
    const [showTransactionsModal, setShowTransactionsModal] = useState(false);
    const [loadingTransactions, setLoadingTransactions] = useState(false);

    useEffect(() => {
        fetchCategories();
    }, []);

    const fetchCategories = async () => {
        try {
            const response = await api.get('/categories');
            setCategories(response.data);

            // Initialize default categories if none exist and not already initialized
            if (response.data.length === 0 && !initialized) {
                setInitialized(true);
                await api.post('/categories/initialize');
                const updatedResponse = await api.get('/categories');
                setCategories(updatedResponse.data);
            }
        } catch (error) {
            console.error('Failed to fetch categories', error);
            setError('Failed to load categories');
        } finally {
            setLoading(false);
        }
    };

    const handleAddCategory = async () => {
        if (!newCategory.name.trim()) {
            setError('Category name is required');
            return;
        }

        try {
            const response = await api.post('/categories', newCategory);
            setCategories([...categories, response.data]);
            setNewCategory({ name: '', type: 'EXPENSE', color: '#ef4444' });
            setShowModal(false);
            setError('');
        } catch (error) {
            setError(error.response?.data?.message || 'Failed to create category');
        }
    };

    const handleDeleteCategory = async (id) => {
        if (!window.confirm('Are you sure you want to delete this category?')) {
            return;
        }

        try {
            await api.delete(`/categories/${id}`);
            setCategories(categories.filter(cat => cat.id !== id));
        } catch (error) {
            console.error('Failed to delete category', error);
            setError('Failed to delete category');
        }
    };

    const handleCategoryClick = async (category) => {
        setSelectedCategory(category);
        setLoadingTransactions(true);
        setShowTransactionsModal(true);

        try {
            const response = await api.get('/transactions');
            const filtered = response.data.filter((t) => t.category === category.name);
            setCategoryTransactions(filtered);
        } catch (error) {
            console.error('Failed to fetch transactions', error);
            setError('Failed to load transactions');
        } finally {
            setLoadingTransactions(false);
        }
    };

    const incomeCategories = categories.filter(c => c.type === 'INCOME');
    const expenseCategories = categories.filter(c => c.type === 'EXPENSE');

    if (loading) {
        return <div className="flex justify-center items-center h-64">Loading categories...</div>;
    }

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h2 className="text-3xl font-bold text-gray-800">Categories</h2>
                <button
                    onClick={() => setShowModal(true)}
                    className="flex items-center gap-2 bg-primary text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
                >
                    <Plus size={20} />
                    Add Category
                </button>
            </div>

            {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
                    {error}
                </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Income Categories */}
                <div className="card">
                    <h3 className="text-xl font-semibold mb-4 text-green-600 flex items-center gap-2">
                        <Tag size={20} />
                        Income Categories
                    </h3>
                    <div className="space-y-3">
                        {incomeCategories.map(category => (
                            <div
                                key={category.id}
                                className="flex items-center justify-between p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors cursor-pointer"
                                onClick={() => handleCategoryClick(category)}
                            >
                                <div className="flex items-center gap-3">
                                    <div className="w-4 h-4 rounded-full" style={{ backgroundColor: category.color }}></div>
                                    <span className="font-medium">{category.name}</span>
                                </div>
                                <div className="flex gap-2">
                                    <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            handleDeleteCategory(category.id);
                                        }}
                                        className="text-red-600 hover:text-red-800 p-1"
                                    >
                                        <Trash2 size={16} />
                                    </button>
                                </div>
                            </div>
                        ))}
                        {incomeCategories.length === 0 && (
                            <p className="text-gray-500 text-center py-4">No income categories yet</p>
                        )}
                    </div>
                </div>

                {/* Expense Categories */}
                <div className="card">
                    <h3 className="text-xl font-semibold mb-4 text-red-600 flex items-center gap-2">
                        <Tag size={20} />
                        Expense Categories
                    </h3>
                    <div className="space-y-3">
                        {expenseCategories.map(category => (
                            <div
                                key={category.id}
                                className="flex items-center justify-between p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors cursor-pointer"
                                onClick={() => handleCategoryClick(category)}
                            >
                                <div className="flex items-center gap-3">
                                    <div className="w-4 h-4 rounded-full" style={{ backgroundColor: category.color }}></div>
                                    <span className="font-medium">{category.name}</span>
                                </div>
                                <div className="flex gap-2">
                                    <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            handleDeleteCategory(category.id);
                                        }}
                                        className="text-red-600 hover:text-red-800 p-1"
                                    >
                                        <Trash2 size={16} />
                                    </button>
                                </div>
                            </div>
                        ))}
                        {expenseCategories.length === 0 && (
                            <p className="text-gray-500 text-center py-4">No expense categories yet</p>
                        )}
                    </div>
                </div>
            </div>

            {/* Add Category Modal */}
            {showModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg p-6 w-full max-w-md">
                        <h3 className="text-xl font-bold mb-4">Add New Category</h3>
                        {error && (
                            <div className="bg-red-50 border border-red-200 text-red-700 px-3 py-2 rounded mb-4 text-sm">
                                {error}
                            </div>
                        )}
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Category Name</label>
                                <input
                                    type="text"
                                    value={newCategory.name}
                                    onChange={(e) => setNewCategory({ ...newCategory, name: e.target.value })}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                                    placeholder="e.g., Groceries"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Type</label>
                                <select
                                    value={newCategory.type}
                                    onChange={(e) => setNewCategory({ ...newCategory, type: e.target.value })}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                                >
                                    <option value="EXPENSE">Expense</option>
                                    <option value="INCOME">Income</option>
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Color</label>
                                <input
                                    type="color"
                                    value={newCategory.color}
                                    onChange={(e) => setNewCategory({ ...newCategory, color: e.target.value })}
                                    className="w-full h-10 border border-gray-300 rounded-lg cursor-pointer"
                                />
                            </div>
                        </div>
                        <div className="flex gap-3 mt-6">
                            <button
                                onClick={() => {
                                    setShowModal(false);
                                    setError('');
                                }}
                                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={handleAddCategory}
                                className="flex-1 px-4 py-2 bg-primary text-white rounded-lg hover:bg-blue-700 transition-colors"
                            >
                                Add Category
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* View Transactions Modal */}
            {showTransactionsModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-lg p-6 w-full max-w-4xl max-h-[80vh] overflow-y-auto">
                        <div className="flex justify-between items-center mb-4">
                            <h3 className="text-xl font-bold">
                                Transactions in "{selectedCategory?.name}"
                            </h3>
                            <button
                                onClick={() => setShowTransactionsModal(false)}
                                className="text-gray-500 hover:text-gray-700"
                            >
                                ✕
                            </button>
                        </div>

                        {loadingTransactions ? (
                            <div className="text-center py-8">Loading transactions...</div>
                        ) : categoryTransactions.length === 0 ? (
                            <div className="text-center py-8 text-gray-500">
                                No transactions found in this category
                            </div>
                        ) : (
                            <div className="overflow-x-auto">
                                <table className="min-w-full divide-y divide-gray-200">
                                    <thead className="bg-gray-50">
                                        <tr>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Date</th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Description</th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Type</th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Amount</th>
                                        </tr>
                                    </thead>
                                    <tbody className="bg-white divide-y divide-gray-200">
                                        {categoryTransactions.map((transaction) => (
                                            <tr key={transaction.id}>
                                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                    {transaction.date}
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                                                    {transaction.description}
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap text-sm">
                                                    <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${transaction.type === 'INCOME' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                                                        }`}>
                                                        {transaction.type}
                                                    </span>
                                                </td>
                                                <td className={`px-6 py-4 whitespace-nowrap text-sm font-bold ${transaction.type === 'INCOME' ? 'text-green-600' : 'text-red-600'
                                                    }`}>
                                                    {transaction.type === 'INCOME' ? '+' : '-'}${transaction.amount}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}

                        <div className="mt-6 flex justify-end">
                            <button
                                onClick={() => setShowTransactionsModal(false)}
                                className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors"
                            >
                                Close
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Categories;
