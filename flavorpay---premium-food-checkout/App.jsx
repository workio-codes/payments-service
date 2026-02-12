import React, { useState } from 'react';
import CheckoutPage from './components/CheckoutPage';
import HistoryPage from './components/HistoryPage';
import { backendService } from './services/backendService';

const App = () => {
  const [currentView, setCurrentView] = useState('CHECKOUT');
  const [isProcessing, setIsProcessing] = useState(false);
  const [showSuccessToast, setShowSuccessToast] = useState(false);
  const [error, setError] = useState(null);

  const currentOrder = {
    customerName: 'Guest User',
    totalAmount: 31.49
  };

  const ensureRazorpayScript = () =>
    new Promise((resolve, reject) => {
      if (window.Razorpay) {
        resolve();
        return;
      }

      const script = document.createElement('script');
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      script.async = true;
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('Unable to load Razorpay checkout SDK'));
      document.body.appendChild(script);
    });

  const openRazorpayCheckout = (checkoutOptions) =>
    new Promise((resolve) => {
      const instance = new window.Razorpay({
        ...checkoutOptions,
        handler: (response) => resolve({ type: 'success', response }),
        modal: {
          ondismiss: () => resolve({ type: 'dismissed' })
        }
      });

      instance.on('payment.failed', (failure) => {
        resolve({ type: 'failed', failure });
      });

      instance.open();
    });

  const handlePay = async () => {
    setIsProcessing(true);
    setError(null);
    let appOrderId = null;
    const paymentMethod = 'Razorpay';

    try {
      await ensureRazorpayScript();

      const orderPayload = {
        customerName: currentOrder.customerName,
        totalAmount: currentOrder.totalAmount,
        paymentMethod
      };

      const appOrder = await backendService.createOrder(orderPayload);
      appOrderId = appOrder.orderId;
      const razorpayOrder = await backendService.createRazorpayOrder({
        orderId: appOrder.orderId,
        amount: currentOrder.totalAmount,
        paymentMethod,
        customerName: currentOrder.customerName
      });

      const checkoutResult = await openRazorpayCheckout({
        key: razorpayOrder.keyId,
        amount: razorpayOrder.amount,
        currency: razorpayOrder.currency,
        name: 'FlavorPay',
        description: `Order ${appOrder.orderId}`,
        order_id: razorpayOrder.razorpayOrderId,
        prefill: {
          name: currentOrder.customerName
        },
        theme: {
          color: '#2563eb'
        }
      });

      if (checkoutResult.type === 'success') {
        await backendService.verifyRazorpayPayment({
          orderId: appOrder.orderId,
          razorpayOrderId: checkoutResult.response.razorpay_order_id,
          razorpayPaymentId: checkoutResult.response.razorpay_payment_id,
          razorpaySignature: checkoutResult.response.razorpay_signature
        });

        setShowSuccessToast(true);
        setCurrentView('HISTORY');
        return;
      }

      await backendService.markOrderPaymentFailed(appOrder.orderId);
      setError('Payment was cancelled or failed. Please try again.');
    } catch (err) {
      if (appOrderId) {
        try {
          await backendService.markOrderPaymentFailed(appOrderId);
        } catch (markFailedError) {
          console.error('Unable to mark order payment failed:', markFailedError);
        }
      }
      console.error('Payment error:', err);
      setError(err.message || 'Unable to process payment. Please check your connection and try again.');
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-100">
      <div className="mx-auto max-w-6xl px-4 py-8 lg:px-8 lg:py-10">
        <header className="mb-6 flex flex-col gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">FlavorPay</p>
            <h1 className="text-2xl font-bold text-slate-900">Checkout Console</h1>
            <p className="text-sm text-slate-500">Desktop payment experience inspired by modern processor UIs.</p>
          </div>

          <nav className="inline-flex w-full rounded-xl border border-slate-200 bg-slate-50 p-1 lg:w-auto">
            <button
              onClick={() => setCurrentView('CHECKOUT')}
              className={`flex-1 rounded-lg px-4 py-2 text-sm font-semibold transition-colors lg:flex-none ${
                currentView === 'CHECKOUT' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-800'
              }`}
            >
              Checkout
            </button>
            <button
              onClick={() => setCurrentView('HISTORY')}
              className={`flex-1 rounded-lg px-4 py-2 text-sm font-semibold transition-colors lg:flex-none ${
                currentView === 'HISTORY' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-800'
              }`}
            >
              Payments
            </button>
          </nav>
        </header>

        <main className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-xl shadow-slate-200/70">
          {currentView === 'CHECKOUT' ? (
            <CheckoutPage
              currentOrder={currentOrder}
              onPay={handlePay}
              isProcessing={isProcessing}
              error={error}
            />
          ) : (
            <HistoryPage
              showToast={showSuccessToast}
              onCloseToast={() => setShowSuccessToast(false)}
              onBack={() => setCurrentView('CHECKOUT')}
            />
          )}
        </main>
      </div>
    </div>
  );
};

export default App;
