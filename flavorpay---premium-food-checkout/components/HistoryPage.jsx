import React, { useEffect, useMemo, useState } from 'react';
import { geminiService } from '../services/geminiService';
import { backendService } from '../services/backendService';

const HistoryPage = ({ showToast, onCloseToast, onBack }) => {
  const [transactions, setTransactions] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [insight, setInsight] = useState('Analyzing your spending...');
  const [activeFilter, setActiveFilter] = useState('All');

  const mapOrderStatus = (orderStatus) => {
    const mapping = {
      Confirmed: 'COMPLETED',
      Pending: 'PENDING',
      'Payment Failed': 'FAILED',
      CANCELLED: 'REFUNDED',
      CANCELLATION_PENDING: 'PENDING'
    };
    return mapping[orderStatus] || 'COMPLETED';
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'Just now';

    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit'
    });
  };

  const statusStyles = {
    COMPLETED: 'bg-emerald-50 text-emerald-700 border-emerald-100',
    FAILED: 'bg-red-50 text-red-700 border-red-100',
    REFUNDED: 'bg-amber-50 text-amber-700 border-amber-100',
    PENDING: 'bg-slate-100 text-slate-700 border-slate-200'
  };

  const filteredTransactions = useMemo(() => {
    let result = [...transactions];

    if (activeFilter === 'Refunds') {
      result = result.filter((tx) => tx.status === 'REFUNDED');
    }

    if (activeFilter === 'Amount') {
      result.sort((a, b) => b.amount - a.amount);
    } else {
      result.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    }

    return result;
  }, [activeFilter, transactions]);

  useEffect(() => {
    const fetchTransactions = async () => {
      setIsLoading(true);
      try {
        const orders = await backendService.getAllOrders();

        const transformedTransactions = orders.map((order) => ({
          id: order.orderId,
          name: `Order ${order.orderId}`,
          date: formatDate(order.createdAt),
          createdAt: order.createdAt || new Date().toISOString(),
          amount: order.totalAmount,
          status: mapOrderStatus(order.status)
        }));

        setTransactions(transformedTransactions);

        const total = transformedTransactions.reduce((sum, tx) => sum + tx.amount, 0);
        const latestAmount = transformedTransactions[0]?.amount || 0;

        const text = await geminiService.getTransactionAdvice(latestAmount, total);
        setInsight(text || `You have spent $${total.toFixed(2)} this month.`);
      } catch (err) {
        console.error('Failed to fetch transactions:', err);
        setError('Unable to load transaction history.');
        setTransactions([]);
      } finally {
        setIsLoading(false);
      }
    };

    fetchTransactions();
  }, []);

  const totalSpent = useMemo(
    () => transactions.reduce((sum, transaction) => sum + transaction.amount, 0),
    [transactions]
  );

  return (
    <div className="animate-slide-up space-y-6 p-6 lg:p-10">
      <header className="flex flex-col gap-4 border-b border-slate-200 pb-5 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex items-center gap-3">
          <button
            onClick={onBack}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
          >
            <span className="material-icons text-base">arrow_back</span>
            Back
          </button>
          <div>
            <h2 className="text-2xl font-bold text-slate-900">Payments</h2>
            <p className="text-sm text-slate-500">Track every transaction from your desktop dashboard.</p>
          </div>
        </div>

        <div className="inline-flex rounded-xl border border-slate-200 bg-slate-50 p-1">
          {['All', 'Date', 'Amount', 'Refunds'].map((filter) => (
            <button
              key={filter}
              onClick={() => setActiveFilter(filter)}
              className={`rounded-lg px-3 py-2 text-xs font-semibold uppercase tracking-wide transition-colors ${
                activeFilter === filter ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-900'
              }`}
            >
              {filter}
            </button>
          ))}
        </div>
      </header>

      {showToast && (
        <div className="flex items-center justify-between rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
          <p className="font-semibold">Payment confirmed and synced across services.</p>
          <button onClick={onCloseToast} className="text-emerald-700/80 hover:text-emerald-900">
            <span className="material-icons text-base">close</span>
          </button>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-[1.35fr_0.65fr]">
        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
          {isLoading ? (
            <div className="p-10 text-center text-sm text-slate-500">Loading transactions...</div>
          ) : error ? (
            <div className="p-10 text-center text-sm text-red-600">{error}</div>
          ) : filteredTransactions.length === 0 ? (
            <div className="p-10 text-center text-sm text-slate-500">No transactions found.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-sm">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-4 py-3 text-left font-semibold text-slate-500">Order</th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-500">Date</th>
                    <th className="px-4 py-3 text-right font-semibold text-slate-500">Amount</th>
                    <th className="px-4 py-3 text-right font-semibold text-slate-500">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filteredTransactions.map((transaction) => (
                    <tr key={transaction.id} className="hover:bg-slate-50/80">
                      <td className="px-4 py-4 font-medium text-slate-800">{transaction.name}</td>
                      <td className="px-4 py-4 text-slate-600">{transaction.date}</td>
                      <td className="px-4 py-4 text-right font-semibold text-slate-900">
                        ${transaction.amount.toFixed(2)}
                      </td>
                      <td className="px-4 py-4 text-right">
                        <span
                          className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold ${
                            statusStyles[transaction.status] || statusStyles.PENDING
                          }`}
                        >
                          {transaction.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <aside className="space-y-4">
          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">Total Processed</p>
            <p className="mt-2 text-3xl font-bold text-slate-900">${totalSpent.toFixed(2)}</p>
            <p className="mt-1 text-sm text-slate-500">{transactions.length} transactions on record</p>
          </div>

          <div className="rounded-2xl border border-blue-200 bg-blue-50 p-5">
            <div className="mb-3 flex items-center justify-between">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-blue-700">AI Insight</p>
              <span className="material-icons text-blue-700">insights</span>
            </div>
            <p className="text-sm leading-relaxed text-slate-700">{insight}</p>
          </div>

          <button className="w-full rounded-xl border border-dashed border-slate-300 bg-white px-4 py-3 text-sm font-semibold text-slate-600 hover:border-slate-400 hover:text-slate-800">
            Need help with a transaction?
          </button>
        </aside>
      </div>
    </div>
  );
};

export default HistoryPage;
