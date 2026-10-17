import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { 
  TrendingUp, TrendingDown, DollarSign, Sparkles, 
  AlertTriangle, ArrowUpRight, X, ShieldAlert, CheckCircle2 
} from 'lucide-react';

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
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [txnRes, alertRes] = await Promise.allSettled([
          api.get('/transactions'),
          api.get('/ai/agent/alerts')
        ]);

        if (txnRes.status === 'fulfilled') {
          setTransactions(txnRes.value.data || []);
        }
        if (alertRes.status === 'fulfilled') {
          setAlerts(alertRes.value.data || []);
        }
      } catch (error) {
        console.error('Failed to fetch dashboard data', error);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const handleDismissAlert = async (id, e) => {
    e.stopPropagation();
    try {
      await api.put(`/ai/agent/alerts/${id}/dismiss`);
      setAlerts(prev => prev.filter(a => a.id !== id));
    } catch (err) {
      console.error('Failed to dismiss alert', err);
    }
  };

  const income = transactions
    .filter((t) => t.type === 'INCOME')
    .reduce((acc, t) => acc + t.amount, 0);

  const expense = transactions
    .filter((t) => t.type === 'EXPENSE')
    .reduce((acc, t) => acc + t.amount, 0);

  const balance = income - expense;

  // Chart.js Data Structures
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
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-3xl font-bold text-gray-800">Financial Overview</h2>
          <p className="text-sm text-gray-500 mt-0.5">Real-time ledger analytics & autonomous agent intelligence.</p>
        </div>
        <button
          onClick={() => navigate('/ai-chat')}
          className="flex items-center gap-2 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white px-4 py-2.5 rounded-xl font-medium text-sm shadow-sm transition-all transform hover:-translate-y-0.5 self-start md:self-auto"
        >
          <Sparkles size={16} />
          <span>Ask FinAgent</span>
        </button>
      </div>

      {/* Proactive Agent Hunter Alerts Banner */}
      {alerts.length > 0 && (
        <div className="space-y-3">
          <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-indigo-700">
            <Sparkles size={14} className="text-indigo-600 animate-pulse" />
            <span>Autonomous Hunter Insights ({alerts.length})</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {alerts.map((alert) => {
              const isHike = alert.alertType === 'PRICE_HIKE';
              const isSavings = alert.alertType === 'SAVINGS_OPPORTUNITY';

              return (
                <div
                  key={alert.id}
                  onClick={() => navigate('/ai-chat')}
                  className={`relative p-4 rounded-2xl border transition-all cursor-pointer group shadow-2xs ${
                    isHike 
                      ? 'bg-amber-50/70 border-amber-200 hover:bg-amber-100/60' 
                      : isSavings 
                        ? 'bg-emerald-50/70 border-emerald-200 hover:bg-emerald-100/60' 
                        : 'bg-indigo-50/70 border-indigo-200 hover:bg-indigo-100/60'
                  }`}
                >
                  <button
                    onClick={(e) => handleDismissAlert(alert.id, e)}
                    className="absolute top-3 right-3 p-1 text-gray-400 hover:text-gray-700 hover:bg-black/5 rounded-lg transition-colors"
                    title="Dismiss alert"
                  >
                    <X size={14} />
                  </button>

                  <div className="flex items-start gap-3 pr-6">
                    <div className={`p-2 rounded-xl shrink-0 ${
                      isHike 
                        ? 'bg-amber-100 text-amber-700' 
                        : isSavings 
                          ? 'bg-emerald-100 text-emerald-700' 
                          : 'bg-indigo-100 text-indigo-700'
                    }`}>
                      {isHike ? <ShieldAlert size={18} /> : isSavings ? <CheckCircle2 size={18} /> : <AlertTriangle size={18} />}
                    </div>
                    <div>
                      <h4 className="font-bold text-gray-900 text-sm flex items-center gap-1.5">
                        {alert.title}
                        <ArrowUpRight size={14} className="text-gray-400 group-hover:text-primary transition-transform group-hover:translate-x-0.5 duration-200" />
                      </h4>
                      <p className="text-xs text-gray-600 mt-1 leading-relaxed">
                        {alert.message}
                      </p>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

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