import React, { useEffect, useState } from 'react';
import api from '../services/api';
import { TrendingUp, TrendingDown, DollarSign } from 'lucide-react';

// 1. Import Chart.js core and react-chartjs-2 components
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';
import { Bar, Pie } from 'react-chartjs-2';

// 2. Register required Chart.js controllers and plugins
ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend
);

const Dashboard = () => {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchTransactions = async () => {
      try {
        const response = await api.get('/transactions');
        setTransactions(response.data);
      } catch (error) {
        console.error('Failed to fetch transactions', error);
      } finally {
        setLoading(false);
      }
    };
    fetchTransactions();
  }, []);

  const income = transactions
    .filter((t) => t.type === 'INCOME')
    .reduce((acc, t) => acc + t.amount, 0);

  const expense = transactions
    .filter((t) => t.type === 'EXPENSE')
    .reduce((acc, t) => acc + t.amount, 0);

  const balance = income - expense;

  // 3. Define Chart.js Data Structures
  const barData = {
    labels: ['Income', 'Expense'],
    datasets: [
      {
        label: 'Amount ($)',
        data: [income, expense],
        backgroundColor: ['#22c55e', '#ef4444'], // green-500 and red-500
        borderRadius: 6,
      },
    ],
  };

  const pieData = {
    labels: ['Income', 'Expense'],
    datasets: [
      {
        data: [income, expense],
        backgroundColor: ['#22c55e', '#ef4444'],
        borderWidth: 1,
        borderColor: '#ffffff',
        hoverOffset: 6,
      },
    ],
  };

  // 4. Chart Configuration Options
  const barOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
    },
    scales: {
      x: {
        grid: { color: '#f1f5f9' },
        ticks: { color: '#475569', font: { family: 'Inter', size: 12 } }
      },
      y: {
        beginAtZero: true,
        grid: { color: '#f1f5f9' },
        ticks: { color: '#475569', font: { family: 'Inter', size: 12 } }
      },
    },
  };

  const pieOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { 
        position: 'bottom',
        labels: { color: '#475569', font: { family: 'Inter', size: 12 } }
      },
    },
  };

  if (loading) return <div className="text-gray-600">Loading dashboard...</div>;

  return (
    <div className="space-y-6">
      <h2 className="text-3xl font-bold text-gray-800">Financial Overview</h2>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="card flex items-center gap-4">
          <div className="p-3 bg-blue-100 text-blue-600 rounded-full">
            <DollarSign size={24} />
          </div>
          <div>
            <p className="text-sm text-gray-500 font-medium">Total Balance</p>
            <p className="text-2xl font-bold text-gray-900 font-mono tracking-tight">${balance.toFixed(2)}</p>
          </div>
        </div>

        <div className="card flex items-center gap-4">
          <div className="p-3 bg-green-100 text-green-600 rounded-full">
            <TrendingUp size={24} />
          </div>
          <div>
            <p className="text-sm text-gray-500 font-medium">Total Income</p>
            <p className="text-2xl font-bold text-green-600 font-mono tracking-tight">+${income.toFixed(2)}</p>
          </div>
        </div>

        <div className="card flex items-center gap-4">
          <div className="p-3 bg-red-100 text-red-600 rounded-full">
            <TrendingDown size={24} />
          </div>
          <div>
            <p className="text-sm text-gray-500 font-medium">Total Expenses</p>
            <p className="text-2xl font-bold text-red-600 font-mono tracking-tight">-${expense.toFixed(2)}</p>
          </div>
        </div>
      </div>

      {/* Chart Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="card h-96">
          <h3 className="text-lg font-semibold text-gray-700 mb-4">Income vs Expense</h3>
          <div className="h-72">
            <Bar data={barData} options={barOptions} />
          </div>
        </div>

        <div className="card h-96">
          <h3 className="text-lg font-semibold text-gray-700 mb-4">Distribution</h3>
          <div className="h-72 flex items-center justify-center">
            <Pie data={pieData} options={pieOptions} />
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;